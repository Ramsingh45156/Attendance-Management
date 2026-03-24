import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
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
        server.createContext("/api/classes", new ListClassesHandler());
        server.createContext("/api/addClass", new AddClassHandler());
        server.createContext("/api/switchClass", new SwitchClassHandler());
        server.createContext("/api/history", new HistoryHandler());
        server.createContext("/api/currentClass", new CurrentClassHandler());
        server.createContext("/api/setClassStatus", new SetClassStatusHandler());

        // Static File Contexts
        server.createContext("/", new StaticFileHandler());
// ... (rest of main)
        server.start();
    }

    // --- API Handlers ---

    static class StudentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
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
            } catch (Exception e) {
                sendResponse(exchange, "[]", "application/json");
            }
        }
    }

    static class AddStudentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).readLine();
                String[] params = body.split("&");
                String name = ""; String course = "";
                for (String p : params) {
                    String[] kv = p.split("=");
                    if (kv.length < 2) continue;
                    if (kv[0].equals("name")) name = java.net.URLDecoder.decode(kv[1], "UTF-8");
                    if (kv[0].equals("course")) course = java.net.URLDecoder.decode(kv[1], "UTF-8");
                }
                manager.addStudent(name, course);
                sendResponse(exchange, "{\"status\":\"ok\"}", "application/json");
            }
        }
    }

    static class DeleteStudentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).readLine();
                    int roll = Integer.parseInt(body.split("=")[1]);
                    manager.deleteStudent(roll);
                    sendResponse(exchange, "{\"status\":\"ok\"}", "application/json");
                } catch (Exception e) {
                    sendResponse(exchange, "{\"status\":\"error\", \"message\":\"Invalid Roll Number\"}", "application/json");
                }
            }
        }
    }

    static class ToggleAttendanceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).readLine();
                    String[] parts = body.split("&");
                    int roll = Integer.parseInt(parts[0].split("=")[1]);
                    boolean isCurrentlyIn = Boolean.parseBoolean(parts[1].split("=")[1]);
                    
                    boolean success;
                    if (isCurrentlyIn) {
                        success = manager.checkOutStudent(roll);
                    } else {
                        success = manager.checkInStudent(roll);
                    }
                    
                    if (success) {
                        sendResponse(exchange, "{\"status\":\"ok\"}", "application/json");
                    } else {
                        sendResponse(exchange, "{\"status\":\"error\", \"message\":\"Toggle failed\"}", "application/json");
                    }
                } catch (Exception e) {
                    sendResponse(exchange, "{\"status\":\"error\", \"message\":\"Failed to toggle attendance\"}", "application/json");
                }
            }
        }
    }

    static class ListClassesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<String> classes = manager.listClasses();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < classes.size(); i++) {
                String name = classes.get(i);
                String status = manager.getClassStatus(name);
                json.append(String.format("{\"name\":\"%s\",\"status\":\"%s\"}", name, status));
                if (i < classes.size() - 1) json.append(",");
            }
            json.append("]");
            sendResponse(exchange, json.toString(), "application/json");
        }
    }

    static class AddClassHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).readLine();
                String className = java.net.URLDecoder.decode(body.split("=")[1], "UTF-8");
                manager.createClass(className);
                sendResponse(exchange, "{\"status\":\"ok\"}", "application/json");
            }
        }
    }

    static class SwitchClassHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).readLine();
                String className = java.net.URLDecoder.decode(body.split("=")[1], "UTF-8");
                manager.switchClass(className);
                sendResponse(exchange, "{\"status\":\"ok\"}", "application/json");
            }
        }
    }

    static class CurrentClassHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendResponse(exchange, "{\"className\":\"" + manager.getCurrentClass() + "\"}", "application/json");
        }
    }

    static class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<String> history = manager.getHistory();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < history.size(); i++) {
                String[] parts = history.get(i).split(",");
                if (parts.length >= 5) {
                    json.append(String.format("{\"time\":\"%s\",\"roll\":\"%s\",\"name\":\"%s\",\"type\":\"%s\",\"course\":\"%s\"}",
                            parts[0], parts[1], parts[2], parts[3], parts[4]));
                    if (i < history.size() - 1) json.append(",");
                }
            }
            json.append("]");
            sendResponse(exchange, json.toString(), "application/json");
        }
    }

    static class SetClassStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).readLine();
                String[] parts = body.split("&");
                String name = java.net.URLDecoder.decode(parts[0].split("=")[1], "UTF-8");
                String status = java.net.URLDecoder.decode(parts[1].split("=")[1], "UTF-8");
                manager.setClassStatus(name, status);
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
