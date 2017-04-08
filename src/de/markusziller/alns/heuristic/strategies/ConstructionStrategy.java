package de.markusziller.alns.heuristic.strategies;

import de.markusziller.alns.common.entities.Instance;
import de.markusziller.alns.common.entities.Solution;
import de.markusziller.alns.common.exceptions.GeneralInfeasibilityException;
import de.markusziller.alns.heuristic.SolverConfiguration;

public interface ConstructionStrategy {

    Solution getInitialSolution(Instance i) throws GeneralInfeasibilityException;

    Solution getInitialSolution(Instance i, SolverConfiguration sc) throws GeneralInfeasibilityException;

    Solution getInitialSolution(Instance i, SolverConfiguration sc, boolean multithreaded);
}