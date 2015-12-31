package server;

import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;
import org.zeromq.ZMQ.Poller;

import java.util.Random;

//
//Asynchronous client-to-server (DEALER to ROUTER)
//
//While this example runs in a single process, that is just to make
//it easier to start and stop the example. Each task has its own
//context and conceptually acts as a separate process.

public class ZeroMQServer {

	/**
	 * 
	 */
	private static Random rand = new Random(System.nanoTime());

	/**
	 * the maximum number of server worker threads
	 */
	private static final int KMaxThread = 10;

	/**
	 * @author saeed
	 * 
	 *         This is our server task. It uses the multithreaded server model
	 *         to deal requests out to a pool of workers and route replies back
	 *         to clients. One worker can handle one request at a time but one
	 *         client can talk to multiple workers at once.
	 */
	private static class server_task implements Runnable {
		public void run() {
			ZContext ctx = new ZContext();

			// Frontend socket talks to clients over TCP
			Socket frontend = ctx.createSocket(ZMQ.ROUTER);
			frontend.bind("tcp://*:5570");

			// Backend socket talks to workers over inproc
			Socket backend = ctx.createSocket(ZMQ.DEALER);
			backend.bind("inproc://backend");

			// Launch pool of worker threads, precise number is not critical
			for (int threadNbr = 0; threadNbr < KMaxThread; threadNbr++)
				new Thread(new server_worker(ctx)).start();

			// Connect backend to frontend via a proxy
			ZMQ.proxy(frontend, backend, null);

			ctx.destroy();
		}
	}

	/**
	 * @author saeed Each worker task works on one request at a time and sends a
	 *         random number of replies back, with random delays between
	 *         replies:
	 */
	private static class server_worker implements Runnable {
		private ZContext ctx;

		public server_worker(ZContext ctx) {
			this.ctx = ctx;
		}

		public void run() {
			Socket worker = ctx.createSocket(ZMQ.DEALER);
			worker.connect("inproc://backend");

			while (!Thread.currentThread().isInterrupted()) {
				// The DEALER socket gives us the address envelope and message
				ZMsg msg = ZMsg.recvMsg(worker);
				ZFrame address = msg.pop();
				ZFrame content = msg.pop();
				assert (content != null);
				msg.destroy();

				address.send(worker, 128 + ZMQ.SNDMORE);
				content.send(worker, 128);

				address.destroy();
				content.destroy();
			}

			ctx.destroy();
		}
	}

	/**
	 * The main thread simply starts the server, and then waits for it to
	 * finish.
	 */
	public static void main(String[] args) throws Exception {
		// output version
		System.out.println(String.format("0MQ %s", ZMQ.getVersionString()));

		// start the server
		new Thread(new server_task()).start();
		System.in.read();

	}
}