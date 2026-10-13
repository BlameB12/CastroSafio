package com.castrosafio.menu;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
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
                                .then(Commands.literal("agregar")
                                        .then(Commands.argument("jugador", EntityArgument.player())
                                                .executes(ctx -> setOp(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), true))))
                                .then(Commands.literal("quitar")
                                        .then(Commands.argument("jugador", EntityArgument.player())
                                                .executes(ctx -> setOp(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), false)))))
                        .then(Commands.literal("agregarop")
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .executes(ctx -> setOp(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), true))))
                        .then(Commands.literal("quitarop")
                                .then(Commands.argument("jugador", EntityArgument.player())
                                        .executes(ctx -> setOp(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), false))))
        );
    }

    private static int setOp(CommandSourceStack source, ServerPlayer target, boolean add) {
        if (target == null) {
            source.sendFailure(Component.literal("[CastroSafio] No se encontro al jugador."));
            return 0;
        }

        String name = target.getGameProfile().getName();
        if (add) {
            source.getServer().getPlayerList().op(target.getGameProfile());
            source.sendSuccess(() -> Component.literal("[CastroSafio] " + name + " ahora es OP y puede usar los editores."), true);
            target.sendSystemMessage(Component.literal("[CastroSafio] Ahora tienes permisos de OP."));
        } else {
            source.getServer().getPlayerList().deop(target.getGameProfile());
            source.sendSuccess(() -> Component.literal("[CastroSafio] Se quito el OP a " + name + "."), true);
            target.sendSystemMessage(Component.literal("[CastroSafio] Se quitaron tus permisos de OP."));
        }
        return 1;
    }
}
