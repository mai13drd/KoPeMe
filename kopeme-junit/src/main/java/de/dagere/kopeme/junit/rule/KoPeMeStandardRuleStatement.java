package de.dagere.kopeme.junit.rule;

import static de.dagere.kopeme.PerformanceTestUtils.saveData;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datacollection.TestResult;
import de.dagere.kopeme.datastorage.SaveableTestData;

/**
 * Represents an execution of all runs of one test TODO: Overthink weather directly configure test runs in KoPeMeRule
 * would be more nice
 *
 * @author dagere
 */
public class KoPeMeStandardRuleStatement extends KoPeMeBasicStatement {

	static Logger LOG = LogManager.getLogger(KoPeMeStandardRuleStatement.class);

	public KoPeMeStandardRuleStatement(final TestRunnables runnables, final Method method, final String filename) {
		super(runnables, method, filename);
	}

	private TestResult testResult;

	@Override
	public void evaluate() throws Throwable {
		final Thread mainThread = new Thread(new Runnable() {
			@Override
			public void run() {
				testResult = new TestResult(method.getName(), annotation.executionTimes());
				try {
					executeSimpleTest();
					if (!assertationvalues.isEmpty()) {
						testResult.checkValues(assertationvalues);
					}
				} catch (IllegalAccessException | InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		});

		mainThread.start();
		mainThread.join(annotation.timeout());
		if (mainThread.isAlive()) {
			mainThread.interrupt();
		}

		LOG.info("Test {} beendet", filename);
	}

	private void executeSimpleTest() throws IllegalAccessException, InvocationTargetException {
		final String methodString = method.getClass().getName() + "." + method.getName();
		runWarmup(methodString);

		testResult = new TestResult(method.getName(), annotation.timeout());

		if (!checkCollectorValidity(testResult)) {
			LOG.warn("Not all Collectors are valid!");
		}
		try {
			runMainExecution(testResult);
		} finally {
			testResult.finalizeCollection();
			saveData(SaveableTestData.createFineTestData(method.getName(), filename, testResult, annotation.warmupExecutions(), true));
		}
	}

	public TestResult getTestResults() {
		return testResult;
	}
}
