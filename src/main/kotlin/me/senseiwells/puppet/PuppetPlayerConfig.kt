package me.senseiwells.puppet

import kotlinx.serialization.Contextual
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import net.casual.arcade.utils.TimeUtils.Ticks
import net.casual.arcade.utils.serialization.kotlin.CodecSerializersModule
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.fabricmc.loader.api.FabricLoader
import org.apache.commons.lang3.SerializationException
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

@Serializable
@OptIn(ExperimentalSerializationApi::class)
class PuppetPlayerConfig(
    @SerialName("reload_puppet_players")
    val reloadPuppetPlayers: Boolean = true,
    @SerialName("respawn_puppet_players")
    val respawnPuppetPlayers: Boolean = true,
    @Contextual
    @SerialName("puppet_player_death_delay")
    val puppetPlayerDeathDelay: MinecraftTimeDuration = 0.Ticks,
    @SerialName("operator_required_for_puppets")
    val operatorRequiredForPuppets: Boolean = true,
    @SerialName("can_players_puppet_themselves")
    val canPlayersPuppetThemselves: Boolean = true,
    @SerialName("use_mine_tools_api")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val useMineToolsApi: Boolean = false
) {
    companion object {
        private val path: Path = FabricLoader.getInstance().configDir.resolve("puppet-player-config.json")
        private val json = Json {
            encodeDefaults = true
            prettyPrint = true
            prettyPrintIndent = "  "
            serializersModule = CodecSerializersModule {
                contextual(MinecraftTimeDuration.CODEC)
            }
        }

        fun read(): PuppetPlayerConfig {
            if (!this.path.exists()) {
                return PuppetPlayerConfig().also { this.write(it) }
            }
            return try {
                this.path.inputStream().use {
                    json.decodeFromStream(it)
                }
            } catch (e: Exception) {
                PuppetPlayers.logger.error("Failed to read puppet-player config, generating default", e)
                PuppetPlayerConfig().also { this.write(it) }
            }
        }

        fun write(config: PuppetPlayerConfig) {
            try {
                this.path.parent.createDirectories()
                this.path.outputStream().use {
                    json.encodeToStream(config, it)
                }
            } catch (e: IOException) {
                PuppetPlayers.logger.error("Failed to write puppet-player config", e)
            } catch (e: SerializationException) {
                PuppetPlayers.logger.error("Failed to serialize puppet-player config", e)
            }
        }
    }
}