package ch.epfl.chacun;

/**
 * Represents Base32 encoding and decoding to binary (Integers)
 * @author Adam BEKKAR (379476)
 * */
public class Base32 {
    /** The alphabet used for encoding and decoding */
    public static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    /** Private constructor to prevent instantiation */
    private Base32() {};

    public static boolean isValid(String s) {
        return s.chars().allMatch(c -> ALPHABET.indexOf(c) != -1);
    }

    /**
     * Encodes an integer into a Base32 string
     * @param integer The integer to encode
     * @param count The number of Base32 characters to use
     * @return The Base32 string
     */
    private static String encodeBits(int integer, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            // We take the 5 least significant bits of the integer and get the corresponding character
            sb.append(ALPHABET.charAt(integer & 0x1f));
            // We shift right by 5 to get the next 5 bits
            integer >>= 5;
        }
        return sb.reverse().toString();
    }

    /**
     * Encodes an integer into a Base32 string using one character
     * @param integer The integer to encode
     * @return The Base32 string
     */

    public static String encodeBits5(int integer) {
        return encodeBits(integer, 1);
    }

    /**
     * Encodes an integer into a Base32 string using two characters
     * @param integer The integer to encode
     * @return The Base32 string
     */
    public static String encodeBits10(int integer) {
        return encodeBits(integer, 2);
    }

    /**
     * Decode a Base32 string of on or two characters into an integer
     * @param s The integer to encode
     * @return The Base32 string
     */
    public static Integer decode(String s) {
        // Check that the string is not empty and that it's valid
        Preconditions.checkArgument(!s.isEmpty() && s.length() <= 2 && isValid(s));
        int result = 0;
        for (int i = 0; i < s.length(); i++) {
            // Decoding a character is just getting its index in the alphabet
            int value = ALPHABET.indexOf(s.charAt(i));
            // We shift left by 5 to multiply by 32 and then add the value of the character
            result = (result << 5) | value;
        }
        return result;
    }
}
