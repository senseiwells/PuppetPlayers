package me.senseiwells.puppet.puppeteer

import me.senseiwells.puppet.PuppetPlayer
import me.senseiwells.puppet.extensions.PlayerActionsExtension.Companion.actions
import me.senseiwells.puppet.extensions.PlayerPuppeteerExtension
import me.senseiwells.puppet.extensions.PlayerPuppeteerExtension.Companion.puppeteerExtension
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.phase.BuiltInEventPhases
import net.casual.arcade.events.server.player.PlayerDeathEvent
import net.casual.arcade.events.server.player.PlayerLeaveEvent
import net.casual.arcade.events.utils.register
import net.casual.arcade.extensions.event.PlayerExtensionEvent
import net.casual.arcade.npc.FakePlayer
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket
import net.minecraft.server.level.ServerPlayer

object PlayerPuppeting {
    fun start(puppeteer: ServerPlayer, puppet: PuppetPlayer): Boolean {
        if (!this.canPuppet(puppeteer, puppet)) {
            return false
        }

        puppeteer.closeContainer()
        puppeteer.stopRiding()
        puppet.actions.reset()
        puppet.closeContainer()
        puppet.stopRiding()

        val puppetData = PlayerDataSnapshot.of(puppet)
        val ownData = this.stop(puppeteer, keepBody = true, restore = false) ?: PlayerDataSnapshot.of(puppeteer)

        PlayerPuppeteerExtension.link(puppeteer, puppet)
        ownData.load(puppet)
        puppetData.load(puppeteer)
        PuppeteerDisguiser.refresh(puppeteer)
        PuppeteerDisguiser.refresh(puppet)
        return true
    }

    fun stop(puppeteer: ServerPlayer): Boolean {
        return this.stop(puppeteer, keepBody = true, restore = true) != null
    }

    private fun canPuppet(puppeteer: ServerPlayer, puppet: ServerPlayer): Boolean {
        if (puppeteer is FakePlayer || puppeteer.hasDisconnected()) {
            return false
        }
        if (puppet !is PuppetPlayer) {
            return false
        }
        return !puppet.puppeteerExtension.isAwayPuppeting()
    }

    private fun stop(
        puppeteer: ServerPlayer,
        keepBody: Boolean,
        restore: Boolean
    ): PlayerDataSnapshot? {
        val body = puppeteer.puppeteerExtension.originBody() ?: return null

        puppeteer.closeContainer()
        puppeteer.stopRiding()

        val puppetData = PlayerDataSnapshot.of(puppeteer)
        val ownData = PlayerDataSnapshot.of(body)

        PlayerPuppeteerExtension.unlink(puppeteer, body)

        if (restore) {
            this.restore(puppeteer, ownData)
        }
        puppetData.load(body)

        if (keepBody) {
            body.actions.reset()
            PuppeteerDisguiser.refresh(body)
        }
        return ownData
    }

    private fun restore(puppeteer: ServerPlayer, data: PlayerDataSnapshot) {
        data.load(puppeteer)
        PuppeteerDisguiser.refresh(puppeteer)
    }

    internal fun registerEvents() {
        PuppeteerDisguiser.registerEvents()

        GlobalEventHandler.Server.register<PlayerExtensionEvent> { event ->
            event.addExtension(::PlayerPuppeteerExtension)
        }
        GlobalEventHandler.Server.register<PlayerLeaveEvent>(phase = PlayerLeaveEvent.PHASE_PRE) { event ->
            val player = event.player
            this.stop(player)

            val owner = player.puppeteerExtension.ownerBody()
            if (owner != null) {
                this.stop(owner, keepBody = false, restore = true)
            }
        }
        GlobalEventHandler.Server.register<PlayerDeathEvent>(phase = BuiltInEventPhases.POST) { (player) ->
            if (player.puppeteerExtension.isPuppeting()) {
                this.onPuppeteerDeath(player)
            }
        }
    }

    private fun onPuppeteerDeath(puppeteer: ServerPlayer) {
        val connection = puppeteer.connection
        val data = this.stop(connection.player, keepBody = true, restore = false) ?: return

        connection.handleClientCommand(
            ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN)
        )
        this.restore(connection.player, data)
    }
}
