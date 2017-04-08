package de.markusziller.alns.heuristic.strategies.phasetwo;

import de.markusziller.alns.common.abstraction.ALNSObserver;
import de.markusziller.alns.common.entities.Instance;
import de.markusziller.alns.common.entities.Solution;
import de.markusziller.alns.common.utils.OutputUtil;
import de.markusziller.alns.heuristic.visualization.ALNSProcessVisualizationManager;
import de.markusziller.alns.heuristic.strategies.alns.config.IALNSConfig;
import de.markusziller.alns.heuristic.strategies.alns.insertion.GreedyRepair;
import de.markusziller.alns.heuristic.strategies.alns.insertion.IALNSRepair;
import de.markusziller.alns.heuristic.strategies.alns.insertion.NRegretRepair;
import de.markusziller.alns.heuristic.strategies.alns.removal.*;
import de.markusziller.alns.ui.charts.OperationsLinechart;
import de.markusziller.alns.ui.charts.SolutionsLinechart;

import java.util.Random;
import java.util.concurrent.Callable;

public class ALNSProcess implements Callable<Solution> {
    private final ALNSObserver o = new ALNSObserver();
    private long id;
    private final ALNSProcessVisualizationManager apvm = new ALNSProcessVisualizationManager();
    private final IALNSConfig config;
    private final IALNSDestroy[] destroy_ops = new IALNSDestroy[]{new ProximityZoneDestroy(), new ZoneDestroy(), new NodesCountDestroy(false), new SubrouteDestroy(),
            // new RelatedDestroy(),
            new RandomDestroy(), new RandomRouteDestroy(), new WorstCostDestroy()};
    private final IALNSRepair[] repair_ops = new IALNSRepair[]{new NRegretRepair(2), new NRegretRepair(3), new GreedyRepair()};
    // Globale beste L�sung
    private Solution s_g = null;
    // Aktuelle L�sung
    private Solution s_c = null;
    private boolean cpng = false;
    private int i = 0;
    private double T;
    private double T_s;
    private long t_start;
    private final Instance is;
    // private double c_uns;
    private final int F_log = 100;
    private final double T_end_t = 0.01;
    private double T_end;

    public ALNSProcess(Solution s_, IALNSConfig c) {
        is = s_.getInstance();
        s_.nullifyWrapper();
        config = c;
        s_g = s_.clone();
        s_g.refreshWrapper();
        s_g.update();
        s_g.removeHistory();
        s_g.renewUID();
        s_c = s_g;
        initStrategies();
    }

    public ALNSProcess(Solution s_, IALNSConfig c, ControlParameter cp) throws InterruptedException {
        cpng = cp.isSolutionImages();
        is = s_.getInstance();
        s_.nullifyWrapper();
        config = c;
        s_g = s_.clone();
        s_g.refreshWrapper();
        s_g.update();
        s_g.removeHistory();
        s_g.renewUID();
        s_c = s_g;
        initStrategies();
        if (cp.isSolutionsLinechart()) {
            o.add(new SolutionsLinechart(this));
        }
        if (cp.isOperationsLinechart()) {
            o.add(new OperationsLinechart(this));
        }
    }

    private double fitness(Solution s) {
        return s.getCostFitness();
    }

    @Override
    public Solution call() throws Exception {
        setLogStrategy();
        id = Thread.currentThread().getId();
        o.onThreadStart(this);
        T_s = -(config.getDelta() / Math.log(config.getBig_omega())) * fitness(s_c);
        T = T_s;
        T_end = T_end_t * T_s;
        t_start = System.currentTimeMillis();
        o.onStartConfigurationObtained(this);
        while (i <= config.getOmega()) {
            Solution s_c_new = s_c.clone();
            int q = getQ(s_c_new);
            IALNSDestroy _destroy = getALNSDestroyOperator();
            IALNSRepair _repair = getALNSRepairOperator();
            o.onDestroyRepairOperationsObtained(this, _destroy, _repair, s_c_new, q);
            Solution s_destroy = _destroy.destroy(s_c_new, q);
            o.onSolutionDestroy(this, s_destroy);
            Solution s_t = _repair.repair(s_destroy);
            o.onSolutionRepaired(this, s_t);
            if (fitness(s_t) < fitness(s_c)) {
                s_c = s_t;
                if (fitness(s_t) < fitness(s_g)) {
                    System.out.println(String.format("[%d]: Found new global minimum: %.2f, Required Vehicles: %d, I_uns: %d", i, s_t.getCostFitness(), s_t.activeVehicles(), s_g.getUnscheduledJobs().size()));
                    if (this.cpng) {
                        OutputUtil.createPNG(s_t, i);
                    }
                    s_g = s_t;
                    _destroy.addToPi(config.getSigma_1());
                    _repair.addToPi(config.getSigma_1());
                } else {
                    _destroy.addToPi(config.getSigma_2());
                    _repair.addToPi(config.getSigma_2());
                }
            } else {
                double val = Math.exp(-((fitness(s_t) - fitness(s_c)) / T));
                if (Math.random() < val) {
                    s_c = s_t;
                }
                _destroy.addToPi(config.getSigma_3());
                _repair.addToPi(config.getSigma_3());
            }
            o.onAcceptancePhaseFinsihed(this, s_t);
            if (i % config.getTau() == 0 && i > 0) {
                segmentFinsihed();
                o.onSegmentFinsihed(this, s_t);
            }
            T = config.getC() * T;
            o.onIterationFinished(this, s_t);
            i++;
        }
        s_g.complete();
        long s = (System.currentTimeMillis() - t_start) / 1000;
        System.out.println(String.format("%02d:%02d", (s % 3600) / 60, (s % 60)));
        System.out.printf("FINAL RESULT [T%d][I%d]\t f(s_g): %-10.1f  \n", id, i, s_g.getCosts());
        return s_g;
    }

    private void setLogStrategy() {
        apvm.all(apvm.NONE);
        for (IALNSDestroy a : destroy_ops) {
            a.getVisualizationManager().disableAll();
        }
        for (IALNSRepair a : repair_ops) {
            a.getVisualizationManager().disableAll();
        }
    }

    /**
     * Berechnet die Gr��e der destroy operation
     *
     * @param s_c2
     * @return
     * @author Markus Z.
     * @date 11.12.2013
     */
    private int getQ(Solution s_c2) {
        int q_l = Math.min((int) Math.ceil(0.05 * s_c2.getNoOfScheduledJobs()), 10);
        int q_u = Math.min((int) Math.ceil(0.20 * s_c2.getNoOfScheduledJobs()), 30);

        Random r = new Random();
        int rr = r.nextInt(q_u - q_l + 1) + q_l;
        return rr;
    }

    /**
     * F�hrt die Operationen aus, die nach Anschluss eines Segments notwendig sind.<br>
     * - Berechnet die Wahrscheinlichkeiten neu<br>
     * - Setzt Pi_x zur�ck
     *
     * @author Markus Z.
     * @date 11.12.2013
     */
    private void segmentFinsihed() {
        double w_sum = 0;
        // Update neue Gewichtung der Destroy Operatoren
        for (int i = 0; i < destroy_ops.length; i++) {
            IALNSDestroy dstr = destroy_ops[i];
            double w_old1 = dstr.getW() * (1 - config.getR_p());
            double recentFactor = dstr.getDraws() < 1 ? 0 : (double) dstr.getPi() / (double) dstr.getDraws();
            double w_old2 = config.getR_p() * recentFactor;
            double w_new = w_old1 + w_old2;
            w_sum += w_new;
            dstr.setW(w_new);
        }
        // Update neue Wahrs. der Destroy Operatoren
        for (int i = 0; i < destroy_ops.length; i++) {
            IALNSDestroy dstr = destroy_ops[i];
            dstr.setP(dstr.getW() / w_sum);
            dstr.setDraws(0);
            dstr.setPi(0);
        }
        w_sum = 0;
        // Update neue Gewichtung der Repair Operatoren
        for (int i = 0; i < repair_ops.length; i++) {
            IALNSRepair rpr = repair_ops[i];
            double recentFactor = rpr.getDraws() < 1 ? 0 : (double) rpr.getPi() / (double) rpr.getDraws();
            double w_new = (rpr.getW() * (1 - config.getR_p())) + config.getR_p() * recentFactor;
            w_sum += w_new;
            rpr.setW(w_new);
        }
        // Update neue Wahrs. der Repair Operatoren
        for (int i = 0; i < repair_ops.length; i++) {
            IALNSRepair rpr = repair_ops[i];
            rpr.setP(rpr.getW() / w_sum);
            rpr.setDraws(0);
            rpr.setPi(0);
        }
    }

    /**
     * Liefert eine ALNS-Repair durch Roulette-Ziehung
     *
     * @return
     * @author Markus Z.
     * @date 11.12.2013
     */
    private IALNSRepair getALNSRepairOperator() {
        double random = Math.random();
        double threshold = 0.;
        for (int i = 0; i < repair_ops.length; i++) {
            IALNSRepair rpr = repair_ops[i];
            threshold += rpr.getP();
            if (random <= threshold) {
                rpr.drawn();
                return rpr;
            }
        }
        repair_ops[repair_ops.length - 1].drawn();
        return repair_ops[repair_ops.length - 1];
    }

    /**
     * Liefert einen ALNS-Destroy Operator durch Roulette-Ziehung
     *
     * @return
     * @author Markus Z.
     * @date 11.12.2013
     */
    private IALNSDestroy getALNSDestroyOperator() {
        double random = Math.random();
        double threshold = 0.;
        for (int i = 0; i < destroy_ops.length; i++) {
            IALNSDestroy dstr = destroy_ops[i];
            threshold += dstr.getP();
            if (random <= threshold) {
                dstr.drawn();
                return dstr;
            }
        }
        destroy_ops[destroy_ops.length - 1].drawn();
        return destroy_ops[destroy_ops.length - 1];
    }

    /**
     * Initialisiert die Strategien
     *
     * @author Markus Z.
     * @date 11.12.2013
     */
    private void initStrategies() {
        for (int i = 0; i < destroy_ops.length; i++) {
            IALNSDestroy dstr = destroy_ops[i];
            dstr.setPi(0);
            dstr.setW(1.);
            dstr.setP(1 / (double) destroy_ops.length);
        }
        for (int i = 0; i < repair_ops.length; i++) {
            IALNSRepair rpr = repair_ops[i];
            rpr.setPi(0);
            rpr.setW(1.);
            rpr.setP(1 / (double) repair_ops.length);
        }
    }

    public ALNSObserver getO() {
        return this.o;
    }

    public long getId() {
        return this.id;
    }

    public ALNSProcessVisualizationManager getApvm() {
        return this.apvm;
    }

    public IALNSConfig getConfig() {
        return this.config;
    }

    public IALNSDestroy[] getDestroy_ops() {
        return this.destroy_ops;
    }

    public IALNSRepair[] getRepair_ops() {
        return this.repair_ops;
    }

    public Solution getS_g() {
        return this.s_g;
    }

    public Solution getS_c() {
        return this.s_c;
    }

    public boolean isCpng() {
        return this.cpng;
    }

    public int getI() {
        return this.i;
    }

    public double getT() {
        return this.T;
    }

    public double getT_s() {
        return this.T_s;
    }

    public long getT_start() {
        return this.t_start;
    }

    public Instance getIs() {
        return this.is;
    }

    public int getF_log() {
        return this.F_log;
    }

    public double getT_end_t() {
        return this.T_end_t;
    }

    public double getT_end() {
        return this.T_end;
    }
}
