package semeco.podushkatools.client

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text

class CommandMenuScreen : Screen(Text.literal("Podushka Tools")) {

    private data class CategoryLabel(
        val name: String,
        val y: Int,
        val backgroundLeft: Int,
        val backgroundRight: Int
    )

    private val categoryLabels = mutableListOf<CategoryLabel>()

    override fun init() {
        clearChildren()
        categoryLabels.clear()

        ConfigManager.load()

        val config = ConfigManager.config

        val maxColumns = (config.columns ?: 2).coerceIn(1, 6)
        val categories = config.categories ?: listOf()

        val gapX = 8
        val gapY = 6

        val buttonHeight = 20
        val minButtonWidth = 80
        val maxButtonWidth = 280
        val horizontalScreenPadding = 80
        val availableWidth = (width - horizontalScreenPadding).coerceAtLeast(minButtonWidth)

        val categoryGapTop = 18
        val categoryTitleHeight = 12
        val categoryGapBottom = 16

        var currentY = 24

        categories.forEach { category ->
            val categoryName = category.name?.takeIf { it.isNotBlank() } ?: "Category"
            val buttons = category.buttons ?: listOf()

            val buttonLabels = buttons.map { button ->
                button.label?.takeIf { it.isNotBlank() } ?: "Button"
            }

            val maxTextWidth = buttonLabels
                .maxOfOrNull { label -> textRenderer.getWidth(label) }
                ?: 40

            val desiredButtonWidth = (maxTextWidth + 36).coerceIn(minButtonWidth, maxButtonWidth)

            var categoryColumns = if (buttons.isEmpty()) {
                1
            } else {
                maxColumns.coerceAtMost(buttons.size)
            }

            while (
                categoryColumns > 1 &&
                categoryColumns * desiredButtonWidth + (categoryColumns - 1) * gapX > availableWidth
            ) {
                categoryColumns--
            }

            val maxWidthForCurrentColumns =
                ((availableWidth - (categoryColumns - 1) * gapX) / categoryColumns)
                    .coerceAtLeast(minButtonWidth)

            val buttonWidth = desiredButtonWidth
                .coerceAtMost(maxWidthForCurrentColumns)
                .coerceAtLeast(minButtonWidth)

            val totalWidth = categoryColumns * buttonWidth + (categoryColumns - 1) * gapX
            val startX = (width - totalWidth) / 2

            currentY += categoryGapTop

            val categoryTextWidth = textRenderer.getWidth(categoryName) + 24
            val labelWidth = maxOf(totalWidth + 16, categoryTextWidth, 160)

            categoryLabels.add(
                CategoryLabel(
                    name = categoryName,
                    y = currentY,
                    backgroundLeft = width / 2 - labelWidth / 2,
                    backgroundRight = width / 2 + labelWidth / 2
                )
            )

            val buttonsStartY = currentY + categoryTitleHeight + 5

            buttons.forEachIndexed { indexInCategory, toolButton ->
                val column = indexInCategory % categoryColumns
                val row = indexInCategory / categoryColumns

                val x = startX + column * (buttonWidth + gapX)
                val y = buttonsStartY + row * (buttonHeight + gapY)

                val label = toolButton.label?.takeIf { it.isNotBlank() } ?: "Button"
                val command = toolButton.command ?: ""

                addDrawableChild(
                    ButtonWidget.builder(Text.literal(label)) {
                        PodushkaToolsClient.sendCommand(command)
                        close()
                    }
                        .dimensions(x, y, buttonWidth, buttonHeight)
                        .build()
                )
            }

            val rows = if (buttons.isEmpty()) {
                0
            } else {
                (buttons.size + categoryColumns - 1) / categoryColumns
            }

            currentY = buttonsStartY + rows * (buttonHeight + gapY) + categoryGapBottom
        }

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Edit")) {
                client?.setScreen(ConfigEditorScreen())
            }
                .dimensions(width / 2 - 104, height - 32, 100, 20)
                .build()
        )

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Close")) {
                close()
            }
                .dimensions(width / 2 + 4, height - 32, 100, 20)
                .build()
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0xAA000000.toInt())

        super.render(context, mouseX, mouseY, delta)

        categoryLabels.forEach { label ->
            context.fill(
                label.backgroundLeft,
                label.y - 3,
                label.backgroundRight,
                label.y + 11,
                0x66000000
            )

            context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal(label.name),
                width / 2,
                label.y,
                0xFFFFD966.toInt()
            )
        }
    }

    override fun shouldPause(): Boolean {
        return false
    }
}