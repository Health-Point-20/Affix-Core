package net.yixi_xun.affix_core.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.yixi_xun.affix_core.AffixCoreMod;

public class AffixCoreModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AffixCoreMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AffixCoreMod.MOD_ID);

    public static final RegistryObject<Block> RAFFLE_BLOCK = BLOCKS.register("raffle_block", () -> new RaffleBlock(BlockBehaviour.Properties.of()
            .strength(0.5f, 10.0f)
            .sound(SoundType.STONE)));
    public static final RegistryObject<BlockEntityType<RaffleBlockEntity>> RAFFLE_BLOCK_ENTITY = BLOCK_ENTITIES.register("raffle_block_entity", 
        () -> BlockEntityType.Builder.of(RaffleBlockEntity::new, RAFFLE_BLOCK.get()).build(null));

}