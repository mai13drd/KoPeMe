package de.dagere.kopeme.junit.exampletests.runner.parameterized;

import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import de.dagere.kopeme.annotations.PerformanceTest;
import de.dagere.kopeme.junit.rule.KoPeMeRuleParameters;

public class ExampleUpperBoundaryParameterTest {

	@Rule
	public TestRule perform = new KoPeMeRuleParameters(this);

	@KoPeMeRuleParameters.Parameters(collectorname = "de.dagere.kopeme.datacollection.TimeDataCollector", totalBoundary = 150000)
	public static List<Integer> performanceParameters() {
		return Arrays.asList(20, 50, 100, 120, 180, 200, 250);
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
