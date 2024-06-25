package ch.epfl.chacun.extensions.json;

/** Used to represent json values */
public class JSONValue {
    /** The string representation of this object */
    private String value;

    /**
     * Used to set the value of this object
     * @param value (String): The new value of this object
     */
    private JSONValue set(String value) {
        this.value = value;
        return this;
    }

    /**
     * Used to get the value of this object
     * @return (String): The value of this object
     */
    public String asString() {
        return value;
    }

    /**
     * Used to get the value of this object
     * @throws UnsupportedOperationException if the value is not a boolean
     * @return (Boolean): The value of this object
     */
    public Boolean asBoolean() {
        if (JSONUtils.isBoolean(value)) return Boolean.parseBoolean(value);
        else throw new UnsupportedOperationException("Value is not a boolean");
    }

    /**
     * Used to get the value of this object
     * @throws UnsupportedOperationException if the value is not an integer
     * @return (Integer): The value of this object
     */
    public Integer asInteger() {
        if (JSONUtils.isInteger(value)) return Integer.parseInt(value);
        else throw new UnsupportedOperationException("Value is not an integer");
    }

    /**
     * Used to get the value of this object
     * @throws UnsupportedOperationException if the value is not a number
     * @return (Double): The value of this object
     */
    public Double asDouble() {
        if (JSONUtils.isDouble(value)) return Double.parseDouble(value);
        else throw new UnsupportedOperationException("Value is not a number");
    }

    /**
     * Used to get the value of this object
     * @param key (String): The key of the value to get
     * @throws UnsupportedOperationException if the method is not supported
     * @return (JSONValue): The value of this object
     */
    public JSONValue get(String key) {
        throw new UnsupportedOperationException(STR."This method is not supported for \{getClass().getSimpleName()}'s");
    }

    /**
     * Used to get the value of this object
     * @param index (int): The index of the value to get
     * @throws UnsupportedOperationException if the method is not supported
     * @return (JSONValue): The value of this object
     */
    public JSONValue get(int index) {
        throw new UnsupportedOperationException(STR."This method is not supported for \{getClass().getSimpleName()}'s");
    }

    /**
     * Used to get the value of this object as an JSONArray
     * @param key (String): The key of the value to get
     * @throws UnsupportedOperationException if the method is not supported
     * @return (JSONObject): The value of this object
     */
    public JSONArray getArray(String key) {
        throw new UnsupportedOperationException(STR."This method is not supported for \{getClass().getSimpleName()}'s");
    }

    /**
     * Used to get the value of this object as an JSONObject
     * @param key (String): The key of the value to get
     * @throws UnsupportedOperationException if the method is not supported
     * @return (JSONObject): The value of this object
     */
    public JSONObject getObject(String key) {
        throw new UnsupportedOperationException(STR."This method is not supported for \{getClass().getSimpleName()}'s");
    }

    /**
     * Used to get the value of this object as an JSONArray
     * @param index (int): The index of the value to get
     * @throws UnsupportedOperationException if the method is not supported
     * @return (JSONObject): The value of this object
     */
    public JSONArray getArray(int index) {
        throw new UnsupportedOperationException(STR."This method is not supported for \{getClass().getSimpleName()}'s");
    }

    /**
     * Used to get the value of this object as an JSONObject
     * @param index (int): The index of the value to get
     * @throws UnsupportedOperationException if the method is not supported
     * @return (JSONObject): The value of this object
     */
    public JSONObject getObject(int index) {
        throw new UnsupportedOperationException(STR."This method is not supported for \{getClass().getSimpleName()}'s");
    }

    /**
     * Used to get the value of this object as an JSONArray
     * @throws UnsupportedOperationException if the value is not a JSONArray
     * @return (JSONArray): The value of this object
     */
    public JSONArray toJSONArray() {
        if (this instanceof JSONArray array) return array;
        else throw new UnsupportedOperationException("This value is not a for JSONArray");
    }

    /**
     * Used to get the value of this object as an JSONObject
     * @throws UnsupportedOperationException if the value is not a JSONObject
     * @return (JSONObject): The value of this object
     */
    public JSONObject toJSONObject() {
        if (this instanceof JSONObject object) return object;
        else throw new UnsupportedOperationException("This value is not a for JSONObject");
    }

    /**
     * @implNote This method formats the value of this object into a json string,
     *           use asString() to get the value of this object
     */
    @Override
    public String toString() {
        return (JSONUtils.isBoolean(value) || JSONUtils.isInteger(value) || JSONUtils.isNull(value) || JSONUtils.isDouble(value)) ? value : JSONUtils.QUOTE + value + JSONUtils.QUOTE;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        JSONValue other = (JSONValue) obj;
        return toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Parse the given json value into a JSONString
     * @param value (String): the json value
     * @return the created json value
     */
    public static JSONValue parse(String value) {
        if (value == null) return new JSONValue().set(null);

        // Remove the quote if needed
        if (value.endsWith(STR."\{JSONUtils.COMMA}")) value = value.substring(0, value.length() - 1);

        // Remove the quotes from the value if needed
        value = value.trim().replaceAll("\"", "");
        return new JSONValue().set(value);
    }
}