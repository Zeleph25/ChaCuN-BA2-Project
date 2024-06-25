package ch.epfl.chacun.extensions.json;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;

/** Used to read JSON files and write to JSON files */
public final class JSONParser {
    /**
     * The valid file extensions for JSON files
     */
    private static final String[] FILE_EXTENSIONS = new String[] { ".json", ".txt" };

    /**
     * Used to read JSON files and get their contents as a JSONObject
     *
     * @param path (String): the path to the file
     * @return (JSONValue): the contents of the file
     */
    public static JSONValue readJSONFromFile(String path) {
        validateFile(path, true);

        // Read the file
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            ArrayList<String> lines = new ArrayList<>();
            String line;

            while ((line = reader.readLine()) != null) lines.add(line.trim());

            // Join the string so that it can be formatted correctly by the parser
            return readJSONFromString(String.join("", lines));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Used to read JSON strings and get their contents as a JSONObject
     *
     * @param json (String): the JSON string
     * @return (JSONValue): the contents of the string
     */
    public static JSONValue readJSONFromString(String json) {
        if (json == null || json.isEmpty())
            throw new IllegalArgumentException(STR."Invalid JSON string: can not be \{json == null ? "null" : "empty"}");

        return parse(json);
    }

    /**
     * Used to write a JSONObject to a file
     *
     * @param path (String): the path to the file
     * @param json (JSONObject): the JSONObject to write
     */
    public static void writeJSONToFile(String path, JSONValue json) {
        validateFile(path, false);

        try {
            final File file = new File(path);
            if (!file.exists()) Files.createFile(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Check if the JSONObject is null
        if (json == null) throw new IllegalArgumentException("The JSON to write cannot be null");

        final JSONObject obj = new JSONObject();
        if (json.getClass().getSimpleName().equals("JSONValue")) obj.add("key", json);

        // Write to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write((json.getClass().getSimpleName().equals("JSONValue") ? obj : json).toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Used to validate a file path
     *
     * @param path (String): the path to validate
     */
    private static void validateFile(String path, boolean checkExists) {
        // Check if the path is null or empty
        if (path == null || path.isEmpty())
            throw new IllegalArgumentException("Invalid path: it can not be null or empty");

        boolean valid = false;
        for (String extension : FILE_EXTENSIONS) {
            final int length = extension.length();
            final String ext = path.substring(path.length() - length);
            // Check if the file extension is valid
            if (ext.toLowerCase().equals(extension)) {
                valid = true;
                break;
            }
        }
        if (!valid) throw new IllegalArgumentException(STR."Invalid file extension: .\{path.split("\\.")[1]}");
        if (!checkExists) return;

        // Check if the file exists and is readable
        final File file = new File(path);
        if (!file.exists()) throw new IllegalArgumentException(STR."File not found: \{path}");
        if (!file.canRead()) throw new IllegalArgumentException(STR."File is not readable: \{path}");
    }

    /**
     * Used to parse a JSON string
     *
     * @param json (String): the JSON string to parse
     * @return (JSONValue): the parsed JSON string
     */
    private static JSONValue parse(String json) {
        if (json.startsWith(STR."\{JSONUtils.OBJECT_START}")) return JSONObject.parse(json);
        else if (json.startsWith(STR."\{JSONUtils.ARRAY_START}")) return JSONArray.parse(json);
        else throw new IllegalArgumentException(STR."Invalid JSON string: \{json}");
    }
}