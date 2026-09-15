package me.senseiwells.puppet.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import me.senseiwells.puppet.PuppetPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @WrapWithCondition(
        method = "lambda$halt$0",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;remove(Lnet/minecraft/server/level/ServerPlayer;)V"
        )
    )
    private boolean dontRemovePuppetsYet(PlayerList instance, ServerPlayer player) {
        return !(player instanceof PuppetPlayer);
    }
}
