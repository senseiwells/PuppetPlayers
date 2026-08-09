package me.senseiwells.puppet.action.impl

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.puppet.PuppetPlayer
import me.senseiwells.puppet.action.PlayerAction
import me.senseiwells.puppet.action.PuppetPlayerAction
import me.senseiwells.puppet.action.PlayerActionProvider
import net.casual.arcade.commands.argument
import net.casual.arcade.commands.hasArgument
import net.casual.arcade.commands.literal
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.core.UUIDUtil
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.Vec3
import java.util.*

sealed class MoveToAction(
    private val sprint: Boolean,
    private val jump: Boolean
): PuppetPlayerAction {
    private var target: Vec3? = null

    abstract fun getTarget(player: PuppetPlayer): Vec3?

    override fun run(player: PuppetPlayer): PlayerAction.Result {
        val target = this.getTarget(player) ?: return PlayerAction.Result.Complete
        val current = this.target

        if (current != null && current.closerThan(target, 2.0)) {
            if (player.navigation.isInProgress()) {
                if (this.sprint) {
                    player.input.sprint = true
                }
                if (this.jump) {
                    player.moveControl.jump()
                }
                return PlayerAction.Result.Incomplete
            }
            this.target = null
            return PlayerAction.Result.Complete
        }

        val canNavigate = player.navigation.moveTo(target.x, target.y, target.z, 1.0)
        if (canNavigate) {
            this.target = target
            return PlayerAction.Result.Incomplete
        }
        return PlayerAction.Result.Complete
    }

    override fun provider(): PlayerActionProvider {
        return MoveToAction
    }

    private class MoveToPositionAction(
        val target: Vec3,
        sprint: Boolean,
        jump: Boolean
    ): MoveToAction(sprint, jump) {
        override fun getTarget(player: PuppetPlayer): Vec3 {
            return this.target
        }
    }

    private class MoveToEntityAction(
        val uuid: UUID,
        sprint: Boolean,
        jump: Boolean
    ): MoveToAction(sprint, jump) {
        override fun getTarget(player: PuppetPlayer): Vec3? {
            val entity = player.level().getEntity(this.uuid)
            return entity?.position()
        }
    }

    companion object: PlayerActionProvider {
        private val POSITION_CODEC = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Vec3.CODEC.fieldOf("target").forGetter(MoveToPositionAction::target),
                Codec.BOOL.fieldOf("sprint").forGetter(MoveToAction::sprint),
                Codec.BOOL.fieldOf("jump").forGetter(MoveToAction::jump)
            ).apply(instance, ::MoveToPositionAction)
        }

        private val ENTITY_CODEC = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                UUIDUtil.CODEC.fieldOf("uuid").forGetter(MoveToEntityAction::uuid),
                Codec.BOOL.fieldOf("sprint").forGetter(MoveToAction::sprint),
                Codec.BOOL.fieldOf("jump").forGetter(MoveToAction::jump)
            ).apply(instance, ::MoveToEntityAction)
        }

        override val id: Identifier = Identifier.withDefaultNamespace("move_to")

        override val codec: MapCodec<out MoveToAction> = Codec.mapEither(POSITION_CODEC, ENTITY_CODEC).xmap(
            { either -> either.map({ it }, { it }) },
            { action -> if (action is MoveToPositionAction) Either.left(action) else Either.right(action as MoveToEntityAction) }
        )

        override fun addCommandArguments(
            builder: LiteralArgumentBuilder<CommandSourceStack>,
            command: Command<CommandSourceStack>
        ) {
            builder.literal("position") {
                argument("position", Vec3Argument.vec3()) {
                    executes(command)
                    argument("sprint", BoolArgumentType.bool()) {
                        executes(command)
                        argument("jump", BoolArgumentType.bool()) {
                            executes(command)
                        }
                    }
                }
            }
            builder.literal("entity") {
                argument("entity", EntityArgument.entity()) {
                    executes(command)
                    argument("sprint", BoolArgumentType.bool()) {
                        executes(command)
                        argument("jump", BoolArgumentType.bool()) {
                            executes(command)
                        }
                    }
                }
            }
        }

        override fun createCommandAction(context: CommandContext<CommandSourceStack>): PlayerAction {
            val sprint = context.hasArgument("sprint") && BoolArgumentType.getBool(context, "sprint")
            val jump = context.hasArgument("jump") && BoolArgumentType.getBool(context, "jump")
            if (context.hasArgument("position")) {
                val position = Vec3Argument.getVec3(context, "position")
                return MoveToPositionAction(position, sprint, jump)
            }
            val entity = EntityArgument.getEntity(context, "entity")
            return MoveToEntityAction(entity.uuid, sprint, jump)
        }
    }
}