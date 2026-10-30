package com.castrosafio.abyssor;

import com.castrosafio.abyssor.entity.AbyssorEntity;
import com.castrosafio.abyssor.entity.AbyssorPartEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private ModEntities() {}
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, AbyssorMod.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<AbyssorEntity>> ABYSSOR =
        ENTITY_TYPES.register("abyssor", () -> EntityType.Builder.of(AbyssorEntity::new, MobCategory.MONSTER)
            .sized(6.0F, 6.0F).eyeHeight(3.7F).clientTrackingRange(128).updateInterval(1).fireImmune()
            .build(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(AbyssorMod.MODID, "abyssor"))));
    public static final DeferredHolder<EntityType<?>, EntityType<AbyssorPartEntity>> ABYSSOR_PART =
        ENTITY_TYPES.register("abyssor_part", () -> EntityType.Builder.of(AbyssorPartEntity::new, MobCategory.MISC)
            .sized(5.0F, 4.5F).clientTrackingRange(128).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(AbyssorMod.MODID, "abyssor_part"))));
}
