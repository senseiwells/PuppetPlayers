package me.senseiwells.puppet.utils

import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import net.minecraft.resources.Identifier

sealed interface EnabledActions {
    fun check(id: Identifier): Boolean

    data object All: EnabledActions {
        override fun check(id: Identifier): Boolean {
            return true
        }
    }

    class Specified(val ids: List<Identifier>): EnabledActions {
        override fun check(id: Identifier): Boolean {
            return this.ids.contains(id)
        }
    }

    companion object {
        private val ALL_CODEC: Codec<All> = Codec.STRING.comapFlatMap(
            { string -> if (string == "*") DataResult.success(All) else DataResult.error { "Expected '*'" } },
            { actions -> "*" }
        )
        private val SPECIFIED_CODEC: Codec<Specified> = Identifier.CODEC.listOf().xmap(::Specified, Specified::ids)

        val CODEC: Codec<EnabledActions> = Codec.either(ALL_CODEC, SPECIFIED_CODEC).xmap(
            { either -> either.map({ it }, { it }) },
            { actions -> if (actions is Specified) Either.right(actions) else Either.left(All) }
        )
    }
}