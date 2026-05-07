package com.jef.justenoughfakepixel.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

// Advanced calculator for math expressions with functions, operators, and multipliers
public class CalculatorUtils {

    public static final DecimalFormat FORMAT = new DecimalFormat("#,##0.##########");
    private static final String BINOPS = "+-*/x^%";
    private static final String POSTOPS = "mkbts";
    private static final String DIGITS = "0123456789";

    private static final Map<String, BigDecimal> CONSTANTS = new HashMap<>();
    static {
        CONSTANTS.put("pi", new BigDecimal(Math.PI));
        CONSTANTS.put("e", new BigDecimal(Math.E));
    }

    // Calculate and format result
    public static String calculateAndFormat(String expression) {
        if (expression == null || expression.isEmpty()) return null;
        try {
            BigDecimal result = calculate(expression);
            return FORMAT.format(result);
        } catch (CalculatorException e) {
            return null;
        }
    }

    // Parse expression into result
    public static BigDecimal calculate(String source) throws CalculatorException {
        return evaluate(shuntingYard(lex(source.toLowerCase(Locale.ROOT))));
    }

    private static void readDigitsInto(Token token, String source, boolean decimals) {
        int start = token.tokenStart + token.tokenLength;
        for (int j = 0; j + start < source.length(); j++) {
            int d = DIGITS.indexOf(source.charAt(j + start));
            if (d == -1) return;
            if (decimals) token.exponent--;
            token.numericValue = token.numericValue * 10 + d;
            token.tokenLength++;
        }
    }

    private static List<Token> lex(String source) throws CalculatorException {
        List<Token> tokens = new ArrayList<>();

        for (int i = 0; i < source.length(); ) {
            char c = source.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            Token t = new Token();
            t.tokenStart = i;

            if (BINOPS.indexOf(c) != -1) {
                t.tokenLength = 1;
                t.type = TokenType.BINOP;
                t.operatorValue = String.valueOf(c);
            } else if (c == ')' || c == '(') {
                t.tokenLength = 1;
                t.type = c == ')' ? TokenType.RPAREN : TokenType.LPAREN;
                t.operatorValue = String.valueOf(c);
            } else if (c == ',') {
                t.tokenLength = 1;
                t.type = TokenType.COMMA;
                t.operatorValue = ",";
            } else if (c == '.') {
                t.tokenLength = 1;
                t.type = TokenType.NUMBER;
                readDigitsInto(t, source, true);
                if (t.tokenLength == 1) throw new CalculatorException("Invalid number literal", i, 1);
            } else if (DIGITS.indexOf(c) != -1) {
                t.type = TokenType.NUMBER;
                readDigitsInto(t, source, false);
                if (i + t.tokenLength < source.length() && source.charAt(i + t.tokenLength) == '.') {
                    t.tokenLength++;
                    readDigitsInto(t, source, true);
                }

                // Check for postfix after number (100k, 50m, etc)
                if (i + t.tokenLength < source.length()) {
                    char next = source.charAt(i + t.tokenLength);
                    if (POSTOPS.indexOf(next) != -1) {
                        tokens.add(t);
                        i += t.tokenLength;

                        t = new Token();
                        t.tokenStart = i;
                        t.tokenLength = 1;
                        t.type = TokenType.POSTOP;
                        t.operatorValue = String.valueOf(next);
                    }
                }
            } else if (Character.isLetter(c)) {
                int start = i;
                int end = i;
                while (end < source.length() && Character.isLetter(source.charAt(end))) {
                    end++;
                }
                String name = source.substring(start, end);
                t.tokenLength = end - start;

                if (CONSTANTS.containsKey(name)) {
                    t.type = TokenType.CONSTANT;
                    t.operatorValue = name;
                } else {
                    t.type = TokenType.FUNCTION;
                    t.operatorValue = name;
                }
            } else {
                throw new CalculatorException("Unknown character: " + c, i, 1);
            }

            tokens.add(t);
            i += t.tokenLength;
        }
        return tokens;
    }

    private static int getPrecedence(Token t) throws CalculatorException {
        switch (t.operatorValue) {
            case "+":
            case "-":
                return 1;
            case "*":
            case "/":
            case "x":
            case "%":
                return 2;
            case "^":
                return 3;
            default:
                throw new CalculatorException("Unknown operator " + t.operatorValue, t.tokenStart, t.tokenLength);
        }
    }

    private static boolean isRightAssociative(Token t) {
        return "^".equals(t.operatorValue);
    }

    private static List<Token> shuntingYard(List<Token> tokens) throws CalculatorException {
        Deque<Token> op = new ArrayDeque<>();
        List<Token> out = new ArrayList<>();

        for (Token t : tokens) {
            switch (t.type) {
                case NUMBER:
                case POSTOP:
                case CONSTANT:
                    out.add(t);
                    break;
                case FUNCTION:
                    op.push(t);
                    break;
                case COMMA:
                    while (!op.isEmpty() && op.peek().type != TokenType.LPAREN) {
                        out.add(op.pop());
                    }
                    break;
                case BINOP:
                    int p = getPrecedence(t);
                    boolean rightAssoc = isRightAssociative(t);
                    while (!op.isEmpty() && op.peek().type == TokenType.BINOP) {
                        int opPrec = getPrecedence(op.peek());
                        if ((rightAssoc && opPrec > p) || (!rightAssoc && opPrec >= p)) {
                            out.add(op.pop());
                        } else {
                            break;
                        }
                    }
                    op.push(t);
                    break;
                case LPAREN:
                    op.push(t);
                    break;
                case RPAREN:
                    while (true) {
                        if (op.isEmpty())
                            throw new CalculatorException("Unbalanced right parenthesis", t.tokenStart, t.tokenLength);
                        Token l = op.pop();
                        if (l.type == TokenType.LPAREN) break;
                        out.add(l);
                    }
                    // Pop function if present after closing paren
                    if (!op.isEmpty() && op.peek().type == TokenType.FUNCTION) {
                        out.add(op.pop());
                    }
                    break;
            }
        }
        while (!op.isEmpty()) {
            Token l = op.pop();
            if (l.type == TokenType.LPAREN)
                throw new CalculatorException("Unbalanced left parenthesis", l.tokenStart, l.tokenLength);
            out.add(l);
        }
        return out;
    }

    private static BigDecimal evaluate(List<Token> rpn) throws CalculatorException {
        Deque<BigDecimal> stack = new ArrayDeque<>();
        try {
            for (Token t : rpn) {
                switch (t.type) {
                    case NUMBER:
                        stack.push(new BigDecimal(t.numericValue).scaleByPowerOfTen(t.exponent));
                        break;
                    case CONSTANT:
                        stack.push(CONSTANTS.get(t.operatorValue));
                        break;
                    case BINOP: {
                        BigDecimal r = stack.pop();
                        BigDecimal l = stack.pop();
                        switch (t.operatorValue) {
                            case "x":
                            case "*":
                                stack.push(l.multiply(r).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros());
                                break;
                            case "/":
                                try {
                                    BigDecimal result = l.divide(r, 10, RoundingMode.HALF_UP).stripTrailingZeros();
                                    stack.push(result);
                                } catch (ArithmeticException e) {
                                    throw new CalculatorException("Division by zero", t.tokenStart, t.tokenLength);
                                }
                                break;
                            case "+":
                                stack.push(l.add(r).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros());
                                break;
                            case "-":
                                stack.push(l.subtract(r).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros());
                                break;
                            case "^":
                                stack.push(new BigDecimal(Math.pow(l.doubleValue(), r.doubleValue())));
                                break;
                            case "%":
                                stack.push(l.remainder(r));
                                break;
                            default:
                                throw new CalculatorException("Unknown operator " + t.operatorValue, t.tokenStart, t.tokenLength);
                        }
                        break;
                    }
                    case POSTOP: {
                        BigDecimal v = stack.pop();
                        switch (t.operatorValue) {
                            case "s":
                                stack.push(v.multiply(new BigDecimal(64)));
                                break;
                            case "k":
                                stack.push(v.multiply(new BigDecimal(1_000)));
                                break;
                            case "m":
                                stack.push(v.multiply(new BigDecimal(1_000_000)));
                                break;
                            case "b":
                                stack.push(v.multiply(new BigDecimal(1_000_000_000)));
                                break;
                            case "t":
                                stack.push(v.multiply(new BigDecimal("1000000000000")));
                                break;
                            default:
                                throw new CalculatorException("Unknown postop " + t.operatorValue, t.tokenStart, t.tokenLength);
                        }
                        break;
                    }
                    case FUNCTION: {
                        BigDecimal result = evaluateFunction(t.operatorValue, stack, t);
                        stack.push(result);
                        break;
                    }
                    default:
                        throw new CalculatorException("Unexpected token", t.tokenStart, t.tokenLength);
                }
            }
            return stack.pop().stripTrailingZeros();
        } catch (NoSuchElementException e) {
            throw new CalculatorException("Unfinished expression", 0, 0);
        }
    }

    private static BigDecimal evaluateFunction(String func, Deque<BigDecimal> stack, Token t) throws CalculatorException {
        switch (func) {
            // Trig (radians)
            case "sin":
                return new BigDecimal(Math.sin(stack.pop().doubleValue()));
            case "cos":
                return new BigDecimal(Math.cos(stack.pop().doubleValue()));
            case "tan":
                return new BigDecimal(Math.tan(stack.pop().doubleValue()));
            case "asin": // arcsine
                return new BigDecimal(Math.asin(stack.pop().doubleValue()));
            case "acos": // arccosine
                return new BigDecimal(Math.acos(stack.pop().doubleValue()));
            case "atan": // arctangent
                return new BigDecimal(Math.atan(stack.pop().doubleValue()));

            // Trig (degrees)
            case "sind":
                return new BigDecimal(Math.sin(Math.toRadians(stack.pop().doubleValue())));
            case "cosd":
                return new BigDecimal(Math.cos(Math.toRadians(stack.pop().doubleValue())));
            case "tand": // tangent degrees
                return new BigDecimal(Math.tan(Math.toRadians(stack.pop().doubleValue())));

            // Logarithms
            case "log":
            case "log10":
                return new BigDecimal(Math.log10(stack.pop().doubleValue()));
            case "ln":
                return new BigDecimal(Math.log(stack.pop().doubleValue()));
            case "log2":
                return new BigDecimal(Math.log(stack.pop().doubleValue()) / Math.log(2));

            // Powers and roots
            case "sqrt":
                return new BigDecimal(Math.sqrt(stack.pop().doubleValue()));
            case "cbrt":
                return new BigDecimal(Math.cbrt(stack.pop().doubleValue()));
            case "pow": {
                BigDecimal exp = stack.pop();
                BigDecimal base = stack.pop();
                return new BigDecimal(Math.pow(base.doubleValue(), exp.doubleValue()));
            }

            // Rounding
            case "abs":
                return stack.pop().abs();
            case "ceil":
                return new BigDecimal(Math.ceil(stack.pop().doubleValue()));
            case "floor":
                return new BigDecimal(Math.floor(stack.pop().doubleValue()));
            case "round":
                return stack.pop().setScale(0, RoundingMode.HALF_UP);

            // Multi-arg
            case "max": {
                BigDecimal b = stack.pop();
                BigDecimal a = stack.pop();
                return a.max(b);
            }
            case "min": {
                BigDecimal b = stack.pop();
                BigDecimal a = stack.pop();
                return a.min(b);
            }

            default:
                throw new CalculatorException("Unknown function: " + func, t.tokenStart, t.tokenLength);
        }
    }

    private enum TokenType {NUMBER, BINOP, LPAREN, RPAREN, POSTOP, FUNCTION, CONSTANT, COMMA}

    private static class Token {
        TokenType type;
        String operatorValue;
        long numericValue;
        int exponent, tokenStart, tokenLength;
    }

    public static class CalculatorException extends Exception {
        public final int offset;
        public final int length;

        public CalculatorException(String message, int offset, int length) {
            super(message);
            this.offset = offset;
            this.length = length;
        }
    }
}
