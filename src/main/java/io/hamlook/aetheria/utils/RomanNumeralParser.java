package io.hamlook.aetheria.utils;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RomanNumeralParser {

    private static final NavigableMap<Integer, String> INT_ROMAN_MAP = new TreeMap<>();
    private static final Pattern VALIDATION = Pattern.compile("^(?=[MDCLXVI])M*(C[MD]|D?C{0,3})(X[CL]|L?X{0,3})(I[XV]|V?I{0,3})$");
    private static final Pattern FINDER = Pattern.compile(" (?=[MDCLXVI])(M*(?:C[MD]|D?C{0,3})(?:X[CL]|L?X{0,3})(?:I[XV]|V?I{0,3}))(.?)");

    static {
        INT_ROMAN_MAP.put(1000, "M");
        INT_ROMAN_MAP.put(900, "CM");
        INT_ROMAN_MAP.put(500, "D");
        INT_ROMAN_MAP.put(400, "CD");
        INT_ROMAN_MAP.put(100, "C");
        INT_ROMAN_MAP.put(90, "XC");
        INT_ROMAN_MAP.put(50, "L");
        INT_ROMAN_MAP.put(40, "XL");
        INT_ROMAN_MAP.put(10, "X");
        INT_ROMAN_MAP.put(9, "IX");
        INT_ROMAN_MAP.put(5, "V");
        INT_ROMAN_MAP.put(4, "IV");
        INT_ROMAN_MAP.put(1, "I");
    }

    private RomanNumeralParser() {
    }

    public static boolean isValid(String s) {
        return VALIDATION.matcher(s).matches();
    }

    public static int parse(String s) {
        if (!isValid(s)) throw new IllegalArgumentException("Not a valid Roman numeral: " + s);
        int value = 0;
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            Numeral cur = Numeral.of(chars[i]);
            Numeral next = i + 1 < chars.length ? Numeral.of(chars[i + 1]) : null;
            if (next != null && next.value > cur.value) {
                value += next.value - cur.value;
                i++;
            } else {
                value += cur.value;
            }
        }
        return value;
    }

    public static String toRoman(int number) {
        if (number <= 0) return "I";
        StringBuilder out = new StringBuilder();
        for (Map.Entry<Integer, String> entry : INT_ROMAN_MAP.descendingMap().entrySet()) {
            while (number >= entry.getKey()) {
                number -= entry.getKey();
                out.append(entry.getValue());
            }
        }
        return out.toString();
    }

    public static String replaceInString(String input) {
        if (input == null || input.isEmpty()) return input;
        StringBuffer result = new StringBuffer();
        Matcher matcher = FINDER.matcher(input);
        while (matcher.find()) {
            String numeral = matcher.group(1);
            String after = matcher.group(2);
            if (numeral.isEmpty() || Pattern.compile("^[\\w-']").matcher(after).find()) continue;
            int value = parse(numeral);
            if (value == 1 && !after.equals("§") && !after.isEmpty()) continue;
            matcher.appendReplacement(result, " " + value + "$2");
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private enum Numeral {
        I(1), V(5), X(10), L(50), C(100), D(500), M(1000);
        final int value;

        Numeral(int v) {
            this.value = v;
        }

        static Numeral of(char c) {
            try {
                return valueOf(String.valueOf(c));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid numeral char: " + c);
            }
        }
    }
}