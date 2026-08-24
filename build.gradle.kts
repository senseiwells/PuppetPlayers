plugins {
    val jvmVersion = libs.versions.fabric.kotlin.get()
        .split("+kotlin.")[1]
        .split("+")[0]

    kotlin("jvm").version(jvmVersion)
    kotlin("plugin.serialization").version(jvmVersion)
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.mod.publish)
    `maven-publish`
    java
}

repositories {
    maven("https://maven.parchmentmc.org/")
    maven("https://maven.supersanta.me/snapshots")
    maven("https://api.modrinth.com/maven")
    mavenCentral()
    mavenLocal()
}

val modVersion = "2.0.1"
val releaseVersion = "${modVersion}+${libs.versions.minecraft.get()}"
version = releaseVersion
group = "me.senseiwells"

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.kotlin)

    api(libs.bundles.arcade)
    include(libs.bundles.arcade)
}

loom {
    runs {
        getByName("server") {
            runDirectory.set(file("run/server"))
        }
    }
}

java {
    withSourcesJar()
}

tasks {
    processResources {
        inputs.property("version", modVersion)
        filesMatching("fabric.mod.json") {
            expand(mutableMapOf(
                "version" to modVersion,
                "fabric_loader_dependency" to libs.versions.fabric.loader.get(),
                "fabric_kotlin_dependency" to libs.versions.fabric.kotlin.get(),
                "minecraft_dependency" to "~${libs.versions.minecraft.get()}",
            ))
        }
    }


    publishMods {
        file = jar.get().archiveFile
        changelog.set(
            """
            - Added **puppeteering**
              - By running `/puppet <puppet_username>` on a puppet
                you can now puppeteer that puppet. You will resume their
                position and play as if you were the puppet, your original
                body will become a puppet and remain in the world until you
                log off, die, or stop puppeting (by running `/puppet <your_username`).
              - This feature is disabled by default, you will need to set
                `"enable_puppeteering"` to `true` in the config.
              - See the mod page for more details on how this works!
            - Reworked player `move_to` action pathfinding
              - Puppets can now parkour over gaps
              - Puppets can now climb ladders/vines/scaffolding
              - Pathfinding in general should be less janky
            - Added some more configs:
              - `"can_spawn_puppets_anywhere"` - Whether the `/puppet <username> spawn` command is enabled
              - `"can_spawn_whitelisted_players_as_puppets"` - Whether you can spawn whitelisted players as puppets
              - `"enable_puppeteering"` - Whether players can puppeteer puppets
              - `"enabled_actions"` - The list of enabled puppet actions, `"*"` for all actions, or list them
                out for granular control, e.g. `["use", "attack", "drop"]`
              - These are primarily aimed at allowing for a more survival friendly experience while
                keeping all the features available for creative/testing use
                
            Please report any issues to the [bug tracker](https://github.com/senseiwells/PuppetPlayers/issues)
            """.trimIndent()
        )
        type = STABLE
        modLoaders.add("fabric")

        displayName = "PuppetPlayers $modVersion for ${libs.versions.minecraft.get()}"
        version = releaseVersion

        modrinth {
            accessToken = providers.environmentVariable("MODRINTH_API_KEY")
            projectId = "8fH4Iml8"
            minecraftVersions.add(libs.versions.minecraft)

            projectDescription.set(createProjectDescription())

            requires {
                id = "P7dR8mSH"
            }
            requires {
                id = "Ha28R6CL"
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("MavenJava") {
            groupId = "me.senseiwells"
            artifactId = "puppet-players"
            version = "${modVersion}+${libs.versions.minecraft.get()}"
            from(components["java"])

            updateReadme("./README.md")
        }
    }

    repositories {
        val mavenUrl = System.getenv("MAVEN_URL")
        if (mavenUrl != null) {
            maven {
                url = uri(mavenUrl)
                val mavenUsername = System.getenv("MAVEN_USERNAME")
                val mavenPassword = System.getenv("MAVEN_PASSWORD")
                if (mavenUsername != null && mavenPassword != null) {
                    credentials {
                        username = mavenUsername
                        password = mavenPassword
                    }
                }
            }
        }
    }
}

private fun MavenPublication.updateReadme(vararg readmes: String) {
    val location = "${groupId}:${artifactId}"
    val regex = Regex("""${Regex.escape(location)}:[\d\.\-a-zA-Z+]+""")
    val locationWithVersion = "${location}:${version}"
    for (path in readmes) {
        val readme = file(path)
        readme.writeText(readme.readText().replace(regex, locationWithVersion))
    }
}

fun createProjectDescription(): String {
    var description = StringBuilder(file("README.md").readText())

    fun replaceNotes() {
        val regex = Regex("""\[!([A-Z]+)\]""")
        for (result in regex.findAll(description)) {
            val range = result.groups[0]!!.range
            val type = result.groups[1]!!.value
            val formatted = type.lowercase().replaceFirstChar { c -> c.uppercase() }
            description.replace(range.first, range.last + 1, "$formatted:")
        }
    }

    fun replaceModrinthLink() {
        val regex = Regex("""## Getting Started[\s\S]*## Usage""")
        description = StringBuilder(description.replace(regex, "## Usage"))
    }

    fun removeDevelopers() {
        val index = description.indexOf("### Developers")
        description = StringBuilder(description.substring(0, index))
    }

    replaceNotes()
    replaceModrinthLink()
    removeDevelopers()
    return description.toString()
}