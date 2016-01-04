/**
 * 
 */
package client;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.json.simple.JSONObject;

/**
 * @author saeed
 * 
 */
public class HTTPClient {
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
	private static volatile boolean[] clientFinished = new boolean[KMaxThread];

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
	 * @author saeed A simple HTTP client worker thread
	 */
	private static class Client implements Runnable {
		private int clientID;

		public Client(int id) {
			this.clientID = id;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			System.out.println("Client#" + clientID + " started...");
			// request
			JSONObject obj = new JSONObject();
			obj.put("id", "telecell");
			obj.put("type", "telecell");
			obj.put("isPattern", false);
			obj.put("att", "value");

			// send requests
			for (int requestNbr = 0; requestNbr < SAMPLE_SIZE; requestNbr++) {
				try {
					URL url = new URL(SERVER_ENDPOINT);

					java.net.HttpURLConnection con = (java.net.HttpURLConnection) url
							.openConnection();

					// add reuqest header
					con.setRequestMethod("POST");
					con.setRequestProperty("Content-Type", "application/json");
					con.setRequestProperty("charset", "utf-8");
					con.setDoOutput(true);
					con.setUseCaches(false);

					// Send post request
					obj.put("id", "telecell" + requestNbr);
					obj.put("att", "value" + requestNbr);
					OutputStreamWriter wr = new OutputStreamWriter(
							con.getOutputStream());
					wr.write(obj.toString());

					int responseCode = con.getResponseCode();

					// count transaction
					if (responseCode == 200) {
						transactionCount++;
					}
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// end
			System.out.println("Client#" + clientID + " finished...");
			clientFinished[clientID] = true;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// start the timer on the server
		shuffleServerTimer();

		// call TPS calculator every second
		Timer timer = new Timer();
		timer.schedule(new TPSCalculator(), 1000, 1000);

		for (int threadNbr = 0; threadNbr < KMaxThread; threadNbr++) {
			clientFinished[threadNbr] = false;
			new Thread(new Client(threadNbr)).start();
		}

		// wait untill all the clients are finished
		boolean allClientsFinished;
		do {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			allClientsFinished = true;
			for (int i = 0; i < KMaxThread; i++) {
				if (!clientFinished[i]) {
					allClientsFinished = false;
					break;
				}
			}

		} while (!allClientsFinished);

		// stop the timer on the server
		shuffleServerTimer();

		// stop the timer
		timer.cancel();

		// printout results
		printResults();
	}

}
