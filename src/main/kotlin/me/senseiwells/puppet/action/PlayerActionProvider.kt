package me.senseiwells.puppet.action

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import me.senseiwells.puppet.utils.PuppetPlayerRegistries
import net.casual.arcade.utils.serialization.codec.CodecProvider
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.Registry

/**
 * This interface provides methods for creating
 * [PlayerAction]s through commands and [codec]s.
 *
 * Implementations of this interface should be registered
 * to the [PuppetPlayerRegistries.ACTION_PROVIDERS].
 */
interface PlayerActionProvider: CodecProvider<PlayerAction> {
    /**
     * Whether the action can be run stand-alone.
     */
    val canRunAction: Boolean
        get() = true

    /**
     * Whether the action can be added to an action chain.
     */
    val canChainAction: Boolean
        get() = true

    /**
     * Adds arguments for that are required in [createCommandAction].
     *
     * @param builder The argument builder.
     * @param command The command to run.
     */
    fun addCommandArguments(builder: LiteralArgumentBuilder<CommandSourceStack>, command: Command<CommandSourceStack>)

    /**
     * Creates the action from the command context.
     *
     * Arguments added in [addCommandArguments] can be read here.
     *
     * @param context The command context.
     * @return The created action.
     */
    fun createCommandAction(context: CommandContext<CommandSourceStack>): PlayerAction

    companion object {
        /**
         * Registers an action provider to a registry.
         *
         * @param registry The registry to register to.
         */
        fun PlayerActionProvider.register(registry: Registry<PlayerActionProvider>) {
            Registry.register(registry, this.id, this)
        }
    }
}