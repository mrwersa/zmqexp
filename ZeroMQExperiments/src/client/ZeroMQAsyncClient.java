package client;

import org.json.simple.JSONObject;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;
import org.zeromq.ZMQ.Poller;

import java.util.Random;

import java.util.ArrayList;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * @author saeed
 * 
 */
public class ZeroMQAsyncClient {

	private static final int SAMPLE_SIZE = 100000;
	/**
	 * maximum number of client worker threads
	 */
	private static final int KMaxThread = 1;
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

	private static Random rand = new Random(System.nanoTime());

	/**
	 * @author saeed
	 * 
	 */
	private static class ClientTask implements Runnable {
		private int clientID;

		public ClientTask(int id) {
			clientID = id;
		}

		public void run() {
			ZContext ctx = new ZContext();
			Socket client = ctx.createSocket(ZMQ.DEALER);

			// Set random identity to make tracing easier
			UUID identity = UUID.randomUUID();
			client.setIdentity(identity.toString().getBytes());
			client.connect("tcp://localhost:5570");

			System.out.println("Client#" + clientID + " started...");

			// create the request json
			JSONObject obj = new JSONObject();
			obj.put("id", "telecell");
			obj.put("type", "telecell");
			obj.put("isPattern", false);
			obj.put("att", "value");

			for (int requestNbr = 0; requestNbr < SAMPLE_SIZE; requestNbr++) {
				// send a request
				obj.put("id", "telecell" + requestNbr);
				obj.put("att", "value" + requestNbr);
				client.send(obj.toString());

				// get the reply
				byte[] reply = client.recv(0);

				// count the transaction
				transactionCount++;
			}
			System.out.println("Client#" + clientID + " finished...");
			clientFinished[clientID] = true;
			ctx.destroy();
		}
	}

	/**
	 * @param args
	 * @throws Exception
	 * 
	 *             The main thread simply starts several clients and wait for
	 *             them to finish
	 */
	public static void main(String[] args) {
		// output version
		System.out.println(String.format("0MQ %s", ZMQ.getVersionString()));

		// call TPS calculator every second
		Timer timer = new Timer();
		timer.schedule(new TPSCalculator(), 1000, 1000);

		// run clients
		for (int threadNbr = 0; threadNbr < KMaxThread; threadNbr++) {
			clientFinished[threadNbr] = false;
			new Thread(new ClientTask(threadNbr)).start();
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

		// stop the timer
		timer.cancel();

		// printout results
		printResults();
	}
}
