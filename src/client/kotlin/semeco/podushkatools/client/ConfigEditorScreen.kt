package semeco.podushkatools.client

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text

class ConfigEditorScreen : Screen(Text.literal("Podushka Tools Config Editor")) {

    private lateinit var columnsField: TextFieldWidget

    override fun init() {
        clearChildren()

        ConfigManager.load()

        val config = ConfigManager.config
        val categories = config.categories ?: listOf()

        val centerX = width / 2

        columnsField = TextFieldWidget(
            textRenderer,
            centerX - 40,
            32,
            80,
            20,
            null,
            Text.literal("Columns")
        )
        columnsField.setMaxLength(2)
        columnsField.setText((config.columns ?: 2).toString())
        addDrawableChild(columnsField)

        val listTop = 78
        val rowHeight = 24

        categories.forEachIndexed { index, category ->
            val rowY = listTop + index * rowHeight
            val categoryName = category.name?.takeIf { it.isNotBlank() } ?: "(empty category)"

            addDrawableChild(
                ButtonWidget.builder(Text.literal(categoryName)) {
                    saveColumnsOnly()
                    client?.setScreen(CategoryEditScreen(index))
                }
                    .dimensions(centerX - 170, rowY, 250, 20)
                    .build()
            )

            addDrawableChild(
                ButtonWidget.builder(Text.literal("Del")) {
                    deleteCategory(index)
                }
                    .dimensions(centerX + 88, rowY, 45, 20)
                    .build()
            )
        }

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Add Category")) {
                addCategory()
            }
                .dimensions(centerX - 155, height - 58, 145, 20)
                .build()
        )

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Save & Back")) {
                saveColumnsOnly()
                client?.setScreen(CommandMenuScreen())
            }
                .dimensions(centerX + 10, height - 58, 145, 20)
                .build()
        )

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Back")) {
                client?.setScreen(CommandMenuScreen())
            }
                .dimensions(centerX - 50, height - 32, 100, 20)
                .build()
        )
    }

    private fun saveColumnsOnly() {
        val old = ConfigManager.config

        val columns = columnsField.getText()
            .toIntOrNull()
            ?.coerceIn(1, 6)
            ?: 2

        ConfigManager.updateConfig(
            old.copy(
                title = null,
                columns = columns,
                buttonWidth = null,
                buttonHeight = null
            )
        )
    }

    private fun addCategory() {
        saveColumnsOnly()

        val old = ConfigManager.config
        val categories = old.categories.orEmpty().toMutableList()

        categories.add(
            ToolCategory(
                name = "",
                buttons = listOf()
            )
        )

        ConfigManager.updateConfig(
            old.copy(
                title = null,
                buttonWidth = null,
                buttonHeight = null,
                categories = categories
            )
        )

        client?.setScreen(CategoryEditScreen(categories.lastIndex))
    }

    private fun deleteCategory(index: Int) {
        saveColumnsOnly()

        val old = ConfigManager.config
        val categories = old.categories.orEmpty().toMutableList()

        if (index in categories.indices) {
            categories.removeAt(index)
        }

        ConfigManager.updateConfig(
            old.copy(
                title = null,
                buttonWidth = null,
                buttonHeight = null,
                categories = categories
            )
        )

        client?.setScreen(ConfigEditorScreen())
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0xAA000000.toInt())

        super.render(context, mouseX, mouseY, delta)

        val centerX = width / 2

        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("Config Editor"),
            centerX,
            10,
            0xFFFFD966.toInt()
        )

        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Columns"),
            centerX - 40,
            22,
            0xFFFFFFFF.toInt()
        )

        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("Categories"),
            centerX,
            62,
            0xFFFFFFFF.toInt()
        )
    }

    override fun shouldPause(): Boolean {
        return false
    }
}