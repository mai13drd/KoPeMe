package de.dagere.kopeme.junit.rule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains the results of a parameterized performance measurement plus convinienceMethods.
 *
 * @author krauss
 */
public class ParameterTestResult {

	private final Map<KoPeMeBasicStatement, Object[]> parameters = new LinkedHashMap<>();
	private final Map<KoPeMeBasicStatement, Double> weights = new LinkedHashMap<>();

	private final Long totalBoundary;
	private final Double relativeBoundary;
	private final String collectorName;

	// For quick assessment of the relativeBoundary
	private double bestWeightedPerformance = Long.MAX_VALUE;

	public ParameterTestResult(final Long totalBoundary, final Double relativeBoundary, final String collectorName) {
		this.totalBoundary = totalBoundary;
		this.relativeBoundary = relativeBoundary;
		this.collectorName = collectorName;
	}

	/**
	 * @param statement
	 * @param parameters
	 * @param weight
	 * @return
	 */
	public boolean addResult(final KoPeMeBasicStatement statement, final Object[] parameters, final Double weight) {
		this.parameters.put(statement, parameters);
		this.weights.put(statement, weight);
		final long performance = statement.getTestResults().getValue(collectorName);
		final Double weightedPerformance = performance / weight;

		if (weightedPerformance > totalBoundary || weightedPerformance > bestWeightedPerformance / relativeBoundary) {
			return false;
		}
		if (weightedPerformance < bestWeightedPerformance) {
			bestWeightedPerformance = weightedPerformance;
		}
		return true;
	}

	public Long getTotalBoundary() {
		return totalBoundary;
	}

	public Double getRelativeBoundary() {
		return relativeBoundary;
	}

	public String getCollectorName() {
		return collectorName;
	}

	public KoPeMeBasicStatement getHeaviestWeightedRun() {
		KoPeMeBasicStatement heaviestRun = null;
		final long heaviestPerformance = Long.MAX_VALUE;
		for (final Map.Entry<KoPeMeBasicStatement, Double> run : weights.entrySet()) {
			final long performance = run.getKey().getTestResults().getValue(collectorName);
			if (performance / run.getValue() > heaviestPerformance) {
				heaviestRun = run.getKey();
			}
		}
		return heaviestRun;
	}

	public KoPeMeBasicStatement getHeaviestRun() {
		KoPeMeBasicStatement heaviestRun = null;
		final long heaviestPerformance = 0;
		for (final Map.Entry<KoPeMeBasicStatement, Double> run : weights.entrySet()) {
			final long performance = run.getKey().getTestResults().getValue(collectorName);
			if (performance > heaviestPerformance) {
				heaviestRun = run.getKey();
			}
		}
		return heaviestRun;
	}

	public KoPeMeBasicStatement getBestWeightedRun() {
		KoPeMeBasicStatement bestRun = null;
		final long bestPerformance = Long.MAX_VALUE;
		for (final Map.Entry<KoPeMeBasicStatement, Double> run : weights.entrySet()) {
			final long performance = run.getKey().getTestResults().getValue(collectorName);
			if (performance / run.getValue() > bestPerformance) {
				bestRun = run.getKey();
			}
		}
		return bestRun;
	}

	public KoPeMeBasicStatement getBestRun() {
		KoPeMeBasicStatement bestRun = null;
		final long bestPerformance = Long.MAX_VALUE;
		for (final Map.Entry<KoPeMeBasicStatement, Double> run : weights.entrySet()) {
			final long performance = run.getKey().getTestResults().getValue(collectorName);
			if (performance > bestPerformance) {
				bestRun = run.getKey();
			}
		}
		return bestRun;
	}

	/**
	 * @return Returns the sorted List of all Statements that were run.
	 */
	public List<KoPeMeBasicStatement> getRuns() {
		return new ArrayList<>(weights.keySet());
	}

	public Object[] getParameters(final KoPeMeBasicStatement run) {
		return parameters.get(run);
	}

	public Long getPerformance(final KoPeMeBasicStatement run) {
		return run.getTestResults().getValue(collectorName);
	}

	public Double getWeight(final KoPeMeBasicStatement run) {
		return weights.get(run);
	}
}
