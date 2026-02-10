package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.yixi_xun.affix_core.affix.AffixContext;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.yixi_xun.affix_core.AffixCoreMod.LOGGER;

/**
 * 命令执行操作，用于执行服务器命令
 */
public class CommandOperation implements IOperation {
    private final String command;            // 命令表达式，可能包含变量
    private final String executor;           // 执行者类型：server(服务器), self(玩家)

    public CommandOperation(String command, String executor) {
        this.command = command != null ? command : "";
        this.executor = executor != null ? executor : "server"; // 默认为服务器执行
    }

    @Override
    public void apply(AffixContext context) {
        // 检查命令表达式是否为空
        if (command.trim().isEmpty()) {
            return; // 如果命令表达式为空，则不执行任何操作
        }
        
        MinecraftServer server = context.getWorld().getServer();
        if (server == null) {
            return;
        }

        // 替换命令表达式中的变量
        String processedCommand = replaceVariables(command, context.getVariables());

        // 确保命令以'/'开头
        if (!processedCommand.startsWith("/")) {
            processedCommand = "/" + processedCommand;
        }

        try {
            if ("server".equals(executor)) {
                // 作为服务器执行命令
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), processedCommand);
            } else {
                // 作为特定实体执行命令
                Entity executorEntity = getExecutorEntity(context);
                if (executorEntity != null) {
                    // 获取执行者的命令源
                    CommandSourceStack sourceStack = server.createCommandSourceStack().withEntity(executorEntity);
                    server.getCommands().performPrefixedCommand(sourceStack, processedCommand);
                } else {
                    // 如果实体不能作为命令源，则使用服务器执行
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), processedCommand);
                }
            }
        } catch (Exception e) {
            // 捕获执行命令时可能出现的异常
            LOGGER.warn("执行命令时出现错误: {}", processedCommand, e);
        }
    }

    /**
     * 替换命令字符串中的变量占位符
     * 支持 ${variableName} 或 {variableName} 格式的变量
     */
    private String replaceVariables(String command, Map<String, Object> variables) {
        // 使用正则表达式匹配 ${variableName} 或 {variableName} 格式的变量
        Pattern pattern = Pattern.compile("\\$?\\{([^}]+)}");
        Matcher matcher = pattern.matcher(command);
        StringBuilder buffer = new StringBuilder();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            
            // 如果变量存在于上下文中，则替换它
            if (value != null) {
                // 对于非数值类型，可能需要特别处理
                String replacement = value.toString();
                
                // 如果是字符串类型，且包含特殊字符，可能需要转义
                if (value instanceof String) {
                    // 转义反斜杠和美元符号，防止进一步的正则替换
                    replacement = replacement.replace("\\", "\\\\").replace("$", "\\$");
                }
                
                matcher.appendReplacement(buffer, replacement);
            } else {
                // 如果变量不存在，保留原样
                matcher.appendReplacement(buffer, matcher.group(0));
            }
        }
        matcher.appendTail(buffer);
        
        return buffer.toString();
    }

    /**
     * 获取命令执行实体
     */
    private Entity getExecutorEntity(AffixContext context) {
        return switch (executor.toLowerCase()) {
            case "self" -> context.getOwner();
            case "target" -> context.getTarget();
            case "server" -> null;
            default -> context.getOwner(); // 默认为持有者
        };
    }

    @Override
    public void remove(AffixContext context) {
        // 命令操作不需要特殊的移除逻辑
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", getType());
        nbt.putString("Command", command);
        nbt.putString("Executor", executor);
        return nbt;
    }

    @Override
    public String getType() {
        return "command_execute";
    }

    /**
     * 工厂方法，从NBT创建CommandOperation
     */
    public static CommandOperation fromNBT(CompoundTag nbt) {
        String command = nbt.contains("Command") ? nbt.getString("Command") : "";
        String executor = nbt.contains("Executor") ? nbt.getString("Executor") : "server";

        return new CommandOperation(command, executor);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("command_execute", CommandOperation::fromNBT);
    }
}