package wirth.adrian.http;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * A singleton class that provides a RequestQueue object
 *
 * Inspired by https://developer.android.com/training/volley/requestqueue.html#singleton
 */
public class RequestQueueProvider {

	private static RequestQueueProvider instance;	// singleton instance
	private static Context context;
	private RequestQueue requestQueue;	// the requestqueue

	/**
	 * Constructor
	 *
	 * @param context the context
	 */
	private RequestQueueProvider (Context context) {

		RequestQueueProvider.context = context;
		this.requestQueue = this.getRequestQueue ();
	}

	/**
	 * Returns the singleton instance
	 *
	 * @param context the context
	 * @return the RequestQueueProvider instance
	 */
	public static synchronized RequestQueueProvider getInstance (Context context) {

		if (instance == null) {
			instance = new RequestQueueProvider (context.getApplicationContext ());
		}
		return instance;
	}

	/**
	 * Returns the RequestQueue object
	 * @return the RequestQueue object
	 */
	public RequestQueue getRequestQueue () {

		if (this.requestQueue == null) {
			this.requestQueue = Volley.newRequestQueue (context.getApplicationContext ());
		}
		return this.requestQueue;
	}

	/**
	 * Adds the specified request to the RequestQueue
	 * @param request the request to add
	 * @param <T> the request's type parameter
	 */
	public <T> void addToRequestQueue (Request<T> request) {
		this.getRequestQueue ().add (request);
	}
}
