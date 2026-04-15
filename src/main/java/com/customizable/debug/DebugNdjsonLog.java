package com.customizable.debug;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;

/** Session debug log: NDJSON lines to workspace {@code debug-054da2.log} (parent of Gradle {@code run/} when cwd is run). */
public final class DebugNdjsonLog {
    private static final String SESSION = "054da2";
    private static final ConcurrentHashMap<String, Boolean> ONCE = new ConcurrentHashMap<>();

    private DebugNdjsonLog() {}

    private static Path resolveLogPath() {
        Path ud = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        if (ud.endsWith("run")) {
            Path parent = ud.getParent();
            if (parent != null) return parent.resolve("debug-054da2.log");
        }
        return ud.resolve("debug-054da2.log");
    }

    public static void log(String hypothesisId, String location, String message, String dataJsonObject) {
        // #region agent log
        try {
            String data = (dataJsonObject == null || dataJsonObject.isEmpty()) ? "{}" : dataJsonObject;
            String line = "{\"sessionId\":\"" + SESSION + "\",\"timestamp\":" + System.currentTimeMillis()
                    + ",\"hypothesisId\":\"" + esc(hypothesisId) + "\",\"location\":\"" + esc(location)
                    + "\",\"message\":\"" + esc(message) + "\",\"data\":" + data + "}\n";
            Files.writeString(resolveLogPath(), line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable ignored) {}
        // #endregion
    }

    public static void logOnce(String key, String hypothesisId, String location, String message, String dataJsonObject) {
        // #region agent log
        if (ONCE.putIfAbsent(key, Boolean.TRUE) != null) return;
        log(hypothesisId, location, message, dataJsonObject);
        // #endregion
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", " ").replace("\n", " ");
    }

    /** JSON object fields: pathTail, pathLen (filename only, no directory). */
    public static String pathFieldsJson(String path) {
        if (path == null) return "{\"pathTail\":\"\",\"pathLen\":0}";
        int n = path.length();
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String tail = slash >= 0 && slash + 1 < path.length() ? path.substring(slash + 1) : path;
        if (tail.length() > 48) tail = tail.substring(0, 48);
        return "{\"pathTail\":\"" + esc(tail) + "\",\"pathLen\":" + n + "}";
    }

    public static String throwableFields(Throwable e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.length() > 100) msg = msg.substring(0, 100);
        return "{\"ex\":\"" + esc(e.getClass().getSimpleName()) + "\",\"msg\":\"" + esc(msg) + "\"}";
    }

    /** Merge two JSON objects (shallow): {@code {"a":1}} + {@code {"b":2}}. */
    public static String mergeObjects(String jsonA, String jsonB) {
        if (jsonA == null || jsonA.length() < 2) return jsonB == null ? "{}" : jsonB;
        if (jsonB == null || jsonB.length() < 2) return jsonA;
        String inner = jsonA.substring(1, jsonA.length() - 1).trim();
        String inner2 = jsonB.substring(1, jsonB.length() - 1).trim();
        if (inner.isEmpty()) return jsonB;
        if (inner2.isEmpty()) return jsonA;
        return "{" + inner + "," + inner2 + "}";
    }
}
