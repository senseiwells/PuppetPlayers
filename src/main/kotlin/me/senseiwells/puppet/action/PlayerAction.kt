package me.senseiwells.puppet.action

import com.mojang.serialization.Codec
import me.senseiwells.puppet.action.PlayerActionProvider.Companion.register
import me.senseiwells.puppet.action.impl.*
import me.senseiwells.puppet.utils.PuppetPlayerRegistries
import net.minecraft.core.Registry
import net.minecraft.server.level.ServerPlayer

/**
 * This interface represents an action that can be
 * run by a fake player.
 */
interface PlayerAction {
    /**
     * Whether the action will be run immediately or
     * whether to schedule it in the action tick phase.
     */
    val immediate: Boolean get() = false

    /**
     * This runs the action, this method will be called
     * every tick until the action has finished running.
     *
     * This method should only return `true` after it
     * has finished running.
     * If this method returns `false` then the run method
     * must be called again the next tick.
     *
     * @param player The player doing the action.
     * @return Whether the action is finished.
     */
    fun run(player: ServerPlayer): Result

    /**
     * The provider for the given action.
     *
     * @return The action provider.
     */
    fun provider(): PlayerActionProvider

    enum class Result {
        Incomplete,
        Complete
    }

    companion object {
        val CODEC: Codec<PlayerAction> = Codec.lazyInitialized {
            PuppetPlayerRegistries.ACTION_PROVIDERS.byNameCodec()
                .dispatch(PlayerAction::provider, PlayerActionProvider::codec)
        }

        internal fun bootstrap(registry: Registry<PlayerActionProvider>) {
            AttackAction.register(registry)
            DelayAction.register(registry)
            DropAction.register(registry)
            InterruptLookAtAction.register(registry)
            InterruptMoveToAction.register(registry)
            JumpAction.register(registry)
            LookAction.register(registry)
            LookAtAction.register(registry)
            MoveToAction.register(registry)
            OffhandAction.register(registry)
            SneakAction.register(registry)
            SprintAction.register(registry)
            SwapSlotAction.register(registry)
            UseAction.register(registry)
        }
    }
}

