package net.yixi_xun.affix_core.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMaxDamageMixin {

    @Inject(
        method = "getMaxDamage()I",
        at = @At("HEAD"),
        cancellable = true
    )
    private void injectRarityMaxDurability(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        CompoundTag tag = stack.getTag();

        if (tag != null && tag.contains("Affix_Durability")) {
            int customMax = tag.getInt("Affix_Durability");

            int currentDamage = tag.getInt("Damage");
            if (currentDamage > customMax) {
                tag.putInt("Damage", customMax);
            }
            cir.setReturnValue(customMax);
        }
    }
}