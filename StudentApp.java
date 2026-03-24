import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.awt.Desktop;
import java.net.URI;

public class StudentApp {
    private static StudentManager manager = new StudentManager();

    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // API Contexts
        server.createContext("/api/students", new StudentsHandler());
        server.createContext("/api/add", new AddStudentHandler());
        server.createContext("/api/delete", new DeleteStudentHandler());
        server.createContext("/api/toggle", new ToggleAttendanceHandler());

        // Static File Contexts
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        System.out.println("Server started on http://localhost:" + port);
        
        // Open browser automatically
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI("http://localhost:" + port));
            }
        } catch (Exception e) {
            System.out.println("Could not open browser automatically: " + e.getMessage());
        }

        server.start();
    }

    // --- API Handlers ---

    static class StudentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Student> students = manager.getStudents();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < students.size(); i++) {
                Student s = students.get(i);
                json.append(String.format("{\"name\":\"%s\",\"roll\":\"%s\",\"course\":\"%s\",\"count\":%d,\"isIn\":%b}",
                        s.getName(), String.valueOf(s.getRollNo()), s.getCourse(), s.getAttendanceCount(), s.isCheckedIn()));
                if (i < students.size() - 1) json.append(",");
            }
            json.append("]");
            sendResponse(exchange, json.toString(), "application/json");
        }
    }

    static class AddStudentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).readLine();
                String[] params = body.split("&");
                String name = ""; int roll = 0; String course = "";
                for (String p : params) {
                    String[] kv = p.split("=");
                    if (kv[0].equals("name")) name = java.net.URLDecoder.decode(kv[1], "UTF-8");
                    if (kv[0].equals("roll")) roll = Integer.parseInt(kv[1]);
                    if (kv[0].equals("course")) course = java.net.URLDecoder.decode(kv[1], "UTF-8");
                }
                manager.addStudent(name, roll, course);
                sendResponse(exchange, "{\"status\":\"ok\"}", "application/json");
            }
        }
    }

    static class DeleteStudentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).readLine();
                int roll = Integer.parseInt(body.split("=")[1]);
                manager.deleteStudent(roll);
                sendResponse(exchange, "{\"status\":\"ok\"}", "application/json");
            }
        }
    }

    static class ToggleAttendanceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).readLine();
                String[] parts = body.split("&");
                int roll = Integer.parseInt(parts[0].split("=")[1]);
                boolean isCurrentlyIn = Boolean.parseBoolean(parts[1].split("=")[1]);
                
                if (isCurrentlyIn) {
                    manager.checkOutStudent(roll);
                } else {
                    manager.checkInStudent(roll);
                }
                
                sendResponse(exchange, "{\"status\":\"ok\"}", "application/json");
            }
        }
    }

    // --- Static File Handler ---

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            File file = new File("." + path);

            if (file.exists() && !file.isDirectory()) {
                String contentType = Files.probeContentType(file.toPath());
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, file.length());
                OutputStream os = exchange.getResponseBody();
                Files.copy(file.toPath(), os);
                os.close();
            } else {
                String msg = "404 Not Found";
                exchange.sendResponseHeaders(404, msg.length());
                OutputStream os = exchange.getResponseBody();
                os.write(msg.getBytes());
                os.close();
            }
        }
    }

    private static void sendResponse(HttpExchange exchange, String response, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
