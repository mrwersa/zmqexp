/**
 * 
 */
package client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.simple.JSONObject;
import org.zeromq.ZMQ;

import xrap.XRapLibrary;


/**
 * @author saeed
 * 
 */
public class ZeroMQXRAPClient {
	private static final int SAMPLE_SIZE = 8000000;
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

		// call TPS calculator every second
		Timer timer = new Timer();
		timer.schedule(new TPSCalculator(), 1000, 1000);

		ZMQ.Context context = ZMQ.context(1);
		// Socket to talk to server
		ZMQ.Socket requester = context.socket(ZMQ.PUSH);
		requester.connect("tcp://localhost:5555");

		// create the request body
		JSONObject obj = new JSONObject();
		obj.put("id", "telecell");
		obj.put("type", "telecell");
		obj.put("isPattern", false);
		obj.put("att", "value");

		// create xrap request
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Content-Type", "application/json");
		String request = XRapLibrary.getInstance().createPOST(
				"/updateContext/", headers, obj.toString());

		for (int requestNbr = 0; requestNbr != SAMPLE_SIZE; requestNbr++) {
			// send a request
			obj.put("id", "telecell" + requestNbr);
			obj.put("att", "value" + requestNbr);
			request = XRapLibrary.getInstance().createPOST("/updateContext/",
					headers, obj.toString());
			requester.send(request);

			//byte[] reply = requester.recv(0);

			// count the transaction
			transactionCount++;
		}

		// stop the timer
		timer.cancel();

		// printout results
		printResults();

		requester.close();
		context.term();
	}

}
