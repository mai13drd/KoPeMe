package de.dagere.kopeme.datacollection.consumption;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class DataConsumer {
	
	public abstract void addData(Map<String, Long> data);

	public abstract long getMaximumCurrentValue(String datacollector);
	public abstract long getMinimumCurrentValue(String datacollector);
	public abstract double getRelativeStandardDeviation(String datacollector);
	public abstract double getAverageCurrentValue(String datacollector);
	public abstract List<Long> getValues(String datacollector);

	public abstract int getDataCount();

	public abstract Set<String> getKeys();

	
}
