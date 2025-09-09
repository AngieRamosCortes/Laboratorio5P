package rcg;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a simple HTTP response with status, headers and body.
 * Provides helpers to build common responses in plain text or JSON.
 */
public class HttpResponse {
    private final int status;
    private final String reason;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final String body;

    /**
     * Creates an HTTP response with the given values.
     *
     * @param status      numeric status code
     * @param reason      textual reason phrase
     * @param contentType mime content type
     * @param body        payload to send
     */
    public HttpResponse(int status, String reason, String contentType, String body) {
        this.status = status;
        this.reason = reason;
        this.body = body == null ? "" : body;
        headers.put("Content-Type", contentType);
        headers.put("Content-Length", String.valueOf(this.body.getBytes(StandardCharsets.UTF_8).length));
        headers.put("Connection", "close");
        headers.put("Access-Control-Allow-Origin", "*");
    }

    /**
     * Builds the HTTP response string ready to be written to a socket.
     * Includes status line, headers and body separated by CRLF.
     *
     * @return raw HTTP response text
     */
    public String toHttpString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(status).append(' ').append(reason).append("\r\n");
        for (Map.Entry<String, String> e : headers.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
        }
        sb.append("\r\n");
        sb.append(body);
        return sb.toString();
    }

    /**
     * Creates a JSON response with status 200 OK.
     *
     * @param json json payload
     * @return response instance
     */
    public static HttpResponse okJson(String json) {
        return new HttpResponse(200, "OK", "application/json; charset=UTF-8", json);
    }

    /**
     * Creates a plain text response with status 200 OK.
     *
     * @param text text payload
     * @return response instance
     */
    public static HttpResponse okText(String text) {
        return new HttpResponse(200, "OK", "text/plain; charset=UTF-8", text);
    }

    /**
     * Creates a simple 404 Not Found response.
     *
     * @param message description of the missing resource
     * @return response instance
     */
    public static HttpResponse notFound(String message) {
        String json = "{\"error\":\"" + message.replace("\"", "'") + "\"}";
        return new HttpResponse(404, "Not Found", "application/json; charset=UTF-8", json);
    }

    /**
     * Creates a simple 400 Bad Request response.
     *
     * @param message description of the request issue
     * @return response instance
     */
    public static HttpResponse badRequest(String message) {
        String json = "{\"error\":\"" + message.replace("\"", "'") + "\"}";
        return new HttpResponse(400, "Bad Request", "application/json; charset=UTF-8", json);
    }

    /**
     * Creates a simple 500 Internal Server Error response.
     *
     * @param message description of the failure
     * @return response instance
     */
    public static HttpResponse serverError(String message) {
        String json = "{\"error\":\"" + message.replace("\"", "'") + "\"}";
        return new HttpResponse(500, "Internal Server Error", "application/json; charset=UTF-8", json);
    }
}
