package com.castrosafio.ofidia;

import com.castrosafio.ofidia.entity.OfidiaEntity;
import com.castrosafio.ofidia.entity.OfidiaPartEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, OfidiaMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<OfidiaEntity>> OFIDIA =
        ENTITY_TYPES.register("ofidia", () -> EntityType.Builder.of(OfidiaEntity::new, MobCategory.MONSTER)
            .sized(4.5F, 7.5F)
            .eyeHeight(6.4F)
            .clientTrackingRange(128)
            .updateInterval(1)
            .fireImmune()
            .build("ofidia:ofidia"));

    public static final DeferredHolder<EntityType<?>, EntityType<OfidiaPartEntity>> OFIDIA_PART =
        ENTITY_TYPES.register("ofidia_part", () -> EntityType.Builder.of(OfidiaPartEntity::new, MobCategory.MISC)
            .sized(3.4F, 3.4F)
            .clientTrackingRange(128)
            .updateInterval(1)
            .build("ofidia:ofidia_part"));
}
