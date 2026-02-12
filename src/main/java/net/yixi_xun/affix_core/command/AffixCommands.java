package net.yixi_xun.affix_core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.yixi_xun.affix_core.affix.Affix;
import net.yixi_xun.affix_core.affix.AffixManager;
import net.yixi_xun.affix_core.affix.operation.IOperation;
import net.yixi_xun.affix_core.affix.operation.OperationManager;
import net.yixi_xun.affix_core.api.AffixEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static net.minecraftforge.common.MinecraftForge.EVENT_BUS;
import static net.yixi_xun.affix_core.AffixCoreMod.LOGGER;

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
                .then(Commands.literal("list")
                    .executes(AffixCommands::listAffixes))
                .then(Commands.literal("remove")
                    .then(Commands.argument("index", IntegerArgumentType.integer(0))
                        .executes(context -> removeAffix(context, 
                            IntegerArgumentType.getInteger(context, "index")))))
                .then(Commands.literal("clear")
                    .executes(AffixCommands::clearAffixes))
                .then(Commands.literal("add")
                    .then(Commands.argument("nbt_data", StringArgumentType.greedyString())
                        .executes(context -> addAffix(context, StringArgumentType.getString(context, "nbt_data")))))
                .then(Commands.literal("merge")
                    .then(Commands.argument("nbt_data", StringArgumentType.greedyString())
                        .executes(context -> mergeNBT(context, StringArgumentType.getString(context, "nbt_data")))))
                .then(Commands.literal("template")
                    .then(Commands.argument("operation_type", StringArgumentType.string())
                        .suggests(AffixCommands::suggestOperationTypes)
                        .executes(context -> template(context, StringArgumentType.getString(context, "operation_type")))))
                .then(Commands.literal("trigger")
                        .then(Commands.argument("message", StringArgumentType.string())
                                .executes(context -> trigger(context, StringArgumentType.getString(context, "message")))))
        );
    }

    private static CompletableFuture<Suggestions> suggestOperationTypes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        Map<String, OperationManager.OperationFactory> factoryMap = OperationManager.getFactoryMap();
        for (String type : factoryMap.keySet()) {
            if (type.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(type);
            }
        }
        return builder.buildFuture();
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

    private static int addAffix(CommandContext<CommandSourceStack> context, String nbtData) {
        ItemStack itemStack = isValidItem(context);
        if (itemStack == null) return 0;

        try {
            // 解析NBT字符串，这应该是一个词缀的NBT格式
            CompoundTag parsedNBT = TagParser.parseTag(nbtData);
            
            // 从NBT创建词缀对象
            Affix affix = Affix.fromNBT(parsedNBT, 0); // 索引会在addAffix中重新分配
            
            if (affix == null) {
                context.getSource().sendFailure(Component.literal("创建词缀对象失败"));
                return 0;
            }
            
            // 向物品添加词缀
            AffixManager.addAffix(itemStack, affix);
            
            context.getSource().sendSuccess(() -> Component.literal("成功向物品添加词缀"), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("解析词缀NBT数据失败: " + e.getMessage()));
            return 0;
        }
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

    private static int template(CommandContext<CommandSourceStack> context, String operationType) {
        ItemStack itemStack = isValidItem(context);
        if (itemStack == null) return 0;
        
        try {
            // 获取所有可用的操作类型
            Map<String, OperationManager.OperationFactory> factoryMap = OperationManager.getFactoryMap();
            
            if (!factoryMap.containsKey(operationType)) {
                StringBuilder availableTypes = new StringBuilder();
                for (String type : factoryMap.keySet()) {
                    if (!availableTypes.isEmpty()) {
                        availableTypes.append(", ");
                    }
                    availableTypes.append(type);
                }
                
                context.getSource().sendFailure(Component.literal("未知的操作类型: " + operationType + ". 可用类型: " + availableTypes));
                return 0;
            }
            
            // 创建一个默认的操作NBT标签
            CompoundTag operationNbt = new CompoundTag();
            operationNbt.putString("Type", operationType);
            
            // 使用工厂创建操作实例，这样会使用默认值
            IOperation operation = factoryMap.get(operationType).create(operationNbt);
            
            if (operation == null) {
                context.getSource().sendFailure(Component.literal("无法创建操作类型: " + operationType));
                return 0;
            }
            
            // 创建一个默认的词缀对象，包含默认操作
            Affix defaultAffix = new Affix("on_attack", "", operation, 0L, 0, null, null, 0,0);
            
            // 将词缀转换回NBT格式以获取完整的默认NBT结构
            CompoundTag affixNbt = defaultAffix.toNBT();
            
            String nbtString = affixNbt.toString();
            
            // 直接将词缀添加到物品上
            AffixManager.addAffix(itemStack, defaultAffix);
            
            context.getSource().sendSuccess(() -> Component.literal("已将操作类型为 '" + operationType + "' 的样板词缀添加到物品上"), true);
            context.getSource().sendSuccess(() -> Component.literal("词缀NBT: " + nbtString), true);
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("生成样板词缀失败: " + e.getMessage()));
            LOGGER.warn("生成样板词缀失败: {}", e.getMessage());
            return 0;
        }
    }

    private static int trigger(CommandContext<CommandSourceStack> context, String message) {
        if (context.getSource().getEntity() instanceof LivingEntity entity) {
            AffixEvent.CustomMessageEvent event = new AffixEvent.CustomMessageEvent(entity, message);
            EVENT_BUS.post(event);
            context.getSource().sendSuccess(() -> Component.literal("已触发自定义消息事件: " + message), true);
            return 1;
        }
        context.getSource().sendFailure(Component.literal("触发目标不符合要求"));
        return 0;
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