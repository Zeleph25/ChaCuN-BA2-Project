package ch.epfl.chacun.extensions.backend;

import ch.epfl.chacun.extensions.json.JSONArray;
import ch.epfl.chacun.extensions.json.JSONObject;
import ch.epfl.chacun.extensions.json.JSONValue;

/**
 * Used to represent the response of a request to the backend
 * @param body The body of the response
 * @param statusCode The status code of the response
 * @author Adam Bekkar (379476)
 */
public record Response(String body, int statusCode, boolean acceptNullBody) {
    public Response(String body, int statusCode) {
        this(body, statusCode, false);
    }

    public Response(JSONObject json, int statusCode) {
        this(json.toString(), statusCode);
    }

    /**
     * Used to check if the request was successful or not
     * @return True if the request was successful, False otherwise
     */
    public boolean isSuccess() {
        return (acceptNullBody && body.equals("null") && 200 <= statusCode && statusCode < 300) ||
                (!body.equals("null") && 200 <= statusCode && statusCode < 300);
    }

    /**
     * Used to get the error message of the response
     * @return The error message
     */
    public String errorMessage() {
        return JSONObject.parse(body).get("error").asString();
    }

    /**
     * Used to parse the body of the response to a JSONValue
     * @return The parsed body or a new JSONValue
     */
    public JSONValue jsonValue() {
        return isSuccess() ? JSONValue.parse(body) : new JSONValue();
    }

    /**
     * Used to parse the body of the response to a JSONObject
     * @return The parsed body or a new JSONObject
     */
    public JSONArray jsonArray() {
        return isSuccess() ? JSONArray.parse(body) : new JSONArray();
    }
    /**
     * Used to parse the body of the response to a JSONObject
     * @return The parsed body or a new JSONObject
     */
    public JSONObject jsonObject() {
        return isSuccess() ? JSONObject.parse(body) : new JSONObject();
    }
}
