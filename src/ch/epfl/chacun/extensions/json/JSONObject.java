package ch.epfl.chacun.extensions.json;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Used to represent json objects */
public class JSONObject extends JSONValue {
    /** The map of values stored in this object */
    private final Map<String, JSONValue> values = new LinkedHashMap<>();

    /**
     * Adds a value to this object
     * @param key (String): The key of the value to add
     * @param value (JSONValue): The value to add
     * @return (JSONObject): This object
     */
    public JSONObject add(String key, JSONValue value) {
        if (key == null || key.isEmpty()) throw new IllegalArgumentException("Key cannot be null or empty");
        else if (value == null) throw new IllegalArgumentException("Value cannot be null");
        else if (values.containsKey(key)) throw new IllegalArgumentException(STR."Duplicate key: \{key}");

        values.put(key, value);
        return this;
    }

    /**
     * Removes the value with the given key
     * @param key (String): The key of the value to remove
     * @return (JSONObject): This object
     */
    public JSONObject remove(String key) {
        if (key == null || key.isEmpty()) throw new IllegalArgumentException("Key cannot be null or empty");
        else if (!values.containsKey(key)) throw new IllegalArgumentException(STR."Key not found: \{key}");

        values.remove(key);
        return this;
    }

    /**
     * Returns all the values in this object
     * @return (Map<String, JSONValue>): The value of this object
     */
    public Map<String, JSONValue> get() {
        return values;
    }

    @Override
    public JSONValue get(String key) {
        if (key == null || key.isEmpty()) throw new IllegalArgumentException("Key cannot be null or empty");
        else if (!values.containsKey(key)) throw new IllegalArgumentException(STR."Key not found: \{key}");

        return values.get(key);
    }

    @Override
    public JSONArray getArray(String key) {
        if (!(values.get(key) instanceof JSONArray)) throw new IllegalArgumentException("Value is not a JSONArray");
        return values.get(key).toJSONArray();
    }

    @Override
    public JSONObject getObject(String key) {
        if (!(values.get(key) instanceof JSONObject)) throw new IllegalArgumentException("Value is not a JSONObject");
        return values.get(key).toJSONObject();
    }

    /**
     * Returns whether this object contains the given key
     * @param key (String): The key to check
     * @return (boolean): Whether this object contains the given key
     */
    public boolean containsKey(String key) {
        if (key == null || key.isEmpty()) throw new IllegalArgumentException("Key cannot be null or empty");

        return values.containsKey(key);
    }

    @Override
    public String toString() {
        if (values.isEmpty()) return "{}";

        final String indent = "  ";
        final StringBuilder builder = new StringBuilder().append("{\n");
        boolean firstEntry = true;

        for (Map.Entry<String, JSONValue> entry : values.entrySet()) {
            if (!firstEntry) builder.append(",\n");
            builder.append(indent).append("\"").append(entry.getKey()).append("\":");

            // Convert nested objects to string
            if (entry.getValue() instanceof JSONObject) {
                // (?m)^ is a regex that activates multiline mode to match the start of each line
                builder.append((STR."\{entry.getValue()}").replaceAll("(?m)^", indent));
            }
            // Convert all other JSONValue's to strings
            else builder.append(entry.getValue());
            firstEntry = false;
        }

        return builder.append("\n}").toString().replaceAll(":\\s*", ": ");
    }

    /**
     * Parse a list of lines into a JSONObject
     * @param lines (List<String>): The lines to parse
     * @return (JSONObject): The parsed JSONObject
     */
    private static JSONObject parse(List<String> lines) {
        // Remove the containing brackets if needed
        if (lines.getFirst().equals("{") && (lines.getLast().equals("}") || lines.getLast().equals("},"))) {
            lines.removeFirst();
            lines.removeLast();
        }

        final JSONObject json = new JSONObject();
        boolean parsed;

        // Variables used to parse JSONObjects
        boolean inObject = false;
        String objectKey = "";
        int objectStart = -1;
        int curlyBracketCount = 0;

        // Variables used to parse JSONArrays
        boolean inArray = false;
        String arrayKey = "";
        int arrayStart = -1;
        int bracketCount = 0;

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i) == null || lines.get(i).isEmpty()) continue;

            // Get the current line and split it into parts
            parsed = false;
            String line = lines.get(i).trim();
            String[] parts = new String[0];
            String key = "";

            if (!inObject && !inArray) {
                parts = JSONUtils.splitLine(line);
                if (!parts[0].contains("\"")) throw new IllegalArgumentException(STR."Invalid key: \{parts[0]}");
                key = parts[0].replaceAll("\"", "");
            }

            // Check if we are looking at an empty object or array
            if (line.replaceAll("\\s*", "").contains(JSONUtils.OBJECT_EMPTY)
                    || line.replaceAll("\\s*", "").contains(JSONUtils.ARRAY_EMPTY)) {
                json.add(key, (line.contains(JSONUtils.OBJECT_EMPTY)) ? new JSONObject() : new JSONArray());
            }

            // Check if we are at the start of an object or nested object
            if (!inArray && line.matches(JSONUtils.CHECK_OBJECT_START)) {
                if (!inObject) {
                    inObject = true;
                    objectKey = key;
                    objectStart = i;
                }
                curlyBracketCount += JSONUtils.findAll(line, STR."\{JSONUtils.OBJECT_START}");
            }
            // Check if we are at the end of an object or nested object
            if (!inArray && inObject && line.matches(JSONUtils.CHECK_OBJECT_END)) {
                curlyBracketCount -= JSONUtils.findAll(line, STR."\{JSONUtils.OBJECT_END}");
                if (curlyBracketCount == 0) {
                    json.add(objectKey, JSONObject.parse(String.join("", JSONUtils.format(lines, objectStart, i))));

                    // Reset the variables
                    objectKey = "";
                    objectStart = -1;
                    inObject = false;
                    parsed = true;
                }
            }

            // Check if we are at the start of an array or nested array
            if (!inObject && line.matches(JSONUtils.CHECK_ARRAY_START)) {
                if (!inArray) {
                    inArray = true;
                    arrayKey = key;
                    arrayStart = i;
                }
                bracketCount += JSONUtils.findAll(line, STR."\{JSONUtils.ARRAY_START}");
            }
            // Check if we are at the end of an array or nested array
            if (!inObject && inArray && line.matches(JSONUtils.CHECK_ARRAY_END)) {
                bracketCount -= JSONUtils.findAll(line, STR."\{JSONUtils.ARRAY_END}");
                if (bracketCount == 0) {
                    json.add(arrayKey, JSONArray.parse(String.join("", JSONUtils.format(lines, arrayStart, i))));

                    // Reset the variables
                    arrayKey = "";
                    arrayStart = -1;
                    inArray = false;
                    parsed = true;
                }
            }

            // Parse the line
            if (!inObject && !inArray && !parsed) json.add(key, JSONValue.parse(parts[1]));
        }
        return json;
    }

    /**
     * Parse a json object into a JSONObject
     * @param json (String): The json object to parse
     * @return (JSONObject): The parsed JSONObject
     */
    public static JSONObject parse(String json) {
        json = json.trim();
        if (!json.startsWith(STR."\{JSONUtils.OBJECT_START}") ||
                !(json.endsWith(STR."\{JSONUtils.OBJECT_END}") ||json.endsWith(JSONUtils.OBJECT_END_COMMA))) {
            throw new IllegalArgumentException("Invalid JSON object format");
        }
        // Remove the last comma if it exists
        if (json.endsWith(JSONUtils.OBJECT_END_COMMA)) json = json.substring(0, json.length() - 1);

        // Removing the outermost brackets and parse the lines
        return parse(JSONUtils.split(json));
    }
}