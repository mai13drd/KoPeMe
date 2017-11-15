package de.dagere.kopeme.datacollection.consumption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datacollection.TestResult;

public class LocalDataConsumer extends DataConsumer {
	
	private static final Logger LOG = LogManager.getLogger(LocalDataConsumer.class);
	
	protected final Set<String> keys = new HashSet<>();
	protected List<Map<String, Long>> realValues;
	
	public LocalDataConsumer(int expectedSize){
		realValues = new ArrayList<>(expectedSize);
	}
	
	public void addData(Map<String, Long> data){
		realValues.add(data);
		keys.addAll(data.keySet());
	}

	@Override
	public long getMaximumCurrentValue(String key) {
		long max = 0;
		for (int i = 0; i < realValues.size(); i++) {
			if (realValues.get(i).get(key) > max)
				max = realValues.get(i).get(key);
		}
		return max;
	}

	@Override
	public long getMinimumCurrentValue(String key) {
		long min = Long.MAX_VALUE;
		for (int i = 0; i < realValues.size(); i++) {
			if (realValues.get(i).get(key) < min)
				min = realValues.get(i).get(key);
		}
		return min;
	}

	@Override
	public double getRelativeStandardDeviation(String datacollector) {
		final long[] currentValues = new long[realValues.size()];
		for (int i = 0; i < realValues.size(); i++) {
			final Map<String, Long> map = realValues.get(i);
			currentValues[i] = map.get(datacollector);
		}
//		if (datacollector.equals("de.kopeme.datacollection.CPUUsageCollector") || datacollector.equals("de.kopeme.datacollection.TimeDataCollector")) {
//			LOG.trace(Arrays.toString(currentValues));
//		}
		final SummaryStatistics st = new SummaryStatistics();
		for (final Long l : currentValues) {
			st.addValue(l);
		}

		LOG.trace("Mittel: {} Standardabweichung: {}", st.getMean(), st.getStandardDeviation());
		return st.getStandardDeviation() / st.getMean();
	}

	@Override
	public int getDataCount() {
		return realValues.size();
	}

	@Override
	public Set<String> getKeys() {
		return keys;
	}

	@Override
	public double getAverageCurrentValue(String key) {
		final long[] currentValues = new long[realValues.size()];
		for (int i = 0; i < realValues.size(); i++) {
			final Map<String, Long> map = realValues.get(i);
			currentValues[i] = map.get(key);
		}
		final SummaryStatistics st = new SummaryStatistics();
		for (final Long l : currentValues) {
			st.addValue(l);
		}

		LOG.trace("Mittel: {} Standardabweichung: {}", st.getMean(), st.getStandardDeviation());
		return st.getMean();
	}

	@Override
	public List<Long> getValues(String datacollector) {
		List<Long> values = new LinkedList<>();
		for (int i = 0; i < realValues.size(); i++){
			values.add(realValues.get(i).get(datacollector));
		}
		return values;
	}
}
