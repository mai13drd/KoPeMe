package de.dagere.kopeme.junit.exampletests.runner.parameterized;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import de.dagere.kopeme.annotations.PerformanceTest;
import de.dagere.kopeme.junit.rule.KoPeMeRuleParameters;

public class ExampleRelativeBoundaryParametersTest {

	@Rule
	public TestRule perform = new KoPeMeRuleParameters(this);

	@KoPeMeRuleParameters.Parameters(collectorname = "de.dagere.kopeme.datacollection.TimeDataCollector", relativeBoundary = 0.9)
	public static Map<Integer, Double> performanceParameters() {
		final Map<Integer, Double> weightedParameters = new LinkedHashMap<>();
		for (final int i : Arrays.asList(100, 200, 250, 180, 120, 50, 20)) {
			weightedParameters.put(i, (double) (i / 10));
		}
		return weightedParameters;
	}

	@KoPeMeRuleParameters.Parameter
	public int waiting;

	@Test
	@PerformanceTest(executionTimes = 10, warmupExecutions = 5)
	public void testParameterizedExample() {
		try {
			Thread.sleep(waiting);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}
}
