package net.yixi_xun.affix_core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.yixi_xun.affix_core.affix.Affix;
import net.yixi_xun.affix_core.affix.AffixManager;
import net.yixi_xun.affix_core.affix.operation.OperationManager;
import net.yixi_xun.affix_core.api.ExpressionHelper;
import net.yixi_xun.affix_core.gui.screen.AffixListScreen;
import net.yixi_xun.affix_core.network.NetworkManager;
import net.yixi_xun.affix_core.network.OpenAffixListPacket;

import javax.xml.xpath.XPathExpressionException;
import java.util.List;

/**
 * 词缀系统的命令类
 */
@Mod.EventBusSubscriber
public class AffixCommands {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        // 注册命令
        AffixCommands.register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("affix")
                .then(Commands.literal("add")
                    .then(Commands.argument("trigger", StringArgumentType.string())
                        .then(Commands.argument("operation", StringArgumentType.string())
                            .executes(context -> addAffix(context, 
                                StringArgumentType.getString(context, "trigger"),
                                StringArgumentType.getString(context, "operation"),
                                null,
                                0,
                                null))
                            .then(Commands.argument("condition", StringArgumentType.string())
                                .executes(context -> addAffix(context,
                                    StringArgumentType.getString(context, "trigger"),
                                    StringArgumentType.getString(context, "operation"),
                                    StringArgumentType.getString(context, "condition"),
                                    0,
                                    null))
                                .then(Commands.argument("cooldown", IntegerArgumentType.integer(0))
                                    .then(Commands.argument("slot", StringArgumentType.string())
                                        .executes(context -> addAffix(context,
                                            StringArgumentType.getString(context, "trigger"),
                                            StringArgumentType.getString(context, "operation"),
                                            StringArgumentType.getString(context, "condition"),
                                            IntegerArgumentType.getInteger(context, "cooldown"),
                                            StringArgumentType.getString(context, "slot")))))))))
                .then(Commands.literal("list")
                    .executes(AffixCommands::listAffixes))
                .then(Commands.literal("remove")
                    .then(Commands.argument("index", IntegerArgumentType.integer(0))
                        .executes(context -> removeAffix(context, 
                            IntegerArgumentType.getInteger(context, "index")))))
                .then(Commands.literal("clear")
                    .executes(AffixCommands::clearAffixes))
                .then(Commands.literal("merge_nbt")
                    .then(Commands.argument("nbt_data", StringArgumentType.greedyString())
                        .executes(context -> mergeNBT(context, StringArgumentType.getString(context, "nbt_data")))))
                .then(Commands.literal("gui")
                    .executes(AffixCommands::gui))
                .then(Commands.literal("clearCache")
                    .executes(AffixCommands::clearCache))
        );
    }

    private static int addAffix(CommandContext<CommandSourceStack> context, String trigger, String operationType, 
                               String condition, int cooldown, String slot) {
        ItemStack itemStack = isValidItem(context);
        if (itemStack == null) return 0;

        try {
            // 创建操作
            CompoundTag operationTag = new CompoundTag();
            operationTag.putString("type", operationType);
            var operation = OperationManager.createOperation(operationTag);

            if (operation == null) {
                context.getSource().sendFailure(Component.literal("未知的操作类型: " + operationType));
                return 0;
            }

            // 创建词缀
            List<Affix> affixes = AffixManager.getAffixes(itemStack);
            EquipmentSlot equipmentSlot = null;
            if (slot != null && !slot.isEmpty()) {
                try {
                    equipmentSlot = EquipmentSlot.byName(slot.toLowerCase());
                } catch (IllegalArgumentException e) {
                    context.getSource().sendFailure(Component.literal("无效的槽位: " + slot));
                    return 0;
                }
            }

            Affix affix = new Affix(trigger, condition, operation, (long) cooldown, 0, equipmentSlot, affixes.size());
            AffixManager.addAffix(itemStack, affix);

            context.getSource().sendSuccess(() -> Component.literal("成功添加词缀: " + trigger), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("添加词缀失败: " + e.getMessage()));
            return 0;
        }
    }

    private static int listAffixes(CommandContext<CommandSourceStack> context) {
        ItemStack itemStack = isValidItem(context);
        if (itemStack == null) return 0;

        List<Affix> affixes = AffixManager.getAffixes(itemStack);

        if (affixes.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("该物品没有词缀"), true);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("物品词缀列表:"), true);
            for (int i = 0; i < affixes.size(); i++) {
                int finalI = i; // 创建final变量以供lambda使用
                Affix affix = affixes.get(i);
                context.getSource().sendSuccess(() -> Component.literal(
                    String.format("[%d] 触发器: %s, 操作: %s, 条件: %s, 冷却: %d, 槽位: %s",
                        finalI,
                        affix.trigger() != null ? affix.trigger() : "无",
                        affix.operation() != null ? affix.operation().getType() : "无",
                        affix.condition() != null ? affix.condition() : "无",
                        affix.cooldown(),
                        affix.slot() != null ? affix.slot().getName() : "任意")
                ), true);
            }
        }

        return affixes.size();
    }

    private static int removeAffix(CommandContext<CommandSourceStack> context, int index) {
        ItemStack itemStack = isValidItem(context);
        if (itemStack == null) return 0;

        if (!AffixManager.removeAffix(itemStack, index, context.getSource().getLevel(), context.getSource().getPlayer())) {
            context.getSource().sendFailure(Component.literal("无效的词缀索引: " + index));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("成功移除索引为 " + index + " 的词缀"), true);
        return 1;
    }

    private static int clearAffixes(CommandContext<CommandSourceStack> context) {
        ItemStack itemStack = isValidItem(context);
        if (itemStack == null) return 0;

        AffixManager.clearAffixes(itemStack);

        context.getSource().sendSuccess(() -> Component.literal("成功清除所有词缀"), true);
        return 1;
    }

    private static int mergeNBT(CommandContext<CommandSourceStack> context, String nbtData) {
        ItemStack itemStack = isValidItem(context);
        if (itemStack == null) return 0;

        try {
            // 解析NBT字符串
            CompoundTag parsedNBT = TagParser.parseTag(nbtData);
            
            // 合并NBT数据到当前物品
            itemStack.getOrCreateTag().merge(parsedNBT);
            
            context.getSource().sendSuccess(() -> Component.literal("成功将NBT数据合并到物品"), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("解析NBT数据失败: " + e.getMessage()));
            return 0;
        }
    }

    private static int gui(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("打开词缀GUI"), true);
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;
        ItemStack itemStack = isValidItem(context);
        if (itemStack == null) return 0;

        NetworkManager.sendToPlayer(new OpenAffixListPacket(itemStack), player);
        
        return 1;
    }

    private static int clearCache(CommandContext<CommandSourceStack> context) {
        ExpressionHelper.clearCache();
        context.getSource().sendSuccess(() -> Component.literal("成功清除解析式缓存"), true);
        return 1;
    }

    private static ItemStack isValidItem(CommandContext<CommandSourceStack> context) {ServerPlayer player;
        player = context.getSource().getPlayer();
        if (player == null) return null;
        ItemStack itemStack = player.getMainHandItem();

        if (itemStack.isEmpty()) {
            context.getSource().sendFailure(Component.literal("你必须手持一个物品"));
            return null;
        }
        return itemStack;
    }
}