package rcg;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Provides reflection-based command execution for exploring Java classes.
 * Supports listing declared fields and methods and invoking static methods.
 */
public class ReflectiveEngine {

    /**
     * Executes a command string of the assignment grammar and returns JSON.
     * Accepts Class, invoke, unaryInvoke and binaryInvoke operations.
     *
     * @param rawCommand command with parentheses and comma-separated values
     * @return JSON string with the result or error
     */
    public String execute(String rawCommand) {
        try {
            String trimmed = rawCommand == null ? "" : rawCommand.trim();
            if (trimmed.isEmpty()) {
                return errorJson("Empty command");
            }
            int p = trimmed.indexOf('(');
            int q = trimmed.lastIndexOf(')');
            if (p < 0 || q < p) {
                return errorJson("Malformed command");
            }
            String op = trimmed.substring(0, p).trim();
            String inside = trimmed.substring(p + 1, q).trim();
            List<String> args = splitArgs(inside);
            switch (op) {
                case "Class":
                    return doClass(args);
                case "invoke":
                    return doInvoke(args);
                case "unaryInvoke":
                    return doUnaryInvoke(args);
                case "binaryInvoke":
                    return doBinaryInvoke(args);
                default:
                    return errorJson("Unknown operation: " + op);
            }
        } catch (Exception e) {
            return errorJson(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Lists declared fields and methods for the provided class name.
     *
     * @param args single element list with the class name
     * @return JSON with class, fields and methods
     */
    private String doClass(List<String> args) throws Exception {
        if (args.size() != 1) {
            return errorJson("Class expects 1 argument");
        }
        Class<?> clazz = Class.forName(args.get(0));
        List<String> fields = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            fields.add(f.getType().getTypeName() + " " + f.getName());
        }
        List<String> methods = new ArrayList<>();
        for (Method m : clazz.getDeclaredMethods()) {
            String params = Arrays.stream(m.getParameterTypes())
                    .map(Class::getTypeName)
                    .collect(Collectors.joining(", "));
            methods.add(m.getReturnType().getTypeName() + " " + m.getName() + "(" + params + ")");
        }
        String json = "{\"class\":\"" + clazz.getName() + "\"," +
                toJsonArray("fields", fields) + "," +
                toJsonArray("methods", methods) + "}";
        return json;
    }

    /**
     * Invokes a zero-parameter static method and returns the result.
     *
     * @param args [className, methodName]
     * @return JSON with the value
     */
    private String doInvoke(List<String> args) throws Exception {
        if (args.size() != 2) {
            return errorJson("invoke expects 2 arguments");
        }
        Class<?> clazz = Class.forName(args.get(0));
        Method m = clazz.getDeclaredMethod(args.get(1));
        if (!Modifier.isStatic(m.getModifiers())) {
            return errorJson("Only static methods allowed");
        }
        m.setAccessible(true);
        Object result = m.invoke(null);
        return valueJson(result);
    }

    /**
     * Invokes a one-parameter static method with supported primitive types.
     *
     * @param args [className, methodName, paramType, paramValue]
     * @return JSON with the value
     */
    private String doUnaryInvoke(List<String> args) throws Exception {
        if (args.size() != 4) {
            return errorJson("unaryInvoke expects 4 arguments");
        }
        Class<?> clazz = Class.forName(args.get(0));
        String method = args.get(1);
        Class<?> pType = mapType(args.get(2));
        Object pVal = parseValue(pType, args.get(3));
        Method m = clazz.getDeclaredMethod(method, pType);
        if (!Modifier.isStatic(m.getModifiers())) {
            return errorJson("Only static methods allowed");
        }
        m.setAccessible(true);
        Object result = m.invoke(null, pVal);
        return valueJson(result);
    }

    /**
     * Invokes a two-parameter static method with supported primitive types.
     *
     * @param args [className, methodName, type1, val1, type2, val2]
     * @return JSON with the value
     */
    private String doBinaryInvoke(List<String> args) throws Exception {
        if (args.size() != 6) {
            return errorJson("binaryInvoke expects 6 arguments");
        }
        Class<?> clazz = Class.forName(args.get(0));
        String method = args.get(1);
        Class<?> t1 = mapType(args.get(2));
        Object v1 = parseValue(t1, args.get(3));
        Class<?> t2 = mapType(args.get(4));
        Object v2 = parseValue(t2, args.get(5));
        Method m = clazz.getDeclaredMethod(method, t1, t2);
        if (!Modifier.isStatic(m.getModifiers())) {
            return errorJson("Only static methods allowed");
        }
        m.setAccessible(true);
        Object result = m.invoke(null, v1, v2);
        return valueJson(result);
    }

    private static String toJsonArray(String name, List<String> items) {
        String joined = items.stream()
                .map(s -> "\"" + s.replace("\"", "'") + "\"")
                .collect(Collectors.joining(","));
        return "\"" + name + "\":[" + joined + "]";
    }

    private static String valueJson(Object value) {
        if (value == null) {
            return "{\"value\":null}";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return "{\"value\":" + value.toString() + "}";
        }
        return "{\"value\":\"" + value.toString().replace("\"", "'") + "\"}";
    }

    private static String errorJson(String msg) {
        return "{\"error\":\"" + msg.replace("\"", "'") + "\"}";
    }

    private static Class<?> mapType(String s) throws Exception {
        String t = s.trim().toLowerCase(Locale.ROOT);
        switch (t) {
            case "int":
                return int.class;
            case "double":
                return double.class;
            case "string":
                return String.class;
            default:
                throw new IllegalArgumentException("Unsupported type: " + s);
        }
    }

    private static Object parseValue(Class<?> type, String raw) {
        if (type == int.class) {
            return Integer.parseInt(raw.trim());
        } else if (type == double.class) {
            return Double.parseDouble(raw.trim());
        } else {
            return unquote(raw);
        }
    }

    private static String unquote(String s) {
        String t = s.trim();
        if ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'"))) {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }

    private static List<String> splitArgs(String inside) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;
        for (int i = 0; i < inside.length(); i++) {
            char c = inside.charAt(i);
            if ((c == '"' || c == '\'') && (i == 0 || inside.charAt(i - 1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (quoteChar == c) {
                    inQuotes = false;
                }
                cur.append(c);
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        String last = cur.toString().trim();
        if (!last.isEmpty()) {
            out.add(last);
        }
        return out;
    }
}
