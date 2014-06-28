package de.tum.ziller.thesis.thrp.heuristic.strategies.alns.removal;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.TreeMultimap;

import de.tum.ziller.thesis.thrp.common.entities.Node;
import de.tum.ziller.thesis.thrp.common.entities.Route;
import de.tum.ziller.thesis.thrp.common.entities.Solution;
import de.tum.ziller.thesis.thrp.common.entities.Therapist;
import de.tum.ziller.thesis.thrp.heuristic.strategies.alns.ALNSAbstractOperation;

/**
 * Entfernt eine zuf�llige Route
 * 
 * @author Markus
 * 
 */
public class RandomRouteDestroy extends ALNSAbstractOperation implements IALNSDestroy {
	@Override
	public Solution destroy(Solution s, int q) throws Exception {
		TreeMultimap<Therapist, Route> map = s.getRoutes();
		ArrayList<Route> routes = new ArrayList<>();
		ArrayList<Removal> removals = new ArrayList<>();
		for (Therapist t : map.keySet()) {
			Set<Route> R = map.get(t);
			for (Route r : R) {
				if (r.noOfTreatmentJobs() > 0) {
					routes.add(r);
				}
			}
		}
		Random random = new Random();
		Route r = routes.get(random.nextInt(routes.size()));
		for (Node m : r.getN()) {
			if (q > 0) {
				if (m.isTreatment()) {
					removals.add(new Removal(m, r));
					q--;
				}
			} else {
				break;
			}
		}
		for (Removal w : removals) {
			s.remove(w.n, w.r);
		}
		return s;
	}
}
