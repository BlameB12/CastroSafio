package com.castrosafio.fogata;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

public final class ReviveMenu extends ChestMenu {
    private static final int MENU_SIZE = 27;

    private final SimpleContainer display;
    private final ServerLevel altarLevel;
    private final BlockPos altarPos;
    private final InteractionHand requiredHand;
    private final Item requiredItem;
    private final UUID[] targetBySlot = new UUID[MENU_SIZE];
    private UUID confirmingTarget;

    public ReviveMenu(int id, Inventory playerInventory, ServerLevel altarLevel, BlockPos altarPos,
                      InteractionHand requiredHand, Item requiredItem) {
        this(id, playerInventory, new SimpleContainer(MENU_SIZE), altarLevel, altarPos, requiredHand, requiredItem);
    }

    private ReviveMenu(int id, Inventory playerInventory, SimpleContainer display, ServerLevel altarLevel,
                       BlockPos altarPos, InteractionHand requiredHand, Item requiredItem) {
        super(MenuType.GENERIC_9x3, id, playerInventory, display, 3);
        this.display = display;
        this.altarLevel = altarLevel;
        this.altarPos = altarPos.immutable();
        this.requiredHand = requiredHand;
        this.requiredItem = requiredItem;
        showSpectators();
    }

    private void clearDisplay() {
        Arrays.fill(targetBySlot, null);
        for (int i = 0; i < MENU_SIZE; i++) {
            display.setItem(i, ItemStack.EMPTY);
        }
    }

    private static ItemStack named(Item item, String name, ChatFormatting color) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(color, ChatFormatting.BOLD));
        return stack;
    }

    private void showSpectators() {
        confirmingTarget = null;
        clearDisplay();

        List<ServerPlayer> spectators = altarLevel.getServer().getPlayerList().getPlayers().stream()
                .filter(Player::isSpectator)
                .toList();

        if (spectators.isEmpty()) {
            display.setItem(13, named(Items.BARRIER, "No hay jugadores muertos", ChatFormatting.RED));
            broadcastChanges();
            return;
        }

        for (int i = 0; i < Math.min(spectators.size(), MENU_SIZE); i++) {
            ServerPlayer target = spectators.get(i);
            ItemStack icon = named(Items.TOTEM_OF_UNDYING, target.getGameProfile().getName(), ChatFormatting.YELLOW);
            display.setItem(i, icon);
            targetBySlot[i] = target.getUUID();
        }
        broadcastChanges();
    }

    private void showConfirmation(UUID targetId) {
        ServerPlayer target = altarLevel.getServer().getPlayerList().getPlayer(targetId);
        if (target == null || !target.isSpectator()) {
            showSpectators();
            return;
        }

        confirmingTarget = targetId;
        clearDisplay();
        display.setItem(11, named(Items.LIME_WOOL, "REVIVIR", ChatFormatting.GREEN));
        display.setItem(13, named(Items.TOTEM_OF_UNDYING, target.getGameProfile().getName(), ChatFormatting.YELLOW));
        display.setItem(15, named(Items.RED_WOOL, "Cancelar", ChatFormatting.RED));
        broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (slotId >= 0 && slotId < MENU_SIZE) {
            if (confirmingTarget == null) {
                UUID target = targetBySlot[slotId];
                if (target != null) {
                    showConfirmation(target);
                }
            } else {
                if (slotId == 11) {
                    revive(serverPlayer, confirmingTarget);
                } else if (slotId == 15) {
                    showSpectators();
                }
            }
            return;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level() != altarLevel) return false;
        if (!altarLevel.getBlockState(altarPos).is(FogataMod.FOGATA.get())) return false;
        double dx = player.getX() - (altarPos.getX() + 0.5);
        double dy = player.getY() - (altarPos.getY() + 0.5);
        double dz = player.getZ() - (altarPos.getZ() + 0.5);
        return dx * dx + dy * dy + dz * dz <= 64.0;
    }

    private void revive(ServerPlayer reviver, UUID targetId) {
        ItemStack held = reviver.getItemInHand(requiredHand);
        if (held.isEmpty() || held.getItem() != requiredItem) {
            reviver.sendSystemMessage(Component.literal("Debes seguir sosteniendo el mismo objeto de resurrección.")
                    .withStyle(ChatFormatting.RED));
            reviver.closeContainer();
            return;
        }

        ServerPlayer target = altarLevel.getServer().getPlayerList().getPlayer(targetId);
        if (target == null || !target.isSpectator()) {
            reviver.sendSystemMessage(Component.literal("Ese jugador ya no está conectado en modo espectador.")
                    .withStyle(ChatFormatting.RED));
            showSpectators();
            return;
        }

        if (!altarLevel.getBlockState(altarPos).is(FogataMod.FOGATA.get())) {
            reviver.sendSystemMessage(Component.literal("La fogata ya no existe.").withStyle(ChatFormatting.RED));
            reviver.closeContainer();
            return;
        }

        if (!target.setGameMode(GameType.SURVIVAL)) {
            reviver.sendSystemMessage(Component.literal("No se pudo cambiar al jugador a supervivencia.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        target.teleportTo(altarLevel,
                altarPos.getX() + 0.5,
                altarPos.getY() + 6.0,
                altarPos.getZ() + 0.5,
                target.getYRot(), target.getXRot());

        target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 8, 0, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 12, 0, false, false));

        if (!reviver.getAbilities().instabuild) {
            held.shrink(1);
        }

        double px = altarPos.getX() + 0.5;
        double py = altarPos.getY() + 2.0;
        double pz = altarPos.getZ() + 0.5;
        altarLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, px, py, pz, 80, 1.3, 2.2, 1.3, 0.15);
        altarLevel.sendParticles(ParticleTypes.FLAME, px, py, pz, 45, 1.0, 1.5, 1.0, 0.05);
        altarLevel.playSound(null, altarPos, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.2F, 1.0F);

        altarLevel.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal(reviver.getGameProfile().getName()).withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(" revivió a ").withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(target.getGameProfile().getName()).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" con el poder de la fogata").withStyle(ChatFormatting.WHITE)),
                false
        );

        reviver.closeContainer();
    }
}
