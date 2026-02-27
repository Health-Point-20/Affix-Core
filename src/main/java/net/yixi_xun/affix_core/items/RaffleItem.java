package net.yixi_xun.affix_core.items;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RaffleItem extends Item {
    
    public RaffleItem() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));
    }
    
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // 普通右键进行抽奖
        if (!level.isClientSide && !player.isShiftKeyDown()) {
            List<ItemStack> rewards = drawRewards(stack, level);
            
            if (rewards.isEmpty()) {
                player.displayClientMessage(
                    Component.translatable("message.raffle.no_rewards").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            
            // 给予奖励物品
            for (ItemStack reward : rewards) {
                if (!player.getInventory().add(reward)) {
                    player.drop(reward, false);
                }
            }
            
            player.displayClientMessage(
                Component.translatable("message.raffle.success", rewards.size()).withStyle(ChatFormatting.GREEN), true);
        }
        
        return InteractionResultHolder.success(stack);
    }
    
    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        
        // Shift右键绑定容器
        if (player != null && player.isShiftKeyDown()) {
            return bindContainer(stack, player, level, clickedPos);
        }
        
        return InteractionResult.PASS;
    }
    
    /**
     * 绑定容器
     */
    public static InteractionResult bindContainerFromBlock(CompoundTag nbt, Player player, Level level, BlockPos containerPos) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        
        // 检查权限（创造模式且至少2级权限）
        if (!serverPlayer.isCreative() || !serverPlayer.hasPermissions(2)) {
            player.displayClientMessage(Component.translatable("message.raffle.no_permission").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        
        // 验证方块是否为有效容器
        if (!isValidContainer(level, containerPos)) {
            player.displayClientMessage(Component.translatable("message.raffle.not_container").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        
        // 保存容器位置到NBT
        RaffleDataManager.setBoundContainerPos(nbt, containerPos);
        
        player.displayClientMessage(
            Component.translatable("message.raffle.bind_success", 
                containerPos.getX(), containerPos.getY(), containerPos.getZ())
                .withStyle(ChatFormatting.GREEN), true);
                
        return InteractionResult.SUCCESS;
    }
    
    /**
     * 绑定容器到抽奖物品
     */
    public static InteractionResult bindContainer(ItemStack stack, Player player, Level level, BlockPos containerPos) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        
        // 检查权限（创造模式且至少2级权限）
        if (!serverPlayer.isCreative() || !serverPlayer.hasPermissions(2)) {
            player.displayClientMessage(Component.translatable("message.raffle.no_permission").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        
        // 验证方块是否为有效容器
        if (!isValidContainer(level, containerPos)) {
            player.displayClientMessage(Component.translatable("message.raffle.not_container").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        
        // 保存容器位置到NBT
        RaffleDataManager.setBoundContainerPos(stack, containerPos);
        
        player.displayClientMessage(
            Component.translatable("message.raffle.bind_success", 
                containerPos.getX(), containerPos.getY(), containerPos.getZ())
                .withStyle(ChatFormatting.GREEN), true);
                
        return InteractionResult.SUCCESS;
    }
    
    /**
     * 验证方块是否为有效容器
     */
    private static boolean isValidContainer(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity != null && blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
    }
    
    /**
     * 抽取奖励物品
     */
    private List<ItemStack> drawRewards(ItemStack stack, Level level) {
        // 优先检查是否有有效的物品列表
        if (RaffleDataManager.hasValidItemList(stack)) {
            List<ItemStack> itemListRewards = RaffleDataManager.drawFromItemList(stack);
            if (!itemListRewards.isEmpty()) {
                return itemListRewards;
            }
        }
        
        // 否则从绑定的容器抽取
        return RaffleDataManager.drawFromContainer(stack, level);
    }
    
    @Override
    public void appendHoverText(@NotNull ItemStack stack, Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        
        tooltip.add(Component.translatable("tooltip.raffle.item").withStyle(ChatFormatting.GOLD));
        
        // 显示基础信息
        int drawCount = RaffleDataManager.getDrawCount(stack);
        boolean allowRepeat = RaffleDataManager.getAllowRepeat(stack);
        boolean consumeItems = RaffleDataManager.getConsumeItems(stack);
        boolean consumeSelf = RaffleDataManager.getConsumeSelf(stack);
        String yes = Component.translatable("raffle.yes").getString();
        String no = Component.translatable("raffle.no").getString();

        tooltip.add(Component.translatable("raffle.draw_count", drawCount)
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("raffle.repeat_draw", (allowRepeat ? yes : no))
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.raffle.consume_items", (consumeItems ? yes : no))
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.raffle.consume_self", (consumeSelf ? yes : no))
            .withStyle(ChatFormatting.GRAY));
        
        // 按住Shift显示详细概率信息
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.empty());
            List<Component> probabilityInfo = RaffleDataManager.getProbabilityInfo(stack, level);
            if (!probabilityInfo.isEmpty()) {
                tooltip.addAll(RaffleDataManager.getProbabilityInfo(stack, level));
            } else {
                tooltip.add(Component.translatable("tooltip.raffle.configure_list").withStyle(ChatFormatting.RED));
            }

        } else {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("tooltip.raffle.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
        
        // 显示使用说明
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.raffle.usage").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  • ").append(Component.translatable("tooltip.raffle.use_normal")).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  • ").append(Component.translatable("tooltip.raffle.use_shift")).withStyle(ChatFormatting.GRAY));
    }
}