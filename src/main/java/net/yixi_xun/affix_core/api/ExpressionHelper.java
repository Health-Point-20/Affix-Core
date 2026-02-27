package net.yixi_xun.affix_core.api;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.registries.ForgeRegistries;

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
 * 支持的运算符：+, -, *, /, ^(幂运算), &&(and), ||(or), !(not), ==, !=, <, >, <=, >=
 * 支持的函数：min, max, abs, sqrt, log
 * 支持变量：通过 Map 传入
 * 支持条件表达式：返回布尔值
 * 支持字符串：支持字符串字面量 ("...") 及其比较
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
     * @param Variables 本次计算所用的变量映射，值可以是任意Number类型或String
     * @return 计算结果的 double 值。如果表达式无效或计算失败，则返回 0
     */
    public static double evaluate(String expression, Map<String, ?> Variables) {
        Map<String, Object> localVariables = new HashMap<>(Variables);

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
    public static double evaluate(String expression, String varName, Object varValue) {
        return evaluate(expression, Map.of(varName, varValue));
    }

    /**
     * 便捷方法：使用两个变量计算表达式
     */
    public static double evaluate(String expression, String var1Name, Object var1Value,
                                  String var2Name, Object var2Value) {
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
            "(\\d+\\.\\d+|\\d+)" +  // 1: 数字
                    "|(true|false)" +  // 2: 布尔值
                    "|(\"[^\"]*\")|('[^']*')" +  // 3: 双引号字符串, 4: 单引号字符串
                    "|([a-zA-Z_][a-zA-Z0-9_.]*)" +  // 5: 标识符（变量、函数）
                    "|(&&|\\|\\||==|!=|<=|>=|<|>|!|\\+|-|\\*|/|\\^|\\(|\\))" +  // 6: 运算符和括号
                    "|(\\s+)"  // 7: 空白字符
    );

    private record Token(Type type, String value) {
        enum Type {
            NUMBER, VARIABLE, BOOLEAN, STRING, // 新增 STRING
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
            String doubleQuotedString = matcher.group(3);  // 双引号字符串
            String singleQuotedString = matcher.group(4);  // 单引号字符串
            String identifier = matcher.group(5);
            String operator = matcher.group(6);

            if (number != null) {
                tokens.add(new Token(Token.Type.NUMBER, number));
            } else if (booleanValue != null) {
                tokens.add(new Token(Token.Type.BOOLEAN, booleanValue));
            } else if (doubleQuotedString != null) {
                // 处理双引号字符串
                String content = doubleQuotedString.substring(1, doubleQuotedString.length() - 1);
                tokens.add(new Token(Token.Type.STRING, content));
            } else if (singleQuotedString != null) {
                // 处理单引号字符串
                String content = singleQuotedString.substring(1, singleQuotedString.length() - 1);
                tokens.add(new Token(Token.Type.STRING, content));
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
                case NUMBER, VARIABLE, STRING -> output.add(token); // 添加 STRING 到输出
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
                // 添加比较运算符的处理
                case OPERATOR_EQ, OPERATOR_NEQ, OPERATOR_LT, OPERATOR_LTE, OPERATOR_GT, OPERATOR_GTE -> {
                    while (!operatorStack.isEmpty() &&
                            operatorStack.peek().type != Token.Type.LEFT_PAREN &&
                            OPERATOR_PRECEDENCE.get(token.type) <= OPERATOR_PRECEDENCE.get(operatorStack.peek().type)) {
                        output.add(operatorStack.pop());
                    }
                    operatorStack.push(token);
                }
                // 添加逻辑运算符的处理
                case OPERATOR_AND, OPERATOR_OR -> {
                    while (!operatorStack.isEmpty() &&
                            operatorStack.peek().type != Token.Type.LEFT_PAREN &&
                            OPERATOR_PRECEDENCE.get(token.type) <= OPERATOR_PRECEDENCE.get(operatorStack.peek().type)) {
                        output.add(operatorStack.pop());
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
    private static double evaluateRPN(List<Token> rpn, Map<String, ?> variables) {
        Stack<Object> valueStack = new Stack<>();

        for (Token token : rpn) {
            switch (token.type) {
                case NUMBER -> valueStack.push(Double.parseDouble(token.value));
                case STRING -> valueStack.push(token.value); // 直接推入字符串值
                case VARIABLE -> {
                    String varName = token.value;
                    if (variables.containsKey(varName)) {
                        Object value = variables.get(varName);
                        // 如果获取到的值为null，使用默认值0.0
                        valueStack.push(value != null ? value : 0.0);
                    } else {
                        // 尝试解析带点的变量路径
                        Object resolvedValue = resolveVariablePath(varName, variables);
                        if (resolvedValue != null) {
                            valueStack.push(resolvedValue);
                        } else {
                            throw new IllegalArgumentException("Unknown variable: " + varName);
                        }
                    }
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
            throw new IllegalArgumentException("Invalid expression: incorrect number of values remaining. Stack size: " + 
                    valueStack.size() + ", Expected: 1");
        }
        Object result = valueStack.pop();
        return result instanceof Number ? ((Number) result).doubleValue() : 0.0;
    }

    // 解析带点的变量路径，如 self.effect.minecraft:speed.duration
    private static Object resolveVariablePath(String varPath, Map<String, ?> variables) {
        if (variables.containsKey(varPath)) {
            return variables.get(varPath);
        }

        String[] parts = varPath.split("\\.");
        // 查找根变量
        for (int i = parts.length; i > 0; i--) {
            String rootVar = String.join(".", Arrays.copyOfRange(parts, 0, i));
            if (variables.containsKey(rootVar)) {
                Object rootValue = variables.get(rootVar);
                if (i < parts.length) {
                    return navigatePath(rootValue, Arrays.copyOfRange(parts, i, parts.length));
                } else {
                    return rootValue;
                }
            }
        }
        return null;
    }

    // 导航对象路径
    private static Object navigatePath(Object obj, String[] pathParts) {
        Object current = obj;

        for (String part : pathParts) {
            if (current == null) return 0.0;

            if (current instanceof Map<?, ?> map) {
                if (map.containsKey(part)) {
                    // 获取map中的值
                    current = map.get(part);
                } else if (map.containsKey("entity_ref")) {
                    // 检测到 entity_ref 时，动态处理 attribute/effect/nbt
                    // 避免在创建 Map 时就遍历所有属性/效果
                    Object entityRef = map.get("entity_ref");
                    if (entityRef instanceof LivingEntity entity) {
                        current = handleEntityPropertyDynamic(entity, part);
                    } else {
                        return 0.0;
                    }
                } else {
                    return 0.0;
                }
            } else if (current instanceof LivingEntity entity) {
                current = handleEntityPropertyDynamic(entity, part);
            } else {
                return 0.0;
            }
        }
        return current;
    }

    // 动态处理实体属性，避免全量加载
    private static Object handleEntityPropertyDynamic(LivingEntity entity, String part) {
        return switch (part) {
            case "attribute" -> new EntityAttributeWrapper(entity); // 返回一个包装器，支持后续路径查找
            case "effect" -> new EntityEffectWrapper(entity);
            case "nbt" -> parseNbtCompound(entity.getPersistentData());
            default -> 0.0;
        };
    }

    // 包装器类，用于延迟获取特定属性
    private static class EntityAttributeWrapper extends HashMap<String, Object> {
        private final LivingEntity entity;

        public EntityAttributeWrapper(LivingEntity entity) {
            this.entity = entity;
        }

        @Override
        public Object get(Object key) {
            // 实时查找属性，而不是遍历注册表
            // 这里的 key 可能是 "generic.attack_damage" 或 "minecraft:generic.attack_damage"
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.tryParse(key.toString()));
            if (attribute != null) {
                AttributeInstance instance = entity.getAttribute(attribute);
                return instance != null ? instance.getValue() : 0.0;
            }
            return 0.0;
        }

        @Override
        public boolean containsKey(Object key) {
            return ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.tryParse(key.toString())) != null;
        }
    }

    // 包装器类，用于延迟获取效果
    private static class EntityEffectWrapper extends HashMap<String, Object> {
        private final LivingEntity entity;

        public EntityEffectWrapper(LivingEntity entity) {
            this.entity = entity;
        }

        @Override
        public Object get(Object key) {
            // key 通常是 registry name，如 "minecraft:speed"
            ResourceLocation effectId = ResourceLocation.tryParse(key.toString());
            var effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
            if (effect != null) {
                MobEffectInstance instance = entity.getEffect(effect);
                if (instance != null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("duration", instance.getDuration());
                    data.put("amplifier", instance.getAmplifier());
                    return data;
                }
            }
            return 0.0;
        }
    }

    // 分离的算术运算处理方法
    private static void processArithmeticOperation(Stack<Object> valueStack, Token token) {
        if (valueStack.size() < 2) {
            throw new IllegalArgumentException("Invalid expression: insufficient values for operator " + token.value);
        }
        Object bObj = valueStack.pop();
        Object aObj = valueStack.pop();

        double b = bObj instanceof Number ? ((Number) bObj).doubleValue() : 0.0;
        double a = aObj instanceof Number ? ((Number) aObj).doubleValue() : 0.0;

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

    // 解析NBT复合标签
    public static Map<String, Object> parseNbtCompound(CompoundTag compound) {
        Map<String, Object> result = new HashMap<>();

        for (String key : compound.getAllKeys()) {
            Tag tag = compound.get(key);
            result.put(key, parseNbtTag(tag));
        }

        return result;
    }

    // 解析NBT标签
    public static Object parseNbtTag(Tag tag) {
        if (tag instanceof NumericTag numericTag) {
            return numericTag.getAsDouble();
        } else if (tag instanceof StringTag stringTag) {
            return stringTag.getAsString();
        } else if (tag instanceof CompoundTag compoundTag) {
            return parseNbtCompound(compoundTag);
        } else if (tag instanceof ListTag listTag) {
            // 对于列表，只取数值类型元素的平均值
            if (!listTag.isEmpty() && listTag.getElementType() == Tag.TAG_ANY_NUMERIC) {
                double sum = 0;
                for (int i = 0; i < listTag.size(); i++) {
                    sum += listTag.getDouble(i);
                }
                return sum / listTag.size();
            }
            return listTag.size(); // 返回列表大小
        } else {
            return tag.getAsString();
        }
    }

    // 分离的比较运算处理方法
    private static void processComparisonOperation(Stack<Object> valueStack, Token token) {
        if (valueStack.size() < 2) {
            throw new IllegalArgumentException("Invalid expression: insufficient values for operator " + token.value);
        }
        Object bObj = valueStack.pop();
        Object aObj = valueStack.pop();

        // 如果两边都是字符串，进行字符串比较
        if (aObj instanceof String aStr && bObj instanceof String bStr) {
            double result = switch (token.type) {
                case OPERATOR_EQ -> aStr.equals(bStr) ? 1.0 : 0.0;
                case OPERATOR_NEQ -> !aStr.equals(bStr) ? 1.0 : 0.0;
                // 字符串不支持大小比较，视情况返回 false 或抛出异常，这里返回 false
                default -> 0.0;
            };
            valueStack.push(result);
            return;
        }

        // 否则进行数字比较 (如果其中一方不是数字，视为 0.0)
        double b = bObj instanceof Number ? ((Number) bObj).doubleValue() : 0.0;
        double a = aObj instanceof Number ? ((Number) aObj).doubleValue() : 0.0;

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

    // 辅助方法：获取对象的布尔值
    private static boolean getBooleanValue(Object obj) {
        if (obj instanceof Number num) {
            return Math.abs(num.doubleValue()) > 1e-10; // 非零为真
        } else if (obj instanceof String str) {
            return !str.isEmpty(); // 非空字符串为真
        }
        return false;
    }

    // 分离的逻辑运算处理方法
    private static void processLogicalOperation(Stack<Object> valueStack, Token token) {
        if (valueStack.size() < 2) {
            throw new IllegalArgumentException("Invalid expression: insufficient values for operator " + token.value);
        }
        Object bObj = valueStack.pop();
        Object aObj = valueStack.pop();

        boolean a = getBooleanValue(aObj);
        boolean b = getBooleanValue(bObj);

        double result = switch (token.type) {
            case OPERATOR_AND -> (a && b) ? 1.0 : 0.0;
            case OPERATOR_OR -> (a || b) ? 1.0 : 0.0;
            default -> throw new IllegalArgumentException("Unknown operator: " + token.value);
        };
        valueStack.push(result);
    }

    // 分离的一元逻辑运算处理方法
    private static void processLogicalNot(Stack<Object> valueStack, Token token) {
        if (valueStack.isEmpty()) {
            throw new IllegalArgumentException("Invalid expression: insufficient values for unary operator " + token.value);
        }
        Object valueObj = valueStack.pop();
        boolean value = getBooleanValue(valueObj);
        valueStack.push((!value) ? 1.0 : 0.0);
    }

    // 分离的函数处理方法
    private static void processFunction(Stack<Object> valueStack, Token token) {
        if (valueStack.isEmpty()) {
            throw new IllegalArgumentException("Invalid expression: insufficient values for function " + token.value);
        }

        switch (token.value) {
            case "min" -> {
                if (valueStack.size() < 2) {
                    throw new IllegalArgumentException("Invalid expression: insufficient values for min function");
                }
                Object bObj = valueStack.pop();
                Object aObj = valueStack.pop();

                double b = bObj instanceof Number ? ((Number) bObj).doubleValue() : 0.0;
                double a = aObj instanceof Number ? ((Number) aObj).doubleValue() : 0.0;

                valueStack.push(Math.min(a, b));
            }
            case "max" -> {
                if (valueStack.size() < 2) {
                    throw new IllegalArgumentException("Invalid expression: insufficient values for max function");
                }
                Object maxBObj = valueStack.pop();
                Object maxAObj = valueStack.pop();

                double maxB = maxBObj instanceof Number ? ((Number) maxBObj).doubleValue() : 0.0;
                double maxA = maxAObj instanceof Number ? ((Number) maxAObj).doubleValue() : 0.0;

                valueStack.push(Math.max(maxA, maxB));
            }
            case "abs" -> {
                Object valueObj = valueStack.pop();
                double value = valueObj instanceof Number ? ((Number) valueObj).doubleValue() : 0.0;
                valueStack.push(Math.abs(value));
            }
            case "sqrt" -> {
                Object valueObj = valueStack.pop();
                double value = valueObj instanceof Number ? ((Number) valueObj).doubleValue() : 0.0;
                valueStack.push(Math.sqrt(value));
            }
            case "log" -> {
                Object valueObj = valueStack.pop();
                double value = valueObj instanceof Number ? ((Number) valueObj).doubleValue() : 0.0;
                valueStack.push(Math.log(value));
            }
            default -> throw new IllegalArgumentException("Unknown function: " + token.value);
        }
    }

    // 分离的一元负号处理方法
    private static void processUnaryMinus(Stack<Object> valueStack, Token token) {
        if (valueStack.isEmpty()) {
            throw new IllegalArgumentException("Invalid expression: insufficient values for unary operator " + token.value);
        }
        Object valueObj = valueStack.pop();
        double value = valueObj instanceof Number ? -((Number) valueObj).doubleValue() : 0.0;
        valueStack.push(value);
    }
}
