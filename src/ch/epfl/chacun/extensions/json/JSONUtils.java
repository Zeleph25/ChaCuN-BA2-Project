package ch.epfl.chacun.extensions.json;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSONUtils {
    public static final char COMMA = ',';
    public static final char QUOTE = '\"';
    public static final char BACKSLASH = '\\';
    public static final String QUOTE_COMMA = STR."\{QUOTE}\{COMMA}";
    public static final char COLON = ':';
    public static final String SEPARATOR = ": ";
    public static final char NEW_LINE = '\n';
    public static final char OBJECT_START = '{';
    public static final char OBJECT_END = '}';
    public static final String OBJECT_END_COMMA = STR."\{OBJECT_END}\{COMMA}";
    public static final String OBJECT_EMPTY = STR."\{OBJECT_START}\{OBJECT_END}";
    public static final char ARRAY_START = '[';
    public static final char ARRAY_END = ']';
    public static final String ARRAY_END_COMMA = STR."\{ARRAY_END}\{COMMA}";
    public static final String ARRAY_EMPTY = STR."\{ARRAY_START}\{ARRAY_END}";
    public static final String CHECK_OBJECT_START = ".*(?<!\"[^\"{])\\{(?![^{]*\").*";
    public static final String CHECK_OBJECT_END = ".*(?<!\"[^\"}])\\}(?![^}]*\").*";
    public static final String CHECK_ARRAY_START = ".*(?<!\"[^\"\\[])\\[(?![^\\[]*\").*";
    public static final String CHECK_ARRAY_END = ".*(?<!\"[^\"\\]])\\](?![^\\]]*\").*";

    /** Private constructor to prevent instantiation */
    private JSONUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Splits a line into two parts: the key and the value
     * @param line (String): the line to split
     * @return (String[]): the key and the value (in that order)
     */
    protected static String[] splitLine(String line) {
        if (line == null || line.isEmpty())
            throw  new InvalidParameterException(STR."Invalid line: \{line}");

        String[] parts = line.split(STR."\{COLON} ", 2);
        if (parts.length != 2) throw new InvalidParameterException(STR."Invalid JSON string: \{line}");
        return parts;
    }

    /**
     * Splits a json object/array into its parts
     * @param line (String): The json array to split
     * @return (List<String>): The parts of the json array
     */
    protected static List<String> split(String line) {
        if (line == null || line.isEmpty()) throw new IllegalArgumentException(STR."Invalid line: \{line}");

        StringBuilder formattedString = new StringBuilder();
        boolean inQuotes = false;
        int curlyBracketCount = 0;
        int bracketCount = 0;

        char[] chars = line.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            char lastChar = (i > 0) ? chars[i - 1] : ' ';
            char lastLastChar = (i > 1) ? chars[i - 2] : ' ';

            if (lastChar == BACKSLASH && !(STR."\{c}").matches("[bfunrt\"'\\\\/]"))
                throw new IllegalArgumentException(STR."Invalid escape sequence: \{lastChar}\{c}");

            // Check if the current characters are in between quotes
            if (c == QUOTE && lastChar != BACKSLASH && lastLastChar != BACKSLASH) inQuotes = !inQuotes;

            // Put newlines in the correct places
            if ((c == OBJECT_START || c == ARRAY_START || c == COMMA) && !inQuotes) {
                formattedString.append(c).append(NEW_LINE);
                if (c == OBJECT_START) curlyBracketCount++;
                else if (c == ARRAY_START) bracketCount++;
            }
            else if ((c == OBJECT_END || c == ARRAY_END) && !inQuotes) {
                formattedString.append(NEW_LINE).append(c);
                if (c == OBJECT_END) curlyBracketCount--;
                else bracketCount--;
            }
            else if (c == ' ' && inQuotes) formattedString.append(c);
            else formattedString.append(c);
        }

        // Make sure there are the same numbers of brackets and curly brackets
        if (curlyBracketCount > 0 || bracketCount > 0) throw new IllegalArgumentException(STR."Invalid JSON: \{line}");

        // Join the strings and split into lines
        String[] parts = formattedString.toString().replaceAll(STR."\{COLON}\\s*", SEPARATOR).split(STR."\{NEW_LINE}");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                // Check for invalid json
                if (findAll(part, QUOTE + SEPARATOR, true) > 1)
                    throw new IllegalArgumentException(STR."Missing comma in JSON: \{part}");

                // Check if we need to format the line
                else if (part.equals(STR."\{OBJECT_END}\{OBJECT_START}"))
                    result.addAll(List.of(STR."\{OBJECT_END}", STR."\{OBJECT_START}"));
                else if (part.equals(STR."\{ARRAY_END}\{ARRAY_START}"))
                    result.addAll(List.of(STR."\{ARRAY_END}", STR."\{ARRAY_START}"));

                // Add the default line
                else result.add(part);
            }
        }

        // Check for an extra comma at the end of the array
        if (result.getLast().endsWith(STR."\{OBJECT_END}") || result.getLast().endsWith(STR."\{ARRAY_END}") ||
                result.getLast().endsWith(ARRAY_END_COMMA) || result.getLast().endsWith(OBJECT_END_COMMA)) {
            if (result.get(result.size() - 2).endsWith(STR."\{COMMA}"))
                throw new IllegalArgumentException("Invalid Trailing Comma in JSON object: " +
                        STR."\{result.get(result.size() - 2)}");
        }
        // Check for a missing comma in between two lines

        return result;
    }

    /**
     * Used to check how many times a regex is found in a string
     * @param line (String): the string to check
     * @param regex (CharSequence): the regex to check against
     * @return (int): the number of times the regex is found in the string
     */
    protected static int findAll(String line, CharSequence regex) {
        return findAll(line, regex, false);
    }

    /**
     * Used to check how many times a regex is found in a string
     * @param line (String): the string to check
     * @param regex (CharSequence): the regex to check against
     * @return (int): the number of times the regex is found in the string
     */
    protected static int findAll(String line, CharSequence regex, boolean useRegexPatternMatching) {
        if (line == null || line.isEmpty()) throw  new InvalidParameterException(STR."Invalid line: \{line}");
        else if (regex == null || regex.isEmpty()) throw  new InvalidParameterException(STR."Invalid regex: \{line}");

        int count = 0;
        if (!useRegexPatternMatching) {
            int index = 0;
            while ((index = line.indexOf(regex.toString(), index)) != -1) {
                count++;
                index++;
            }
        } else {
            Pattern pattern = Pattern.compile(regex.toString());
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) count++;
        }
        return count;
    }

    /**
     * Used to format a list of lines
     * @param lines (List<String>): the lines to format
     * @param start (int): the start index
     * @param end (int): the end index
     * @return (ArrayList<String>): the formatted lines
     */
    protected static ArrayList<String> format(List<String> lines, int start, int end) {
        if (lines.get(start).contains(STR."\{COLON}")) {
            ArrayList<String> formattedLines = new ArrayList<>(
                    Collections.singleton(lines.get(start).split(STR."\{COLON}\\s*", 2)[1])
            );
            formattedLines.addAll(lines.subList(start + 1, end + 1));
            return formattedLines;
        } else return new ArrayList<>(lines.subList(start, end + 1));
    }

    /**
     * Checks if the given string is an integer
     * @param str (String): The string to check
     * @return (boolean): True if the string is an integer, false otherwise
     */
    public static boolean isInteger(String str) {
        // "-?(?!0\d)\d+" => match whole numbers
        //  - "-?" => check for an optional - sign before the actual number
        //  - "(?!0\d)" => make sure the number doesn't start with a 0
        //  - "\\d*" => check for at least ont number characters (0123456789)
        return str.matches("-?(?!0\\d)\\d+");
    }

    /**
     * Checks if the given string is a boolean
     * @param str (String): The string to check
     * @return (boolean): True if the string is a boolean, false otherwise
     */
    public static boolean isNull(String str) {
        return "null".equalsIgnoreCase(str);
    }

    /**
     * Checks if the given string is a boolean
     * @param str (String): The string to check
     * @return (boolean): True if the string is a boolean, false otherwise
     */
    public static boolean isBoolean(String str) {
        return "true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str);
    }

    /**
     * Checks if the given string is a double
     * @param str (String): The string to check
     * @return (boolean): True if the string is a double, false otherwise
     */
    public static boolean isDouble(String str) {
        // "NaN|-?Infinity|-?(?!0\d)\d+(\.\d*)?([eE][+-]?\d+)?" => match normal and scientifique notation numbers
        //  - "NaN" => check for not a number
        //  - "-?Infinity" => check for positive or negative infinity
        //  - "-?" => check for an optional - sign before the actual number
        //  - "(?!0\d)" => make sure the number doesn't start with a 0
        //  - "\\d+" => check for at least one number character (0123456789)
        //  - "(\.\d+)?" => check for an optional fractional part of the number
        //  - "([eE][+-]?\d+)?" => check for exponential notation and numbers after it
        return str.matches("NaN|-?Infinity|-?(?!0\\d)\\d+(\\.\\d*)?([eE][+-]?\\d+)?");
    }
}
