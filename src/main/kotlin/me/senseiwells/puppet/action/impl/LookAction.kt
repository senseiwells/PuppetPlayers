package me.senseiwells.puppet.action.impl

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.puppet.action.PlayerAction
import me.senseiwells.puppet.action.PlayerActionProvider
import net.casual.arcade.commands.argument
import net.casual.arcade.utils.serialization.codec.ArcadeExtraCodecs
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.coordinates.RotationArgument
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec2

class LookAction(private val rotation: Vec2): PlayerAction {
    override fun run(player: ServerPlayer): PlayerAction.Result {
        player.connection.handleMovePlayer(
            ServerboundMovePlayerPacket.Rot(this.rotation.y, this.rotation.x, player.onGround(), player.horizontalCollision)
        )
        return PlayerAction.Result.Complete
    }

    override fun provider(): PlayerActionProvider {
        return LookAction
    }

    companion object: PlayerActionProvider {
        override val id: Identifier = Identifier.withDefaultNamespace("look")

        override val codec: MapCodec<out LookAction> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                ArcadeExtraCodecs.VEC2.fieldOf("rotation").forGetter(LookAction::rotation)
            ).apply(instance, ::LookAction)
        }

        override fun addCommandArguments(
            builder: LiteralArgumentBuilder<CommandSourceStack>,
            command: Command<CommandSourceStack>
        ) {
            builder.argument("rotation", RotationArgument.rotation()) {
                executes(command)
            }
        }
        override fun createCommandAction(context: CommandContext<CommandSourceStack>): PlayerAction {
            return LookAction(RotationArgument.getRotation(context, "rotation").getRotation(context.source))
        }
    }
}