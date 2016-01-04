package client;

import org.json.simple.JSONObject;
import org.zeromq.ZMQ;

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
public class ZeroMQZurlClient {
	/**
	 * 
	 */
	private static final String SERVER_ENDPOINT = "http://127.0.0.1:8080";

	/**
	 * 
	 */
	private static final int SAMPLE_SIZE = 100000;
	/**
	 * number of transactions
	 */
	private static volatile long transactionCount = 0;

	/**
	 * an arraylist holding tps samples
	 */
	private static volatile ArrayList<Long> tpss = new ArrayList<Long>();

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
	 * 
	 */
	private static void printResults() {
		// calculate mean TPS
		double tps = calculateAverage(tpss);
		// calculate mean response time (milliseconds)
		// double mrt = calculateAverage(mrts) / 1000;

		// print out
		System.out.println("======================");
		System.out.format("TPS: %.2f%n", tps);
		// System.out.format("Mean Response Time: %.2fms%n", mrt);
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
		// output version
		System.out.println(String.format("0MQ %s", ZMQ.getVersionString()));

		// start the timer on the server
		shuffleServerTimer();

		// call TPS calculator every second
		Timer timer = new Timer();
		timer.schedule(new TPSCalculator(), 1000, 1000);

		ZMQ.Context context = ZMQ.context(1);
		// Socket to talk to server
		ZMQ.Socket requester = context.socket(ZMQ.PUSH);
		requester.connect("ipc:///tmp/zurl-in");

		// create the request json
		JSONObject obj = new JSONObject();
		obj.put("method", "POST");
		obj.put("uri", SERVER_ENDPOINT);

		JSONObject data = new JSONObject();
		data.put("id", "telecell");
		data.put("type", "telecell");
		data.put("isPattern", false);
		data.put("att", "value");

		obj.put("data", data);

		for (int requestNbr = 0; requestNbr != SAMPLE_SIZE; requestNbr++) {
			// send a request
			data.put("id", "telecell" + requestNbr);
			data.put("att", "value" + requestNbr);
			obj.put("data", data);

			requester.send("J" + obj.toString());

			// count the transaction
			transactionCount++;
		}

		// stop the timer
		timer.cancel();

		// stop the timer on the server
		// shuffleServerTimer();

		// printout results
		printResults();

		requester.close();
		context.term();
	}

}
