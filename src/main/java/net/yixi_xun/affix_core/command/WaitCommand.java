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
public class WaitCommand {

	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

		LiteralArgumentBuilder<CommandSourceStack> waitCommand = Commands.literal("wait")
				.requires(source -> source.hasPermission(2))
				.then(Commands.argument("ticks", IntegerArgumentType.integer(1))
						.then(Commands.literal("run")
								.fork(dispatcher.getRoot(), context -> {
									int ticks = IntegerArgumentType.getInteger(context, "ticks");

									// 提取run后面的所有输入字符串
									String input = context.getInput();
									ParsedCommandNode<CommandSourceStack> lastNode = context.getNodes().get(context.getNodes().size() - 1);
									int cursor = lastNode.getRange().getEnd();

									String commandToExecute = input.substring(cursor).trim();

									if (!commandToExecute.isEmpty()) {
										CommandSourceStack source = context.getSource();
										Vec3 pos = source.getPosition();
										Entity entity = source.getEntity();
										CommandSourceStack delayedSource = source.withSuppressedOutput();

										AffixCoreMod.queueServerWork(ticks, () -> {
											try {
												// 恢复执行上下文
												CommandSourceStack executionSource = delayedSource;
												if (entity != null) {
													executionSource = executionSource.withEntity(entity);
												} else {
													executionSource = executionSource.withPosition(pos);
												}

												executionSource.getServer().getCommands().performPrefixedCommand(executionSource, commandToExecute);

												if (AffixCoreMod.LOGGER.isDebugEnabled()) {
													AffixCoreMod.LOGGER.debug("Executed delayed command after {} ticks: {}", ticks, commandToExecute);
												}
											} catch (Exception e) {
												AffixCoreMod.LOGGER.error("Failed to execute delayed command: {}", commandToExecute, e);
											}
										});

										source.sendSuccess(() -> Component.translatable("commands.wait.success"), true);
									}

									// 返回空集合作为副作用，阻止Brigadier继续执行
									return Collections.emptyList();
								})
						));

		dispatcher.register(waitCommand);
	}
}