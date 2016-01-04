package client;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

public class AsyncHTTPClient {
	/**
	 * 
	 */
	private static final String SERVER_ENDPOINT = "http://localhost:8080";
	/**
	 * maximum number of client worker threads
	 */
	private static final int KMaxThread = 1;
	/**
	 * number of requests to be sent to the server by each client worker
	 */
	private static final int SAMPLE_SIZE = 50000;

	/**
	 * number of transactions
	 */
	private static volatile long transactionCount = 0;

	/**
	 * an arraylist holding tps samples
	 */
	private static volatile ArrayList<Long> tpss = new ArrayList<Long>();

	/**
	 * an arraylist holding response time samplesp
	 */
	private static volatile ArrayList<Long> mrts = new ArrayList<Long>();

	/**
	 * 
	 */
	private static void shuffleServerTimer() {
		// send a PUT request to the server to start/stop the timer

		try {
			URL obj = new URL(SERVER_ENDPOINT);
			java.net.HttpURLConnection con = (java.net.HttpURLConnection) obj
					.openConnection();
			// add request header
			con.setRequestMethod("PUT");

			// Send put request
			con.getResponseCode();

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * @author saeed
	 * 
	 *         capture and store the number of transactions per second
	 */
	static class TPSCalculator extends TimerTask {
		public void run() {
			// store tps
			tpss.add(transactionCount);
			// reset transaction count
			System.out.println("* " + transactionCount + " TPS");
			transactionCount = 0;
		}
	}

	/**
	 * print the results (TPS)
	 */
	private static void printResults() {
		// calculate mean TPS
		double tps = calculateAverage(tpss);
		// calculate mean response time (milliseconds)
		double mrt = calculateAverage(mrts) / 1000;

		// print out
		System.out.println("======================");
		System.out.format("TPS: %.2f%n", tps);
		System.out.format("Mean Response Time: %.2fms%n", mrt);
		System.out.println("======================");
	}

	/**
	 * @param results
	 * @return
	 */
	private static double calculateAverage(ArrayList<Long> results) {
		Long sum = new Long(0);
		if (results != null && !results.isEmpty()) {
			for (Long result : results) {
				sum += result;
			}
			return sum.doubleValue() / results.size();
		}

		return sum;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// start the timer on the server
		// shuffleServerTimer();

		// call TPS calculator every second
		Timer timer = new Timer();
		timer.schedule(new TPSCalculator(), 0, 1000);

		// make POST requests
		for (int i = 0; i < SAMPLE_SIZE * KMaxThread; i++) {
			Unirest.post(SERVER_ENDPOINT).asStringAsync(new Callback<String>() {

				@Override
				public void cancelled() {
					// TODO Auto-generated method stub

				}

				@Override
				public void completed(HttpResponse<String> response) {
					// TODO Auto-generated method stub
					int code = response.getStatus();

					if (code == 200) {
						// count the transaction
						transactionCount++;
					}
				}

				@Override
				public void failed(UnirestException arg0) {
					// TODO Auto-generated method stub

				}

			});

		}

		// stop the timer on the server
		// shuffleServerTimer();

		// stop the timer
		timer.cancel();

		// printout results
		printResults();

	}
}
