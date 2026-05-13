package me.senseiwells.puppet.action.impl

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.serialization.MapCodec
import me.senseiwells.puppet.PuppetPlayer
import me.senseiwells.puppet.action.PlayerAction
import me.senseiwells.puppet.action.PuppetPlayerAction
import me.senseiwells.puppet.action.PlayerActionProvider
import me.senseiwells.puppet.extensions.PlayerActionsExtension.Companion.actions
import net.minecraft.commands.CommandSourceStack
import net.minecraft.resources.Identifier

object InterruptLookAtAction: PuppetPlayerAction, PlayerActionProvider {
    override val id: Identifier = Identifier.withDefaultNamespace("interrupt_look_at")

    override val codec: MapCodec<out PlayerAction> = MapCodec.unit(this)

    override val immediate: Boolean get() = true

    override fun run(player: PuppetPlayer): PlayerAction.Result {
        player.actions.remove { it is LookAtAction }
        return PlayerAction.Result.Complete
    }

    override fun provider(): PlayerActionProvider {
        return this
    }

    override fun addCommandArguments(
        builder: LiteralArgumentBuilder<CommandSourceStack>,
        command: Command<CommandSourceStack>
    ) {
        builder.executes(command)
    }

    override fun createCommandAction(context: CommandContext<CommandSourceStack>): PlayerAction {
        return this
    }
}