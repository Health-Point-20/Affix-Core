package net.yixi_xun.affix_core.api;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.yixi_xun.affix_core.AffixCoreMod.LOGGER;

/**
 * 一个无外部依赖的配置表达式计算器。
 * 它内部使用调度场算法来解析和计算表达式。
 * 在出错时记录日志并返回 0 或 false。
 * <p>
 * 支持的运算符：+, -, *, /, ^(幂运算), &&(and), ||(or), !(not)
 * 支持的函数：min, max, abs, sqrt, log
 * 支持变量：通过 Map 传入
 * 支持条件表达式：返回布尔值
 */
public class ExpressionHelper {

    // 运算符优先级
    private static final Map<Token.Type, Integer> OPERATOR_PRECEDENCE = new EnumMap<>(Token.Type.class);
    static {
        // 逻辑运算符优先级最低
        OPERATOR_PRECEDENCE.put(Token.Type.OPERATOR_OR, 0);
        OPERATOR_PRECEDENCE.put(Token.Type.OPERATOR_AND, 1);
        // 比较运算符
        OPERATOR_PRECEDENCE.put(Token.Type.OPERATOR_EQ, 2);
        OPERATOR_PRECEDENCE.put(Token.Type.OPERATOR_NEQ, 2);
        OPERATOR_PRECEDENCE.put(Token.Type.OPERATOR_LT, 2);
        OPERATOR_PRECEDENCE.put(Token.Type.OPERATOR_LTE, 2);
        OPERATOR_PRECEDENCE.put(Token.Type.OPERATOR_GT, 2);
        OPERATOR_PRECEDENCE.put(Token.Type.OPERATOR_GTE, 2);
        // 算术运算符
        OPERATOR_PRECEDENCE.put(Token.Type.OPERATOR_ADD, 3);
        OPERATOR_PRECEDENCE.put(Token.Type.OPERATOR_SUB, 3);
        OPERATOR_PRECEDENCE.put(Token.Type.OPERATOR_MUL, 4);
        OPERATOR_PRECEDENCE.put(Token.Type.OPERATOR_DIV, 4);
        OPERATOR_PRECEDENCE.put(Token.Type.OPERATOR_POW, 5);
        // 一元运算符优先级最高
        OPERATOR_PRECEDENCE.put(Token.Type.OPERATOR_NOT, 6);
        OPERATOR_PRECEDENCE.put(Token.Type.OPERATOR_UNARY_MINUS, 6);
    }

    // 使用 HashSet 存储函数名
    private static final Set<String> FUNCTIONS = Set.of("min", "max", "abs", "sqrt", "log");

    // 表达式缓存
    private static final Map<String, List<Token>> EXPRESSION_CACHE = new ConcurrentHashMap<>();

    /**
     * 计算数学表达式
     *
     * @param expression 要计算的数学表达式字符串
     * @param Variables 本次计算所用的变量映射，值可以是任意Number类型
     * @return 计算结果的 double 值。如果表达式无效或计算失败，则返回 0
     */
    public static double evaluate(String expression, Map<String, ?> Variables) {
        Map<String, Number> localVariables = new HashMap<>();
        for (Map.Entry<String, ?> entry : Variables.entrySet()) {
            if ((entry.getValue() instanceof Number number)){
                localVariables.put(entry.getKey(), number);
            }
        }

        try {
            if (expression == null || expression.trim().isEmpty()) {
                LOGGER.error("Expression is null or empty.");
                return 0;
            }

            // 1. 词法分析
            List<Token> tokens = EXPRESSION_CACHE.computeIfAbsent(expression, ExpressionHelper::tokenize);
            // 2. 语法分析
            List<Token> rpn = infixToRPN(tokens);
            // 3. 计算
            return evaluateRPN(rpn, localVariables);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Failed to evaluate expression: '{}'. Error: {}", expression, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred while evaluating expression: '{}'. Error: {}",
                    expression, e.getMessage(), e);
        }
        return 0;
    }

    /**
     * 便捷方法：使用单个变量计算表达式
     */
    public static double evaluate(String expression, String varName, Number varValue) {
        return evaluate(expression, Map.of(varName, varValue));
    }

    /**
     * 便捷方法：使用两个变量计算表达式
     */
    public static double evaluate(String expression, String var1Name, Number var1Value,
                                  String var2Name, Number var2Value) {
        return evaluate(expression, Map.of(var1Name, var1Value, var2Name, var2Value));
    }

    /**
     * 判断条件表达式是否为真
     *
     * @param expression 要计算的条件表达式字符串
     * @param localVariables 本次计算所用的变量映射
     * @return 如果条件为真则返回true，否则返回false
     */
    public static boolean evaluateCondition(String expression, Map<String, ?> localVariables) {
        double result = evaluate(expression, localVariables);
        return Math.abs(result) > 1e-10; // 非零视为真
    }

    // --- 词法分析 ---
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(\\d+\\.\\d+|\\d+)" +  // 数字
                    "|(true|false)" +  // 布尔值
                    "|([a-zA-Z_][a-zA-Z0-9_]*)" +  // 标识符（变量、函数）
                    "|(&&|\\|\\||==|!=|<=|>=|<|>|!|\\+|-|\\*|/|\\^|\\(|\\))" +  // 运算符和括号
                    "|(\\s+)"  // 空白字符
    );

    private record Token(Type type, String value) {
        enum Type {
            NUMBER, VARIABLE, BOOLEAN,
            OPERATOR_ADD, OPERATOR_SUB, OPERATOR_MUL, OPERATOR_DIV, OPERATOR_POW,
            OPERATOR_AND, OPERATOR_OR, OPERATOR_NOT,
            OPERATOR_EQ, OPERATOR_NEQ, OPERATOR_LT, OPERATOR_LTE, OPERATOR_GT, OPERATOR_GTE,
            LEFT_PAREN, RIGHT_PAREN,
            FUNCTION, OPERATOR_UNARY_MINUS
        }
    }

    private static List<Token> tokenize(String expression) {
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(expression);
        int lastEnd = 0;

        while (matcher.find()) {
            // 检查是否有非法字符
            String invalidChars = expression.substring(lastEnd, matcher.start()).trim();
            if (!invalidChars.isEmpty()) {
                throw new IllegalArgumentException("Invalid character sequence: " + invalidChars);
            }

            String number = matcher.group(1);
            String booleanValue = matcher.group(2);
            String identifier = matcher.group(3);
            String operator = matcher.group(4);
            // String whitespace = matcher.group(5); // 忽略空格

            if (number != null) {
                tokens.add(new Token(Token.Type.NUMBER, number));
            } else if (booleanValue != null) {
                tokens.add(new Token(Token.Type.BOOLEAN, booleanValue));
            } else if (identifier != null) {
                if (FUNCTIONS.contains(identifier)) {
                    tokens.add(new Token(Token.Type.FUNCTION, identifier));
                } else {
                    tokens.add(new Token(Token.Type.VARIABLE, identifier));
                }
            } else if (operator != null) {
                switch (operator) {
                    case "(" -> tokens.add(new Token(Token.Type.LEFT_PAREN, operator));
                    case ")" -> tokens.add(new Token(Token.Type.RIGHT_PAREN, operator));
                    case "+" -> tokens.add(new Token(Token.Type.OPERATOR_ADD, operator));
                    case "-" -> tokens.add(new Token(Token.Type.OPERATOR_SUB, operator));
                    case "*" -> tokens.add(new Token(Token.Type.OPERATOR_MUL, operator));
                    case "/" -> tokens.add(new Token(Token.Type.OPERATOR_DIV, operator));
                    case "^" -> tokens.add(new Token(Token.Type.OPERATOR_POW, operator));
                    case "&&" -> tokens.add(new Token(Token.Type.OPERATOR_AND, operator));
                    case "||" -> tokens.add(new Token(Token.Type.OPERATOR_OR, operator));
                    case "!" -> tokens.add(new Token(Token.Type.OPERATOR_NOT, operator));
                    case "==" -> tokens.add(new Token(Token.Type.OPERATOR_EQ, operator));
                    case "!=" -> tokens.add(new Token(Token.Type.OPERATOR_NEQ, operator));
                    case "<" -> tokens.add(new Token(Token.Type.OPERATOR_LT, operator));
                    case "<=" -> tokens.add(new Token(Token.Type.OPERATOR_LTE, operator));
                    case ">" -> tokens.add(new Token(Token.Type.OPERATOR_GT, operator));
                    case ">=" -> tokens.add(new Token(Token.Type.OPERATOR_GTE, operator));
                }
            }
            lastEnd = matcher.end();
        }

        // 检查末尾是否有非法字符
        String remaining = expression.substring(lastEnd).trim();
        if (!remaining.isEmpty()) {
            throw new IllegalArgumentException("Invalid character sequence at end: " + remaining);
        }

        return tokens;
    }

    // --- 调度场算法 ---
    private static List<Token> infixToRPN(List<Token> tokens) {
        List<Token> output = new ArrayList<>();
        Stack<Token> operatorStack = new Stack<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            // 处理一元负号
            if (token.type == Token.Type.OPERATOR_SUB) {
                if (i == 0 || tokens.get(i - 1).type == Token.Type.LEFT_PAREN ||
                        tokens.get(i - 1).type == Token.Type.OPERATOR_ADD ||
                        tokens.get(i - 1).type == Token.Type.OPERATOR_SUB ||
                        tokens.get(i - 1).type == Token.Type.OPERATOR_MUL ||
                        tokens.get(i - 1).type == Token.Type.OPERATOR_DIV ||
                        tokens.get(i - 1).type == Token.Type.OPERATOR_POW) {
                    operatorStack.push(new Token(Token.Type.OPERATOR_UNARY_MINUS, "u-"));
                    continue;
                }
            }

            switch (token.type) {
                case NUMBER, VARIABLE -> output.add(token);
                case FUNCTION, LEFT_PAREN -> operatorStack.push(token);
                case OPERATOR_ADD, OPERATOR_SUB, OPERATOR_MUL, OPERATOR_DIV, OPERATOR_POW -> {
                    while (!operatorStack.isEmpty() &&
                            (operatorStack.peek().type == Token.Type.OPERATOR_ADD ||
                                    operatorStack.peek().type == Token.Type.OPERATOR_SUB ||
                                    operatorStack.peek().type == Token.Type.OPERATOR_MUL ||
                                    operatorStack.peek().type == Token.Type.OPERATOR_DIV ||
                                    operatorStack.peek().type == Token.Type.OPERATOR_POW ||
                                    operatorStack.peek().type == Token.Type.OPERATOR_UNARY_MINUS)) {
                        Token opTop = operatorStack.peek();
                        if (OPERATOR_PRECEDENCE.get(token.type) <= OPERATOR_PRECEDENCE.get(opTop.type)) {
                            output.add(operatorStack.pop());
                        } else {
                            break;
                        }
                    }
                    operatorStack.push(token);
                }
                case RIGHT_PAREN -> {
                    while (!operatorStack.isEmpty() && operatorStack.peek().type != Token.Type.LEFT_PAREN) {
                        output.add(operatorStack.pop());
                    }
                    if (operatorStack.isEmpty()) {
                        throw new IllegalArgumentException("Mismatched parentheses");
                    }
                    operatorStack.pop(); // 弹出左括号

                    // 如果栈顶是函数，则弹出并加入输出
                    if (!operatorStack.isEmpty() && operatorStack.peek().type == Token.Type.FUNCTION) {
                        output.add(operatorStack.pop());
                    }
                }
            }
        }

        while (!operatorStack.isEmpty()) {
            Token op = operatorStack.pop();
            if (op.type == Token.Type.LEFT_PAREN || op.type == Token.Type.RIGHT_PAREN) {
                throw new IllegalArgumentException("Mismatched parentheses");
            }
            output.add(op);
        }
        return output;
    }

    // --- 后缀表达式求值 ---
    private static double evaluateRPN(List<Token> rpn, Map<String, ? extends Number> variables) {
        Stack<Double> valueStack = new Stack<>();

        for (Token token : rpn) {
            switch (token.type) {
                case NUMBER -> valueStack.push(Double.parseDouble(token.value));
                case VARIABLE -> {
                    String varName = token.value;
                    if (!variables.containsKey(varName)) {
                        throw new IllegalArgumentException("Unknown variable: " + varName);
                    }
                    valueStack.push(variables.get(varName).doubleValue());
                }
                case BOOLEAN -> {
                    String boolStr = token.value.toLowerCase();
                    valueStack.push("true".equals(boolStr) ? 1.0 : 0.0);
                }
                case FUNCTION -> processFunction(valueStack, token);
                case OPERATOR_UNARY_MINUS -> processUnaryMinus(valueStack, token);
                case OPERATOR_ADD, OPERATOR_SUB, OPERATOR_MUL, OPERATOR_DIV, OPERATOR_POW -> processArithmeticOperation(valueStack, token);
                case OPERATOR_EQ, OPERATOR_NEQ, OPERATOR_LT, OPERATOR_LTE, OPERATOR_GT, OPERATOR_GTE -> processComparisonOperation(valueStack, token);
                case OPERATOR_AND, OPERATOR_OR -> processLogicalOperation(valueStack, token);
                case OPERATOR_NOT -> processLogicalNot(valueStack, token);
            }
        }

        if (valueStack.size() != 1) {
            throw new IllegalArgumentException("Invalid expression: incorrect number of values remaining");
        }
        return valueStack.pop();
    }

    // 分离的算术运算处理方法
    private static void processArithmeticOperation(Stack<Double> valueStack, Token token) {
        if (valueStack.size() < 2) {
            throw new IllegalArgumentException("Invalid expression: insufficient values for operator " + token.value);
        }
        double b = valueStack.pop();
        double a = valueStack.pop();
        double result = switch (token.type) {
            case OPERATOR_ADD -> a + b;
            case OPERATOR_SUB -> a - b;
            case OPERATOR_MUL -> a * b;
            case OPERATOR_DIV -> {
                if (b == 0) {
                    throw new IllegalArgumentException("Division by zero");
                }
                yield a / b;
            }
            case OPERATOR_POW -> Math.pow(a, b);
            default -> throw new IllegalArgumentException("Unknown operator: " + token.value);
        };
        valueStack.push(result);
    }

    // 分离的比较运算处理方法
    private static void processComparisonOperation(Stack<Double> valueStack, Token token) {
        if (valueStack.size() < 2) {
            throw new IllegalArgumentException("Invalid expression: insufficient values for operator " + token.value);
        }
        double b = valueStack.pop();
        double a = valueStack.pop();
        double result = switch (token.type) {
            case OPERATOR_EQ -> (a == b) ? 1.0 : 0.0;
            case OPERATOR_NEQ -> (a != b) ? 1.0 : 0.0;
            case OPERATOR_LT -> (a < b) ? 1.0 : 0.0;
            case OPERATOR_LTE -> (a <= b) ? 1.0 : 0.0;
            case OPERATOR_GT -> (a > b) ? 1.0 : 0.0;
            case OPERATOR_GTE -> (a >= b) ? 1.0 : 0.0;
            default -> throw new IllegalArgumentException("Unknown operator: " + token.value);
        };
        valueStack.push(result);
    }

    // 分离的逻辑运算处理方法
    private static void processLogicalOperation(Stack<Double> valueStack, Token token) {
        if (valueStack.size() < 2) {
            throw new IllegalArgumentException("Invalid expression: insufficient values for operator " + token.value);
        }
        double b = valueStack.pop();
        double a = valueStack.pop();
        double result = switch (token.type) {
            case OPERATOR_AND -> ((Math.abs(a) > 1e-10) && (Math.abs(b) > 1e-10)) ? 1.0 : 0.0; // 非零视为真
            case OPERATOR_OR -> ((Math.abs(a) > 1e-10) || (Math.abs(b) > 1e-10)) ? 1.0 : 0.0;  // 非零视为真
            default -> throw new IllegalArgumentException("Unknown operator: " + token.value);
        };
        valueStack.push(result);
    }

    // 分离的一元逻辑运算处理方法
    private static void processLogicalNot(Stack<Double> valueStack, Token token) {
        if (valueStack.isEmpty()) {
            throw new IllegalArgumentException("Invalid expression: insufficient values for unary operator " + token.value);
        }
        double value = valueStack.pop();
        valueStack.push((Math.abs(value) < 1e-10) ? 1.0 : 0.0); // 零视为假，非零视为真
    }

    // 分离的函数处理方法
    private static void processFunction(Stack<Double> valueStack, Token token) {
        if (valueStack.isEmpty()) {
            throw new IllegalArgumentException("Invalid expression: insufficient values for function " + token.value);
        }

        switch (token.value) {
            case "min" -> {
                if (valueStack.size() < 2) {
                    throw new IllegalArgumentException("Invalid expression: insufficient values for min function");
                }
                double b = valueStack.pop();
                double a = valueStack.pop();
                valueStack.push(Math.min(a, b));
            }
            case "max" -> {
                if (valueStack.size() < 2) {
                    throw new IllegalArgumentException("Invalid expression: insufficient values for max function");
                }
                double maxB = valueStack.pop();
                double maxA = valueStack.pop();
                valueStack.push(Math.max(maxA, maxB));
            }
            case "abs" -> valueStack.push(Math.abs(valueStack.pop()));
            case "sqrt" -> valueStack.push(Math.sqrt(valueStack.pop()));
            case "log" -> valueStack.push(Math.log(valueStack.pop()));
            default -> throw new IllegalArgumentException("Unknown function: " + token.value);
        }
    }

    // 分离的一元负号处理方法
    private static void processUnaryMinus(Stack<Double> valueStack, Token token) {
        if (valueStack.isEmpty()) {
            throw new IllegalArgumentException("Invalid expression: insufficient values for unary operator " + token.value);
        }
        valueStack.push(-valueStack.pop());
    }
}
