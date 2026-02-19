package net.yixi_xun.affix_core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.yixi_xun.affix_core.items.RaffleDataManager;
import net.yixi_xun.affix_core.items.RaffleItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * 抽奖物品配置指令
 */
@Mod.EventBusSubscriber
public class RaffleItemCommands {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        // 注册命令
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("raffle")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.literal("draw_count")
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> setDrawCount(ctx,
                                                        EntityArgument.getPlayers(ctx, "players"),
                                                        IntegerArgumentType.getInteger(ctx, "count")))))
                                .then(Commands.literal("repeat")
                                        .then(Commands.argument("allow", BoolArgumentType.bool())
                                                .executes(ctx -> setAllowRepeat(ctx,
                                                        EntityArgument.getPlayers(ctx, "players"),
                                                        BoolArgumentType.getBool(ctx, "allow")))))
                                .then(Commands.literal("probability")
                                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("chance", DoubleArgumentType.doubleArg(0, 100))
                                                        .executes(ctx -> setSlotProbability(ctx,
                                                                EntityArgument.getPlayers(ctx, "players"),
                                                                IntegerArgumentType.getInteger(ctx, "slot"),
                                                                DoubleArgumentType.getDouble(ctx, "chance") / 100.0)))))
                                .then(Commands.literal("consume_items")
                                        .then(Commands.argument("allow", BoolArgumentType.bool())
                                                .executes(ctx -> setConsumeItems(ctx,
                                                        EntityArgument.getPlayers(ctx, "players"),
                                                        BoolArgumentType.getBool(ctx, "allow")))))
                                .then(Commands.literal("consume_self")
                                        .then(Commands.argument("allow", BoolArgumentType.bool())
                                                .executes(ctx -> setConsumeSelf(ctx,
                                                        EntityArgument.getPlayers(ctx, "players"),
                                                        BoolArgumentType.getBool(ctx, "allow")))))
                        )
                )
                .then(Commands.literal("copy_from")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(RaffleItemCommands::copyContainerItems))
                )
                .then(Commands.literal("clear")
                        .then(Commands.literal("items")
                                .executes(RaffleItemCommands::clearItemList))
                        .then(Commands.literal("container")
                                .executes(RaffleItemCommands::unbindContainer))
                )
                .then(Commands.literal("info")
                        .executes(RaffleItemCommands::showRaffleInfo)
                )
                .then(Commands.literal("init")
                        .executes(RaffleItemCommands::initializeDefaults)
                )
        );
    }

    /**
     * 设置抽取次数
     */
    private static int setDrawCount(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players, int count) {
        for (ServerPlayer player : players) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() instanceof RaffleItem) {
                RaffleDataManager.setDrawCount(mainHand, count);
                ctx.getSource().sendSuccess(() ->
                        Component.translatable("command.raffle.drawcount_set", player.getName(), count)
                                .withStyle(ChatFormatting.GREEN), true);
            } else {
                ctx.getSource().sendFailure(
                        Component.translatable("command.raffle.not_raffle_item", player.getName()));
            }
        }
        return players.size();
    }

    /**
     * 设置是否允许重复抽取
     */
    private static int setAllowRepeat(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players, boolean allow) {
        for (ServerPlayer player : players) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() instanceof RaffleItem) {
                RaffleDataManager.setAllowRepeat(mainHand, allow);
                ctx.getSource().sendSuccess(() ->
                        Component.translatable("command.raffle.repeat_set", player.getName(),
                                        Component.translatable(allow ? "raffle.yes" : "raffle.no"))
                                .withStyle(ChatFormatting.GREEN), true);
            } else {
                ctx.getSource().sendFailure(
                        Component.translatable("command.raffle.not_raffle_item", player.getName()));
            }
        }
        return players.size();
    }

    /**
     * 设置特定槽位的概率
     */
    private static int setSlotProbability(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players, int slot, double chance) {
        for (ServerPlayer player : players) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() instanceof RaffleItem) {
                RaffleDataManager.setSlotProbability(mainHand, slot, chance);
                ctx.getSource().sendSuccess(() ->
                        Component.translatable("command.raffle.probability_set", player.getName(), slot, chance * 100)
                                .withStyle(ChatFormatting.GREEN), true);
            } else {
                ctx.getSource().sendFailure(
                        Component.translatable("command.raffle.not_raffle_item", player.getName()));
            }
        }
        return players.size();
    }

    /**
     * 设置是否消耗抽取物品
     */
    private static int setConsumeItems(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players, boolean consume) {
        for (ServerPlayer player : players) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() instanceof RaffleItem) {
                RaffleDataManager.setConsumeItems(mainHand, consume);
                ctx.getSource().sendSuccess(() ->
                        Component.translatable("command.raffle.consume_set", player.getName(),
                                        Component.translatable(consume ? "raffle.yes" : "raffle.no"))
                                .withStyle(ChatFormatting.GREEN), true);
            } else {
                ctx.getSource().sendFailure(
                        Component.translatable("command.raffle.not_raffle_item", player.getName()));
            }
        }
        return players.size();
    }

    /**
     * 设置是否消耗自身
     */
    private static int setConsumeSelf(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players, boolean consume) {
        for (ServerPlayer player : players) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() instanceof RaffleItem) {
                RaffleDataManager.setConsumeSelf(mainHand, consume);
                ctx.getSource().sendSuccess(() ->
                        Component.translatable("command.raffle.consume_self_set", player.getName(),
                                        Component.translatable(consume ? "raffle.yes" : "raffle.no"))
                                .withStyle(ChatFormatting.GREEN), true);
            } else {
                ctx.getSource().sendFailure(
                        Component.translatable("command.raffle.not_raffle_item", player.getName()));
            }
        }
        return players.size();
    }

    /**
     * 清空内置物品列表
     */
    private static int clearItemList(CommandContext<CommandSourceStack> ctx) {
        try {
            CommandSourceStack source = ctx.getSource();
            ServerPlayer player = source.getPlayerOrException();
            ItemStack mainHand = player.getMainHandItem();

            if (!(mainHand.getItem() instanceof RaffleItem)) {
                source.sendFailure(Component.translatable("command.raffle.not_raffle_item_hand"));
                return 0;
            }

            RaffleDataManager.clearItemList(mainHand);
            source.sendSuccess(() ->
                    Component.translatable("command.raffle.items_cleared")
                            .withStyle(ChatFormatting.GREEN), true);

            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * 解绑容器
     */
    private static int unbindContainer(CommandContext<CommandSourceStack> ctx) {
        try {
            CommandSourceStack source = ctx.getSource();
            ServerPlayer player = source.getPlayerOrException();
            ItemStack mainHand = player.getMainHandItem();

            if (!(mainHand.getItem() instanceof RaffleItem)) {
                source.sendFailure(Component.translatable("command.raffle.not_raffle_item_hand"));
                return 0;
            }

            RaffleDataManager.unbindContainer(mainHand);
            source.sendSuccess(() ->
                    Component.translatable("command.raffle.container_unbound")
                            .withStyle(ChatFormatting.GREEN), true);

            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * 从指定位置的容器复制物品到抽奖物品
     */
    private static int copyContainerItems(CommandContext<CommandSourceStack> ctx) {
        try {
            CommandSourceStack source = ctx.getSource();
            ServerPlayer player = source.getPlayerOrException();
            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
            Level level = source.getLevel();

            ItemStack mainHand = player.getMainHandItem();
            if (!(mainHand.getItem() instanceof RaffleItem)) {
                source.sendFailure(Component.translatable("command.raffle.not_raffle_item_hand"));
                return 0;
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null) {
                source.sendFailure(Component.translatable("command.raffle.no_block_entity", pos.toShortString()));
                return 0;
            }

            var itemHandlerOpt = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
            if (itemHandlerOpt.isEmpty()) {
                source.sendFailure(Component.translatable("command.raffle.not_container", pos.toShortString()));
                return 0;
            }

            IItemHandler itemHandler = itemHandlerOpt.get();
            List<ItemStack> items = new ArrayList<>();

            for (int i = 0; i < itemHandler.getSlots(); i++) {
                ItemStack stack = itemHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    items.add(stack.copy());
                }
            }

            if (items.isEmpty()) {
                source.sendFailure(Component.translatable("command.raffle.container_empty", pos.toShortString()));
                return 0;
            }

            // 清除旧的容器绑定信息
            RaffleDataManager.unbindContainer(mainHand);
            
            RaffleDataManager.setItemList(mainHand, items);
            source.sendSuccess(() ->
                    Component.translatable("command.raffle.items_copied", items.size(), pos.toShortString())
                            .withStyle(ChatFormatting.GREEN), true);

            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * 显示当前抽奖物品信息
     */
    private static int showRaffleInfo(CommandContext<CommandSourceStack> ctx) {
        try {
            CommandSourceStack source = ctx.getSource();
            ServerPlayer player = source.getPlayerOrException();
            ItemStack mainHand = player.getMainHandItem();

            if (!(mainHand.getItem() instanceof RaffleItem)) {
                source.sendFailure(Component.translatable("command.raffle.not_raffle_item_hand"));
                return 0;
            }

            int drawCount = RaffleDataManager.getDrawCount(mainHand);
            boolean allowRepeat = RaffleDataManager.getAllowRepeat(mainHand);
            boolean consumeItems = RaffleDataManager.getConsumeItems(mainHand);
            boolean consumeSelf = RaffleDataManager.getConsumeSelf(mainHand);

            source.sendSuccess(() -> Component.literal("=== 抽奖物品信息 ===").withStyle(ChatFormatting.GOLD), false);
            source.sendSuccess(() -> Component.literal("抽取次数: " + drawCount), false);
            source.sendSuccess(() -> Component.literal("允许重复: " + (allowRepeat ? "是" : "否")), false);
            source.sendSuccess(() -> Component.literal("消耗物品: " + (consumeItems ? "是" : "否")), false);
            source.sendSuccess(() -> Component.literal("消耗自身: " + (consumeSelf ? "是" : "否")), false);

            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 初始化默认值
     */
    private static int initializeDefaults(CommandContext<CommandSourceStack> ctx) {
        try {
            CommandSourceStack source = ctx.getSource();
            ServerPlayer player = source.getPlayerOrException();
            ItemStack mainHand = player.getMainHandItem();

            if (!(mainHand.getItem() instanceof RaffleItem)) {
                source.sendFailure(Component.translatable("command.raffle.not_raffle_item_hand"));
                return 0;
            }

            // 初始化默认值
            boolean success = RaffleDataManager.initializeDefaults(mainHand);
            
            if (success) {
                source.sendSuccess(() -> Component.translatable("command.raffle.init_success", mainHand.getDisplayName()), true);
                return 1;
            } else {
                source.sendFailure(Component.literal("初始化失败"));
                return 0;
            }
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("初始化出现异常"));
            return 0;
        }
    }
}