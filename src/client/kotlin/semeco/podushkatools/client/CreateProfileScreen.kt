package semeco.podushkatools.client

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text

class CreateProfileScreen : Screen(Text.literal("Create Profile")) {

    private lateinit var profileNameField: TextFieldWidget
    private lateinit var createButton: ButtonWidget

    override fun init() {
        clearChildren()

        val centerX = width / 2

        profileNameField = TextFieldWidget(
            textRenderer,
            centerX - 150,
            52,
            300,
            20,
            null,
            Text.literal("Profile name")
        )
        profileNameField.setMaxLength(40)
        profileNameField.setText("")
        profileNameField.setChangedListener {
            updateCreateButtonState()
        }
        addDrawableChild(profileNameField)

        createButton = ButtonWidget.builder(Text.literal("Create")) {
            val rawName = profileNameField.getText()

            if (rawName.isNotBlank()) {
                ConfigManager.createProfile(rawName)
                client?.setScreen(ProfilesScreen())
            }
        }
            .dimensions(centerX - 155, height - 32, 145, 20)
            .build()

        createButton.active = false
        addDrawableChild(createButton)

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Back")) {
                client?.setScreen(ProfilesScreen())
            }
                .dimensions(centerX + 10, height - 32, 145, 20)
                .build()
        )

        updateCreateButtonState()
    }

    private fun updateCreateButtonState() {
        if (::createButton.isInitialized) {
            createButton.active = profileNameField.getText().isNotBlank()
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0xAA000000.toInt())

        super.render(context, mouseX, mouseY, delta)

        val centerX = width / 2

        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("Create Profile"),
            centerX,
            18,
            0xFFFFD966.toInt()
        )

        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Profile name"),
            centerX - 150,
            40,
            0xFFFFFFFF.toInt()
        )

        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("The new profile will be created empty and will not be selected automatically."),
            centerX,
            82,
            0xFFAAAAAA.toInt()
        )
    }

    override fun shouldPause(): Boolean {
        return false
    }
}