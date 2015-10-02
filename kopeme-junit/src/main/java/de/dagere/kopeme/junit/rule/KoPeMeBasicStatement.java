package de.dagere.kopeme.junit.rule;

import static de.dagere.kopeme.PerformanceTestUtils.saveData;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import de.dagere.kopeme.PerformanceTestUtils;
import de.dagere.kopeme.annotations.Assertion;
import de.dagere.kopeme.annotations.MaximalRelativeStandardDeviation;
import de.dagere.kopeme.annotations.PerformanceTest;
import de.dagere.kopeme.datacollection.DataCollectorList;
import de.dagere.kopeme.datacollection.TestResult;
import de.dagere.kopeme.datastorage.SaveableTestData;
import de.dagere.kopeme.kieker.KoPeMeKiekerSupport;

/**
 * A statement for running performance tests. Should once become base class of several TestExecutingStatements - isn't
 * yet.
 *
 * @author reichelt
 */
public class KoPeMeBasicStatement extends Statement {

	private static final Logger LOG = LogManager.getLogger(KoPeMeBasicStatement.class);

	protected final Statement base;
	protected final Description description;
	private final Object testObject;
	private final DataCollectorList collectors;

	protected Map<String, Double> maximumRelativeStandardDeviation;
	protected Map<String, Long> assertationvalues;
	protected String filename;
	protected Method method;
	protected TestRunnables runnables;
	protected TestResult testResult;
	protected PerformanceTest annotation;

	/**
	 * Initializes the KoPemeBasicStatement.
	 *
	 * @param collectors
	 * @param runnables Runnables that should be run
	 * @param method Method that should be executed
	 * @param filename Name of the
	 * @throws Exception If no {@literal @}PerformanceTest Annotation is present
	 */
	public KoPeMeBasicStatement(final Statement base, final Description description, final Object testObject,
			final DataCollectorList collectors) throws Exception {
		this.collectors = collectors;
		this.base = applyMeasuredRules(base, description);
		this.description = description;
		this.testObject = testObject;
		final Class<?> testClass = testObject.getClass();
		this.method = acquireMethod(description.getMethodName(), testClass);
		this.runnables = new TestRunnables(testClass, testObject);
		this.filename = testClass.getName() + ".yaml";

		annotation = method.getAnnotation(PerformanceTest.class);

		getAnnotatedData();
	}

	/**
	 * Initializes the KoPemeBasicStatement.
	 *
	 * @param collectors
	 * @param runnables Runnables that should be run
	 * @param method Method that should be executed
	 * @param filename Name of the
	 * @throws Exception If no {@literal @}PerformanceTest Annotation is present
	 */
	public KoPeMeBasicStatement(final Statement base, final Description description, final Object testObject) throws Exception {
		this.collectors = DataCollectorList.STANDARD;
		this.base = applyMeasuredRules(base, description);
		this.description = description;
		this.testObject = testObject;
		final Class<?> testClass = testObject.getClass();
		this.method = acquireMethod(description.getMethodName(), testClass);
		this.runnables = new TestRunnables(testClass, testObject);
		this.filename = testClass.getName() + ".yaml";

		annotation = method.getAnnotation(PerformanceTest.class);

		getAnnotatedData();
	}

	protected void getAnnotatedData() throws Exception {
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
			throw new Exception("No @PerformanceTest-Annotation present!");
		}
	}

	protected Statement applyMeasuredRules(final Statement singleRunStatement, final Description description) {
		return singleRunStatement;
	}

	private Throwable throwable = null;

	/**
	 * Used as a mechanism to rethrow the throwable from inner Statements.
	 *
	 * @param throwable Throwable from the evaluation of the inner Statements.
	 */
	private synchronized void setThrowable(final Throwable throwable) {
		this.throwable = throwable;
	}

	@Override
	public void evaluate() throws Throwable {

		final Thread mainThread = new Thread(new Runnable() {
			@Override
			public void run() {
				testResult = new TestResult(method.getName(), annotation.executionTimes(), collectors);
				checkCollectorValidity();
				try {
					runWarmup();
					runMainExecution();
				} catch (final Throwable e) {
					setThrowable(e);
				} finally {
					finalizeMeasurement();
				}
				if (!assertationvalues.isEmpty()) {
					testResult.checkValues(assertationvalues);
				}
			}
		});

		mainThread.start();
		mainThread.join(annotation.timeout());
		if (mainThread.isAlive()) {
			mainThread.interrupt();
		}
		if (throwable != null) {
			throw throwable;
		}
		LOG.info("Test {} beendet", filename);
	}

	protected Statement applyInnerRules(final Statement singleWarmupStatement, final Description descr) {
		return singleWarmupStatement;
	}

	public TestResult getTestResults() {
		return testResult;
	}

	/**
	 * Tests whether the collectors given in the assertions and the maximum relative standard deviations are valid.
	 *
	 * @param tr Test Result that should be checked
	 * @return Weather the result is valid
	 */
	protected boolean checkCollectorValidity() {
		if (!PerformanceTestUtils.checkCollectorValidity(testResult, assertationvalues, maximumRelativeStandardDeviation)) {
			LOG.warn("Not all Collectors are valid!");
			return false;
		}
		return true;
	}

	protected void runMainExecution() throws Throwable {

		final Statement singleRunStatement = new Statement() {
			@Override
			public void evaluate() throws Throwable {
				runnables.getBeforeRunnable().run();
				testResult.startCollection();
				base.evaluate();
				testResult.stopCollection();
				runnables.getAfterRunnable().run();
			}
		};

		final Statement completeSingleRunStatement = applyInnerRules(singleRunStatement, description);

		int executions;
		for (executions = 1; executions <= annotation.executionTimes(); executions++) {

			LOG.debug("--- Starting execution " + executions + "/" + annotation.executionTimes() + " ---");
			completeSingleRunStatement.evaluate();
			testResult.setRealExecutions(executions - 1);
			LOG.debug("--- Stopping execution " + executions + "/" + annotation.executionTimes() + " ---");
			for (final Map.Entry<String, Double> entry : maximumRelativeStandardDeviation.entrySet()) {
				LOG.trace("Entry: {} {}", entry.getKey(), entry.getValue());
			}
			if (executions >= annotation.minEarlyStopExecutions() && !maximumRelativeStandardDeviation.isEmpty()
					&& testResult.isRelativeStandardDeviationBelow(maximumRelativeStandardDeviation)) {
				break;
			}
		}
		LOG.debug("Executions: " + (executions - 1));
		testResult.setRealExecutions(executions - 1);
	}

	protected void runWarmup() throws Throwable {

		final Statement singleWarmupStatement = new Statement() {
			@Override
			public void evaluate() throws Throwable {
				runnables.getBeforeRunnable().run();
				base.evaluate();
				runnables.getAfterRunnable().run();
			}
		};

		final Statement completeSingleRunStatement = applyInnerRules(singleWarmupStatement, description);

		final String methodString = method.getClass().getName() + "." + method.getName();
		for (int execution = 1; execution <= annotation.warmupExecutions(); execution++) {
			final int index = execution;

			LOG.info("--- Starting warmup execution " + methodString + " " + index + "/" + annotation.warmupExecutions() + " ---");
			completeSingleRunStatement.evaluate();
			LOG.info("--- Stopping warmup execution " + index + "/" + annotation.warmupExecutions() + " ---");
		}
	}

	protected void finalizeMeasurement() {
		testResult.finalizeCollection();
		saveData(SaveableTestData.createFineTestData(method.getName(), filename, testResult, annotation.warmupExecutions(), true));
	}

	/**
	 * Will return just the method with the given name if available, else it looks for the method by decoding standard
	 * naming Patterns of JUnit Runners. Works for unchanged names or changes that follow the pattern of the
	 * Parameterized JUnit runner.
	 *
	 * @param methodName Name of the method as given by the runner
	 * @param testClass Class containing the method
	 * @return The Method with the given Name
	 */
	protected Method acquireMethod(final String methodName, final Class<?> testClass) {
		Method testMethod = null;
		try {
			// testClass = Class.forName(descr.getClassName());
			testMethod = testClass.getMethod(methodName);
		} catch (final NoSuchMethodException wrongName) {
			if (methodName.contains("[") && methodName.contains("]")) {
				try {
					testMethod = testClass.getMethod(methodName.substring(0, methodName.lastIndexOf("[")));
				} catch (final NoSuchMethodException stillWrongName) {
					wrongName.printStackTrace();
				}
			} else {
				wrongName.printStackTrace();
			}
		} catch (final SecurityException e) {
			e.printStackTrace();
		}
		return testMethod;
	}

	public Object getTestObject() {
		return testObject;
	}

	public DataCollectorList getCollectors() {
		return collectors;
	}
}