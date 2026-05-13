package me.senseiwells.puppet.action

import me.senseiwells.puppet.PuppetPlayer
import net.minecraft.server.level.ServerPlayer

interface PuppetPlayerAction: PlayerAction {
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
    fun run(player: PuppetPlayer): PlayerAction.Result

    override fun run(player: ServerPlayer): PlayerAction.Result {
        if (player is PuppetPlayer) {
            return this.run(player)
        }
        throw IllegalStateException("Regular player cannot run puppet only action!")
    }
}