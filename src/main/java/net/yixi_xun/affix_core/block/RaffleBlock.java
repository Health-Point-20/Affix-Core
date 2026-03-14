package net.yixi_xun.affix_core.block;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.yixi_xun.affix_core.api.RaffleDataManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RaffleBlock extends BaseEntityBlock {

    public RaffleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void playerDestroy(@NotNull Level level, @NotNull Player player, @NotNull BlockPos pos, @NotNull BlockState state, BlockEntity blockEntity, @NotNull ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);

        // 挖掘后触发抽奖效果
        if (!level.isClientSide && blockEntity instanceof RaffleBlockEntity raffleEntity) {
            CompoundTag nbt = raffleEntity.getPersistentData();
            List<ItemStack> rewards = new ArrayList<>();
            
            // 优先检查是否有有效的物品列表
            if (RaffleDataManager.hasValidItemList(nbt)) {
                List<ItemStack> itemListRewards = RaffleDataManager.drawFromItemList(nbt);
                if (!itemListRewards.isEmpty()) {
                    rewards = itemListRewards;
                }
            } else {
                // 从绑定的容器抽取（支持多容器）
                rewards = RaffleDataManager.drawFromContainer(nbt, level);
            }

            if (rewards.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.raffle.no_rewards").withStyle(ChatFormatting.RED), true);
                return;
            }

            // 生成物品或执行命令方块中的指令
            for (ItemStack reward : rewards) {
                // 检测是否为命令方块物品（从物品 NBT 中读取命令）
                if (level.getServer() != null && reward.hasTag()) {
                    CompoundTag itemTag = reward.getTag();
                    // 检查是否包含 BlockEntityTag（命令方块物品的 NBT 结构）
                    if (itemTag != null && itemTag.contains("BlockEntityTag", 10)) {
                        CompoundTag blockEntityTag = itemTag.getCompound("BlockEntityTag");
                        String command = blockEntityTag.getString("Command");
                        
                        // 如果命令不为空，则执行
                        if (!command.isEmpty()) {
                            CommandSourceStack sourceStack = level.getServer().createCommandSourceStack()
                                    .withPosition(pos.getCenter())
                                    .withEntity(player);
                            level.getServer().getCommands().performPrefixedCommand(sourceStack, command);
                            continue; // 执行命令后跳过物品掉落
                        }
                    }
                }
                // 普通物品直接掉落
                Vec3 itemPos = pos.getCenter();
                ItemEntity rewardEntity = new ItemEntity(level, itemPos.x, itemPos.y, itemPos.z, reward);
                level.addFreshEntity(rewardEntity);
            }
        }
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new RaffleBlockEntity(pos, state);
    }
}

