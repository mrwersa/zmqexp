package xrap;

import java.util.Map;

public class XRapLibrary {

	/**
	 * Singleton instance
	 */
	private static XRapLibrary _instance = new XRapLibrary();

	/**
	 * protected constructor
	 */
	protected XRapLibrary() {

	}

	/**
	 * @return singleton instance
	 */
	public static XRapLibrary getInstance() {
		return _instance;
	}

	public String createPOST(String resource, Map<String, String> headers,
			String body) {
		POSTRequest request = new POSTRequest(resource, headers, body);

		return request.toString();
	}

}
