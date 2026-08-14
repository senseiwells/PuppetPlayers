package me.senseiwells.puppet.extensions

import com.mojang.authlib.GameProfile
import net.casual.arcade.extensions.PlayerExtension
import net.casual.arcade.extensions.utils.getExtension
import net.casual.arcade.npc.FakePlayer
import net.minecraft.server.level.ServerPlayer

class PlayerPuppeteerExtension(player: ServerPlayer): PlayerExtension(player) {
    private var body: PlayerPuppeteerExtension? = null
    private var owner: PlayerPuppeteerExtension? = null

    private var disguise: GameProfile? = null

    fun disguisedProfile(): GameProfile {
        return this.disguise ?: this.player.gameProfile
    }

    fun isPuppeting(): Boolean {
        return this.originBody() != null
    }

    fun isAwayPuppeting(): Boolean {
        return this.ownerBody() != null
    }

    /**
     * The body that this player originated from.
     * i.e. the puppeteer's original body.
     */
    fun originBody(): FakePlayer? {
        return this.body?.player as? FakePlayer
    }

    /**
     * The player that *actually* owns this body.
     * i.e. the puppeteer that is away puppeting.
     */
    fun ownerBody(): ServerPlayer? {
        return this.owner?.player
    }

    fun counterpartBody(): ServerPlayer? {
        return (this.body ?: this.owner)?.player
    }

    companion object {
        @JvmStatic
        val ServerPlayer.puppeteerExtension: PlayerPuppeteerExtension
            get() = this.getExtension()

        internal fun link(possessor: ServerPlayer, body: FakePlayer) {
            possessor.puppeteerExtension.body = body.puppeteerExtension
            body.puppeteerExtension.owner = possessor.puppeteerExtension

            possessor.puppeteerExtension.disguise = body.gameProfile
            body.puppeteerExtension.disguise = possessor.gameProfile
        }

        internal fun unlink(possessor: ServerPlayer, body: FakePlayer) {
            possessor.puppeteerExtension.body = null
            body.puppeteerExtension.owner = null

            possessor.puppeteerExtension.disguise = null
            body.puppeteerExtension.disguise = null
        }
    }
}