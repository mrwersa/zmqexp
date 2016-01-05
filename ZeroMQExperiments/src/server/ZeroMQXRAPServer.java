/**
 * 
 */
package server;

import org.zeromq.ZMQ;

/**
 * @author saeed
 *
 */
public class ZeroMQXRAPServer {

	/**
	 * @param args
	 */
    public static void main(String[] args) throws Exception {
		// output version
		System.out.println(String.format("Server: 0MQ %s", ZMQ.getVersionString()));
		
    	ZMQ.Context context = ZMQ.context(1);

        //  Socket to talk to clients
        ZMQ.Socket responder = context.socket(ZMQ.PULL);
        responder.bind("tcp://*:5555");

        while (!Thread.currentThread().isInterrupted()) {
            // Wait for next request from the client
            byte[] request = responder.recv(0);

            // Send reply back to client
            String reply = "200 OK";
            //responder.send(reply.getBytes(), 0);
        }
        responder.close();
        context.term();
    }

}
