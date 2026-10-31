package com.castrosafio.ofidia;

import com.castrosafio.ofidia.entity.OfidiaEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@Mod(OfidiaMod.MODID)
public class OfidiaMod {
    public static final String MODID = "ofidia";

    public OfidiaMod(IEventBus modBus) {
        ModEntities.ENTITY_TYPES.register(modBus);
        modBus.addListener(OfidiaMod::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.OFIDIA.get(), OfidiaEntity.createAttributes().build());
    }
}
