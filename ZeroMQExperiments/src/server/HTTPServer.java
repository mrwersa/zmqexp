/**
 * 
 */
package server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.IOException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * @author saeed A simple HTTP server
 */
public class HTTPServer extends AbstractHandler {
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

		// handle POST requets
		if (request.getMethod().equalsIgnoreCase("post")) {
			// response is ok
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
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
