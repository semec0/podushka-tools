package semeco.podushkatools.client

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text

class CategoryEditScreen(
    private val categoryIndex: Int,
    private val requestedPage: Int = 0
) : Screen(Text.literal("Edit Category")) {

    private lateinit var nameField: TextFieldWidget
    private lateinit var saveAndBackButton: ButtonWidget

    private var originalName: String = ""
    private var pageIndex = 0
    private var pageCount = 1

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

        originalName = category.name ?: ""

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
        nameField.setText(originalName)
        nameField.setChangedListener {
            updateSaveButtonState()
        }
        addDrawableChild(nameField)

        val listTop = 78
        val rowHeight = 24
        val pageSize = calculatePageSize(listTop, rowHeight)

        pageCount = if (buttons.isEmpty()) {
            1
        } else {
            ((buttons.size - 1) / pageSize) + 1
        }

        pageIndex = requestedPage.coerceIn(0, pageCount - 1)

        val startIndex = pageIndex * pageSize
        val visibleButtons = buttons.drop(startIndex).take(pageSize)

        visibleButtons.forEachIndexed { visibleIndex, toolButton ->
            val buttonIndex = startIndex + visibleIndex
            val rowY = listTop + visibleIndex * rowHeight
            val label = toolButton.label?.takeIf { it.isNotBlank() } ?: "(empty button)"

            addDrawableChild(
                ButtonWidget.builder(Text.literal(label)) {
                    if (hasChanges()) {
                        saveCategoryName()
                    }
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

        if (pageCount > 1) {
            val previousButton = ButtonWidget.builder(Text.literal("<")) {
                if (hasChanges()) {
                    saveCategoryName()
                }
                client?.setScreen(CategoryEditScreen(categoryIndex, pageIndex - 1))
            }
                .dimensions(centerX - 86, height - 84, 50, 20)
                .build()

            previousButton.active = pageIndex > 0
            addDrawableChild(previousButton)

            val nextButton = ButtonWidget.builder(Text.literal(">")) {
                if (hasChanges()) {
                    saveCategoryName()
                }
                client?.setScreen(CategoryEditScreen(categoryIndex, pageIndex + 1))
            }
                .dimensions(centerX + 36, height - 84, 50, 20)
                .build()

            nextButton.active = pageIndex < pageCount - 1
            addDrawableChild(nextButton)
        }

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Add Button")) {
                addButton()
            }
                .dimensions(centerX - 155, height - 58, 145, 20)
                .build()
        )

        saveAndBackButton = ButtonWidget.builder(Text.literal("Save & Back")) {
            saveCategoryName()
            client?.setScreen(ConfigEditorScreen())
        }
            .dimensions(centerX + 10, height - 58, 145, 20)
            .build()

        saveAndBackButton.active = false
        addDrawableChild(saveAndBackButton)

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Back")) {
                client?.setScreen(ConfigEditorScreen())
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
        return nameField.getText() != originalName
    }

    private fun updateSaveButtonState() {
        if (::saveAndBackButton.isInitialized) {
            saveAndBackButton.active = hasChanges()
        }
    }

    private fun saveCategoryName() {
        val old = ConfigManager.config
        val categories = old.categories.orEmpty().toMutableList()

        if (categoryIndex !in categories.indices) {
            return
        }

        val oldCategory = categories[categoryIndex]
        val newName = nameField.getText()

        categories[categoryIndex] = oldCategory.copy(
            name = newName
        )

        ConfigManager.updateConfig(
            old.copy(
                title = null,
                buttonWidth = null,
                buttonHeight = null,
                categories = categories
            )
        )

        originalName = newName
        updateSaveButtonState()
    }

    private fun addButton() {
        if (hasChanges()) {
            saveCategoryName()
        }

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
        if (hasChanges()) {
            saveCategoryName()
        }

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

        client?.setScreen(CategoryEditScreen(categoryIndex, pageIndex))
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