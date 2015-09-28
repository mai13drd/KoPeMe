package de.dagere.kopeme.junit.rule;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.runners.model.Statement;

import de.dagere.kopeme.PerformanceTestUtils;
import de.dagere.kopeme.annotations.Assertion;
import de.dagere.kopeme.annotations.MaximalRelativeStandardDeviation;
import de.dagere.kopeme.annotations.PerformanceTest;
import de.dagere.kopeme.datacollection.TestResult;
import de.dagere.kopeme.kieker.KoPeMeKiekerSupport;

/**
 * A statement for running performance tests. Should once become base class of several TestExecutingStatements - isn't
 * yet.
 *
 * @author reichelt
 */
public abstract class KoPeMeBasicStatement extends Statement {

	private static final Logger LOG = LogManager.getLogger(KoPeMeBasicStatement.class);

	protected Map<String, Double> maximumRelativeStandardDeviation;
	protected Map<String, Long> assertationvalues;
	protected String filename;
	protected Method method;
	protected TestRunnables runnables;

	protected PerformanceTest annotation;

	/**
	 * Initializes the KoPemeBasicStatement.
	 *
	 * @param runnables Runnables that should be run
	 * @param method Method that should be executed
	 * @param filename Name of the
	 */
	public KoPeMeBasicStatement(final TestRunnables runnables, final Method method, final String filename) {
		super();
		this.runnables = runnables;
		this.filename = filename;
		this.method = method;

		annotation = method.getAnnotation(PerformanceTest.class);

		if (annotation != null) {
			try {
				KoPeMeKiekerSupport.INSTANCE.useKieker(annotation.useKieker(), filename, method.getName());
			} catch (final Exception e) {
				System.err.println("kieker has failed!");
				e.printStackTrace();
			}
			maximumRelativeStandardDeviation = new HashMap<>();
			assertationvalues = new HashMap<>();
			for (final MaximalRelativeStandardDeviation maxDev : annotation.deviations()) {
				maximumRelativeStandardDeviation.put(maxDev.collectorname(), maxDev.maxvalue());
			}

			for (final Assertion a : annotation.assertions()) {
				assertationvalues.put(a.collectorname(), a.maxvalue());
			}
		} else {
			LOG.error("No @PerformanceTest-Annotation present!");
		}
	}

	/**
	 * Tests whether the collectors given in the assertions and the maximum relative standard deviations are valid.
	 *
	 * @param tr Test Result that should be checked
	 * @return Weather the result is valid
	 */
	protected boolean checkCollectorValidity(final TestResult tr) {
		return PerformanceTestUtils.checkCollectorValidity(tr, assertationvalues, maximumRelativeStandardDeviation);
	}

	protected void runMainExecution(final TestResult tr) throws IllegalAccessException, InvocationTargetException {
		int executions;
		for (executions = 1; executions <= annotation.executionTimes(); executions++) {

			LOG.debug("--- Starting execution " + executions + "/" + annotation.executionTimes() + " ---");
			runnables.getBeforeRunnable().run();
			tr.startCollection();
			runnables.getTestRunnable().run();
			tr.stopCollection();
			runnables.getAfterRunnable().run();
			tr.setRealExecutions(executions - 1);
			LOG.debug("--- Stopping execution " + executions + "/" + annotation.executionTimes() + " ---");
			for (final Map.Entry<String, Double> entry : maximumRelativeStandardDeviation.entrySet()) {
				LOG.trace("Entry: {} {}", entry.getKey(), entry.getValue());
			}
			if (executions >= annotation.minEarlyStopExecutions() && !maximumRelativeStandardDeviation.isEmpty()
					&& tr.isRelativeStandardDeviationBelow(maximumRelativeStandardDeviation)) {
				break;
			}
		}
		LOG.debug("Executions: " + (executions - 1));
		tr.setRealExecutions(executions - 1);
	}

	protected void runWarmup(final String methodString) {
		for (int i = 1; i <= annotation.warmupExecutions(); i++) {
			runnables.getBeforeRunnable().run();
			LOG.info("--- Starting warmup execution " + methodString + " " + i + "/" + annotation.warmupExecutions() + " ---");
			runnables.getTestRunnable().run();
			LOG.info("--- Stopping warmup execution " + i + "/" + annotation.warmupExecutions() + " ---");
			runnables.getAfterRunnable().run();
		}
	}
}