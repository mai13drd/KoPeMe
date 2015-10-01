package de.dagere.kopeme.junit.rule.throughput;

import static de.dagere.kopeme.PerformanceTestUtils.saveData;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import de.dagere.kopeme.datacollection.TestResult;
import de.dagere.kopeme.datastorage.SaveableTestData;
import de.dagere.kopeme.junit.rule.KoPeMeBasicStatement;
import junit.framework.AssertionFailedError;

/**
 * @author reichelt
 */
public class ThroughputStatement extends KoPeMeBasicStatement {

	private static final Logger log = LogManager.getLogger(ThroughputStatement.class);

	private final int stepsize, maxsize;

	private final Statement base;

	public ThroughputStatement(final Statement base, final Description descr, final Object testObject,
			final int stepsize,
			final int maxsize) throws Exception {
		super(base, descr, testObject);
		this.base = base;
		this.stepsize = stepsize;
		this.maxsize = maxsize;
	}

	@Override
	public void evaluate() throws Throwable {
		runWarmup();

		int executionTimes = annotation.executionTimes();
		while (executionTimes <= maxsize) {
			testResult = new TestResult(method.getName(), executionTimes);

			checkCollectorValidity();

			try {
				runMainExecution();
			} catch (final AssertionFailedError t) {
				testResult.finalizeCollection();
				saveData(SaveableTestData.createAssertFailedTestData(method.getName(), filename, testResult, 0, true));
				throw t;
			} catch (final Throwable t) {
				testResult.finalizeCollection();
				saveData(SaveableTestData.createErrorTestData(method.getName(), filename, testResult, 0, true));
				throw t;
			}
			testResult.finalizeCollection();
			saveData(SaveableTestData.createFineTestData(method.getName(), filename, testResult, 0, true));
			if (!assertationvalues.isEmpty()) {
				testResult.checkValues(assertationvalues);
			}

			executionTimes += stepsize;
		}

		// PerformanceTestUtils.saveData(method.getName(), tr, false, false, filename, true);
	}

	@Override
	protected void runMainExecution() throws IllegalAccessException, InvocationTargetException {
		int executions;
		final int executionTimes = annotation.executionTimes();
		for (executions = 1; executions <= executionTimes; executions++) {

			log.debug("--- Starting execution " + executions + "/" + executionTimes + " ---");
			runnables.getBeforeRunnable().run();
			testResult.startOrRestartCollection();
			try {
				base.evaluate();
			} catch (final Throwable e) {
				e.printStackTrace();
			}
			testResult.stopCollection();
			runnables.getAfterRunnable().run();

			log.debug("--- Stopping execution " + executions + "/" + executionTimes + " ---");
			for (final Map.Entry<String, Double> entry : maximumRelativeStandardDeviation.entrySet()) {
				log.trace("Entry: {} {}", entry.getKey(), entry.getValue());
			}
			if (executions >= annotation.minEarlyStopExecutions() && !maximumRelativeStandardDeviation.isEmpty()
					&& testResult.isRelativeStandardDeviationBelow(maximumRelativeStandardDeviation)) {
				break;
			}
		}
		log.debug("Executions: " + (executions - 1));
		testResult.setRealExecutions(executions - 1);
	}
}
