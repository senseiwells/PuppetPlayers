package me.senseiwells.puppet.action.impl

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.puppet.action.ActionModifier
import me.senseiwells.puppet.action.PlayerAction
import me.senseiwells.puppet.action.PlayerActionProvider
import me.senseiwells.puppet.extensions.PlayerActionsExtension.Companion.actions
import net.casual.arcade.commands.argument
import net.casual.arcade.commands.arguments.EnumArgument
import net.minecraft.commands.CommandSourceStack
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer

class AttackAction(private val type: ActionModifier): PlayerAction {
    override fun run(player: ServerPlayer): PlayerAction.Result {
        when (this.type) {
            ActionModifier.Hold -> {
                player.actions.attacking = true
                player.actions.attackingHeld = true
            }
            ActionModifier.Once -> {
                player.actions.attacking = true
            }
            ActionModifier.Release -> {
                player.actions.attackingHeld = false
            }
        }
        return PlayerAction.Result.Complete
    }

    override fun provider(): PlayerActionProvider {
        return AttackAction
    }

    companion object: PlayerActionProvider {
        override val id: Identifier = Identifier.withDefaultNamespace("attack")

        override val codec: MapCodec<out AttackAction> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                ActionModifier.CODEC.fieldOf("modifier").forGetter(AttackAction::type)
            ).apply(instance, ::AttackAction)
        }

        override fun addCommandArguments(
            builder: LiteralArgumentBuilder<CommandSourceStack>,
            command: Command<CommandSourceStack>
        ) {
            builder.argument("modifier", EnumArgument.enumeration<ActionModifier> { it.serializedName }) {
                executes(command)
            }
        }

        override fun createCommandAction(context: CommandContext<CommandSourceStack>): PlayerAction {
            return AttackAction(EnumArgument.getEnumeration<ActionModifier>(context, "modifier"))
        }
    }
}