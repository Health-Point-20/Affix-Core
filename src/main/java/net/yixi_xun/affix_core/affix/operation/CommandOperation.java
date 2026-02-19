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
public class CommandOperation extends BaseOperation {
    private final String command;
    private final String executor;

    public CommandOperation(String command, String executor) {
        this.command = command != null ? command : "";
        this.executor = executor != null ? executor : "server";
    }

    @Override
    public void apply(AffixContext context) {
        if (context == null || command.trim().isEmpty()) {
            return;
        }
        
        MinecraftServer server = context.getWorld().getServer();
        if (server == null) {
            LOGGER.warn("无法获取服务器实例，跳过命令执行");
            return;
        }

        try {
            String processedCommand = processCommand(context);
            executeCommand(server, processedCommand, context);
        } catch (Exception e) {
            LOGGER.error("执行命令时发生严重错误: {}", command, e);
        }
    }

    /**
     * 处理命令字符串，包括变量替换和格式化
     */
    private String processCommand(AffixContext context) {
        String processedCommand = replaceVariables(command, context.getVariables());
        
        // 确保命令以'/'开头
        if (!processedCommand.startsWith("/")) {
            processedCommand = "/" + processedCommand;
        }
        
        return processedCommand.trim();
    }

    /**
     * 执行处理后的命令
     */
    private void executeCommand(MinecraftServer server, String processedCommand, AffixContext context) {
        if ("server".equals(executor)) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), processedCommand);
        } else {
            Entity executorEntity = getExecutorEntity(context);
            if (executorEntity != null) {
                CommandSourceStack sourceStack = executorEntity.createCommandSourceStack();
                server.getCommands().performPrefixedCommand(sourceStack, processedCommand);
            } else {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), processedCommand);
                LOGGER.debug("无法获取执行实体，使用服务器命令源执行: {}", processedCommand);
            }
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
            default -> {
                LOGGER.warn("未知的执行者类型: {}, 使用默认(self)", executor);
                yield context.getOwner();
            }
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
        String command = getString(nbt, "Command", "");
        String executor = getString(nbt, "Executor", "server");

        return new CommandOperation(command, executor);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("command_execute", CommandOperation::fromNBT);
    }
}