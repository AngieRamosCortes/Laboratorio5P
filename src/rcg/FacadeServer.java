package rcg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Minimal HTTP server for the facade service.
 * Serves the client page at /cliente and proxies /consulta to the backend.
 */
public class FacadeServer {
    private final int port;
    private final String backendBaseUrl;

    /**
     * Creates the facade with a listening port and backend base URL.
     *
     * @param port           TCP port to listen
     * @param backendBaseUrl base URL like http://localhost:45000
     */
    public FacadeServer(int port, String backendBaseUrl) {
        this.port = port;
        this.backendBaseUrl = backendBaseUrl;
    }

    /**
     * Starts the server accepting connections one by one using blocking IO.
     * Keeps the implementation simple for the exam time limit.
     *
     * @throws IOException if socket operations fail
     */
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Facade ready on port " + port);
            while (true) {
                try (Socket client = serverSocket.accept();
                        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
                    HttpRequest req = HttpRequest.parse(in);
                    HttpResponse res = handle(req);
                    out.print(res.toHttpString());
                    out.flush();
                } catch (Exception e) {
                    System.err.println("Facade error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Routes the request to either the static client or the proxy endpoint.
     * Returns content in JSON or HTML with proper headers.
     *
     * @param req parsed request
     * @return HTTP response ready to send
     */
    private HttpResponse handle(HttpRequest req) {
        if ("/cliente".equals(req.getPath())) {
            return new HttpResponse(200, "OK", "text/html; charset=UTF-8", ClientPage.html());
        }
        if ("/consulta".equals(req.getPath())) {
            String cmd = req.getQueryParam("comando");
            if (cmd == null || cmd.isEmpty()) {
                return HttpResponse.badRequest("Missing 'comando'");
            }
            try {
                String url = backendBaseUrl + "/compreflex?comando=" + URLEncoder.encode(cmd, StandardCharsets.UTF_8);
                String json = httpGet(url);
                return HttpResponse.okJson(json);
            } catch (Exception e) {
                return HttpResponse.serverError(e.getMessage());
            }
        }
        return HttpResponse.notFound("Use /cliente or /consulta");
    }

    /**
     * Performs a blocking GET to the backend and returns the response as text.
     * Sets a basic User-Agent and expects HTTP 200 for success.
     *
     * @param url target URL
     * @return body as a string
     * @throws IOException when the request fails
     */
    private static String httpGet(String url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "MinimalClient/1.0");
        int code = con.getResponseCode();
        BufferedReader rd = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? con.getInputStream() : con.getErrorStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        return sb.toString();
    }

    /**
     * Main method that starts the facade on port 35000 and proxies to localhost
     * backend.
     * Adjust the backend base URL when deploying to another machine.
     *
     * @param args command line arguments (unused)
     */
    public static void main(String[] args) throws IOException {
        new FacadeServer(35000, "http://localhost:45000").start();
    }
}
