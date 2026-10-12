package com.castrosafio.menu;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class MissionCommands {
    private static final List<String> TYPES = List.of("ninguno", "matar", "romper", "conseguir", "craftear", "visitar");

    private MissionCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("castrosafio")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("editor")
                                .then(Commands.literal("misiones").executes(ctx -> openEditor(ctx.getSource(), "missions")))
                                .then(Commands.literal("mobs").executes(ctx -> openEditor(ctx.getSource(), "mobs")))
                                .then(Commands.literal("crafteos").executes(ctx -> openEditor(ctx.getSource(), "crafts"))))
                        .then(Commands.literal("mision")
                                .then(Commands.literal("desbloquear")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, MissionManager.MAX_MISSIONS))
                                                .executes(ctx -> setUnlocked(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id"), true))))
                                .then(Commands.literal("bloquear")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, MissionManager.MAX_MISSIONS))
                                                .executes(ctx -> setUnlocked(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id"), false))))
                                .then(Commands.literal("activar")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, MissionManager.MAX_MISSIONS))
                                                .executes(ctx -> activate(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))))
                                .then(Commands.literal("titulo")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, MissionManager.MAX_MISSIONS))
                                                .then(Commands.argument("texto", StringArgumentType.greedyString())
                                                        .executes(ctx -> setTitle(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id"), StringArgumentType.getString(ctx, "texto"))))))
                                .then(Commands.literal("descripcion")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, MissionManager.MAX_MISSIONS))
                                                .then(Commands.argument("texto", StringArgumentType.greedyString())
                                                        .executes(ctx -> setDescription(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id"), StringArgumentType.getString(ctx, "texto"))))))
                                .then(Commands.literal("objetivo")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, MissionManager.MAX_MISSIONS))
                                                .then(Commands.argument("tipo", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(TYPES, builder))
                                                        .then(Commands.argument("objetivo", StringArgumentType.word())
                                                                .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                                                        .executes(ctx -> setSingleObjective(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id"), StringArgumentType.getString(ctx, "tipo"), StringArgumentType.getString(ctx, "objetivo"), IntegerArgumentType.getInteger(ctx, "cantidad"))))))))
                                .then(Commands.literal("objetivos")
                                        .then(Commands.literal("limpiar")
                                                .then(Commands.argument("id", IntegerArgumentType.integer(1, MissionManager.MAX_MISSIONS))
                                                        .executes(ctx -> clearObjectives(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))))
                                        .then(Commands.literal("agregar")
                                                .then(Commands.argument("id", IntegerArgumentType.integer(1, MissionManager.MAX_MISSIONS))
                                                        .then(Commands.argument("tipo", StringArgumentType.word())
                                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(TYPES, builder))
                                                                .then(Commands.argument("objetivo", StringArgumentType.word())
                                                                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                                                                .executes(ctx -> addObjective(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id"), StringArgumentType.getString(ctx, "tipo"), StringArgumentType.getString(ctx, "objetivo"), IntegerArgumentType.getInteger(ctx, "cantidad")))))))))
                                .then(Commands.literal("info")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, MissionManager.MAX_MISSIONS))
                                                .executes(ctx -> info(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))))
                                .then(Commands.literal("limpiar")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, MissionManager.MAX_MISSIONS))
                                                .executes(ctx -> reset(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id"))))))
                        .then(Commands.literal("mob")
                                .then(Commands.literal("habilitar")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> setMob(ctx.getSource(), StringArgumentType.getString(ctx, "id"), true))))
                                .then(Commands.literal("deshabilitar")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> setMob(ctx.getSource(), StringArgumentType.getString(ctx, "id"), false))))
                                .then(Commands.literal("limpiar").executes(ctx -> clearMobs(ctx.getSource()))))
                        .then(Commands.literal("crafteo")
                                .then(Commands.literal("limpiar")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, CraftConfigManager.MAX_CRAFTS))
                                                .executes(ctx -> clearCraft(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))))
                                .then(Commands.literal("nombre")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, CraftConfigManager.MAX_CRAFTS))
                                                .then(Commands.argument("texto", StringArgumentType.greedyString())
                                                        .executes(ctx -> setCraftName(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id"), StringArgumentType.getString(ctx, "texto"))))))
                                .then(Commands.literal("ingrediente")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, CraftConfigManager.MAX_CRAFTS))
                                                .then(Commands.argument("slot", IntegerArgumentType.integer(0, 8))
                                                        .then(Commands.argument("item", StringArgumentType.word())
                                                                .executes(ctx -> setCraftIngredient(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id"), IntegerArgumentType.getInteger(ctx, "slot"), StringArgumentType.getString(ctx, "item")))))))
                                .then(Commands.literal("resultado")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, CraftConfigManager.MAX_CRAFTS))
                                                .then(Commands.argument("item", StringArgumentType.word())
                                                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 64))
                                                                .executes(ctx -> setCraftResult(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id"), StringArgumentType.getString(ctx, "item"), IntegerArgumentType.getInteger(ctx, "cantidad")))))))
                                .then(Commands.literal("activar")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, CraftConfigManager.MAX_CRAFTS))
                                                .executes(ctx -> setCraftActive(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id"), true))))
                                .then(Commands.literal("desactivar")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, CraftConfigManager.MAX_CRAFTS))
                                                .executes(ctx -> setCraftActive(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id"), false)))))
                        .then(Commands.literal("recargar").executes(ctx -> reload(ctx.getSource())))
                        .then(Commands.literal("ayuda").executes(ctx -> help(ctx.getSource())))
        );
    }

    private static int openEditor(CommandSourceStack source, String mode) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("[CastroSafio] El editor solo puede abrirlo un jugador OP."));
            return 0;
        }
        MissionManager.sync(player);
        MobConfigManager.sync(player);
        PacketDistributor.sendToPlayer(player, new EditorOpenPayload(mode));
        return 1;
    }

    private static int setUnlocked(CommandSourceStack source, int id, boolean unlocked) {
        MissionDefinition mission = MissionManager.get(id);
        if (mission == null) return 0;
        mission.unlocked = unlocked;
        if (!unlocked) mission.active = false;
        saveMissionState();
        source.sendSuccess(() -> Component.literal("[CastroSafio] Mision " + id + (unlocked ? " desbloqueada." : " bloqueada.")), true);
        return 1;
    }

    private static int activate(CommandSourceStack source, int id) {
        MissionManager.activate(id);
        MissionManager.syncAll();
        source.sendSuccess(() -> Component.literal("[CastroSafio] Mision " + id + " activada. Solo esta se mostrara a los jugadores."), true);
        return 1;
    }

    private static int setTitle(CommandSourceStack source, int id, String text) {
        MissionDefinition mission = MissionManager.get(id);
        if (mission == null) return 0;
        mission.title = text;
        saveMissionState();
        return 1;
    }

    private static int setDescription(CommandSourceStack source, int id, String text) {
        MissionDefinition mission = MissionManager.get(id);
        if (mission == null) return 0;
        mission.description = text;
        saveMissionState();
        return 1;
    }

    private static int setSingleObjective(CommandSourceStack source, int id, String type, String target, int amount) {
        MissionDefinition mission = MissionManager.get(id);
        if (mission == null || !validType(type)) return 0;
        MissionManager.clearObjectives(id);
        MissionManager.addObjective(id, type.toLowerCase(), target, amount);
        mission.objectiveType = type.toLowerCase();
        mission.target = target;
        mission.amount = amount;
        saveMissionState();
        return 1;
    }

    private static int clearObjectives(CommandSourceStack source, int id) {
        MissionManager.clearObjectives(id);
        saveMissionState();
        return 1;
    }

    private static int addObjective(CommandSourceStack source, int id, String type, String target, int amount) {
        if (!validType(type)) {
            source.sendFailure(Component.literal("[CastroSafio] Tipo de objetivo invalido."));
            return 0;
        }
        MissionManager.addObjective(id, type.toLowerCase(), target, amount);
        saveMissionState();
        return 1;
    }

    private static boolean validType(String type) { return TYPES.contains(type.toLowerCase()); }

    private static int info(CommandSourceStack source, int id) {
        MissionDefinition mission = MissionManager.get(id);
        if (mission == null) return 0;
        source.sendSuccess(() -> Component.literal("--- CastroSafio | Mision " + id + " ---"), false);
        source.sendSuccess(() -> Component.literal("Activa: " + (mission.active ? "SI" : "NO")), false);
        source.sendSuccess(() -> Component.literal("Titulo: " + mission.title), false);
        source.sendSuccess(() -> Component.literal("Descripcion: " + mission.description), false);
        source.sendSuccess(() -> Component.literal("Objetivos: " + mission.objectiveSummary()), false);
        if (mission.objectives != null) for (MissionObjective objective : mission.objectives) source.sendSuccess(() -> Component.literal(" - " + objective.summary()), false);
        return 1;
    }

    private static int reset(CommandSourceStack source, int id) {
        MissionDefinition mission = MissionManager.get(id);
        if (mission == null) return 0;
        MissionDefinition fresh = new MissionDefinition(id);
        mission.title = fresh.title;
        mission.description = fresh.description;
        mission.objectiveType = fresh.objectiveType;
        mission.target = fresh.target;
        mission.amount = fresh.amount;
        mission.unlocked = false;
        mission.active = false;
        mission.objectives = fresh.objectives;
        saveMissionState();
        return 1;
    }

    private static int setMob(CommandSourceStack source, String id, boolean enabled) {
        MobConfigManager.setEnabled(id, enabled);
        MobConfigManager.syncAll();
        source.sendSuccess(() -> Component.literal("[CastroSafio] " + id + (enabled ? " habilitado." : " deshabilitado.")), true);
        return 1;
    }

    private static int clearMobs(CommandSourceStack source) {
        MobConfigManager.clear();
        MobConfigManager.syncAll();
        source.sendSuccess(() -> Component.literal("[CastroSafio] Lista de mobs habilitados vaciada. Sin lista, no se restringen spawns."), true);
        return 1;
    }

    private static int clearCraft(CommandSourceStack source, int id) {
        CraftDefinition craft = CraftConfigManager.get(id);
        if (craft == null) return 0;
        CraftDefinition fresh = new CraftDefinition(id);
        craft.name = fresh.name;
        craft.ingredients = fresh.ingredients;
        craft.result = fresh.result;
        craft.resultAmount = fresh.resultAmount;
        craft.active = false;
        saveCraftState();
        return 1;
    }

    private static int setCraftName(CommandSourceStack source, int id, String name) {
        CraftDefinition craft = CraftConfigManager.get(id);
        if (craft == null) return 0;
        craft.name = name;
        saveCraftState();
        return 1;
    }

    private static int setCraftIngredient(CommandSourceStack source, int id, int slot, String item) {
        CraftDefinition craft = CraftConfigManager.get(id);
        if (craft == null) return 0;
        craft.ingredients.set(slot, "_".equals(item) ? "" : item);
        saveCraftState();
        return 1;
    }

    private static int setCraftResult(CommandSourceStack source, int id, String item, int amount) {
        CraftDefinition craft = CraftConfigManager.get(id);
        if (craft == null) return 0;
        craft.result = item;
        craft.resultAmount = amount;
        saveCraftState();
        return 1;
    }

    private static int setCraftActive(CommandSourceStack source, int id, boolean active) {
        CraftDefinition craft = CraftConfigManager.get(id);
        if (craft == null) return 0;
        craft.active = active;
        saveCraftState();
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        MissionManager.load();
        MobConfigManager.load();
        CraftConfigManager.load();
        MissionManager.syncAll();
        MobConfigManager.syncAll();
        source.sendSuccess(() -> Component.literal("[CastroSafio] Configuracion recargada."), true);
        return 1;
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("[CastroSafio] Editores OP:"), false);
        source.sendSuccess(() -> Component.literal("/castrosafio editor misiones"), false);
        source.sendSuccess(() -> Component.literal("/castrosafio editor mobs"), false);
        source.sendSuccess(() -> Component.literal("/castrosafio editor crafteos"), false);
        return 1;
    }

    private static void saveMissionState() {
        MissionManager.save();
        MissionManager.syncAll();
    }

    private static void saveCraftState() {
        CraftConfigManager.save();
        MobConfigManager.syncAll();
    }
}
