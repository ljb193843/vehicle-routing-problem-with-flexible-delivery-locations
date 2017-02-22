package de.markusziller.alns.heuristic.visualization;

import de.markusziller.alns.common.entities.Insertion;
import de.markusziller.alns.common.entities.Solution;
import de.markusziller.alns.heuristic.strategies.alns.ALNSAbstractOperation;

public interface IStrategyVisualizer {

    void onJobPlanned(ALNSAbstractOperation a, Insertion i, Solution s);


}
