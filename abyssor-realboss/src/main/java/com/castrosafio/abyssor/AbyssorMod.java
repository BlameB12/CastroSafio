package com.castrosafio.abyssor;

import com.castrosafio.abyssor.entity.AbyssorEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@Mod(AbyssorMod.MODID)
public class AbyssorMod {
    public static final String MODID = "abyssor";
    public AbyssorMod(IEventBus modBus) {
        ModEntities.ENTITY_TYPES.register(modBus);
        modBus.addListener(AbyssorMod::registerAttributes);
    }
    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ABYSSOR.get(), AbyssorEntity.createAttributes().build());
    }
}
