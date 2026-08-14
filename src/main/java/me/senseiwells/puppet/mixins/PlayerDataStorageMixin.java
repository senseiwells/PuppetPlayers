package me.senseiwells.puppet.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.senseiwells.puppet.extensions.PlayerPuppeteerExtension;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Objects;

@Mixin(PlayerDataStorage.class)
public class PlayerDataStorageMixin {
    @WrapOperation(
        method = "save",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;saveWithoutId(Lnet/minecraft/world/level/storage/ValueOutput;)V"
        )
    )
    private void savePossessedData(Player instance, ValueOutput output, Operation<Void> original) {
        if (instance instanceof ServerPlayer player) {
            PlayerPuppeteerExtension extension = PlayerPuppeteerExtension.getPuppeteerExtension(player);
            original.call(Objects.requireNonNullElse(extension.counterpartBody(), player), output);
        } else {
            original.call(instance, output);
        }
    }
}
