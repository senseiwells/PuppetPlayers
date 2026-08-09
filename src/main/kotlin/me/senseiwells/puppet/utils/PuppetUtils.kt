package me.senseiwells.puppet.utils

import me.senseiwells.puppet.PuppetPlayers
import net.casual.arcade.utils.Identifier
import net.minecraft.resources.Identifier

fun puppet(path: String): Identifier {
    return Identifier(PuppetPlayers.MOD_ID, path)
}