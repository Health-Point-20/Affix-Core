package net.yixi_xun.affix_core.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.yixi_xun.affix_core.tooltip.TooltipHandler.processColors;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(
        method = "getMaxDamage()I",
        at = @At("RETURN"),
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

    @Inject(
            method = "getHoverName()Lnet/minecraft/network/chat/Component;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void injectRarityHoverName(CallbackInfoReturnable<Component> cir) {
        Component originalName = cir.getReturnValue();
        // 处理颜色
        if (originalName == null) return;
        MutableComponent finalComponent = processColors(originalName.getString());

        // 只有包含颜色标记的文本才处理颜色，否则保留原有样式
        if (finalComponent != null) {
            cir.setReturnValue(finalComponent);
        }
    }

    @Inject(
            method = "getBarWidth()I",
            at = @At("RETURN"),
            cancellable = true
    )
    private void injectRarityBarWidth(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getTag() != null && stack.getTag().contains("Affix_Durability")) {
            cir.setReturnValue(Math.round(13.0F - (float) stack.getDamageValue() * 13.0F / (float) stack.getMaxDamage()));
        }
    }

    @Inject(
            method = "getBarColor()I",
            at = @At("RETURN"),
            cancellable = true
    )
    private void injectRarityBarColor(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getTag() != null && stack.getTag().contains("Affix_Durability")) {
            float stackMaxDamage = stack.getMaxDamage();
            float f = Math.max(0.0F, (stackMaxDamage - (float)stack.getDamageValue()) / stackMaxDamage);
            cir.setReturnValue(Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F));
        }
    }
}