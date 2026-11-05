package dev.johnpablo.dungeonsartifacts.game;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.johnpablo.dungeonsartifacts.core.PlayerLedger;
import dev.johnpablo.dungeonsartifacts.soul.PlayerData;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

public final class SoulCommands {
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment) -> {
            var root=Commands.literal("Soul").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
            for(String action:new String[]{"add","max"})root.then(Commands.literal(action).then(Commands.argument("players",EntityArgument.players())
                .then(Commands.argument("amount",IntegerArgumentType.integer(0,1000000)).executes(c -> run(c,action,IntegerArgumentType.getInteger(c,"amount"))))));
            root.then(Commands.literal("clear").then(Commands.argument("players",EntityArgument.players()).executes(c -> run(c,"clear",0))));
            var node=dispatcher.register(root);
            dispatcher.register(Commands.literal("soul").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).redirect(node));
        });
    }
    private static int run(CommandContext<CommandSourceStack> c,String action,int amount) throws CommandSyntaxException {
        var players=EntityArgument.getPlayers(c,"players");
        for(var p:players) {
            if(action.equals("max"))p.setAttached(PlayerData.CAPACITY_OVERRIDE,amount);
            var state=ArtifactServer.ledger(p);
            if(action.equals("clear")) {
                var old=state.snapshot();p.setAttached(PlayerData.FRACTION,0);
                state=PlayerLedger.restore(state.capacity(),new PlayerLedger.Snapshot(0,old.cooldowns(),old.bootsStarted()));
            }else if(action.equals("add"))state.collect(amount);
            ArtifactServer.save(p,state);
        }
        c.getSource().sendSuccess(() -> Component.translatable("commands.dungeons_artifacts.soul",action,players.size(),amount),true);
        return players.size();
    }
}
