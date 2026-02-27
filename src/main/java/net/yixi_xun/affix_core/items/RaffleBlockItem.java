package net.yixi_xun.affix_core.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.yixi_xun.affix_core.block.AffixCoreModBlocks;
import net.yixi_xun.affix_core.block.RaffleBlockEntity;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class RaffleBlockItem extends BlockItem {
    public RaffleBlockItem() {
        super(AffixCoreModBlocks.RAFFLE_BLOCK.get(), new Item.Properties());
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        // Shift右键绑定容器
        if (player != null && player.isShiftKeyDown()) {
            return RaffleItem.bindContainer(stack, player, level, clickedPos);
        }

        return super.useOn(context);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(@NotNull BlockPos pos, @NotNull Level level, @Nullable Player player, @NotNull ItemStack stack, @NotNull BlockState state) {
        // 获取放置的方块实体
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RaffleBlockEntity raffleEntity) {
            CompoundTag itemTag = stack.getTag();
            if (itemTag != null) {
                raffleEntity.getPersistentData().merge(itemTag);
            }
            return true;
        }
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        tooltip.add(Component.translatable("tooltip.raffle.item").withStyle(ChatFormatting.GOLD));

        // 显示基础信息
        int drawCount = RaffleDataManager.getDrawCount(stack);
        boolean consumeItems = RaffleDataManager.getConsumeItems(stack);
        String yes = Component.translatable("raffle.yes").getString();
        String no = Component.translatable("raffle.no").getString();

        tooltip.add(Component.translatable("raffle.draw_count", drawCount)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.raffle.consume_items", (consumeItems ? yes : no))
                .withStyle(ChatFormatting.GRAY));
    }
}
