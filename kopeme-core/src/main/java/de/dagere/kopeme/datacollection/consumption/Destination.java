package de.dagere.kopeme.datacollection.consumption;

public enum Destination {
	LOCAL, ELASTIC;

	public static Destination parse(String destination) {
		if (destination.equals("ELASTIC")){
			return Destination.ELASTIC;
		}else{
			return LOCAL;
		}
	}
}
