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
import net.casual.arcade.commands.getArgumentOrElse
import net.casual.arcade.commands.hasArgument
import net.casual.arcade.commands.literal
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityReference
import net.minecraft.world.phys.Vec3
import java.util.*

sealed class LookAtAction(private val lock: Boolean): PuppetPlayerAction {
    override fun run(player: PuppetPlayer): PlayerAction.Result {
        return if (this.lock) PlayerAction.Result.Incomplete else PlayerAction.Result.Complete
    }

    override fun provider(): PlayerActionProvider {
        return LookAtAction
    }

    private class LookAtPositionAction(lock: Boolean, val target: Vec3): LookAtAction(lock) {
        override fun run(player: PuppetPlayer): PlayerAction.Result {
            player.lookControl.setLookAt(this.target)
            return super.run(player)
        }
    }

    private class LookAtEntityAction(lock: Boolean, val target: EntityReference<Entity>): LookAtAction(lock) {
        override fun run(player: PuppetPlayer): PlayerAction.Result {
            val entity = this.target.getEntity(player.level(), Entity::class.java)
            if (entity != null) {
                player.lookControl.setLookAt(entity)
                return super.run(player)
            }
            return PlayerAction.Result.Complete
        }
    }

    companion object: PlayerActionProvider {
        private val POSITION_CODEC = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.BOOL.fieldOf("lock").forGetter(LookAtAction::lock),
                Vec3.CODEC.fieldOf("target").forGetter(LookAtPositionAction::target)
            ).apply(instance, ::LookAtPositionAction)
        }

        private val ENTITY_CODEC = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.BOOL.fieldOf("lock").forGetter(LookAtAction::lock),
                EntityReference.codec<Entity>().fieldOf("target").forGetter(LookAtEntityAction::target)
            ).apply(instance, ::LookAtEntityAction)
        }

        override val id: Identifier = Identifier.withDefaultNamespace("look_at")

        override val codec: MapCodec<out LookAtAction> = Codec.mapEither(POSITION_CODEC, ENTITY_CODEC).xmap(
            { either -> either.map({ it }, { it }) },
            { action -> if (action is LookAtPositionAction) Either.left(action) else Either.right(action as LookAtEntityAction) }
        )

        override fun addCommandArguments(
            builder: LiteralArgumentBuilder<CommandSourceStack>,
            command: Command<CommandSourceStack>
        ) {
            builder.literal("position") {
                argument("position", Vec3Argument.vec3()) {
                    executes(command)

                    argument("lock", BoolArgumentType.bool()) {
                        executes(command)
                    }
                }
            }
            builder.literal("entity") {
                argument("entity", EntityArgument.entity()) {
                    executes(command)
                    argument("lock", BoolArgumentType.bool()) {
                        executes(command)
                    }
                }
            }
        }

        override fun createCommandAction(context: CommandContext<CommandSourceStack>): PlayerAction {
            val lock = context.getArgumentOrElse("lock", BoolArgumentType::getBool) { false }
            if (context.hasArgument("position")) {
                val position = Vec3Argument.getVec3(context, "position")
                return LookAtPositionAction(lock, position)
            }
            val entity = EntityArgument.getEntity(context, "entity")
            return LookAtEntityAction(lock, EntityReference.of(entity)!!)
        }
    }
}