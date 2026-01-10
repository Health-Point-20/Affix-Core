package net.yixi_xun.affix_core.affix.operation;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.yixi_xun.affix_core.affix.AffixContext;

import java.util.Map;

import static net.yixi_xun.affix_core.AffixCoreMod.LOGGER;
import static net.yixi_xun.affix_core.api.ExpressionHelper.evaluate;

/**
 * 命令执行操作，用于执行服务器命令
 */
public class CommandOperation implements IOperation {
    private final String commandExpression;  // 命令表达式，可能包含变量
    private final String executor;           // 执行者类型：server(服务器), self(玩家)

    public CommandOperation(String commandExpression, String executor) {
        this.commandExpression = commandExpression != null ? commandExpression : "";
        this.executor = executor != null ? executor : "server"; // 默认为服务器执行
    }

    @Override
    public void apply(AffixContext context) {
        MinecraftServer server = context.getWorld().getServer();
        if (server == null) {
            return;
        }

        // 计算命令表达式中的变量
        Map<String, Object> variables = context.getVariables();
        Object evaluatedResult = evaluate(commandExpression, variables);
        String evaluatedCommand = evaluatedResult.toString();

        // 确保命令以'/'开头
        if (!evaluatedCommand.startsWith("/")) {
            evaluatedCommand = "/" + evaluatedCommand;
        }

        try {
            if ("server".equals(executor)) {
                // 作为服务器执行命令
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), evaluatedCommand);
            } else {
                // 作为特定实体执行命令
                Entity executorEntity = getExecutorEntity(context);
                if (executorEntity != null) {
                    // 获取执行者的命令源
                    CommandSourceStack sourceStack = server.createCommandSourceStack().withEntity(executorEntity);
                    server.getCommands().performPrefixedCommand(sourceStack, evaluatedCommand);
                } else {
                    // 如果实体不能作为命令源，则使用服务器执行
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), evaluatedCommand);
                }
            }
        } catch (Exception e) {
            // 捕获执行命令时可能出现的异常
            LOGGER.warn("执行命令时出现错误: {}", evaluatedCommand);
        }
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
        nbt.putString("CommandExpression", commandExpression);
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
        String commandExpression = nbt.contains("CommandExpression") ? nbt.getString("CommandExpression") : "";
        String executor = nbt.contains("Executor") ? nbt.getString("Executor") : "server";

        return new CommandOperation(commandExpression, executor);
    }

    /**
     * 注册操作工厂
     */
    public static void register() {
        OperationManager.registerFactory("command_execute", CommandOperation::fromNBT);
    }
}