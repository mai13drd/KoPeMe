package de.kopeme.visualizer;

import java.util.Date;

public class NormalDateConverter implements DateConverter{
	
	public StringDatePair getDisplayStringOfDate(Date d) {
		return new StringDatePair(d.toString(), d);
	}
}
