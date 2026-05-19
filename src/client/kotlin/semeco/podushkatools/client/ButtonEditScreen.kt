package semeco.podushkatools.client

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text

class ButtonEditScreen(
    private val categoryIndex: Int,
    private val buttonIndex: Int
) : Screen(Text.literal("Edit Button")) {

    private lateinit var labelField: TextFieldWidget
    private lateinit var commandField: TextFieldWidget
    private lateinit var saveAndBackButton: ButtonWidget

    private var originalLabel: String = ""
    private var originalCommand: String = ""

    override fun init() {
        clearChildren()

        ConfigManager.load()

        val config = ConfigManager.config
        val categories = config.categories ?: listOf()

        if (categoryIndex !in categories.indices) {
            client?.setScreen(ConfigEditorScreen())
            return
        }

        val category = categories[categoryIndex]
        val buttons = category.buttons ?: listOf()

        if (buttonIndex !in buttons.indices) {
            client?.setScreen(CategoryEditScreen(categoryIndex))
            return
        }

        val button = buttons[buttonIndex]

        val centerX = width / 2

        originalLabel = button.label ?: ""
        originalCommand = button.command ?: ""

        labelField = TextFieldWidget(
            textRenderer,
            centerX - 160,
            45,
            320,
            20,
            null,
            Text.literal("Button label")
        )
        labelField.setMaxLength(100)
        labelField.setText(originalLabel)
        labelField.setChangedListener {
            updateSaveButtonState()
        }
        addDrawableChild(labelField)

        commandField = TextFieldWidget(
            textRenderer,
            centerX - 160,
            90,
            320,
            20,
            null,
            Text.literal("Command")
        )
        commandField.setMaxLength(300)
        commandField.setText(originalCommand)
        commandField.setChangedListener {
            updateSaveButtonState()
        }
        addDrawableChild(commandField)

        saveAndBackButton = ButtonWidget.builder(Text.literal("Save & Back")) {
            saveButton()
            client?.setScreen(CategoryEditScreen(categoryIndex))
        }
            .dimensions(centerX - 155, height - 32, 145, 20)
            .build()

        saveAndBackButton.active = false
        addDrawableChild(saveAndBackButton)

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Back")) {
                client?.setScreen(CategoryEditScreen(categoryIndex))
            }
                .dimensions(centerX + 10, height - 32, 145, 20)
                .build()
        )

        updateSaveButtonState()
    }

    private fun hasChanges(): Boolean {
        return labelField.getText() != originalLabel ||
            commandField.getText() != originalCommand
    }

    private fun updateSaveButtonState() {
        if (::saveAndBackButton.isInitialized) {
            saveAndBackButton.active = hasChanges()
        }
    }

    private fun saveButton() {
        val old = ConfigManager.config
        val categories = old.categories.orEmpty().toMutableList()

        if (categoryIndex !in categories.indices) {
            return
        }

        val category = categories[categoryIndex]
        val buttons = category.buttons.orEmpty().toMutableList()

        if (buttonIndex !in buttons.indices) {
            return
        }

        val newLabel = labelField.getText()
        val newCommand = commandField.getText()

        buttons[buttonIndex] = ToolButton(
            label = newLabel,
            command = newCommand
        )

        categories[categoryIndex] = category.copy(buttons = buttons)

        ConfigManager.updateConfig(
            old.copy(
                title = null,
                buttonWidth = null,
                buttonHeight = null,
                categories = categories
            )
        )

        originalLabel = newLabel
        originalCommand = newCommand
        updateSaveButtonState()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0xAA000000.toInt())

        super.render(context, mouseX, mouseY, delta)

        val centerX = width / 2

        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("Edit Button"),
            centerX,
            16,
            0xFFFFD966.toInt()
        )

        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Label"),
            centerX - 160,
            34,
            0xFFFFFFFF.toInt()
        )

        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Command"),
            centerX - 160,
            79,
            0xFFFFFFFF.toInt()
        )
    }

    override fun shouldPause(): Boolean {
        return false
    }
}