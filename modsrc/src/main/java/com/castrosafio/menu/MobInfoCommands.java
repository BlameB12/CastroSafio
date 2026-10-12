package com.castrosafio.menu;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class MobInfoCommands {
    private MobInfoCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("castrosafio_mobinfo")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("texto", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            String text = StringArgumentType.getString(ctx, "texto").trim();
                                            if ("_".equals(text)) text = "";
                                            MobConfigManager.setInfo(id, text);
                                            MobConfigManager.syncAll();
                                            String finalText = text;
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    finalText.isBlank()
                                                            ? "[CastroSafio] Info de " + id + " eliminada."
                                                            : "[CastroSafio] Info de " + id + " guardada."
                                            ), true);
                                            return 1;
                                        })))
        );
    }
}
