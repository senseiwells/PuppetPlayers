package me.senseiwells.puppet.extensions

import me.senseiwells.puppet.PuppetPlayer
import me.senseiwells.puppet.PuppetPlayers
import me.senseiwells.puppet.action.PlayerActions
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.ListenerRegistry.Companion.register
import net.casual.arcade.events.server.player.PlayerTickEvent
import net.casual.arcade.extensions.Extension
import net.casual.arcade.extensions.PlayerExtension
import net.casual.arcade.extensions.SerializableExtension
import net.casual.arcade.extensions.event.PlayerExtensionEvent
import net.casual.arcade.extensions.utils.getExtension
import net.casual.arcade.utils.arcade
import net.casual.arcade.utils.entity.EntityTransferReason
import net.casual.arcade.utils.impl.DelayedActions
import net.casual.arcade.utils.player.server
import net.minecraft.resources.Identifier
import net.minecraft.server.TickTask
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import org.jetbrains.annotations.ApiStatus.Internal
import kotlin.jvm.optionals.getOrNull

class PlayerActionsExtension(player: ServerPlayer): PlayerExtension(player), SerializableExtension {
    private val actions = PlayerActions(player)

    private var result: InteractionResult? = null

    private fun pushResult(result: InteractionResult) {
        if (this.result != null && this.player is PuppetPlayer) {
            PuppetPlayers.logger.warn("Pushed interaction result before last was popped!")
        }
        this.result = result
    }

    private fun popResult(default: InteractionResult): InteractionResult {
        val result = this.result ?: return default
        this.result = null
        return result
    }

    private fun tick() {
        this.player.server.schedule(TickTask(this.player.server.tickCount) {
            // All player actions should be handled in the packet phase
            this.actions.tick()
        })
    }

    override fun transfer(
        player: ServerPlayer,
        reason: EntityTransferReason,
        delayed: DelayedActions
    ): Extension {
        return PlayerActionsExtension(player)
    }

    override fun id(): Identifier {
        return arcade("player_actions")
    }

    override fun serialize(output: ValueOutput) {
        output.store("fake_actions", PlayerActions.Packed.CODEC, this.actions.pack())
    }

    override fun deserialize(input: ValueInput) {
        val packed = input.read("fake_actions", PlayerActions.Packed.CODEC).getOrNull()
        if (packed != null) {
            this.actions.unpack(packed)
        }
    }

    companion object {
        private val ServerPlayer.actionsExtension
            get() = this.getExtension<PlayerActionsExtension>()

        @JvmStatic
        val ServerPlayer.actions: PlayerActions
            get() = this.actionsExtension.actions

        internal fun registerEvents() {
            GlobalEventHandler.Server.register<PlayerExtensionEvent> {
                it.addExtension(::PlayerActionsExtension)
            }
            GlobalEventHandler.Server.register<PlayerTickEvent> { (player) ->
                player.actionsExtension.tick()
            }
        }

        @Internal
        @JvmStatic
        fun ServerPlayer.pushActionResult(result: InteractionResult) {
            this.actionsExtension.pushResult(result)
        }

        @Internal
        @JvmStatic
        fun ServerPlayer.popActionResult(default: InteractionResult): InteractionResult {
            return this.actionsExtension.popResult(default)
        }
    }
}