package net.yixi_xun.affix_core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.yixi_xun.affix_core.AffixCoreMod;

import java.util.Collections;

@Mod.EventBusSubscriber(modid = AffixCoreMod.MODID)
public class RepeatCommand {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        LiteralArgumentBuilder<CommandSourceStack> repeatCommand = Commands.literal("repeat")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                        .then(Commands.argument("interval", IntegerArgumentType.integer(1))
                                .then(Commands.literal("run")
                                        .fork(dispatcher.getRoot(), context -> {
                                            int count = IntegerArgumentType.getInteger(context, "count");
                                            int interval = IntegerArgumentType.getInteger(context, "interval");

                                            // 提取run后面的所有输入字符串
                                            String input = context.getInput();
                                            ParsedCommandNode<CommandSourceStack> lastNode = context.getNodes().get(context.getNodes().size() - 1);
                                            int cursor = lastNode.getRange().getEnd();

                                            String commandToExecute = input.substring(cursor).trim();

                                            if (!commandToExecute.isEmpty()) {
                                                CommandSourceStack source = context.getSource();
                                                Vec3 pos = source.getPosition();
                                                Entity entity = source.getEntity();
                                                CommandSourceStack originalSource = source.withSuppressedOutput();

                                                // 安排重复执行
                                                for (int i = 1; i <= count; i++) {
                                                    final int iteration = i;
                                                    AffixCoreMod.queueServerWork(i * interval, () -> {
                                                        try {
                                                            // 恢复执行上下文
                                                            CommandSourceStack executionSource = originalSource;
                                                            if (entity != null) {
                                                                executionSource = executionSource.withEntity(entity);
                                                            } else {
                                                                executionSource = executionSource.withPosition(pos);
                                                            }

                                                            executionSource.getServer().getCommands().performPrefixedCommand(executionSource, commandToExecute);

                                                            if (AffixCoreMod.LOGGER.isDebugEnabled()) {
                                                                AffixCoreMod.LOGGER.debug("Executed repeated command [{}/{}]: {}",
                                                                        iteration, count, commandToExecute);
                                                            }
                                                        } catch (Exception e) {
                                                            AffixCoreMod.LOGGER.error("Failed to execute repeated command [{}/{}]: {}",
                                                                    iteration, count, commandToExecute, e);
                                                        }
                                                    });
                                                }

                                                source.sendSuccess(() -> Component.translatable("commands.repeat.success"), true);
                                            }

                                            // 返回空集合作为副作用，阻止Brigadier继续执行
                                            return Collections.emptyList();
                                        })
                                )));

        dispatcher.register(repeatCommand);
    }
}