package semeco.podushkatools.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW

object PodushkaToolsClient : ClientModInitializer {
    const val MOD_ID = "podushkatools"

    private lateinit var openMenuKey: KeyBinding

    private val KEY_CATEGORY: KeyBinding.Category =
        KeyBinding.Category.create(Identifier.of(MOD_ID, "main"))

    override fun onInitializeClient() {
        ConfigManager.load()

        openMenuKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.podushkatools.open_menu",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KEY_CATEGORY
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (openMenuKey.wasPressed()) {
                client.setScreen(CommandMenuScreen())
            }
        }
    }

    fun sendCommand(commandRaw: String) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return

        val command = commandRaw.trim().removePrefix("/")

        if (command.isBlank()) {
            player.sendMessage(Text.literal("Пустая команда в конфиге"), false)
            return
        }

        player.networkHandler.sendChatCommand(command)
    }
}