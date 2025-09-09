package rcg;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a parsed HTTP request with method, path, query and headers.
 * Provides a factory to parse from a socket input stream.
 */
public class HttpRequest {
    private final String method;
    private final String path;
    private final String queryString;
    private final Map<String, String> queryParams;
    private final Map<String, String> headers;

    /**
     * Creates an immutable HTTP request instance.
     *
     * @param method      HTTP method
     * @param path        request path
     * @param queryString raw query string
     * @param headers     header map
     */
    public HttpRequest(String method, String path, String queryString, Map<String, String> headers) {
        this.method = method;
        this.path = path;
        this.queryString = queryString == null ? "" : queryString;
        this.headers = headers == null ? new HashMap<>() : headers;
        this.queryParams = parseQueryParams(this.queryString);
    }

    /**
     * Parses an HTTP request from a reader connected to the socket.
     * Reads the request line and headers until the blank line.
     *
     * @param in buffered reader for the socket input
     * @return parsed HttpRequest
     * @throws IOException when reading fails or request is malformed
     */
    public static HttpRequest parse(BufferedReader in) throws IOException {
        String requestLine = in.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            throw new IOException("Empty request");
        }
        String[] parts = requestLine.split(" ");
        String method = parts.length > 0 ? parts[0] : "GET";
        String fullPath = parts.length > 1 ? parts[1] : "/";
        String path = fullPath;
        String query = "";
        int qIdx = fullPath.indexOf('?');
        if (qIdx >= 0) {
            path = fullPath.substring(0, qIdx);
            query = fullPath.substring(qIdx + 1);
        }
        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = in.readLine()) != null) {
            if (line.isEmpty()) {
                break;
            }
            int c = line.indexOf(":");
            if (c > 0) {
                headers.put(line.substring(0, c).trim(), line.substring(c + 1).trim());
            }
        }
        return new HttpRequest(method, path, query, headers);
    }

    /**
     * Returns the HTTP method such as GET or POST.
     *
     * @return HTTP method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the path component of the URL.
     *
     * @return path
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the raw query string without the leading question mark.
     *
     * @return query string
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * Gets a decoded query parameter by name.
     *
     * @param name parameter name
     * @return value or null
     */
    public String getQueryParam(String name) {
        return queryParams.get(name);
    }

    /**
     * Returns an immutable view of parsed query parameters.
     *
     * @return map with parameters
     */
    public Map<String, String> getQueryParams() {
        return new HashMap<>(queryParams);
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return map;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx >= 0) {
                String key = urlDecode(pair.substring(0, idx));
                String val = urlDecode(pair.substring(idx + 1));
                map.put(key, val);
            } else if (!pair.isEmpty()) {
                map.put(urlDecode(pair), "");
            }
        }
        return map;
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
