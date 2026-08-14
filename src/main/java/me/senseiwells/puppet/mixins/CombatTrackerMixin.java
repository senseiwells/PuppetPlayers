package me.senseiwells.puppet.mixins;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.senseiwells.puppet.puppeteer.PuppeteerDisguiser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CombatTracker.class)
public class CombatTrackerMixin {
    @Shadow @Final private LivingEntity mob;

    @Definition(id = "getDisplayName", method = "Lnet/minecraft/world/entity/LivingEntity;getDisplayName()Lnet/minecraft/network/chat/Component;")
    @Definition(id = "mob", field = "Lnet/minecraft/world/damagesource/CombatTracker;mob:Lnet/minecraft/world/entity/LivingEntity;")
    @Expression("this.mob.getDisplayName()")
    @ModifyExpressionValue(
        method = {"getMessageForAssistedFall", "getFallMessage", "getDeathMessage"},
        at = @At("MIXINEXTRAS:EXPRESSION")
    )
    private Component replacePuppeteerName(Component original) {
        if (this.mob instanceof ServerPlayer player) {
            return PuppeteerDisguiser.INSTANCE.replacePuppeteerName(player, original);
        }
        return original;
    }
}
