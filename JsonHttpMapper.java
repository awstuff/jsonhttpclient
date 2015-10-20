package wirth.adrian.http;

import android.content.Context;
import android.support.annotation.Nullable;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * A class for executing HTTP GET and POST requests.
 * Maps returned json values to objects and objects used as request parameters to json.
 * <p/>
 * Requires Google's Gson and Volley.
 * <p/>
 * Copyright 2015 Adrian Wirth
 * Released under the MIT license
 *
 * @param <T> the type of the returned object
 */

public class JsonHttpMapper<T> {

	private Class target;    // return object class, should be equal to T's class
	private Context context;

	/**
	 * Constructor
	 *
	 * @param target the returned object's class, should be equal to the generic type's class
	 * @param context the context
	 */
	public JsonHttpMapper (Class target, Context context) {

		this.target = target;
		this.context = context;
	}

	/**
	 * Functional interface to provide a callback function for the request
	 *
	 * @param <T> The returned object's type. Probably the same as JsonHttpMapper's generic type, at least type compatible
	 */
	public interface Callback<T> {

		/**
		 * The actual callback method that gets called when the request finishes
		 *
		 * @param success boolean indicating success or failure
		 * @param result the actual returned object
		 */
		void done (boolean success, T result);
	}

	/**
	 * Sends a GET request to the specified url and returns an object of type T
	 *
	 * @param url the web resource's url
	 * @param callback callback implementation, can be null
	 */
	public void getObject (String url, final @Nullable Callback callback) {

		this.requestObject (JsonObjectRequest.Method.GET, url, null, callback);
	}

	/**
	 * Sends a GET request to the specified url and returns a list of objects of type T
	 *
	 * @param url the web resource's url
	 * @param callback callback implementation, can be null
	 */
	public void getList (String url, final @Nullable Callback callback) {

		this.requestList (JsonArrayRequest.Method.GET, url, null, callback);
	}

	/**
	 * Sends a POST request containing the specified data to the specified url and returns an object of type T
	 *
	 * @param url the web resource's url
	 * @param data object to send as POST data
	 * @param callback callback implementation, can be null
	 */
	public void postAndGetObject (String url, Object data, final @Nullable Callback callback) {

		this.requestObject (JsonObjectRequest.Method.POST, url, data, callback);
	}

	/**
	 * Sends a POST request containing the specified data to the specified url and returns a list of objects of type T
	 *
	 * @param url the web resource's url
	 * @param data object to send as POST data
	 * @param callback callback implementation, can be null
	 */
	public void postAndGetList (String url, Object data, final @Nullable Callback callback) {

		this.requestList (JsonArrayRequest.Method.POST, url, data, callback);
	}

	/**
	 * Requests an object from a web resource
	 *
	 * @param method HTTP method to use
	 * @param url the web resource's url
	 * @param data object to send as request data
	 * @param callback callback implementation, can be null
	 */
	@SuppressWarnings ("unchecked")
	private void requestObject (int method, String url, Object data, final @Nullable Callback callback) {

		final Gson gson = new Gson ();	// gson instance

		// parse the data object:
		JSONObject jsonData = null;
		if (data != null) {
			try {
				jsonData = new JSONObject (gson.toJson (data));
			} catch (JSONException e) {
				e.printStackTrace ();
				if (callback != null) {
					callback.done (false, null);
				}
			}
		}

		// the actual request:
		JsonObjectRequest req = new JsonObjectRequest (method, url, jsonData, new Response.Listener<JSONObject> () {

			@Override
			public void onResponse (JSONObject response) {

				// generate result object:
				T result = (T) gson.fromJson (response.toString (), JsonHttpMapper.this.target);

				if (result != null && callback != null) {
					callback.done (true, result);
				}
			}
		}, new Response.ErrorListener () {

			@Override
			public void onErrorResponse (VolleyError error) {

				error.printStackTrace ();
				if (callback != null) {
					callback.done (false, null);
				}
			}
		});

		// execute:
		RequestQueueProvider.getInstance (this.context).addToRequestQueue (req);
	}

	/**
	 * Requests a list of objects from a web resource
	 *
	 * @param method HTTP method to use
	 * @param url the web resource's url
	 * @param data object to send as request data
	 * @param callback callback implementation, can be null
	 */
	@SuppressWarnings ("unchecked")
	private void requestList (int method, String url, Object data, final @Nullable Callback callback) {

		final Gson gson = new Gson ();	// gson instanc

		// parse the data object:
		JSONArray jsonData = null;
		if (data != null) {
			try {
				jsonData = new JSONArray (gson.toJson (data));
			} catch (JSONException e) {
				e.printStackTrace ();
				if (callback != null) {
					callback.done (false, null);
				}
			}
		}

		// the actual request:
		JsonArrayRequest req = new JsonArrayRequest (method, url, jsonData, new Response.Listener<JSONArray> () {

			@Override
			public void onResponse (JSONArray response) {

				// an empty array in order to obtain the needed class:
				T[] emptyTypedArray = (T[]) Array.newInstance (JsonHttpMapper.this.target, 0);
				// generate result array:
				T[] result = (T[]) gson.fromJson (response.toString (), emptyTypedArray.getClass ());

				if (result != null && callback != null) {
					callback.done (true, Arrays.asList (result));
				}
			}
		}, new Response.ErrorListener () {

			@Override
			public void onErrorResponse (VolleyError error) {

				error.printStackTrace ();
				if (callback != null) {
					callback.done (false, null);
				}
			}
		});

		// execute:
		RequestQueueProvider.getInstance (this.context).addToRequestQueue (req);
	}
}
