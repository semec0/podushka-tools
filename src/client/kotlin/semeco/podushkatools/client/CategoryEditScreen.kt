package semeco.podushkatools.client

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text

class CategoryEditScreen(
    private val categoryIndex: Int
) : Screen(Text.literal("Edit Category")) {

    private lateinit var nameField: TextFieldWidget

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

        val centerX = width / 2

        nameField = TextFieldWidget(
            textRenderer,
            centerX - 150,
            32,
            300,
            20,
            null,
            Text.literal("Category name")
        )
        nameField.setMaxLength(80)
        nameField.setText(category.name ?: "")
        addDrawableChild(nameField)

        val listTop = 78
        val rowHeight = 24

        buttons.forEachIndexed { buttonIndex, toolButton ->
            val rowY = listTop + buttonIndex * rowHeight
            val label = toolButton.label?.takeIf { it.isNotBlank() } ?: "(empty button)"

            addDrawableChild(
                ButtonWidget.builder(Text.literal(label)) {
                    saveCategoryName()
                    client?.setScreen(ButtonEditScreen(categoryIndex, buttonIndex))
                }
                    .dimensions(centerX - 170, rowY, 250, 20)
                    .build()
            )

            addDrawableChild(
                ButtonWidget.builder(Text.literal("Del")) {
                    deleteButton(buttonIndex)
                }
                    .dimensions(centerX + 88, rowY, 45, 20)
                    .build()
            )
        }

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Add Button")) {
                addButton()
            }
                .dimensions(centerX - 155, height - 58, 145, 20)
                .build()
        )

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Save & Back")) {
                saveCategoryName()
                client?.setScreen(ConfigEditorScreen())
            }
                .dimensions(centerX + 10, height - 58, 145, 20)
                .build()
        )

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Back")) {
                client?.setScreen(ConfigEditorScreen())
            }
                .dimensions(centerX - 50, height - 32, 100, 20)
                .build()
        )
    }

    private fun saveCategoryName() {
        val old = ConfigManager.config
        val categories = old.categories.orEmpty().toMutableList()

        if (categoryIndex !in categories.indices) {
            return
        }

        val oldCategory = categories[categoryIndex]

        categories[categoryIndex] = oldCategory.copy(
            name = nameField.getText()
        )

        ConfigManager.updateConfig(
            old.copy(
                title = null,
                buttonWidth = null,
                buttonHeight = null,
                categories = categories
            )
        )
    }

    private fun addButton() {
        saveCategoryName()

        val old = ConfigManager.config
        val categories = old.categories.orEmpty().toMutableList()

        if (categoryIndex !in categories.indices) {
            return
        }

        val category = categories[categoryIndex]
        val buttons = category.buttons.orEmpty().toMutableList()

        buttons.add(
            ToolButton(
                label = "",
                command = ""
            )
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

        client?.setScreen(ButtonEditScreen(categoryIndex, buttons.lastIndex))
    }

    private fun deleteButton(buttonIndex: Int) {
        saveCategoryName()

        val old = ConfigManager.config
        val categories = old.categories.orEmpty().toMutableList()

        if (categoryIndex !in categories.indices) {
            return
        }

        val category = categories[categoryIndex]
        val buttons = category.buttons.orEmpty().toMutableList()

        if (buttonIndex in buttons.indices) {
            buttons.removeAt(buttonIndex)
        }

        categories[categoryIndex] = category.copy(buttons = buttons)

        ConfigManager.updateConfig(
            old.copy(
                title = null,
                buttonWidth = null,
                buttonHeight = null,
                categories = categories
            )
        )

        client?.setScreen(CategoryEditScreen(categoryIndex))
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0xAA000000.toInt())

        super.render(context, mouseX, mouseY, delta)

        val centerX = width / 2

        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("Edit Category"),
            centerX,
            10,
            0xFFFFD966.toInt()
        )

        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Category name"),
            centerX - 150,
            22,
            0xFFFFFFFF.toInt()
        )

        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("Buttons"),
            centerX,
            62,
            0xFFFFFFFF.toInt()
        )
    }

    override fun shouldPause(): Boolean {
        return false
    }
}