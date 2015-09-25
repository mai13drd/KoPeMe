package de.dagere.kopeme.junit.exampletests.runner.parameterized;

import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import de.dagere.kopeme.annotations.PerformanceTest;
import de.dagere.kopeme.junit.rule.KoPeMeRule;

@RunWith(Parameterized.class)
public class ExampleParametrizedRuleTest {

	@Rule
	public TestRule perform = new KoPeMeRule(this);
	
	@Parameterized.Parameters
	public static Iterable<Integer> data() {
		return Arrays.asList(10,20,40,60);
	}
	
	private int waiting;
	
	public ExampleParametrizedRuleTest(final int data) {
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
