package com.castrosafio.menu;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class CastroOpCommands {
    private CastroOpCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("castrosafio")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("op")
                                .requires(source -> source.hasPermission(3))
                                .then(Commands.literal("agregar")
                                        .then(Commands.argument("jugador", StringArgumentType.word())
                                                .executes(ctx -> setOp(ctx.getSource(), StringArgumentType.getString(ctx, "jugador"), true))))
                                .then(Commands.literal("quitar")
                                        .then(Commands.argument("jugador", StringArgumentType.word())
                                                .executes(ctx -> setOp(ctx.getSource(), StringArgumentType.getString(ctx, "jugador"), false)))))
        );
    }

    private static int setOp(CommandSourceStack source, String playerName, boolean add) {
        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (target == null) {
            source.sendFailure(Component.literal("[CastroSafio] El jugador debe estar conectado: " + playerName));
            return 0;
        }

        if (add) {
            source.getServer().getPlayerList().op(target.getGameProfile());
            source.sendSuccess(() -> Component.literal("[CastroSafio] " + target.getGameProfile().getName() + " ahora es OP."), true);
        } else {
            source.getServer().getPlayerList().deop(target.getGameProfile());
            source.sendSuccess(() -> Component.literal("[CastroSafio] Se quito el OP a " + target.getGameProfile().getName() + "."), true);
        }
        return 1;
    }
}
