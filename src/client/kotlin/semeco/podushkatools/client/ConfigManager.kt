package semeco.podushkatools.client

import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object ConfigManager {
    private const val DEFAULT_PROFILE = "default"

    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val configDir: Path =
        FabricLoader.getInstance().configDir.resolve(PodushkaToolsClient.MOD_ID)

    private val profilesDir: Path =
        configDir.resolve("profiles")

    private val settingsFile: Path =
        configDir.resolve("settings.json")

    // Старый конфиг до системы профилей.
    // При первом запуске будет скопирован в profiles/default.json.
    private val legacyConfigFile: Path =
        configDir.resolve("buttons.json")

    private var activeProfileName: String = DEFAULT_PROFILE

    var config: ToolConfig = createDefaultConfig()
        private set

    fun load() {
        try {
            ensureDirectories()
            migrateLegacyConfigIfNeeded()

            val settings = loadSettings()
            val safeActiveProfile = sanitizeProfileName(settings.activeProfile ?: DEFAULT_PROFILE)

            activeProfileName = safeActiveProfile.ifBlank { DEFAULT_PROFILE }

            if (!profileFile(activeProfileName).exists()) {
                activeProfileName = DEFAULT_PROFILE

                if (!profileFile(DEFAULT_PROFILE).exists()) {
                    writeConfigToProfile(DEFAULT_PROFILE, createDefaultConfig())
                }

                saveSettings()
            }

            val profilePath = profileFile(activeProfileName)
            val json = profilePath.readText()

            if (json.isBlank()) {
                config = createEmptyConfig()
                writeConfigToProfile(activeProfileName, config)
                return
            }

            val parsed = gson.fromJson(json, ToolConfig::class.java)
            config = sanitizeConfig(parsed)
        } catch (e: Exception) {
            e.printStackTrace()

            activeProfileName = DEFAULT_PROFILE
            config = createDefaultConfig()

            try {
                ensureDirectories()
                writeConfigToProfile(DEFAULT_PROFILE, config)
                saveSettings()
            } catch (ignored: Exception) {
                ignored.printStackTrace()
            }
        }
    }

    fun save() {
        try {
            ensureDirectories()
            writeConfigToProfile(activeProfileName, config)
            saveSettings()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateConfig(newConfig: ToolConfig) {
        config = sanitizeConfig(newConfig)
        save()
    }

    fun getActiveProfileName(): String {
        return activeProfileName
    }

    fun listProfiles(): List<String> {
        ensureDirectories()

        val profiles = Files.list(profilesDir).use { paths ->
            paths
                .filter { path ->
                    Files.isRegularFile(path) && path.fileName.toString().endsWith(".json")
                }
                .map { path ->
                    path.fileName.toString().removeSuffix(".json")
                }
                .sorted()
                .toList()
        }

        if (profiles.isEmpty()) {
            writeConfigToProfile(DEFAULT_PROFILE, createDefaultConfig())
            activeProfileName = DEFAULT_PROFILE
            saveSettings()
            return listOf(DEFAULT_PROFILE)
        }

        return profiles
    }

    fun selectProfile(profileNameRaw: String): Boolean {
        val profileName = sanitizeProfileName(profileNameRaw)

        if (profileName.isBlank()) {
            return false
        }

        if (!profileFile(profileName).exists()) {
            return false
        }

        activeProfileName = profileName
        saveSettings()
        load()

        return true
    }

    fun createProfile(profileNameRaw: String): String {
        ensureDirectories()

        val baseName = sanitizeProfileName(profileNameRaw).ifBlank { "new_profile" }
        val finalName = makeUniqueProfileName(baseName)

        writeConfigToProfile(finalName, createEmptyConfig())

        // Важно:
        // профиль создаётся, но НЕ выбирается автоматически.
        return finalName
    }

    fun deleteProfile(profileNameRaw: String): Boolean {
        ensureDirectories()

        val profileName = sanitizeProfileName(profileNameRaw)

        if (profileName.isBlank()) {
            return false
        }

        // default - системный базовый профиль. Его нельзя удалять.
        if (profileName == DEFAULT_PROFILE) {
            return false
        }

        val profilesBeforeDelete = listProfiles()

        if (profilesBeforeDelete.size <= 1) {
            return false
        }

        val path = profileFile(profileName)

        if (!path.exists()) {
            return false
        }

        Files.deleteIfExists(path)

        if (activeProfileName == profileName) {
            val remainingProfiles = listProfiles()
            activeProfileName = remainingProfiles.firstOrNull() ?: DEFAULT_PROFILE
            saveSettings()
            load()
        }

        return true
    }

    fun getActiveProfilePath(): Path {
        return profileFile(activeProfileName)
    }

    fun getProfilesDir(): Path {
        return profilesDir
    }

    private fun ensureDirectories() {
        if (!configDir.exists()) {
            Files.createDirectories(configDir)
        }

        if (!profilesDir.exists()) {
            Files.createDirectories(profilesDir)
        }
    }

    private fun migrateLegacyConfigIfNeeded() {
        val defaultProfileFile = profileFile(DEFAULT_PROFILE)

        if (!defaultProfileFile.exists() && legacyConfigFile.exists()) {
            Files.copy(
                legacyConfigFile,
                defaultProfileFile,
                StandardCopyOption.REPLACE_EXISTING
            )
        }

        if (!defaultProfileFile.exists()) {
            writeConfigToProfile(DEFAULT_PROFILE, createDefaultConfig())
        }
    }

    private fun loadSettings(): ProfileSettings {
        return try {
            if (!settingsFile.exists()) {
                val settings = ProfileSettings(DEFAULT_PROFILE)
                settingsFile.writeText(gson.toJson(settings))
                settings
            } else {
                gson.fromJson(settingsFile.readText(), ProfileSettings::class.java)
                    ?: ProfileSettings(DEFAULT_PROFILE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ProfileSettings(DEFAULT_PROFILE)
        }
    }

    private fun saveSettings() {
        val settings = ProfileSettings(activeProfileName)
        settingsFile.writeText(gson.toJson(settings))
    }

    private fun profileFile(profileName: String): Path {
        return profilesDir.resolve("${sanitizeProfileName(profileName)}.json")
    }

    private fun writeConfigToProfile(profileName: String, profileConfig: ToolConfig) {
        val safeConfig = sanitizeConfig(profileConfig)
        profileFile(profileName).writeText(gson.toJson(safeConfig))
    }

    private fun sanitizeProfileName(profileNameRaw: String): String {
        val cleaned = profileNameRaw
            .trim()
            // Разрешаем русские/английские/любые Unicode-буквы, цифры, пробел, _ и -
            .replace(Regex("[^\\p{L}\\p{N} _-]"), "_")
            // Схлопываем повторяющиеся пробелы
            .replace(Regex("\\s+"), " ")
            // Схлопываем повторяющиеся подчёркивания
            .replace(Regex("_+"), "_")
            // Убираем мусор по краям
            .trim(' ', '_', '-', '.')
            .take(40)
            .trim(' ', '_', '-', '.')

        if (cleaned.isBlank()) {
            return "new_profile"
        }

        val reservedWindowsNames = setOf(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
        )

        return if (reservedWindowsNames.contains(cleaned.uppercase())) {
            "${cleaned}_profile"
        } else {
            cleaned
        }
    }

    private fun makeUniqueProfileName(baseName: String): String {
        var candidate = baseName
        var index = 2

        while (profileFile(candidate).exists()) {
            candidate = "${baseName}_$index"
            index++
        }

        return candidate
    }

    private fun sanitizeConfig(raw: ToolConfig?): ToolConfig {
        if (raw == null) {
            return createEmptyConfig()
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
            buttonWidth = null,
            buttonHeight = null,
            categories = safeCategories
        )
    }

    private fun createEmptyConfig(): ToolConfig {
        return ToolConfig(
            columns = 2,
            categories = listOf()
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