package de.dagere.kopeme.junit.rule.throughput;

import static de.dagere.kopeme.PerformanceTestUtils.saveData;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import de.dagere.kopeme.datacollection.TestResult;
import de.dagere.kopeme.datastorage.SaveableTestData;
import de.dagere.kopeme.junit.rule.KoPeMeBasicStatement;
import junit.framework.AssertionFailedError;

public class ComplexThroughputStatement extends KoPeMeBasicStatement {

	private static final Logger log = LogManager.getLogger(ComplexThroughputStatement.class);

	private final int stepsize, maxsize;
	private int currentsize;
	private final IOberserveExecutionTimes oberserver;

	private final Statement base;

	public ComplexThroughputStatement(final Statement base, final Description descr, final Object testObject, final int startsize, final int stepsize,
			final int maxsize, final IOberserveExecutionTimes oberserver) throws Exception {
		super(base, descr, testObject);
		this.base = base;
		this.stepsize = stepsize;
		this.maxsize = maxsize;
		this.oberserver = oberserver;
		this.currentsize = startsize;
	}

	@Override
	public void evaluate() throws Throwable {
		runWarmup();

		while (currentsize <= maxsize) {
			testResult = new TestResult(method.getName(), annotation.executionTimes());
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
			testResult.addValue("size", currentsize);
			saveData(SaveableTestData.createFineTestData(method.getName(), filename, testResult, 0, true));
			if (!assertationvalues.isEmpty()) {
				testResult.checkValues(assertationvalues);
			}

			currentsize += stepsize;
			oberserver.setSize(currentsize);
		}
	}

	@Override
	protected void runMainExecution() {
		int executions;
		final int executionTimes = annotation.executionTimes();
		for (executions = 1; executions <= executionTimes; executions++) {

			log.debug("--- Starting execution " + executions + "/" + executionTimes + " ---");
			runnables.getBeforeRunnable().run();
			testResult.startCollection();
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
