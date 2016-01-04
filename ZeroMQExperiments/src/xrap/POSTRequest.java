/**
 * 
 */
package xrap;

import java.util.HashMap;
import java.util.Map;

/**
 * @author saeed
 *
 */
public class POSTRequest {
	private String resource;
	private String body;
	private Map<String,String> headers=new HashMap<String, String>();
	
	public POSTRequest(String resource, Map<String,String> headers, String body) {
		setResource(resource);
		setBody(body);
		setHeaders(headers);
	}

	
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		//add the first line
		String str="POST";
		str += "\t";
		str += getResource();
		str += "\n";
		//add headers
		for (Map.Entry<String, String> entry : getHeaders().entrySet()) {
			str += entry.getKey() + " : " + entry.getValue() + "\n";
		}
		
		//add body
		str += getBody();
		
		return str;
	}



	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String data) {
		this.body = data;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	
	
}
