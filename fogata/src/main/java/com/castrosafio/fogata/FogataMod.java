package com.castrosafio.fogata;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(FogataMod.MOD_ID)
public final class FogataMod {
    public static final String MOD_ID = "fogata";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    public static final DeferredBlock<Block> FOGATA = BLOCKS.register("fogata", () ->
            new ResurrectionCampfireBlock(BlockBehaviour.Properties.of()
                    .strength(-1.0F, 3_600_000.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .lightLevel(state -> 10)));

    public static final DeferredItem<BlockItem> FOGATA_ITEM = ITEMS.registerSimpleBlockItem("fogata", FOGATA);

    public static final DeferredItem<Item> CUCHARA = ITEMS.register("cuchara_resurreccion",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> TENEDOR = ITEMS.register("tenedor_resurreccion",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public FogataMod(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(this::addCreativeItems);
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(FOGATA_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CUCHARA);
            event.accept(TENEDOR);
        }
    }
}
