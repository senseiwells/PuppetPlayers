package me.senseiwells.puppet.mixins;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import me.senseiwells.puppet.puppeteer.PuppeteerDisguiser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DamageSource.class)
public class DamageSourceMixin {
    @Definition(id = "victim", local = @Local(type = LivingEntity.class, name = "victim", argsOnly = true))
    @Definition(id = "getDisplayName", method = "Lnet/minecraft/world/entity/LivingEntity;getDisplayName()Lnet/minecraft/network/chat/Component;")
    @Expression("victim.getDisplayName()")
    @ModifyExpressionValue(
        method = "getLocalizedDeathMessage",
        at = @At("MIXINEXTRAS:EXPRESSION")
    )
    private Component replacePuppeteerName(
        Component original,
        @Local(name = "victim", argsOnly = true)
        LivingEntity victim
    ) {
        if (victim instanceof ServerPlayer player) {
            return PuppeteerDisguiser.INSTANCE.replacePuppeteerName(player, original);
        }
        return original;
    }
}
