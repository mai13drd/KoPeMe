package de.dagere.kopeme.datacollection.consumption;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

public class ElasticConsumer extends DataConsumer {

	private HttpClient client;
	private HttpClientContext context;
	int dataCount = 0;
	String measurementName;

	public ElasticConsumer(String testname) {
		client = HttpClientBuilder.create().build();
		String user = System.getenv("ELASTIC_USER") != null ? System.getenv("ELASTIC_USER") : "elastic";
		String password = System.getenv("ELASTIC_PASSWORD") != null ? System.getenv("ELASTIC_PASSWORD") : "changeme";
		// String host = System.getenv("ELASTIC_HOST") != null ? System.getenv("ELASTIC_HOST") : "http://localhost:9200";

		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user, password);
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(AuthScope.ANY, credentials);

		context = HttpClientContext.create();
		context.setCredentialsProvider(credsProvider);
		measurementName = testname.toLowerCase() + System.nanoTime();
		addIndex(measurementName);
	}

	@Override
	public void addData(Map<String, Long> data) {
		System.out.println("Writing to data");
		for (Map.Entry<String, Long> entry : data.entrySet()) {
			HttpPut request = new HttpPut("http://localhost:9200/" + measurementName + "/" + entry.getKey().toLowerCase() + "/" + dataCount);

			String load = "{\"time\": " + System.nanoTime() + "," +
					"\"value\": " + entry.getValue() +
					"} ";
			request.setEntity(new StringEntity(load, "UTF-8"));
			try {
				HttpResponse response = client.execute(request, context);
				BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
				String output;
				while ((output = br.readLine()) != null) {
					System.out.println(output);
				}
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		System.out.println("Writing finished");

		dataCount++;
	}

	private void addIndex(String index) {
		HttpPut request = new HttpPut("http://localhost:9200/" + index);
		try {
			String load = "{\"mappings\": {  \"doc\": { " +
					"\"properties\": {" +
					"\"time\": {\"type\": \"date\"}," +
					"\"value\": {\"type\": \"double\"}" +
					"} } } }";
			request.setEntity(new StringEntity(load, "UTF-8"));
			System.out.println("Adding " + index);
			HttpResponse response = client.execute(request, context);
			BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
			String output;
			while ((output = br.readLine()) != null) {
				System.out.println(output);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public long getMaximumCurrentValue(String datacollector) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getMinimumCurrentValue(String datacollector) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getRelativeStandardDeviation(String datacollector) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getAverageCurrentValue(String datacollector) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Long> getValues(String datacollector) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getDataCount() {
		// TODO Auto-generated method stub
		return dataCount;
	}

	@Override
	public Set<String> getKeys() {
		return new HashSet<>();
	}

}
