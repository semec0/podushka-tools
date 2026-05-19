package semeco.podushkatools.client

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text

class ConfigEditorScreen(
    private val requestedPage: Int = 0
) : Screen(Text.literal("Podushka Tools Config Editor")) {

    private lateinit var columnsField: TextFieldWidget
    private lateinit var saveAndBackButton: ButtonWidget

    private var originalColumns: String = "2"
    private var pageIndex = 0
    private var pageCount = 1

    override fun init() {
        clearChildren()

        ConfigManager.load()

        val config = ConfigManager.config
        val categories = config.categories ?: listOf()

        val centerX = width / 2

        originalColumns = (config.columns ?: 2).toString()

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
        columnsField.setText(originalColumns)
        columnsField.setChangedListener {
            updateSaveButtonState()
        }
        addDrawableChild(columnsField)

        val listTop = 78
        val rowHeight = 24
        val pageSize = calculatePageSize(listTop, rowHeight)

        pageCount = if (categories.isEmpty()) {
            1
        } else {
            ((categories.size - 1) / pageSize) + 1
        }

        pageIndex = requestedPage.coerceIn(0, pageCount - 1)

        val startIndex = pageIndex * pageSize
        val visibleCategories = categories.drop(startIndex).take(pageSize)

        visibleCategories.forEachIndexed { visibleIndex, category ->
            val realIndex = startIndex + visibleIndex
            val rowY = listTop + visibleIndex * rowHeight
            val categoryName = category.name?.takeIf { it.isNotBlank() } ?: "(empty category)"

            addDrawableChild(
                ButtonWidget.builder(Text.literal(categoryName)) {
                    if (hasChanges()) {
                        saveColumnsOnly()
                    }
                    client?.setScreen(CategoryEditScreen(realIndex))
                }
                    .dimensions(centerX - 170, rowY, 250, 20)
                    .build()
            )

            addDrawableChild(
                ButtonWidget.builder(Text.literal("Del")) {
                    deleteCategory(realIndex)
                }
                    .dimensions(centerX + 88, rowY, 45, 20)
                    .build()
            )
        }

        if (pageCount > 1) {
            val previousButton = ButtonWidget.builder(Text.literal("<")) {
                if (hasChanges()) {
                    saveColumnsOnly()
                }
                client?.setScreen(ConfigEditorScreen(pageIndex - 1))
            }
                .dimensions(centerX - 86, height - 84, 50, 20)
                .build()

            previousButton.active = pageIndex > 0
            addDrawableChild(previousButton)

            val nextButton = ButtonWidget.builder(Text.literal(">")) {
                if (hasChanges()) {
                    saveColumnsOnly()
                }
                client?.setScreen(ConfigEditorScreen(pageIndex + 1))
            }
                .dimensions(centerX + 36, height - 84, 50, 20)
                .build()

            nextButton.active = pageIndex < pageCount - 1
            addDrawableChild(nextButton)
        }

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Add Category")) {
                addCategory()
            }
                .dimensions(centerX - 155, height - 58, 145, 20)
                .build()
        )

        saveAndBackButton = ButtonWidget.builder(Text.literal("Save & Back")) {
            saveColumnsOnly()
            client?.setScreen(CommandMenuScreen())
        }
            .dimensions(centerX + 10, height - 58, 145, 20)
            .build()

        saveAndBackButton.active = false
        addDrawableChild(saveAndBackButton)

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Back")) {
                client?.setScreen(CommandMenuScreen())
            }
                .dimensions(centerX - 50, height - 32, 100, 20)
                .build()
        )

        updateSaveButtonState()
    }

    private fun calculatePageSize(listTop: Int, rowHeight: Int): Int {
        val listBottom = height - 94
        return ((listBottom - listTop) / rowHeight).coerceAtLeast(1)
    }

    private fun hasChanges(): Boolean {
        return columnsField.getText() != originalColumns
    }

    private fun updateSaveButtonState() {
        if (::saveAndBackButton.isInitialized) {
            saveAndBackButton.active = hasChanges()
        }
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

        originalColumns = columns.toString()
        columnsField.setText(originalColumns)
        updateSaveButtonState()
    }

    private fun addCategory() {
        if (hasChanges()) {
            saveColumnsOnly()
        }

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
        if (hasChanges()) {
            saveColumnsOnly()
        }

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

        client?.setScreen(ConfigEditorScreen(pageIndex))
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

        if (pageCount > 1) {
            context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("${pageIndex + 1} / $pageCount"),
                centerX,
                height - 79,
                0xFFFFFFFF.toInt()
            )
        }
    }

    override fun shouldPause(): Boolean {
        return false
    }
}