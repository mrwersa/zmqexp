package client;

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
 *         Asynchronous client-to-server (DEALER to ROUTER)
 * 
 *         While this example runs in a single process, that is just to make it
 *         easier to start and stop the example. Each task has its own context
 *         and conceptually acts as a separate process.
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
	 *         This is our client task It connects to the server, and then sends
	 *         a request once per second It collects responses as they arrive,
	 *         and it prints them out. We will run several client tasks in
	 *         parallel, each with a different random ID.
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

			// variable for instrumentation
			long startTime;

			for (int requestNbr = 0; requestNbr < SAMPLE_SIZE; requestNbr++) {
				// +++++ start instrumentation
				startTime = System.nanoTime();
				// send request
				client.send(String.format("request #%d", requestNbr));

				// get the reply
				// client.recv(0);
				transactionCount++;
				mrts.add(System.nanoTime() - startTime);
				// +++++ end instrumentation
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
	 *             The main thread simply starts several clients, and a server,
	 *             and then waits for the server to finish
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
