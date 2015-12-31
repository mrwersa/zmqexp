/**
 * 
 */
package client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

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
	private static final int KMaxThread = 5;
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

		public void run() {
			System.out.println("Client#" + clientID + " started...");

			// send requests
			for (int requestNbr = 0; requestNbr < SAMPLE_SIZE; requestNbr++) {
				try {
					URL obj = new URL(SERVER_ENDPOINT);

					java.net.HttpURLConnection con = (java.net.HttpURLConnection) obj
							.openConnection();

					// add reuqest header
					con.setRequestMethod("POST");

					// Send post request

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
		// TODO Auto-generated method stub
		for (int threadNbr = 0; threadNbr < KMaxThread; threadNbr++) {
			clientFinished[threadNbr] = false;
			new Thread(new Client(threadNbr)).start();
		}

		// call TPS calculator every second
		Timer timer = new Timer();
		timer.schedule(new TPSCalculator(), 0, 1000);

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

		// stop the timer
		timer.cancel();

		// printout results
		printResults();
	}

}
