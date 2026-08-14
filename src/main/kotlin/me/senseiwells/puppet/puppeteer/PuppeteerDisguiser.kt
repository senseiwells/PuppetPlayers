package me.senseiwells.puppet.puppeteer

import me.senseiwells.puppet.extensions.PlayerPuppeteerExtension.Companion.puppeteerExtension
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.server.player.PlayerClientboundPacketEvent
import net.casual.arcade.events.server.player.PlayerClientboundPacketEvent.Companion.replacePacketRecursively
import net.casual.arcade.events.utils.register
import net.casual.arcade.npc.FakePlayer
import net.casual.arcade.utils.ClientboundPlayerInfoUpdatePacket
import net.casual.arcade.utils.entity.getTrackedEntity
import net.casual.arcade.utils.player.server
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry
import net.minecraft.server.level.ServerPlayer

object PuppeteerDisguiser {
    internal fun registerEvents() {
        GlobalEventHandler.Server.register<PlayerClientboundPacketEvent> { event ->
            event.replacePacketRecursively(::disguisePacket)
        }
    }

    fun replacePuppeteerName(player: ServerPlayer, original: Component): Component {
        val counterpart = player.puppeteerExtension.counterpartBody() ?: return original
        return counterpart.displayName
    }

    fun refresh(player: ServerPlayer) {
        val players = player.server.playerList
        players.broadcastAll(ClientboundPlayerInfoRemovePacket(listOf(player.uuid)))
        players.broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(listOf(player)))

        val tracked = player.getTrackedEntity()
        if (tracked != null) {
            val entity = tracked.getServerEntity()
            for (observer in tracked.getObservers().toList()) {
                val packets = ArrayList<Packet<ClientGamePacketListener>>()
                packets.add(ClientboundRemoveEntitiesPacket(player.id))
                entity.sendPairingData(observer.player, packets::add)
                observer.send(ClientboundBundlePacket(packets))
            }
        }

        if (player !is FakePlayer) {
            this.refreshSelf(player)
        }
    }

    private fun refreshSelf(player: ServerPlayer) {
        val level = player.level()
        player.connection.send(
            ClientboundRespawnPacket(player.createCommonSpawnInfo(level), ClientboundRespawnPacket.KEEP_ALL_DATA)
        )
        player.connection.teleport(player.x, player.y, player.z, player.yRot, player.xRot)

        val players = player.server.playerList
        players.sendActivePlayerEffects(player)
        players.sendLevelInfo(player, level)
        players.sendPlayerPermissionLevel(player)

        player.onUpdateAbilities()
        player.initInventoryMenu()
        player.resetSentInfo()
    }

    private fun disguisePacket(receiver: ServerPlayer, packet: Packet<*>): Packet<*> {
        if (packet !is ClientboundPlayerInfoUpdatePacket || !packet.actions().contains(Action.ADD_PLAYER)) {
            return packet
        }

        val players = receiver.server.playerList
        val disguised = lazy { ArrayList<Entry>(packet.entries()) }
        for ((index, entry) in packet.entries().withIndex()) {
            val player = players.getPlayer(entry.profileId()) ?: continue
            val profile = player.puppeteerExtension.disguisedProfile()
            if (profile == entry.profile()) {
                continue
            }

            disguised.value[index] = Entry(
                entry.profileId(),
                profile,
                entry.listed(),
                entry.latency(),
                entry.gameMode(),
                entry.displayName(),
                entry.showHat(),
                entry.listOrder(),
                entry.chatSession()
            )
        }

        return if (disguised.isInitialized()) ClientboundPlayerInfoUpdatePacket(packet.actions(), disguised.value) else packet
    }
}
