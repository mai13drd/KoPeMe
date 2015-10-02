package de.dagere.kopeme.junit.exampletests.runner.parameterized;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import de.dagere.kopeme.annotations.PerformanceTest;
import de.dagere.kopeme.datacollection.DataCollectorList;
import de.dagere.kopeme.junit.rule.KoPeMeRuleChain;
import de.dagere.kopeme.junit.rule.KoPeMeRuleParameters;
import de.dagere.kopeme.junit.rule.annotations.AfterNoMeasurement;
import de.dagere.kopeme.junit.rule.annotations.BeforeNoMeasurement;

public class ExampleRuleChainTest {

	private static final Logger LOG = LogManager.getLogger(ExampleRuleChainTest.class);

	private TemporaryFolder folder;
	private Path file;
	private TestName name;

	/**
	 * This is an example of a KoPeMeRuleChain where all levels to define Rules are used. <br>
	 * The {@code outerRule(TestRule)} (and any {@code around(TestRule)} called before
	 * {@code koPeMeParameters(testObject)}) will be called only once for every test method, here for example only one
	 * {@code TemporaryFolder} object will be built for the {@code testKoPeMeChainRuleExample}. Of course it is possible
	 * to use {@code ClassRule} to set up additional Rules once for all test methods. <br>
	 * Rules that come after the {@code koPeMeParameters} method will be applied and evaluated (run) for every
	 * parameterization once (or each time, see further down). This may be useful if some setup needs to be adjusted for
	 * different parameterizations, the parameters of the test object are set at that point. <br>
	 * The {@code koPeMe} method is the next split point, every {@code around(TestRule)} occurring afterwards will be
	 * applied and evaluated for each performance measurement, for example if something needs to be set up every time
	 * anew. <br>
	 * The {@code measure(TestRule)} method can be used to specify TestRules that should be included in the performance
	 * measurement. <br>
	 * If {@code koPeMe} is never called the KoPeMeRuleChain will act as if it were called right after
	 * {@code koPeMeParameters} thus all {@code around} called afterwards will be applied for every performance run.
	 */
	@Rule
	public TestRule perform = KoPeMeRuleChain
			.outerRule(folder = new TemporaryFolder())
			.koPeMeParameters(this, DataCollectorList.ONLYTIME)
			.around(new ExternalResource() {
				@Override
				protected void before() throws Throwable {
					file = folder.newFile().toPath();
				}
			})
			.koPeMe()
			.around(name = new TestName())
			.measure(new ExternalResource() {
				@Override
				protected void before() throws InterruptedException {
					Thread.sleep(20);
				}
			});

	@BeforeNoMeasurement
	public void getTempFile() {
		try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.APPEND)) {
			writer.append("Method: " + name.getMethodName())
					.append(", waiting: " + waiting);
		} catch (final IOException e1) {
			e1.printStackTrace();
		}
	}

	@AfterNoMeasurement
	public void readTempFiles() {
		try {
			final Iterator<Path> fileIterator = Files.list(file.getParent()).iterator();
			while (fileIterator.hasNext()) {
				LOG.info(" Content: {}", new String(Files.readAllBytes(fileIterator.next())));
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

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
	public void testKoPeMeChainRuleExample() {
		try {
			Thread.sleep(waiting);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}
}
