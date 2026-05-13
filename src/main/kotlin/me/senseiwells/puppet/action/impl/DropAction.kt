package me.senseiwells.puppet.action.impl

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.puppet.action.PlayerAction
import me.senseiwells.puppet.action.PlayerActionProvider
import net.casual.arcade.commands.argument
import net.casual.arcade.utils.player.updateSelectedSlot
import net.minecraft.commands.CommandSourceStack
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer

class DropAction(private val dropEntireStack: Boolean = false): PlayerAction {
    override fun run(player: ServerPlayer): PlayerAction.Result {
        if (!player.isSpectator) {
            player.drop(this.dropEntireStack)
            player.updateSelectedSlot()
        }
        return PlayerAction.Result.Complete
    }

    override fun provider(): PlayerActionProvider {
        return DropAction
    }

    companion object: PlayerActionProvider {
        override val id: Identifier = Identifier.withDefaultNamespace("drop")

        override val codec: MapCodec<out DropAction> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.BOOL.fieldOf("drop_entire_stack").forGetter(DropAction::dropEntireStack)
            ).apply(instance, ::DropAction)
        }

        override fun addCommandArguments(
            builder: LiteralArgumentBuilder<CommandSourceStack>,
            command: Command<CommandSourceStack>
        ) {
            builder.argument("stack", BoolArgumentType.bool()) {
                executes(command)
            }
        }

        override fun createCommandAction(context: CommandContext<CommandSourceStack>): PlayerAction {
            return DropAction(BoolArgumentType.getBool(context, "stack"))
        }
    }
}