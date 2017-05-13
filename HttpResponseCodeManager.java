package com.adrianwirth.jsonhttpclient;

import android.support.annotation.NonNull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A class for registering event listeners for certain HTTP response codes.
 * For use with JsonHttpClient
 * Copyright 2017 Adrian Wirth
 * Released under the MIT license
 */


public class HttpResponseCodeManager {

	private static HttpResponseCodeManager instance = null;	// singleton instance

	private final Map<Integer, Set<HttpResponseCodeListener>> listeners = new HashMap<> ();	// the registered listeners, grouped by HTTP response code

	/**
	 * (Private) constructor
	 */
	private HttpResponseCodeManager () {}

	/**
	 * Return the singleton instance, constructing it if necessary
	 *
	 * @return the singleton instance
	 */
	public static HttpResponseCodeManager getInstance () {
		if (instance == null) {
			instance = new HttpResponseCodeManager ();
		}

		return instance;
	}

	/**
	 * Functional interface to provide a callback function / event listener
	 */
	public interface HttpResponseCodeListener {

		/**
		 * The actual callback method that is called when the registered response code occurred in an HTTP request
		 */
		void responseCodeOccurred ();
	}

	/**
	 * Register an event listener for the specified HTTP response code
	 *
	 * @param responseCode the response code, such as 200, 401, 500 etc.
	 * @param listener callback implementation
	 */
	public void addListener (int responseCode, @NonNull HttpResponseCodeListener listener) {
//		if (listener == null) {
//			throw new IllegalArgumentException ("HttpResponseCodeListener implementation must not be null");
//		}

		if (this.listeners.get (responseCode) == null) {
			this.listeners.put (responseCode, new HashSet<HttpResponseCodeListener> ());
		}

		this.listeners.get (responseCode).add (listener);
	}

	/**
	 * Call all registered event listeners for the specified HTTP response code
	 *
	 * @param responseCode the HTTP response code
	 */
	protected void emitResponseCode (int responseCode) {
		Set<HttpResponseCodeListener> listeners = this.listeners.get (responseCode);

		if (listeners == null) {
			return;
		}

		for (HttpResponseCodeListener listener : listeners) {
			listener.responseCodeOccurred ();
		}
	}
}
