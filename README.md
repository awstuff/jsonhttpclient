# JsonHttpMapper

## Introduction

A Java class for executing HTTP GET and POST requests via Android.
Maps returned json values to objects and objects used as request parameters to json.

Requires Google's Gson (https://github.com/google/gson) and Volley (https://android.googlesource.com/platform/frameworks/volley).

In order to execute requests, a `RequestQueue` (https://developer.android.com/training/volley/requestqueue.html) is needed. To fulfill that requirement, `RequestQueueProvider.java` was created, but can be replaced by a similar solution (`JsonHttpMapper.java` has to be edited in this case).

## Usage

`JsonHttpMapper` provides four methods:
- `getObject` sends a GET request to the specified url and returns an object
- `getList` sends a GET request to the specified url and returns a list of objects
- `postAndGetObject` sends a POST request containing the specified data to the specified url and returns an object
- `postAndGetList` sends a POST request containing the specified data to the specified url and returns a list of objects

Example:

```java
JsonHttpMapper<MyObject> j = new JsonHttpMapper<> (MyObject.class, Start.this);

j.postAndGetObject ("http://sample.url", new OtherObject ("postdata1", "postdata2"), new JsonHttpMapper.Callback<MyObject> () {
	@Override
	public void done (boolean success, MyObject result) {
		Log.d ("success", Boolean.toString (success));
		Log.d ("result", result.toString());
	}
});

j.getList ("http://some.other.url", new JsonHttpMapper.Callback<List<BlaObject>> () {

	@Override
	public void done (boolean success, List<BlaObject> result) {
		// process list
	}
});
```
