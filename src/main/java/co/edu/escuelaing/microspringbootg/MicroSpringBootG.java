package co.edu.escuelaing.microspringbootg;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class MicroSpringBootG {
    private static final int DEFAULT_PORT = 35000;
    private static final String DEFAULT_SCAN_PACKAGE = "co.edu.escuelaing";
    private static final String STATIC_ROOT = "static";

    private static final Map<String, RouteHandler> controllerMethods = new HashMap<>();

    private static class RouteHandler {
        private final Object instance;
        private final Method method;

        RouteHandler(Object instance, Method method) {
            this.instance = instance;
            this.method = method;
        }
    }

    public static void main(String[] args) throws Exception {
        if (isLegacyInvocation(args)) {
            runLegacyInvocation(args);
            return;
        }

        int port = extractPort(args);
        String scanPackage = extractScanPackage(args);

        loadControllersFromPackage(scanPackage);
        loadControllersFromExplicitClasses(args);

        startServer(port);
    }

    private static boolean isLegacyInvocation(String[] args) {
        return args.length == 2 && args[0].contains(".") && args[1].startsWith("/");
    }

    private static void runLegacyInvocation(String[] args)
            throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, ReflectiveOperationException {
        Class<?> c = Class.forName(args[0]);
        registerController(c);

        RouteHandler handler = controllerMethods.get(args[1]);
        if (handler == null) {
            System.out.println("No handler found for path " + args[1]);
            return;
        }

        Object response = handler.method.invoke(handler.instance);
        System.out.println(response);
    }

    private static int extractPort(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                return Integer.parseInt(arg.substring("--port=".length()));
            }
        }
        return DEFAULT_PORT;
    }

    private static String extractScanPackage(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--scan=")) {
                return arg.substring("--scan=".length());
            }
        }
        return DEFAULT_SCAN_PACKAGE;
    }

    private static void loadControllersFromExplicitClasses(String[] args) throws Exception {
        for (String arg : args) {
            if (arg.startsWith("--") || !arg.contains(".")) {
                continue;
            }
            registerController(Class.forName(arg));
        }
    }

    private static void loadControllersFromPackage(String basePackage) throws Exception {
        for (Class<?> candidate : findClasses(basePackage)) {
            registerController(candidate);
        }
    }

    private static void registerController(Class<?> candidate) throws ReflectiveOperationException {
        if (!candidate.isAnnotationPresent(RestController.class)) {
            return;
        }

        Object instance = candidate.getDeclaredConstructor().newInstance();

        for (Method method : candidate.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(GetMapping.class)) {
                continue;
            }
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            String path = mapping.value();
            controllerMethods.put(path, new RouteHandler(instance, method));
            System.out.println("Mapped GET " + path + " -> " + candidate.getSimpleName() + "." + method.getName());
        }
    }

    private static void startServer(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("MicroSpringBootG listening on http://localhost:" + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleClient(clientSocket);
            }
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (Socket socket = clientSocket;
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))) {

            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isBlank()) {
                return;
            }

            String line;
            while ((line = reader.readLine()) != null && !line.isBlank()) {
                // Consume headers. This prototype only needs the request line.
            }

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2 || !"GET".equals(requestParts[0])) {
                writeResponse(socket, 405, "text/plain", "Only GET is supported".getBytes(StandardCharsets.UTF_8));
                return;
            }

            String target = requestParts[1];
            String path = extractPath(target);
            Map<String, String> queryParams = parseQueryString(target);

            if (controllerMethods.containsKey(path)) {
                byte[] body = invokeController(path, queryParams).getBytes(StandardCharsets.UTF_8);
                writeResponse(socket, 200, "text/plain; charset=UTF-8", body);
                return;
            }

            serveStaticResource(socket, path);
        } catch (Exception e) {
            try {
                byte[] body = ("Internal server error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
                writeResponse(clientSocket, 500, "text/plain; charset=UTF-8", body);
            } catch (IOException ignored) {
            }
        }
    }

    private static String invokeController(String path, Map<String, String> queryParams)
            throws InvocationTargetException, IllegalAccessException {
        RouteHandler handler = controllerMethods.get(path);
        Method method = handler.method;

        if (!String.class.equals(method.getReturnType())) {
            return "Only String return type is supported";
        }

        Object[] args = resolveArguments(method, queryParams);
        Object output = method.invoke(handler.instance, args);
        return Objects.toString(output, "");
    }

    private static Object[] resolveArguments(Method method, Map<String, String> queryParams) {
        Parameter[] parameters = method.getParameters();
        Object[] resolved = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (!String.class.equals(parameter.getType())) {
                throw new IllegalArgumentException("Only String parameters are supported");
            }

            if (!parameter.isAnnotationPresent(RequestParam.class)) {
                throw new IllegalArgumentException("Controller parameters must use @RequestParam");
            }

            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            String value = queryParams.get(requestParam.value());

            if (value == null || value.isBlank()) {
                String defaultValue = requestParam.defaultValue();
                value = defaultValue.isEmpty() ? "" : defaultValue;
            }
            resolved[i] = value;
        }

        return resolved;
    }

    private static void serveStaticResource(Socket socket, String path) throws IOException {
        String resolvedPath = "/".equals(path) ? "/index.html" : path;
        byte[] content = readResourceBytes(STATIC_ROOT + resolvedPath);

        if (content == null) {
            writeResponse(socket, 404, "text/plain; charset=UTF-8",
                    ("Resource not found: " + path).getBytes(StandardCharsets.UTF_8));
            return;
        }

        writeResponse(socket, 200, contentTypeFor(resolvedPath), content);
    }

    private static byte[] readResourceBytes(String classpathResource) throws IOException {
        InputStream inputStream = MicroSpringBootG.class.getClassLoader().getResourceAsStream(classpathResource);
        if (inputStream == null) {
            return null;
        }

        try (BufferedInputStream bis = new BufferedInputStream(inputStream);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }

    private static String contentTypeFor(String path) {
        if (path.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }
        if (path.endsWith(".png")) {
            return "image/png";
        }
        if (path.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (path.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        return "application/octet-stream";
    }

    private static void writeResponse(Socket socket, int statusCode, String contentType, byte[] body) throws IOException {
        String statusText = switch (statusCode) {
            case 200 -> "OK";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            default -> "Internal Server Error";
        };

        String headers = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Date: " + DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()) + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + body.length + "\r\n" +
                "Connection: close\r\n\r\n";

        socket.getOutputStream().write(headers.getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().write(body);
        socket.getOutputStream().flush();
    }

    private static String extractPath(String target) {
        int queryIdx = target.indexOf('?');
        return queryIdx == -1 ? target : target.substring(0, queryIdx);
    }

    private static Map<String, String> parseQueryString(String target) {
        int queryIdx = target.indexOf('?');
        if (queryIdx == -1 || queryIdx == target.length() - 1) {
            return Collections.emptyMap();
        }

        String query = target.substring(queryIdx + 1);
        String[] pairs = query.split("&");
        Map<String, String> params = new HashMap<>();

        for (String pair : pairs) {
            if (pair.isBlank()) {
                continue;
            }
            String[] keyValue = pair.split("=", 2);
            String key = urlDecode(keyValue[0]);
            String value = keyValue.length > 1 ? urlDecode(keyValue[1]) : "";
            params.put(key, value);
        }

        return params;
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static List<Class<?>> findClasses(String basePackage) throws Exception {
        String packagePath = basePackage.replace('.', '/');
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(packagePath);
        List<Class<?>> classes = new ArrayList<>();

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (!"file".equals(resource.getProtocol())) {
                continue;
            }

            String decoded = URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8);
            File directory = new File(decoded);
            classes.addAll(findClassesInDirectory(directory, basePackage));
        }

        return classes;
    }

    private static List<Class<?>> findClassesInDirectory(File directory, String packageName) throws Exception {
        if (!directory.exists()) {
            return Collections.emptyList();
        }

        List<Class<?>> classes = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }

        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClassesInDirectory(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                classes.add(Class.forName(className));
            }
        }
        return classes;
    }
}
