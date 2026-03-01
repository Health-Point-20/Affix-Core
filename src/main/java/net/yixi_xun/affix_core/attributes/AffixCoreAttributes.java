package net.yixi_xun.affix_core.attributes;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.yixi_xun.affix_core.AffixCoreMod;

import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class AffixCoreAttributes {
    private static final double Min = -100000000.0;
    private static final double Max = 100000000.0;
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, AffixCoreMod.MOD_ID);

    public static final RegistryObject<Attribute> EVASION = ATTRIBUTES.register("evasion",
            () -> (new RangedAttribute("attribute." + AffixCoreMod.MOD_ID + ".evasion", 0.0, Min, Max)).setSyncable(true));

    public static final RegistryObject<Attribute> HIT_RATE = ATTRIBUTES.register("hit_rate",
            () -> (new RangedAttribute("attribute." + AffixCoreMod.MOD_ID + ".hit_rate", 1.0, Min, Max)).setSyncable(true));

    public static final RegistryObject<Attribute> FINAL_HIT_RATE = ATTRIBUTES.register("final_hit_rate",
            () -> (new RangedAttribute("attribute." + AffixCoreMod.MOD_ID + ".final_hit_rate", 0.0, Min, Max)).setSyncable(true));

    public static final RegistryObject<Attribute> FINAL_EVASION = ATTRIBUTES.register("final_evasion",
            () -> (new RangedAttribute("attribute." + AffixCoreMod.MOD_ID + ".final_evasion", 0.0, Min, Max)).setSyncable(true));

    public static final RegistryObject<Attribute> IGNORE_INVINCIBLE_TIME = ATTRIBUTES.register("ignore_invincible_time",
            () -> (new RangedAttribute("attribute." + AffixCoreMod.MOD_ID + ".ignore_invincible_time", 0.0, Min, Max)).setSyncable(true));

    public static final RegistryObject<Attribute> PHYSICAL_DAMAGE_REDUCTION = ATTRIBUTES.register("physical_damage_reduction",
            () -> (new RangedAttribute("attribute." + AffixCoreMod.MOD_ID + ".physical_damage_reduction", 0.0, Min, Max)).setSyncable(true));

    public static final RegistryObject<Attribute> MAGIC_DAMAGE_REDUCTION = ATTRIBUTES.register("magic_damage_reduction",
            () -> (new RangedAttribute("attribute." + AffixCoreMod.MOD_ID + ".magic_damage_reduction", 0.0, Min, Max)).setSyncable(true));

    public static final RegistryObject<Attribute> DAMAGE_REDUCTION = ATTRIBUTES.register("damage_reduction",
            () -> (new RangedAttribute("attribute." + AffixCoreMod.MOD_ID + ".damage_reduction", 0.0, Min, Max)).setSyncable(true));

    public static final RegistryObject<Attribute> MAGIC_ARMOR = ATTRIBUTES.register("magic_armor",
            () -> (new RangedAttribute("attribute." + AffixCoreMod.MOD_ID + ".magic_armor", 0, 0, Max)).setSyncable(true));


    @SuppressWarnings("removal")
    @SubscribeEvent
    public static void register(FMLConstructModEvent event) {
        event.enqueueWork(() -> ATTRIBUTES.register(FMLJavaModLoadingContext.get().getModEventBus()));
    }

    @SubscribeEvent
    public static void addAttributes(EntityAttributeModificationEvent event) {
        List<RegistryObject<Attribute>> attributes = Arrays.asList(
                EVASION,
                HIT_RATE,
                FINAL_EVASION,
                FINAL_HIT_RATE,
                IGNORE_INVINCIBLE_TIME,
                PHYSICAL_DAMAGE_REDUCTION,
                MAGIC_DAMAGE_REDUCTION,
                DAMAGE_REDUCTION,
                MAGIC_ARMOR
        );

        for (RegistryObject<Attribute> attribute : attributes) {
            event.getTypes().stream()
                    .filter(type -> type.getBaseClass().isAssignableFrom(LivingEntity.class) && event.has(type, Attributes.ATTACK_DAMAGE))
                    .forEach(type -> event.add(type, attribute.get()));
        }
    }
}
