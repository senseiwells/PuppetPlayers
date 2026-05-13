package me.senseiwells.puppet.action.impl

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.serialization.MapCodec
import me.senseiwells.puppet.action.PlayerAction
import me.senseiwells.puppet.action.PlayerActionProvider
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer

object OffhandAction: PlayerAction, PlayerActionProvider {
    override val id: Identifier = Identifier.withDefaultNamespace("offhand")

    override val codec: MapCodec<out OffhandAction> = MapCodec.unit(this)

    override fun run(player: ServerPlayer): PlayerAction.Result {
        player.connection.handlePlayerAction(
            ServerboundPlayerActionPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN)
        )
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