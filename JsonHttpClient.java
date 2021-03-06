package com.adrianwirth.jsonhttpclient;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A class for executing HTTP GET and POST requests and uploading files.
 * Maps returned JSON values to objects.
 * Copyright 2017 Adrian Wirth
 * Released under the MIT license
 *
 * @param <T> the type of the returned object
 */
public class JsonHttpClient<T> {

    /**
     * The request timeout
     */
    private static final int REQUEST_TIMEOUT = 360000;

    /**
     * The name of the charset used for requests
     */
    private static final String CHARSET = "UTF-8";

    /**
     * The charset object used for requests
     */
    private static final Charset CHARSET_OBJ = Charset.forName(JsonHttpClient.CHARSET);

    /**
     * The name of the content type used for requests
     */
    private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";

    /**
     * The content type object used for requests
     */
    private static final ContentType CONTENT_TYPE_TEXT_PLAIN_OBJ = ContentType.create(JsonHttpClient.CONTENT_TYPE_TEXT_PLAIN, JsonHttpClient.CHARSET_OBJ);

    /**
     * The name of the HTTP GET method
     */
    private static final String HTTP_METHOD_GET = "GET";

    /**
     * The name of the HTTP POST method
     */
    private static final String HTTP_METHOD_POST = "POST";

    /**
     * The name of the HTTP PUT method
     */
    private static final String HTTP_METHOD_PUT = "PUT";

    /**
     * The name of the HTTP DELETE method
     */
    private static final String HTTP_METHOD_DELETE = "DELETE";

    /**
     * The supported XSSI protection prefixes that are stripped off response texts automatically
     */
    private static final String[] XSSI_PREFIXES = {
            "for(;;);",
            "while(1);",
            ")]}',\n",
            ")]}',\\n",
            "throw 1; <dont be evil>",
            "throw 1;"
    };

    /**
     * The name of the HTTPS protocol
     */
    private static final String HTTPS_PROTOCOL_NAME = "https";

    /**
     * The tag used for debug messages
     */
    private static final String DEBUG_TAG = "JsonHttpClient";

    /**
     * The response' target type
     */
    private Class<T> target;

    /**
     * Currently using debug mode
     */
    private boolean debug;

    /**
     * Constructor
     *
     * @param target the returned object's class
     */
    public JsonHttpClient (Class<T> target) {
        this(target, false);
    }

    /**
     * Constructor
     *
     * @param target the returned object's class
     * @param debug log debug messages
     */
    public JsonHttpClient (Class<T> target, boolean debug) {
        this.target = target;
        this.debug = debug;
    }

    /**
     * Functional interface to provide a callback function for the request
     *
     * @param <T> The returned object's type. Probably the same as JsonHttpClient's generic type
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
     * Functional interface to provide a callback function for internal use.
     * Instead of returning the parsed object, this callback receives the raw HTTP response text
     */
    private interface InternalCallback {

        /**
         * The actual callback method that gets called when the request finishes
         *
         * @param success boolean indicating success or failure
         * @param responseText the HTTP response text
         */
        void done (boolean success, String responseText);
    }

    /**
     * Send a GET request to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param callback callback implementation, can be null
     */
    public void getObject (@NonNull String url, final @Nullable Callback<T> callback) {

        this.getObject(url, null, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Send a GET request to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void getObject (@NonNull String url, int expectedResponseCode, final @Nullable Callback<T> callback) {

        this.getObject(url, null, expectedResponseCode, callback);
    }

    /**
     * Send a GET request to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param headers map to send as additional HTTP headers
     * @param callback callback implementation, can be null
     */
    public void getObject (@NonNull String url, @Nullable Map<String, String> headers, final @Nullable Callback<T> callback) {

        this.getObject(url, headers, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Send a GET request to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void getObject (@NonNull String url, @Nullable Map<String, String> headers, int expectedResponseCode, final @Nullable Callback<T> callback) {

        this.requestObject(JsonHttpClient.HTTP_METHOD_GET, url, null, headers, expectedResponseCode, callback);
    }

    /**
     * Send a GET request to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param callback callback implementation, can be null
     */
    public void getList (@NonNull String url, final @Nullable Callback<List<T>> callback) {

        this.getList(url, null, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Send a GET request to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void getList (@NonNull String url, int expectedResponseCode, final @Nullable Callback<List<T>> callback) {

        this.getList(url, null, expectedResponseCode, callback);
    }

    /**
     * Send a GET request to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param headers map to send as additional HTTP headers
     * @param callback callback implementation, can be null
     */
    public void getList (@NonNull String url, @Nullable Map<String, String> headers, final @Nullable Callback<List<T>> callback) {

        this.getList(url, headers, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Send a GET request to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void getList (@NonNull String url, @Nullable Map<String, String> headers, int expectedResponseCode, final @Nullable Callback<List<T>> callback) {

        this.requestList(JsonHttpClient.HTTP_METHOD_GET, url, null, headers, expectedResponseCode, callback);
    }

    /**
     * Send a DELETE request to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param callback callback implementation, can be null
     */
    public void deleteAndGetObject (@NonNull String url, final @Nullable Callback<T> callback) {

        this.deleteAndGetObject(url, null, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Send a DELETE request to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void deleteAndGetObject (@NonNull String url, int expectedResponseCode, final @Nullable Callback<T> callback) {

        this.deleteAndGetObject(url, null, expectedResponseCode, callback);
    }

    /**
     * Send a DELETE request to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param headers map to send as additional HTTP headers
     * @param callback callback implementation, can be null
     */
    public void deleteAndGetObject (@NonNull String url, @Nullable Map<String, String> headers, final @Nullable Callback<T> callback) {

        this.deleteAndGetObject(url, headers, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Send a DELETE request to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void deleteAndGetObject (@NonNull String url, @Nullable Map<String, String> headers, int expectedResponseCode, final @Nullable Callback<T> callback) {

        this.requestObject(JsonHttpClient.HTTP_METHOD_DELETE, url, null, headers, expectedResponseCode, callback);
    }

    /**
     * Send a DELETE request to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param callback callback implementation, can be null
     */
    public void deleteAndGetList (@NonNull String url, final @Nullable Callback<List<T>> callback) {

        this.deleteAndGetList(url, null, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Send a DELETE request to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void deleteAndGetList (@NonNull String url, int expectedResponseCode, final @Nullable Callback<List<T>> callback) {

        this.deleteAndGetList(url, null, expectedResponseCode, callback);
    }

    /**
     * Send a DELETE request to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param headers map to send as additional HTTP headers
     * @param callback callback implementation, can be null
     */
    public void deleteAndGetList (@NonNull String url, @Nullable Map<String, String> headers, final @Nullable Callback<List<T>> callback) {

        this.deleteAndGetList(url, headers, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Send a DELETE request to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void deleteAndGetList (@NonNull String url, @Nullable Map<String, String> headers, int expectedResponseCode, final @Nullable Callback<List<T>> callback) {

        this.requestList(JsonHttpClient.HTTP_METHOD_DELETE, url, null, headers, expectedResponseCode, callback);
    }

    /**
     * Send a POST request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param callback callback implementation, can be null
     */
    public void postAndGetObject (@NonNull String url, @Nullable Map<String, String> payload, final @Nullable Callback<T> callback) {

        this.postAndGetObject(url, payload, null, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Send a POST request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void postAndGetObject (@NonNull String url, @Nullable Map<String, String> payload, int expectedResponseCode, final @Nullable Callback<T> callback) {

        this.postAndGetObject(url, payload, null, expectedResponseCode, callback);
    }

    /**
     * Send a POST request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param callback callback implementation, can be null
     */
    public void postAndGetObject (@NonNull String url, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, final @Nullable Callback<T> callback) {

        this.postAndGetObject(url, payload, headers, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Send a POST request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void postAndGetObject (@NonNull String url, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, int expectedResponseCode, final @Nullable Callback<T> callback) {

        this.requestObject(JsonHttpClient.HTTP_METHOD_POST, url, payload, headers, expectedResponseCode, callback);
    }

    /**
     * Send a POST request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param callback callback implementation, can be null
     */
    public void postAndGetList (@NonNull String url, @Nullable Map<String, String> payload, final @Nullable Callback<List<T>> callback) {

        this.postAndGetList(url, payload, null, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Send a POST request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void postAndGetList (@NonNull String url, @Nullable Map<String, String> payload, int expectedResponseCode, final @Nullable Callback<List<T>> callback) {

        this.postAndGetList(url, payload, null, expectedResponseCode, callback);
    }

    /**
     * Send a POST request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param callback callback implementation, can be null
     */
    public void postAndGetList (@NonNull String url, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, final @Nullable Callback<List<T>> callback) {

        this.postAndGetList(url, payload, headers, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Send a POST request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void postAndGetList (@NonNull String url, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, int expectedResponseCode, final @Nullable Callback<List<T>> callback) {

        this.requestList(JsonHttpClient.HTTP_METHOD_POST, url, payload, headers, expectedResponseCode, callback);
    }

    /**
     * Send a PUT request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param callback callback implementation, can be null
     */
    public void putAndGetObject (@NonNull String url, @Nullable Map<String, String> payload, final @Nullable Callback<T> callback) {

        this.putAndGetObject(url, payload, null, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Send a PUT request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void putAndGetObject (@NonNull String url, @Nullable Map<String, String> payload, int expectedResponseCode, final @Nullable Callback<T> callback) {

        this.putAndGetObject(url, payload, null, expectedResponseCode, callback);
    }

    /**
     * Send a PUT request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param callback callback implementation, can be null
     */
    public void putAndGetObject (@NonNull String url, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, final @Nullable Callback<T> callback) {

        this.putAndGetObject(url, payload, headers, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Send a PUT request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void putAndGetObject (@NonNull String url, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, int expectedResponseCode, final @Nullable Callback<T> callback) {

        this.requestObject(JsonHttpClient.HTTP_METHOD_PUT, url, payload, headers, expectedResponseCode, callback);
    }

    /**
     * Send a PUT request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param callback callback implementation, can be null
     */
    public void putAndGetList (@NonNull String url, @Nullable Map<String, String> payload, final @Nullable Callback<List<T>> callback) {

        this.putAndGetList(url, payload, null, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Send a PUT request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void putAndGetList (@NonNull String url, @Nullable Map<String, String> payload, int expectedResponseCode, final @Nullable Callback<List<T>> callback) {

        this.putAndGetList(url, payload, null, expectedResponseCode, callback);
    }

    /**
     * Send a PUT request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param callback callback implementation, can be null
     */
    public void putAndGetList (@NonNull String url, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, final @Nullable Callback<List<T>> callback) {

        this.putAndGetList(url, payload, headers, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Send a PUT request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void putAndGetList (@NonNull String url, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, int expectedResponseCode, final @Nullable Callback<List<T>> callback) {

        this.requestList(JsonHttpClient.HTTP_METHOD_PUT, url, payload, headers, expectedResponseCode, callback);
    }

    /**
     * Upload the specified file as part of a POST request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPostAndGetObject (@NonNull String url, @NonNull File file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, final @Nullable Callback<T> callback) {

        this.uploadFileUsingPostAndGetObject(url, file, fileFieldName, payload, null, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Upload the specified file as part of a POST request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPostAndGetObject (@NonNull String url, @NonNull File file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, int expectedResponseCode, final @Nullable Callback<T> callback) {

        this.uploadFileUsingPostAndGetObject(url, file, fileFieldName, payload, null, expectedResponseCode, callback);
    }

    /**
     * Upload the specified file as part of a POST request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPostAndGetObject (@NonNull String url, @NonNull File file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, final @Nullable Callback<T> callback) {

        this.uploadFileUsingPostAndGetObject(url, file, fileFieldName, payload, headers, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Upload the specified file as part of a POST request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPostAndGetObject (@NonNull String url, @NonNull File file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, int expectedResponseCode, final @Nullable Callback<T> callback) {

        this.uploadAndRequestObject(JsonHttpClient.HTTP_METHOD_POST, url, file, fileFieldName, payload, headers, expectedResponseCode, callback);
    }

    /**
     * Upload the specified file as part of a POST request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPostAndGetObject (@NonNull String url, @NonNull byte[] file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, final @Nullable Callback<T> callback) {

        this.uploadFileUsingPostAndGetObject(url, file, fileFieldName, payload, null, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Upload the specified file as part of a POST request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPostAndGetObject (@NonNull String url, @NonNull byte[] file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, int expectedResponseCode, final @Nullable Callback<T> callback) {

        this.uploadFileUsingPostAndGetObject(url, file, fileFieldName, payload, null, expectedResponseCode, callback);
    }

    /**
     * Upload the specified file as part of a POST request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPostAndGetObject (@NonNull String url, @NonNull byte[] file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, final @Nullable Callback<T> callback) {

        this.uploadFileUsingPostAndGetObject(url, file, fileFieldName, payload, headers, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Upload the specified file as part of a POST request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPostAndGetObject (@NonNull String url, @NonNull byte[] file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, int expectedResponseCode, final @Nullable Callback<T> callback) {

        this.uploadAndRequestObject(JsonHttpClient.HTTP_METHOD_POST, url, file, fileFieldName, payload, headers, expectedResponseCode, callback);
    }

    /**
     * Upload the specified file as part of a POST request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPostAndGetList (@NonNull String url, @NonNull File file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, final @Nullable Callback<List<T>> callback) {

        this.uploadFileUsingPostAndGetList(url, file, fileFieldName, payload, null, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Upload the specified file as part of a POST request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPostAndGetList (@NonNull String url, @NonNull File file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, int expectedResponseCode, final @Nullable Callback<List<T>> callback) {

        this.uploadFileUsingPostAndGetList(url, file, fileFieldName, payload, null, expectedResponseCode, callback);
    }

    /**
     * Upload the specified file as part of a POST request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPostAndGetList (@NonNull String url, @NonNull File file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, final @Nullable Callback<List<T>> callback) {

        this.uploadFileUsingPostAndGetList(url, file, fileFieldName, payload, headers, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Upload the specified file as part of a POST request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPostAndGetList (@NonNull String url, @NonNull File file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, int expectedResponseCode, final @Nullable Callback<List<T>> callback) {

        this.uploadAndRequestList(JsonHttpClient.HTTP_METHOD_POST, url, file, fileFieldName, payload, headers, expectedResponseCode, callback);
    }

    /**
     * Upload the specified file as part of a POST request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPostAndGetList (@NonNull String url, @NonNull byte[] file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, final @Nullable Callback<List<T>> callback) {

        this.uploadFileUsingPostAndGetList(url, file, fileFieldName, payload, null, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Upload the specified file as part of a POST request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPostAndGetList (@NonNull String url, @NonNull byte[] file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, int expectedResponseCode, final @Nullable Callback<List<T>> callback) {

        this.uploadFileUsingPostAndGetList(url, file, fileFieldName, payload, null, expectedResponseCode, callback);
    }

    /**
     * Upload the specified file as part of a POST request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPostAndGetList (@NonNull String url, @NonNull byte[] file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, final @Nullable Callback<List<T>> callback) {

        this.uploadFileUsingPostAndGetList(url, file, fileFieldName, payload, headers, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Upload the specified file as part of a POST request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPostAndGetList (@NonNull String url, @NonNull byte[] file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, int expectedResponseCode, final @Nullable Callback<List<T>> callback) {

        this.uploadAndRequestList(JsonHttpClient.HTTP_METHOD_POST, url, file, fileFieldName, payload, headers, expectedResponseCode, callback);
    }

    /**
     * Upload the specified file as part of a PUT request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPutAndGetObject (@NonNull String url, @NonNull File file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, final @Nullable Callback<T> callback) {

        this.uploadFileUsingPutAndGetObject(url, file, fileFieldName, payload, null, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Upload the specified file as part of a PUT request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPutAndGetObject (@NonNull String url, @NonNull File file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, int expectedResponseCode, final @Nullable Callback<T> callback) {

        this.uploadFileUsingPutAndGetObject(url, file, fileFieldName, payload, null, expectedResponseCode, callback);
    }

    /**
     * Upload the specified file as part of a PUT request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPutAndGetObject (@NonNull String url, @NonNull File file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, final @Nullable Callback<T> callback) {

        this.uploadFileUsingPutAndGetObject(url, file, fileFieldName, payload, headers, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Upload the specified file as part of a PUT request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPutAndGetObject (@NonNull String url, @NonNull File file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, int expectedResponseCode, final @Nullable Callback<T> callback) {

        this.uploadAndRequestObject(JsonHttpClient.HTTP_METHOD_PUT, url, file, fileFieldName, payload, headers, expectedResponseCode, callback);
    }

    /**
     * Upload the specified file as part of a PUT request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPutAndGetObject (@NonNull String url, @NonNull byte[] file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, final @Nullable Callback<T> callback) {

        this.uploadFileUsingPutAndGetObject(url, file, fileFieldName, payload, null, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Upload the specified file as part of a PUT request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPutAndGetObject (@NonNull String url, @NonNull byte[] file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, int expectedResponseCode, final @Nullable Callback<T> callback) {

        this.uploadFileUsingPutAndGetObject(url, file, fileFieldName, payload, null, expectedResponseCode, callback);
    }

    /**
     * Upload the specified file as part of a PUT request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPutAndGetObject (@NonNull String url, @NonNull byte[] file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, final @Nullable Callback<T> callback) {

        this.uploadFileUsingPutAndGetObject(url, file, fileFieldName, payload, headers, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Upload the specified file as part of a PUT request containing the specified data to the specified url and receive an object of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPutAndGetObject (@NonNull String url, @NonNull byte[] file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, int expectedResponseCode, final @Nullable Callback<T> callback) {

        this.uploadAndRequestObject(JsonHttpClient.HTTP_METHOD_PUT, url, file, fileFieldName, payload, headers, expectedResponseCode, callback);
    }

    /**
     * Upload the specified file as part of a PUT request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPutAndGetList (@NonNull String url, @NonNull File file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, final @Nullable Callback<List<T>> callback) {

        this.uploadFileUsingPutAndGetList(url, file, fileFieldName, payload, null, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Upload the specified file as part of a PUT request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPutAndGetList (@NonNull String url, @NonNull File file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, int expectedResponseCode, final @Nullable Callback<List<T>> callback) {

        this.uploadFileUsingPutAndGetList(url, file, fileFieldName, payload, null, expectedResponseCode, callback);
    }

    /**
     * Upload the specified file as part of a PUT request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPutAndGetList (@NonNull String url, @NonNull File file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, final @Nullable Callback<List<T>> callback) {

        this.uploadFileUsingPutAndGetList(url, file, fileFieldName, payload, headers, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Upload the specified file as part of a PUT request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPutAndGetList (@NonNull String url, @NonNull File file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, int expectedResponseCode, final @Nullable Callback<List<T>> callback) {

        this.uploadAndRequestList(JsonHttpClient.HTTP_METHOD_PUT, url, file, fileFieldName, payload, headers, expectedResponseCode, callback);
    }

    /**
     * Upload the specified file as part of a PUT request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPutAndGetList (@NonNull String url, @NonNull byte[] file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, final @Nullable Callback<List<T>> callback) {

        this.uploadFileUsingPutAndGetList(url, file, fileFieldName, payload, null, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Upload the specified file as part of a PUT request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPutAndGetList (@NonNull String url, @NonNull byte[] file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, int expectedResponseCode, final @Nullable Callback<List<T>> callback) {

        this.uploadFileUsingPutAndGetList(url, file, fileFieldName, payload, null, expectedResponseCode, callback);
    }

    /**
     * Upload the specified file as part of a PUT request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPutAndGetList (@NonNull String url, @NonNull byte[] file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, final @Nullable Callback<List<T>> callback) {

        this.uploadFileUsingPutAndGetList(url, file, fileFieldName, payload, headers, HttpURLConnection.HTTP_OK, callback);
    }

    /**
     * Upload the specified file as part of a PUT request containing the specified data to the specified url and receive a list of objects of type T
     *
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    public void uploadFileUsingPutAndGetList (@NonNull String url, @NonNull byte[] file, @NonNull String fileFieldName, @Nullable Map<String, String> payload, @Nullable Map<String, String> headers, int expectedResponseCode, final @Nullable Callback<List<T>> callback) {

        this.uploadAndRequestList(JsonHttpClient.HTTP_METHOD_PUT, url, file, fileFieldName, payload, headers, expectedResponseCode, callback);
    }

    /**
     * Request an object from a web resource, optionally sending the specified payload
     *
     * @param method HTTP method to use
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    private void requestObject (String method, String url, Map<String, String> payload, Map<String, String> headers, int expectedResponseCode, final Callback<T> callback) {

        this.requestRaw(method, url, payload, headers, expectedResponseCode, new InternalCallback() {

            @Override
            public void done (boolean success, String responseText) {
                JsonHttpClient.this.parseObject(success, responseText, callback);
            }
        });
    }

    /**
     * Upload a file to a web resource and request an object, optionally sending the specified payload
     *
     * @param method HTTP method to use
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    private void uploadAndRequestObject (String method, String url, File file, String fileFieldName, Map<String, String> payload, Map<String, String> headers, int expectedResponseCode, final Callback<T> callback) {

        this.uploadRequestRaw(method, url, file, null, fileFieldName, payload, headers, expectedResponseCode, new InternalCallback() {

            @Override
            public void done (boolean success, String responseText) {
                JsonHttpClient.this.parseObject(success, responseText, callback);
            }
        });
    }

    /**
     * Upload a file to a web resource and request an object, optionally sending the specified payload
     *
     * @param method HTTP method to use
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    private void uploadAndRequestObject (String method, String url, byte[] file, String fileFieldName, Map<String, String> payload, Map<String, String> headers, int expectedResponseCode, final Callback<T> callback) {

        this.uploadRequestRaw(method, url, null, file, fileFieldName, payload, headers, expectedResponseCode, new InternalCallback() {

            @Override
            public void done (boolean success, String responseText) {
                JsonHttpClient.this.parseObject(success, responseText, callback);
            }
        });
    }

    /**
     * Request a list of objects from a web resource, optionally sending the specified payload
     *
     * @param method HTTP method to use
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    private void requestList (String method, String url, Map<String, String> payload, Map<String, String> headers, int expectedResponseCode, final Callback<List<T>> callback) {

        this.requestRaw(method, url, payload, headers, expectedResponseCode, new InternalCallback() {

            @Override
            public void done (boolean success, String responseText) {
                JsonHttpClient.this.parseList(success, responseText, callback);
            }
        });
    }

    /**
     * Upload a file to a web resource and request a list of objects, optionally sending the specified payload
     *
     * @param method HTTP method to use
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    private void uploadAndRequestList (String method, String url, File file, String fileFieldName, Map<String, String> payload, Map<String, String> headers, int expectedResponseCode, final Callback<java.util.List<T>> callback) {

        this.uploadRequestRaw(method, url, file, null, fileFieldName, payload, headers, expectedResponseCode, new InternalCallback() {

            @Override
            public void done (boolean success, String responseText) {
                JsonHttpClient.this.parseList(success, responseText, callback);
            }
        });
    }

    /**
     * Upload a file to a web resource and request a list of objects, optionally sending the specified payload
     *
     * @param method HTTP method to use
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    private void uploadAndRequestList (String method, String url, byte[] file, String fileFieldName, Map<String, String> payload, Map<String, String> headers, int expectedResponseCode, final Callback<java.util.List<T>> callback) {

        this.uploadRequestRaw(method, url, null, file, fileFieldName, payload, headers, expectedResponseCode, new InternalCallback() {

            @Override
            public void done (boolean success, String responseText) {
                JsonHttpClient.this.parseList(success, responseText, callback);
            }
        });
    }

    /**
     * Parse the specified HTTP response text to an object of type T
     *
     * @param success boolean indicating success or failure
     * @param responseText the HTTP response text
     * @param callback callback implementation, can be null
     */
    private void parseObject (boolean success, String responseText, Callback<T> callback) {
        if (callback != null) {
            if (!success) {
                callback.done(false, null);
                return;
            }

            Gson gson = new Gson();

            T result = gson.fromJson(responseText, JsonHttpClient.this.target);

            callback.done(true, result);
        }
    }

    /**
     * Parse the specified HTTP response text to a list of objects of type T
     *
     * @param success boolean indicating success or failure
     * @param responseText the HTTP response text
     * @param callback callback implementation, can be null
     */
    @SuppressWarnings("unchecked")
    private void parseList (boolean success, String responseText, Callback<List<T>> callback) {
        if (callback != null) {
            if (!success) {
                callback.done(false, null);
                return;
            }

            Gson gson = new Gson();

            // an empty array in order to obtain the needed class:
            T[] emptyTypedArray = (T[]) Array.newInstance(JsonHttpClient.this.target, 0);
            // generate result array:
            T[] result = (T[]) gson.fromJson(responseText, emptyTypedArray.getClass());

            callback.done(true, Arrays.asList(result));
        }
    }

    /**
     * The actual HTTP request
     *
     * @param method HTTP method to use
     * @param url the web resource's url
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    private void requestRaw (final String method, final String url, final Map<String, String> payload, final Map<String, String> headers, final int expectedResponseCode, final InternalCallback callback) {

        class RequestTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground (Void... params) {

                URLConnection connection = null;
                boolean useHttps = false;

                try {
                    URL urlObj = new URL(url);

                    connection = JsonHttpClient.this.prepareConnection(urlObj);

                    connection = JsonHttpClient.this.appendHeaders(connection, headers);

                    ((HttpURLConnection) connection).setRequestMethod(method);

                    if (payload != null) {    // append the data payload
                        Uri.Builder builder = new Uri.Builder();

                        for (Map.Entry<String, String> entry : payload.entrySet()) {
                            builder.appendQueryParameter(entry.getKey(), entry.getValue());
                        }

                        String query = builder.build().getEncodedQuery();

                        connection.setDoOutput(true);
                        OutputStream os = connection.getOutputStream();
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, JsonHttpClient.CHARSET));
                        writer.write(query);
                        writer.flush();
                        writer.close();
                        os.close();
                    }

                    useHttps = JsonHttpClient.this.urlUsesHttps(urlObj);

                    JsonHttpClient.this.connectAndFetchResponse(connection, useHttps, expectedResponseCode, callback);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    if (callback != null) {
                        callback.done(false, null);
                    }
                } finally {
                    if (connection != null) {
                        try {
                            if (useHttps) {
                                ((HttpsURLConnection) connection).disconnect();
                            } else {
                                ((HttpURLConnection) connection).disconnect();
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            if (callback != null) {
                                callback.done(false, null);
                            }
                        }
                    }
                }

                return null;
            }
        }

        new RequestTask().execute();
    }

    /**
     * The actual HTTP request to upload the specified file
     *
     * @param method HTTP method to use
     * @param url the web resource's url
     * @param file the file to upload
     * @param fileAsByteArray the file to upload as byte array. Replaces the parameter file if set.
     * @param fileFieldName the name under which the file can be accessed by the web resource
     * @param payload map to send as request data
     * @param headers map to send as additional HTTP headers
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     */
    private void uploadRequestRaw (final String method, final String url, final File file, final byte[] fileAsByteArray, final String fileFieldName, final Map<String, String> payload, final Map<String, String> headers, final int expectedResponseCode, final InternalCallback callback) {

        class UploadRequestTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground (Void... params) {

                URLConnection connection = null;
                boolean useHttps = false;

                try {
                    MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
                    entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

                    if (fileAsByteArray != null) {    // byte array is priority 1
                        entityBuilder.addPart(fileFieldName, new ByteArrayBody(fileAsByteArray, fileFieldName));
                    } else {
                        entityBuilder.addPart(fileFieldName, new FileBody(file));
                    }

                    URL urlObj = new URL(url);

                    connection = JsonHttpClient.this.prepareConnection(urlObj);

                    ((HttpURLConnection) connection).setRequestMethod(method);

                    connection.setDoOutput(true);

                    if (payload != null) {    // append the data payload
                        for (Map.Entry<String, String> entry : payload.entrySet()) {
                            entityBuilder.addPart(entry.getKey(), new StringBody(entry.getValue(), JsonHttpClient.CONTENT_TYPE_TEXT_PLAIN_OBJ));
                        }
                    }

                    HttpEntity entity = entityBuilder.build();

                    connection.setRequestProperty("Connection", "Keep-Alive");
                    connection.setRequestProperty("Content-Length", "" + entity.getContentLength());
                    connection.setRequestProperty(entity.getContentType().getName(), entity.getContentType().getValue());

                    connection = JsonHttpClient.this.appendHeaders(connection, headers);

                    OutputStream os = connection.getOutputStream();
                    entity.writeTo(os);    // append the file
                    os.close();

                    useHttps = JsonHttpClient.this.urlUsesHttps(urlObj);

                    JsonHttpClient.this.connectAndFetchResponse(connection, useHttps, expectedResponseCode, callback);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    if (callback != null) {
                        callback.done(false, null);
                    }
                } finally {
                    if (connection != null) {
                        try {
                            if (useHttps) {
                                ((HttpsURLConnection) connection).disconnect();
                            } else {
                                ((HttpURLConnection) connection).disconnect();
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            if (callback != null) {
                                callback.done(false, null);
                            }
                        }
                    }
                }

                return null;
            }
        }

        new UploadRequestTask().execute();
    }

    /**
     * Determine whether the specified URL's protocol is HTTPS
     *
     * @param url the url to test
     * @return boolean indicating whether the specified URL's protocol is HTTPS or not
     */
    private boolean urlUsesHttps (URL url) {
        return url != null && JsonHttpClient.HTTPS_PROTOCOL_NAME.equals(url.getProtocol());
    }

    /**
     * Prepare a URLConnection for further use by constructing it and setting basic properties
     *
     * @param url the url to use as a basis
     * @return the prepared URLConnection
     * @throws IOException
     */
    private URLConnection prepareConnection (URL url) throws IOException {
        URLConnection connection = url.openConnection();

        connection.setUseCaches(false);
        connection.setConnectTimeout(JsonHttpClient.REQUEST_TIMEOUT);
        connection.setReadTimeout(JsonHttpClient.REQUEST_TIMEOUT);

        return connection;
    }

    /**
     * Connect the specified URLConnection and fetch the resulting response
     *
     * @param connection the ready URLConnection
     * @param useHttps boolean indicating whether to use HTTPS or HTTP
     * @param expectedResponseCode the expected HTTP response code
     * @param callback callback implementation, can be null
     * @throws IOException
     */
    private void connectAndFetchResponse (URLConnection connection, boolean useHttps, int expectedResponseCode, InternalCallback callback) throws IOException {
        int statusCode;
        InputStream responseInputStream = null;
        boolean responseIsEmpty = false;

        if (useHttps) {
            HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;

            httpsConnection.connect();
            statusCode = httpsConnection.getResponseCode();

            try {
                responseInputStream = httpsConnection.getInputStream();
            } catch (FileNotFoundException ex) {    // response is empty
                responseIsEmpty = true;
            }
        } else {
            HttpURLConnection httpConnection = (HttpURLConnection) connection;

            httpConnection.connect();
            statusCode = httpConnection.getResponseCode();

            try {
                responseInputStream = httpConnection.getInputStream();
            } catch (FileNotFoundException ex) {    // response is empty
                responseIsEmpty = true;
            }
        }

        if (this.debug) {
            Log.d(JsonHttpClient.DEBUG_TAG, "Response Code: " + statusCode);
        }

        HttpResponseCodeManager manager = HttpResponseCodeManager.getInstance();
        manager.emitResponseCode(statusCode);

        if (statusCode != expectedResponseCode) {
            if (callback != null) {
                callback.done(false, null);
            }
            return;
        }

        String responseText;

        if (responseIsEmpty) {
            responseText = "";
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(responseInputStream));

            StringBuilder stringBuilder = new StringBuilder();
            String currentLine;

            while ((currentLine = reader.readLine()) != null) {
                stringBuilder.append(currentLine);
                stringBuilder.append("\n");
            }

            reader.close();

            responseText = stringBuilder.toString();
        }

        if (this.debug) {
            Log.d(JsonHttpClient.DEBUG_TAG, "Raw response text: " + responseText);
        }

        if (!responseIsEmpty) {
            for (String prefix : JsonHttpClient.XSSI_PREFIXES) {    // strip possible xssi protection prefix
                if (responseText.startsWith(prefix)) {
                    responseText = responseText.substring(prefix.length());
                    break;
                }
            }
        }

        if (callback != null) {
            callback.done(true, responseText);
        }
    }

    /**
     * Append the specified HTTP headers to the specified URLConnection
     *
     * @param connection the URLConnection
     * @param headers map to use as additional HTTP headers
     * @return the URLConnection with extended headers
     */
    private URLConnection appendHeaders (URLConnection connection, Map<String, String> headers) {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        return connection;
    }
}
