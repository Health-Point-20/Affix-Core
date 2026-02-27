package net.yixi_xun.affix_core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RaffleBlockEntity extends BlockEntity {
    public RaffleBlockEntity(BlockPos pos, BlockState state) {
        super(AffixCoreModBlocks.RAFFLE_BLOCK_ENTITY.get(), pos, state);
    }
}