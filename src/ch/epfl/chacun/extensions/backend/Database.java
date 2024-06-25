package ch.epfl.chacun.extensions.backend;

import ch.epfl.chacun.extensions.json.JSONValue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Represents the HTTP requests to the ChaCuN firebase database
 * @author Antoine Bastide (375407)
 */
public class Database {
    /** The base URL of the ChaCuN server */
    private static final String URL = "https://chacun-d3c72-default-rtdb.europe-west1.firebasedatabase.app/";
    /** The extension of the URL */
    private static final String EXTENSION = "/.json";

    /** Private constructor to prevent instantiation */
    protected Database() {}

    /**
     * Used to send a GET request to the given URL
     * @param path The path of the json to get in the database
     * @return The response body and status code of the GET request
     */
    public static Response get(String path) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL + path + EXTENSION))
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return new Response(response.body(), response.statusCode());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Used to send a POST request to the given URL
     * @param path The path of the json to post in the database
     * @param data The json to post
     */
    public static Response put(String path, JSONValue data) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL + path + EXTENSION))
                    .PUT(HttpRequest.BodyPublishers.ofString(data.toString()))
                    .header("Content-Type", "application/json")
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return new Response(response.body(), response.statusCode());
            }  catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Used to send a DELETE request to the given URL
     * @param path The path of the json to delete in the database
     */
    public static Response delete(String path) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL + path + EXTENSION))
                    .DELETE()
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return new Response(response.body(), response.statusCode(), true);
            }  catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
