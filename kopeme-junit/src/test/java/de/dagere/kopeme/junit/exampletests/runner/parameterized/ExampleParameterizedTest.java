package de.dagere.kopeme.junit.exampletests.runner.parameterized;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import de.dagere.kopeme.annotations.PerformanceTest;
import de.dagere.kopeme.junit.testrunner.parameterized.ParameterizedPerformanceRunnerFactory;

@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(ParameterizedPerformanceRunnerFactory.class)
public class ExampleParameterizedTest {

	@Parameterized.Parameters
	public static Iterable<Integer> data() {
		return Arrays.asList(10,20,40,60);
	}
	
	private int waiting;
	
	public ExampleParameterizedTest(final int data) {
		this.waiting = data;
	}
	
	@Test
	@PerformanceTest(executionTimes = 10, warmupExecutions = 5)
	public void testParameterizedExample() {
		try {
			Thread.sleep(waiting);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
