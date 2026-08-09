package me.senseiwells.puppet

import com.mojang.authlib.GameProfile
import net.casual.arcade.npc.FakePlayer
import net.casual.arcade.npc.utils.AttributeUtils.toBuilder
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ClientInformation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Input
import org.jetbrains.annotations.ApiStatus.Internal

class PuppetPlayer @Internal constructor(
    server: MinecraftServer,
    level: ServerLevel,
    profile: GameProfile,
    info: ClientInformation
): FakePlayer(server, level, profile, info) {
    override fun createRespawned(
        server: MinecraftServer,
        level: ServerLevel,
        profile: GameProfile,
        info: ClientInformation
    ): PuppetPlayer {
        return PuppetPlayer(server, level, profile, info)
    }

    override fun createAttributeSupplier(): AttributeSupplier {
        return super.createAttributeSupplier().toBuilder()
            .add(Attributes.FOLLOW_RANGE)
            .build()
    }

    override fun tryRespawnAfterDeath() {
        if (this.deathTime > PuppetPlayers.config.puppetPlayerDeathDelay.ticks) {
            super.tryRespawnAfterDeath()
            if (!PuppetPlayers.config.respawnPuppetPlayers) {
                this.connection.disconnect(Component.literal("Killed"))
            }
        }
    }
}