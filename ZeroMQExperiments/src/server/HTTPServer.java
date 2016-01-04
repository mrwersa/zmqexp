/**
 * 
 */
package server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * @author saeed A simple HTTP server
 */
public class HTTPServer extends AbstractHandler {

	/**
	 * 
	 */
	private static volatile Timer timer = new Timer();

	private static boolean timerOn = false;

	/**
	 * number of transactions
	 */
	private static volatile long transactionCount = 0;

	/**
	 * an arraylist holding tps samples
	 */
	private static volatile ArrayList<Long> tpss = new ArrayList<Long>();

	/**
	 * an arraylist holding response time samples
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jetty.server.Handler#handle(java.lang.String,
	 * org.eclipse.jetty.server.Request, javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		// handle POST requests
		if (request.getMethod().equalsIgnoreCase("post")) {
			// response is ok
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
			// count it as a transaction
			transactionCount++;
		}
		// handle PUT requests
		else if (request.getMethod().equalsIgnoreCase("put")) {
			// shuffle the timer
			if (timerOn) {
				// stop the timer
				timer.cancel();
				// printout results
				printResults();
				// reset counters
				timerOn = false;
				transactionCount = 0;
				tpss = new ArrayList<Long>();
				mrts = new ArrayList<Long>();
			} else {
				// call TPS calculator every second
				timer.schedule(new TPSCalculator(), 0, 1000);
				timerOn = true;
			}
		}

	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// port 8080
		Server server = new Server(8080);
		server.setHandler(new HTTPServer());

		server.start();
		server.join();
	}
}
