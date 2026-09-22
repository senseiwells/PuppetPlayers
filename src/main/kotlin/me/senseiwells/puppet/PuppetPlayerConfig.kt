package me.senseiwells.puppet

import kotlinx.serialization.Contextual
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.senseiwells.config.JsonConfigFile
import me.senseiwells.puppet.utils.EnabledActions
import net.casual.arcade.utils.TimeUtils.Ticks
import net.casual.arcade.utils.serialization.kotlin.CodecSerializersModule
import net.casual.arcade.utils.time.MinecraftTimeDuration
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Path

@Serializable
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
    @SerialName("can_spawn_puppets_anywhere")
    val canSpawnPuppetsAnywhere: Boolean = true,
    @SerialName("can_spawn_whitelisted_players_as_puppets")
    val canSpawnWhitelistedPlayers: Boolean = true,
    @SerialName("enable_puppeteering")
    val enablePuppeteering: Boolean = false,
    @Contextual
    @SerialName("enabled_actions")
    val enabledActions: EnabledActions = EnabledActions.All,

    @SerialName("use_mine_tools_api")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val useMineToolsApi: Boolean = false
) {

    companion object {
        private val path: Path = FabricLoader.getInstance().configDir.resolve("puppet-player-config.json")

        private val file = JsonConfigFile.create(this.path, ::PuppetPlayerConfig, logger = PuppetPlayers.logger) {
            serializersModule = CodecSerializersModule {
                contextual(MinecraftTimeDuration.CODEC)
                contextual(EnabledActions.CODEC)
            }
        }

        fun read(): PuppetPlayerConfig {
            return this.file.read()
        }

        fun write(config: PuppetPlayerConfig) {
            this.file.write(config)
        }
    }
}