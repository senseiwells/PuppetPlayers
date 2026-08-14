package me.senseiwells.puppet

import me.senseiwells.puppet.command.PuppetPlayerCommand
import me.senseiwells.puppet.extensions.PlayerActionsExtension
import me.senseiwells.puppet.mixins.CachedUserNameToIdResolverAccessor
import me.senseiwells.puppet.mixins.ServicesAccessor
import me.senseiwells.puppet.network.MineToolsGameProfileRepository
import me.senseiwells.puppet.puppeteer.PlayerPuppeting
import me.senseiwells.puppet.utils.PuppetPlayerRegistries
import net.casual.arcade.commands.register
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.server.ServerRegisterCommandEvent
import net.casual.arcade.events.server.ServerSaveEvent
import net.casual.arcade.events.server.ServerStartEvent
import net.casual.arcade.events.server.ServerStopEvent
import net.casual.arcade.events.utils.register
import net.casual.arcade.npc.FakePlayer
import net.casual.arcade.utils.player.kick
import net.casual.arcade.utils.server.players
import net.fabricmc.api.ModInitializer
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.Proxy
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.jvm.optionals.getOrNull

object PuppetPlayers: ModInitializer {
    const val MOD_ID = "puppet-players"

    val logger: Logger = LoggerFactory.getLogger(MOD_ID)

    val config = PuppetPlayerConfig.read()

    override fun onInitialize() {
        PuppetPlayerRegistries.load()
        PlayerActionsExtension.registerEvents()
        PlayerPuppeting.registerEvents()

        GlobalEventHandler.Server.register<ServerRegisterCommandEvent> { event ->
            event.register(PuppetPlayerCommand)
        }
        GlobalEventHandler.Server.register<ServerStartEvent> { (server) ->
            this.loadFakePlayers(server)
            if (this.config.useMineToolsApi) {
                val repository = MineToolsGameProfileRepository(Proxy.NO_PROXY)
                val services = server.services()
                @Suppress("CAST_NEVER_SUCCEEDS")
                (services as ServicesAccessor).setProfileRepository(repository)
                (services.nameToIdCache as CachedUserNameToIdResolverAccessor).setProfileRepository(repository)
            }
        }
        GlobalEventHandler.Server.register<ServerSaveEvent> { event ->
            if (event.isRoutine) {
                this.saveFakePlayers(event.server)
            }
        }
        GlobalEventHandler.Server.register<ServerStopEvent> { (server) ->
            this.saveFakePlayers(server)

            for (player in server.playerList.players.toList()) {
                PlayerPuppeting.stop(player)
            }
            // We dc fake players here because luckperms is silly
            for (player in server.playerList.players.toList()) {
                if (player is PuppetPlayer) {
                    player.kick()
                }
            }
        }
    }

    private fun loadFakePlayers(server: MinecraftServer) {
        val path = this.getFakePlayerDat(server)
        if (!this.config.reloadPuppetPlayers || !path.exists()) {
            return
        }

        try {
            val wrapper = NbtIo.read(path) ?: return
            val players = wrapper.read("players", UUIDUtil.STRING_CODEC.listOf()).getOrNull()
            if (players != null) {
                for (player in players) {
                    FakePlayer.join(server, player, ::PuppetPlayer)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to load puppet players", e)
        }
    }

    private fun saveFakePlayers(server: MinecraftServer) {
        val players = LinkedHashSet<UUID>()
        for (player in server.players) {
            if (player is PuppetPlayer) {
                players.add(player.uuid)
            }
        }

        val wrapper = CompoundTag()
        wrapper.store("players", UUIDUtil.STRING_CODEC.listOf(), players.toList())
        try {
            NbtIo.write(wrapper, this.getFakePlayerDat(server))
        } catch (e: Exception) {
            logger.error("Failed to save puppet players", e)
        }
    }

    private fun getFakePlayerDat(server: MinecraftServer): Path {
        return server.getWorldPath(LevelResource.ROOT).resolve("puppet-players.dat")
    }
}