package de.tum.ziller.thesis.thrp.heuristic.strategies.alns.removal.unused;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.TreeMultimap;

import de.tum.ziller.thesis.thrp.common.entities.Node;
import de.tum.ziller.thesis.thrp.common.entities.Route;
import de.tum.ziller.thesis.thrp.common.entities.Solution;
import de.tum.ziller.thesis.thrp.common.entities.Therapist;
import de.tum.ziller.thesis.thrp.heuristic.strategies.alns.ALNSAbstractOperation;
import de.tum.ziller.thesis.thrp.heuristic.strategies.alns.removal.IALNSDestroy;
import de.tum.ziller.thesis.thrp.heuristic.strategies.alns.removal.Removal;

public class AvgRouteCostDestroy extends ALNSAbstractOperation implements IALNSDestroy {

	
	int cost_greater = 1;
	int cost_lower = -1;
	
	public AvgRouteCostDestroy(boolean highestCostFirst){
		int modifier = highestCostFirst ? -1 : 1;
			cost_greater *= modifier;
			cost_lower *= modifier;
	}
	
	@Override
	public Solution destroy(final Solution from, int q) throws Exception {

		

		TreeMultimap<Therapist, Route> map = from.getRoutes();

		TreeSet<Route> routes = new TreeSet<>(new Comparator<Route>() {

			//Compares its two arguments for order. Returns a negative integer, zero, 
			//or a positive integer as the first argument is less than, equal to, or greater than the second.
			@Override
			public int compare(Route r1, Route r2) {
				if(from.getRouteCosts(r1)/r1.noOfTreatmentOrBreakJobs() > from.getRouteCosts(r2)/r2.noOfTreatmentOrBreakJobs()){
					return cost_greater;
				}
				
				if(from.getRouteCosts(r1)/r1.noOfTreatmentOrBreakJobs() < from.getRouteCosts(r2)/r2.noOfTreatmentOrBreakJobs()){
					return cost_lower;
				}
				return 0;
			}
		});
		
		
		
		ArrayList<Removal> removals = new ArrayList<>();
//		int sum = 0;
		
		for (Therapist t : map.keySet()) {
			Set<Route> R = map.get(t);
			for (Route r : R) {
				
				if (r.noOfTreatmentJobs() > 0) {
					routes.add(r);
//					sum += r.noOfTreatmentJobs();
				}
			}
		}



		for (Route r : routes) {
			if(q < 1){
				break;
			}
			for (Node m : r.getN()) {
				
				if(q > 0){
					if(m.isTreatment()){
						removals.add(new Removal(m, r));
						q--;
					}
				}else{
					break;
				}
			}
		}

//		for (Removal w : removals) {
//				from.remove(w.n, w.r);
//		}

		return from;
	}
}
