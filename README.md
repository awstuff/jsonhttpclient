# JsonHttpClient - An easy-to-use, fully featured, response-parsing HTTP client for Android

THIS README IS STILL WORK IN PROGRESS

## Introduction

This library consists of two Java classes: `JsonHttpClient` for executing HTTP(S) requests via Android and parsing the response into native Java objects, and `HttpResponseCodeManager` for registering event listeners that are called if a request returns with a specific response code.

`JsonHttpClient`supports HTTPS, including SNI (https://en.wikipedia.org/wiki/Server_Name_Indication), and automatically detects whether to use HTTPS or plain HTTP.

You could use `HttpResponseCodeManager` individually (although that would not make much sense). `JsonHttpClient` cannot be used individually, since it depends on `HttpResponseCodeManager`.

This library requires Google's Gson (https://github.com/google/gson) and Apache's HttpComponents (https://hc.apache.org/). You can simply add these dependencies to your project's `build.gradle` file.

## Usage of `JsonHttpClient`

### Features

`JsonHttpClient` provides the following methods:

- `getObject` to send a GET request to the specified url and receive a Java object

- `getList` to send a GET request to the specified url and receive a list of Java objects

- `postAndGetObject` to send a POST request to the specified url, submitting the specified data, and receive a Java object

- `postAndGetList` to send a POST request to the specified url, submitting the specified data, and receive a list of Java objects

- `putAndGetObject` to send a PUT request to the specified url, submitting the specified data, and receive a Java object

- `putAndGetList` to send a PUT request to the specified url, submitting the specified data, and receive a list of Java objects

- `deleteAndGetObject` to send a DELETE request to the specified url and receive a Java object

- `deleteAndGetList` to send a DELETE request to the specified url and receive a list of Java objects

- `uploadFileUsingPostAndGetObject` to upload a file to the specified url via a POST request, submitting the specified data, and receive a Java object

- `uploadFileUsingPostAndGetList` to upload a file to the specified url via a POST request, submitting the specified data, and receive a list of Java objects

- `uploadFileUsingPutAndGetObject` to upload a file to the specified url via a PUT request, submitting the specified data, and receive a Java object

- `uploadFileUsingPutAndGetList` to upload a file to the specified url via a PUT request, submitting the specified data, and receive a list of Java objects

### Examples

Execute a GET request and receive an object of `MyClass` as a response:

```java
// JsonHttpClient<MyClass> client = new JsonHttpClient<> (MyClass.class);
//
// client.postAndGetObject ("http://sample.url", new OtherObject ("postdata1", "postdata2"), new JsonHttpMapper.Callback<MyObject> () {
// 	@Override
// 	public void done (boolean success, MyObject result) {
// 		Log.d ("success", Boolean.toString (success));
// 		Log.d ("result", result.toString());
// 	}
// });
//
// client.getList ("http://some.other.url", new JsonHttpMapper.Callback<List<MyObject>> () {
// 	@Override
// 	public void done (boolean success, List<MyObject> result) {
// 		// process list
// 	}
// });
```

Execute a POST request with some data and additional headers (both could also just be `null`) via HTTPS and receive a list of `MyClass` objects as a response:

```java

```

Upload a file along with some data (could also just be `null`) using a PUT request and receive an object of `MyClass` as a response:

```java

```

## Usage of `HttpResponseCodeManager`

Say you want to execute a specific action every time a HTTP request by `JsonHttpClient` returns with a specific response code. For example, you might want to switch to a special activity every time the response code `403 Forbidden` is received.

`HttpResponseCodeManager` allows you to do this by registering event listeners for response codes. It is really simple to use, just take a look at this example:

```java
HttpResponseCodeManager manager = HttpResponseCodeManager.getInstance();

// blabla
```
