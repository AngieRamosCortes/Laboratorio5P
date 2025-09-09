package rcg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Minimal HTTP server for the backend service.
 * Exposes the path /compreflex and processes the "comando" query parameter.
 * Delegates execution to ReflectiveEngine and returns JSON results.
 */
public class BackendServer {
    private final int port;
    private final ReflectiveEngine engine;

    /**
     * Constructs the backend server with a port and engine.
     *
     * @param port   TCP port to listen
     * @param engine shared reflective engine
     */
    public BackendServer(int port, ReflectiveEngine engine) {
        this.port = port;
        this.engine = engine;
    }

    /**
     * Starts the server loop accepting connections and serving sequentially.
     * Uses blocking IO for simplicity suitable to the exam constraints.
     *
     * @throws IOException if the socket fails
     */
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Backend ready on port " + port);
            while (true) {
                try (Socket client = serverSocket.accept();
                        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
                    HttpRequest req = HttpRequest.parse(in);
                    HttpResponse res = handle(req);
                    out.print(res.toHttpString());
                    out.flush();
                } catch (Exception e) {
                    System.err.println("Backend error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Handles a single request by routing on the path and parameters.
     * Returns valid HTTP responses for success or failure scenarios.
     *
     * @param req parsed request
     * @return response instance
     */
    private HttpResponse handle(HttpRequest req) {
        boolean isEndpoint = "/compreflex".equals(req.getPath()) || "/compreflex".equals(req.getPath());
        if (!isEndpoint) {
            return HttpResponse.notFound("Use /compreflex");
        }
        String cmd = req.getQueryParam("comando");
        if (cmd == null || cmd.isEmpty()) {
            return HttpResponse.badRequest("Missing 'comando'");
        }
        String json = engine.execute(cmd);
        return HttpResponse.okJson(json);
    }

    /**
     * Main method that starts the backend with default configuration.
     * Listens on port 45000 as suggested by the statement.
     *
     * @param args command line arguments (unused)
     */
    public static void main(String[] args) throws IOException {
        new BackendServer(45000, new ReflectiveEngine()).start();
    }
}
