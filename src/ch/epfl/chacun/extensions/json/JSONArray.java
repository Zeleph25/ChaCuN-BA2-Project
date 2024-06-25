package ch.epfl.chacun.extensions.json;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Used to represent json arrays */
public class JSONArray extends JSONValue {
    /** The array stored in this object */
    private final List<JSONValue> values;
    /** The set that stores no duplicate values faster for searches */
    private final Set<JSONValue> valuesSet;

    public JSONArray() {
        this.values = new ArrayList<>();
        this.valuesSet = new HashSet<>();
    }

    public List<JSONValue> values() {
        return List.copyOf(values);
    }

    @Override
    public JSONValue get(int index) {
        if (index < 0 || index >= values.size())
            throw new IndexOutOfBoundsException(STR."Index: \{index}out of bounds for size: \{values.size()}");

        return values.get(index);
    }

    @Override
    public JSONArray getArray(int index) {
        if (!(values.get(index) instanceof JSONArray)) throw new IllegalArgumentException("Value is not a JSONArray");
        return values.get(index).toJSONArray();
    }

    @Override
    public JSONObject getObject(int index) {
        if (!(values.get(index) instanceof JSONObject)) throw new IllegalArgumentException("Value is not a JSONObject");
        return values.get(index).toJSONObject();
    }

    /**
     * Sets the value at the given index
     * @param index (int): The index of the value to set
     * @param value (JSONValue): The new value
     */
    public void set(int index, JSONValue value) {
        if (index < 0 || index >= values.size())
            throw new IndexOutOfBoundsException(STR."Index: \{index}out of bounds for size: \{values.size()}");
        else if (value == null) throw new IllegalArgumentException("Value cannot be null");

        values.set(index, value);
    }

    /**
     * Appends the given value to the array
     * @param value (JSONValue): The value to append
     */
    public JSONArray append(JSONValue value) {
        if (value == null) throw new IllegalArgumentException("Value cannot be null");

        values.add(value);
        valuesSet.add(value);
        return this;
    }

    public JSONArray appendAll(JSONValue[] values) {
        for (JSONValue value : values) append(value);
        return this;
    }

    public JSONArray appendAll(List<JSONValue> values) {
        for (JSONValue value : values) append(value);
        return this;
    }

    /**
     * Removes the value at the given index
     * @param index (int): The index of the value to remove
     */
    public JSONArray remove(int index) {
        if (index < 0 || index >= values.size())
            throw new IndexOutOfBoundsException(STR."Index: \{index}, Size: \{values.size()}");

        JSONValue removedValue = values.remove(index);
        valuesSet.remove(removedValue);
        return this;
    }

    /**
     * Checks if the array contains the given value
     * @param value (JSONValue): The value to check
     * @return (boolean): True if the array contains the value, false otherwise
     */
    public boolean contains(JSONValue value) {
        if (value == null) throw new IllegalArgumentException("Value cannot be null");

        return valuesSet.contains(value);
    }

    /**
     * Removes a given value
     * @param value (JSONValue): The value to remove
     */
    public void remove(JSONValue value) {
        if (!contains(value)) {
            throw new IllegalArgumentException(STR."Invalid value to remove: \{value}");
        }

        values.remove(value);
        valuesSet.remove(value);
    }

    /**
     * Removes a given value
     * @param values (JSONValue[]): The values to remove
     */
    public JSONArray removeAll(JSONValue[] values) {
        for (JSONValue value : values) remove(value);
        return this;
    }

    /**
     * Removes a given value
     * @param values (List<JSONValue>): The values to remove
     */
    public JSONArray removeAll(List<JSONValue> values) {
        for (JSONValue value : values) remove(value);
        return this;
    }

    /**
     * Returns the size of the array
     * @return (int): The size of the array
     */
    public int size() {
        return values.size();
    }

    @Override
    public String toString() {
        if (values.isEmpty()) return JSONUtils.ARRAY_EMPTY;

        final StringBuilder builder = new StringBuilder().append(JSONUtils.ARRAY_START);
        for (int i = 0; i < values.size(); i++) {
            builder.append(values.get(i));
            if (i < values.size() - 1) builder.append(STR."\{JSONUtils.COMMA} ");
        }
        return builder.append("]").toString();
    }

    /**
     * Parse a list of lines into a JSONArray
     * @param lines (List<String>): the lines to parse
     * @return (JSONArray): the parsed JSONArray
     */
    private static JSONArray parse(List<String> lines) {
        // Remove the containing brackets if needed
        if (lines.getFirst().equals(STR."\{JSONUtils.ARRAY_START}") &&
        (lines.getLast().equals(STR."\{JSONUtils.ARRAY_END}") || lines.getLast().equals(JSONUtils.ARRAY_END_COMMA))) {
            lines.removeFirst();
            lines.removeLast();
        }

        final JSONArray json = new JSONArray();
        boolean parsed;

        // Variables used to parse JSONObjects
        boolean inObject = false;
        int objectStart = -1;
        int curlyBracketCount = 0;

        // Variables used to parse JSONArrays
        boolean inArray = false;
        int arrayStart = -1;
        int bracketCount = 0;

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i) == null || lines.get(i).isEmpty()) continue;

            // Get the current line and split it into parts
            parsed = false;
            String line = lines.get(i).trim();

            // Check if we are looking at an empty object or array
            if (!inObject && !inArray && (line.replaceAll("\\s*", "").contains(JSONUtils.OBJECT_EMPTY)
                    || line.replaceAll("\\s*", "").contains(JSONUtils.ARRAY_EMPTY))) {
                json.append(line.contains(JSONUtils.OBJECT_EMPTY) ? new JSONObject() : new JSONArray());
            }

            // Check if we are at the start of an object or nested object
            if (!inArray && line.matches(JSONUtils.CHECK_OBJECT_START)) {
                if (!inObject) {
                    inObject = true;
                    objectStart = i;
                }
                curlyBracketCount += JSONUtils.findAll(line, STR."\{JSONUtils.OBJECT_START}");
            }
            // Check if we are at the end of an object or nested object
            if (!inArray && inObject && line.matches(JSONUtils.CHECK_OBJECT_END)) {
                curlyBracketCount -= JSONUtils.findAll(line, STR."\{JSONUtils.OBJECT_END}");
                if (curlyBracketCount == 0) {
                    json.append(JSONObject.parse(String.join("", JSONUtils.format(lines, objectStart, i))));

                    // Reset the variables
                    objectStart = -1;
                    inObject = false;
                    parsed = true;
                }
            }

            // Check if we are at the start of an array or nested array
            if (!inObject && line.matches(JSONUtils.CHECK_ARRAY_START)) {
                if (!inArray) {
                    inArray = true;
                    arrayStart = i;
                }
                bracketCount += JSONUtils.findAll(line, STR."\{JSONUtils.ARRAY_START}");
            }
            // Check if we are at the end of an array or nested array
            if (!inObject && inArray && line.matches(JSONUtils.CHECK_ARRAY_END)) {
                bracketCount -= JSONUtils.findAll(line, STR."\{JSONUtils.ARRAY_END}");
                if (bracketCount == 0) {
                    json.append(JSONArray.parse(String.join("", JSONUtils.format(lines, arrayStart, i))));

                    // Reset the variables
                    arrayStart = -1;
                    inArray = false;
                    parsed = true;
                }
            }

            // Parse the line
            if (!inObject && !inArray && !parsed) json.append(JSONValue.parse(line));
        }
        return json;
    }

    /**
     * Parses the given json into a JSONArray
     * @param json (String): The json to parse
     * @return (JSONArray): The parsed array
     */
    public static JSONArray parse(String json) {
        if (!json.startsWith(STR."\{JSONUtils.ARRAY_START}") ||
                !(json.endsWith(STR."\{JSONUtils.ARRAY_END}") || json.endsWith(JSONUtils.ARRAY_END_COMMA))) {
            throw new IllegalArgumentException("Invalid JSON array format: bad start or end");
        }
        // Remove the last comma if it exists
        if (json.endsWith(JSONUtils.ARRAY_END_COMMA)) json = json.substring(0, json.length() - 1);

        // Removing the outermost brackets and parse the lines
        return parse(JSONUtils.split(json));
    }
}