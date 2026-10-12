package com.castrosafio.menu;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

public final class MissionCommands {
    private static final List<String> TYPES = List.of(
            "ninguno",
            "matar",
            "romper",
            "conseguir",
            "craftear",
            "visitar"
    );

    private MissionCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("castrosafio")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("mision")
                                .then(Commands.literal("desbloquear")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, MissionManager.MAX_MISSIONS))
                                                .executes(ctx -> setUnlocked(
                                                        ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "id"),
                                                        true
                                                ))))
                                .then(Commands.literal("bloquear")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, MissionManager.MAX_MISSIONS))
                                                .executes(ctx -> setUnlocked(
                                                        ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "id"),
                                                        false
                                                ))))
                                .then(Commands.literal("titulo")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, MissionManager.MAX_MISSIONS))
                                                .then(Commands.argument("texto", StringArgumentType.greedyString())
                                                        .executes(ctx -> setTitle(
                                                                ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "id"),
                                                                StringArgumentType.getString(ctx, "texto")
                                                        )))))
                                .then(Commands.literal("descripcion")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, MissionManager.MAX_MISSIONS))
                                                .then(Commands.argument("texto", StringArgumentType.greedyString())
                                                        .executes(ctx -> setDescription(
                                                                ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "id"),
                                                                StringArgumentType.getString(ctx, "texto")
                                                        )))))
                                .then(Commands.literal("objetivo")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, MissionManager.MAX_MISSIONS))
                                                .then(Commands.argument("tipo", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(TYPES, builder))
                                                        .then(Commands.argument("objetivo", StringArgumentType.word())
                                                                .then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
                                                                        .executes(ctx -> setObjective(
                                                                                ctx.getSource(),
                                                                                IntegerArgumentType.getInteger(ctx, "id"),
                                                                                StringArgumentType.getString(ctx, "tipo"),
                                                                                StringArgumentType.getString(ctx, "objetivo"),
                                                                                IntegerArgumentType.getInteger(ctx, "cantidad")
                                                                        )))))))
                                .then(Commands.literal("info")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, MissionManager.MAX_MISSIONS))
                                                .executes(ctx -> info(
                                                        ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "id")
                                                ))))
                                .then(Commands.literal("limpiar")
                                        .then(Commands.argument("id", IntegerArgumentType.integer(1, MissionManager.MAX_MISSIONS))
                                                .executes(ctx -> reset(
                                                        ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "id")
                                                )))))
                        .then(Commands.literal("recargar")
                                .executes(ctx -> reload(ctx.getSource())))
                        .then(Commands.literal("ayuda")
                                .executes(ctx -> help(ctx.getSource())))
        );
    }

    private static int setUnlocked(CommandSourceStack source, int id, boolean unlocked) {
        MissionDefinition mission = MissionManager.get(id);
        if (mission == null) {
            return 0;
        }

        mission.unlocked = unlocked;
        saveAndSync();
        source.sendSuccess(
                () -> Component.literal("[CastroSafio] Mision " + id + (unlocked ? " desbloqueada." : " bloqueada.")),
                true
        );
        return 1;
    }

    private static int setTitle(CommandSourceStack source, int id, String text) {
        MissionDefinition mission = MissionManager.get(id);
        if (mission == null) {
            return 0;
        }

        mission.title = text;
        saveAndSync();
        source.sendSuccess(() -> Component.literal("[CastroSafio] Titulo de la mision " + id + ": " + text), true);
        return 1;
    }

    private static int setDescription(CommandSourceStack source, int id, String text) {
        MissionDefinition mission = MissionManager.get(id);
        if (mission == null) {
            return 0;
        }

        mission.description = text;
        saveAndSync();
        source.sendSuccess(() -> Component.literal("[CastroSafio] Descripcion de la mision " + id + " actualizada."), true);
        return 1;
    }

    private static int setObjective(CommandSourceStack source, int id, String type, String target, int amount) {
        MissionDefinition mission = MissionManager.get(id);
        if (mission == null) {
            return 0;
        }

        type = type.toLowerCase();
        if (!TYPES.contains(type)) {
            source.sendFailure(Component.literal(
                    "[CastroSafio] Tipo invalido. Usa: ninguno, matar, romper, conseguir, craftear o visitar."
            ));
            return 0;
        }

        mission.objectiveType = type;
        mission.target = "ninguno".equals(type) ? "" : target;
        mission.amount = amount;
        saveAndSync();

        String finalType = type;
        source.sendSuccess(
                () -> Component.literal(
                        "[CastroSafio] Objetivo de mision " + id + ": "
                                + finalType + " " + amount + (mission.target.isBlank() ? "" : " x " + mission.target)
                ),
                true
        );
        return 1;
    }

    private static int info(CommandSourceStack source, int id) {
        MissionDefinition mission = MissionManager.get(id);
        if (mission == null) {
            return 0;
        }

        source.sendSuccess(() -> Component.literal("--- CastroSafio | Mision " + id + " ---"), false);
        source.sendSuccess(() -> Component.literal("Estado: " + (mission.unlocked ? "DESBLOQUEADA" : "BLOQUEADA")), false);
        source.sendSuccess(() -> Component.literal("Titulo: " + mission.title), false);
        source.sendSuccess(() -> Component.literal("Descripcion: " + mission.description), false);
        source.sendSuccess(() -> Component.literal("Objetivo: " + mission.objectiveSummary()), false);
        return 1;
    }

    private static int reset(CommandSourceStack source, int id) {
        MissionDefinition mission = MissionManager.get(id);
        if (mission == null) {
            return 0;
        }

        MissionDefinition fresh = new MissionDefinition(id);
        mission.title = fresh.title;
        mission.description = fresh.description;
        mission.objectiveType = fresh.objectiveType;
        mission.target = fresh.target;
        mission.amount = fresh.amount;
        mission.unlocked = false;
        saveAndSync();

        source.sendSuccess(() -> Component.literal("[CastroSafio] Mision " + id + " reiniciada y bloqueada."), true);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        MissionManager.load();
        MissionManager.syncAll();
        source.sendSuccess(() -> Component.literal("[CastroSafio] Configuracion de misiones recargada."), true);
        return 1;
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("[CastroSafio] Comandos de administrador (solo OP):"), false);
        source.sendSuccess(() -> Component.literal("/castrosafio mision desbloquear <1-65>"), false);
        source.sendSuccess(() -> Component.literal("/castrosafio mision bloquear <1-65>"), false);
        source.sendSuccess(() -> Component.literal("/castrosafio mision titulo <id> <texto>"), false);
        source.sendSuccess(() -> Component.literal("/castrosafio mision descripcion <id> <texto>"), false);
        source.sendSuccess(() -> Component.literal("/castrosafio mision objetivo <id> <tipo> <objetivo> <cantidad>"), false);
        source.sendSuccess(() -> Component.literal("/castrosafio mision info <id>"), false);
        source.sendSuccess(() -> Component.literal("/castrosafio mision limpiar <id>"), false);
        source.sendSuccess(() -> Component.literal("/castrosafio recargar"), false);
        return 1;
    }

    private static void saveAndSync() {
        MissionManager.save();
        MissionManager.syncAll();
    }
}
