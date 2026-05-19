package semeco.podushkatools.client

import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object ConfigManager {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val configDir: Path =
        FabricLoader.getInstance().configDir.resolve(PodushkaToolsClient.MOD_ID)

    private val configFile: Path =
        configDir.resolve("buttons.json")

    var config: ToolConfig = createDefaultConfig()
        private set

    fun load() {
        try {
            if (!configDir.exists()) {
                Files.createDirectories(configDir)
            }

            if (!configFile.exists()) {
                config = createDefaultConfig()
                save()
                return
            }

            val json = configFile.readText()

            if (json.isBlank()) {
                config = createDefaultConfig()
                save()
                return
            }

            val parsed = gson.fromJson(json, ToolConfig::class.java)
            config = sanitizeConfig(parsed)
        } catch (e: Exception) {
            e.printStackTrace()
            config = createDefaultConfig()
            save()
        }
    }

    fun save() {
        try {
            if (!configDir.exists()) {
                Files.createDirectories(configDir)
            }

            configFile.writeText(gson.toJson(config))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateConfig(newConfig: ToolConfig) {
        config = sanitizeConfig(newConfig)
        save()
    }

    fun getConfigPath(): Path {
        return configFile
    }

    private fun sanitizeConfig(raw: ToolConfig?): ToolConfig {
        if (raw == null) {
            return createDefaultConfig()
        }

        val safeCategories = raw.categories
            .orEmpty()
            .map { category ->
                val safeButtons = category.buttons
                    .orEmpty()
                    .map { button ->
                        ToolButton(
                            label = button.label ?: "",
                            command = button.command ?: ""
                        )
                    }

                ToolCategory(
                    name = category.name ?: "",
                    buttons = safeButtons
                )
            }

        return ToolConfig(
            title = null,
            columns = (raw.columns ?: 2).coerceIn(1, 6),

            // Width/Height больше не сохраняем.
            buttonWidth = null,
            buttonHeight = null,

            categories = safeCategories
        )
    }

    private fun createDefaultConfig(): ToolConfig {
        return ToolConfig(
            columns = 2,
            categories = listOf(
                ToolCategory(
                    name = "Main",
                    buttons = listOf(
                        ToolButton("Say Hello", "say Hello from Podushka Tools"),
                        ToolButton("Help", "help"),
                        ToolButton("Seed", "seed"),
                        ToolButton("List Players", "list")
                    )
                ),
                ToolCategory(
                    name = "Player",
                    buttons = listOf(
                        ToolButton("TP Up", "tp @s ~ ~1 ~"),
                        ToolButton("Speed", "effect give @s minecraft:speed 10 1"),
                        ToolButton("Clear Effects", "effect clear @s"),
                        ToolButton("Spawnpoint", "spawnpoint @s")
                    )
                )
            )
        )
    }
}