package me.senseiwells.puppet.puppeteer

import me.senseiwells.puppet.PuppetPlayers
import net.casual.arcade.utils.entity.teleportTo
import net.casual.arcade.utils.math.location.Location
import net.casual.arcade.utils.math.location.location
import net.casual.arcade.utils.player.server
import net.minecraft.core.Holder
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.ProblemReporter
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.TagValueOutput

class PlayerDataSnapshot private constructor(
    private val data: CompoundTag,
    private val dimension: ResourceKey<Level>,
    private val location: Location,
) {
    fun load(player: ServerPlayer) {
        val previous = player.activeEffects.map(MobEffectInstance::getEffect)

        ProblemReporter.ScopedCollector(player.problemPath(), PuppetPlayers.logger).use { reporter ->
            player.load(TagValueInput.create(reporter, player.registryAccess(), this.data))
        }

        val level = player.server.getLevel(this.dimension) ?: player.level()
        player.teleportTo(this.location.with(level))

        this.sync(player, previous)
    }

    private fun sync(player: ServerPlayer, removed: List<Holder<MobEffect>>) {
        for (effect in removed) {
            player.connection.send(ClientboundRemoveMobEffectPacket(player.id, effect))
        }

        val players = player.server.playerList
        players.sendActivePlayerEffects(player)
        players.broadcastAll(ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, player))
        player.onUpdateAbilities()
        player.initInventoryMenu()
        player.resetSentInfo()
    }

    companion object {
        fun of(player: ServerPlayer): PlayerDataSnapshot {
            ProblemReporter.ScopedCollector(player.problemPath(), PuppetPlayers.logger).use { reporter ->
                val output = TagValueOutput.createWithContext(reporter, player.registryAccess())
                player.saveWithoutId(output)
                return PlayerDataSnapshot(output.buildResult(), player.level().dimension(), player.location)
            }
        }
    }
}
