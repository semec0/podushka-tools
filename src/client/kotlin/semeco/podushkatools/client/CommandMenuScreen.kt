package semeco.podushkatools.client

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text

class CommandMenuScreen(
    private val requestedPage: Int = 0
) : Screen(Text.literal("Podushka Tools")) {

    private data class CategoryLabel(
        val name: String,
        val y: Int,
        val backgroundLeft: Int,
        val backgroundRight: Int
    )

    private data class PageCategory(
        val name: String,
        val buttons: List<ToolButton>
    )

    private val categoryLabels = mutableListOf<CategoryLabel>()

    private var pageIndex = 0
    private var pageCount = 1

    private val gapX = 8
    private val gapY = 6
    private val buttonHeight = 20
    private val minButtonWidth = 80
    private val maxButtonWidth = 280
    private val horizontalScreenPadding = 80

    private val categoryGapTop = 18
    private val categoryTitleHeight = 12
    private val categoryGapBottom = 16

    private val contentStartY = 24

    override fun init() {
        clearChildren()
        categoryLabels.clear()

        ConfigManager.load()

        val config = ConfigManager.config
        val pages = buildPages(config)

        pageCount = pages.size.coerceAtLeast(1)
        pageIndex = requestedPage.coerceIn(0, pageCount - 1)

        val pageCategories = pages.getOrElse(pageIndex) { listOf() }

        renderPageWidgets(pageCategories, config)

        if (pageCount > 1) {
            val previousButton = ButtonWidget.builder(Text.literal("<")) {
                client?.setScreen(CommandMenuScreen(pageIndex - 1))
            }
                .dimensions(width / 2 - 86, height - 58, 50, 20)
                .build()

            previousButton.active = pageIndex > 0
            addDrawableChild(previousButton)

            val nextButton = ButtonWidget.builder(Text.literal(">")) {
                client?.setScreen(CommandMenuScreen(pageIndex + 1))
            }
                .dimensions(width / 2 + 36, height - 58, 50, 20)
                .build()

            nextButton.active = pageIndex < pageCount - 1
            addDrawableChild(nextButton)
        }

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Profiles")) {
                client?.setScreen(ProfilesScreen())
            }
                .dimensions(width / 2 - 158, height - 32, 100, 20)
                .build()
        )

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Edit")) {
                client?.setScreen(ConfigEditorScreen())
            }
                .dimensions(width / 2 - 50, height - 32, 100, 20)
                .build()
        )

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Close")) {
                close()
            }
                .dimensions(width / 2 + 58, height - 32, 100, 20)
                .build()
        )
    }

    private fun buildPages(config: ToolConfig): List<List<PageCategory>> {
        val categories = config.categories ?: listOf()
        val pages = mutableListOf<MutableList<PageCategory>>()
        pages.add(mutableListOf())

        var currentY = contentStartY

        fun startNewPage() {
            if (pages.last().isNotEmpty()) {
                pages.add(mutableListOf())
            }
            currentY = contentStartY
        }

        categories.forEach { category ->
            val categoryName = category.name?.takeIf { it.isNotBlank() } ?: "Category"
            val buttons = category.buttons ?: listOf()

            if (buttons.isEmpty()) {
                val estimatedHeight = categoryGapTop + categoryTitleHeight + categoryGapBottom

                if (currentY + estimatedHeight > contentBottomY() && pages.last().isNotEmpty()) {
                    startNewPage()
                }

                pages.last().add(PageCategory(categoryName, listOf()))
                currentY += estimatedHeight
                return@forEach
            }

            var remainingButtons = buttons
            var continuationIndex = 1

            while (remainingButtons.isNotEmpty()) {
                val buttonWidth = calculateButtonWidth(remainingButtons)
                val columns = calculateColumns(remainingButtons.size, buttonWidth)

                var titleY = currentY + categoryGapTop
                var buttonsStartY = titleY + categoryTitleHeight + 5
                var availableHeight = contentBottomY() - buttonsStartY - categoryGapBottom

                if (availableHeight < buttonHeight && pages.last().isNotEmpty()) {
                    startNewPage()

                    titleY = currentY + categoryGapTop
                    buttonsStartY = titleY + categoryTitleHeight + 5
                    availableHeight = contentBottomY() - buttonsStartY - categoryGapBottom
                }

                val rowsThatFit = ((availableHeight + gapY) / (buttonHeight + gapY)).coerceAtLeast(1)
                val buttonsThatFit = (rowsThatFit * columns).coerceAtLeast(1)

                val currentChunk = remainingButtons.take(buttonsThatFit)
                val displayName = if (continuationIndex == 1) {
                    categoryName
                } else {
                    "$categoryName #$continuationIndex"
                }

                pages.last().add(PageCategory(displayName, currentChunk))

                val usedRows = (currentChunk.size + columns - 1) / columns
                currentY = buttonsStartY + usedRows * (buttonHeight + gapY) + categoryGapBottom

                remainingButtons = remainingButtons.drop(buttonsThatFit)

                if (remainingButtons.isNotEmpty()) {
                    continuationIndex++
                    startNewPage()
                }
            }
        }

        return pages.filter { it.isNotEmpty() }.ifEmpty {
            listOf(listOf())
        }
    }

    private fun renderPageWidgets(pageCategories: List<PageCategory>, config: ToolConfig) {
        val maxColumns = (config.columns ?: 2).coerceIn(1, 6)
        val availableWidth = (width - horizontalScreenPadding).coerceAtLeast(minButtonWidth)

        var currentY = contentStartY

        pageCategories.forEach { category ->
            val categoryName = category.name
            val buttons = category.buttons

            val buttonWidth = calculateButtonWidth(buttons)
            val columns = calculateColumns(buttons.size, buttonWidth).coerceAtMost(maxColumns)

            val finalColumns = if (buttons.isEmpty()) {
                1
            } else {
                columns.coerceAtMost(buttons.size)
            }

            val maxWidthForCurrentColumns =
                ((availableWidth - (finalColumns - 1) * gapX) / finalColumns)
                    .coerceAtLeast(minButtonWidth)

            val finalButtonWidth = buttonWidth
                .coerceAtMost(maxWidthForCurrentColumns)
                .coerceAtLeast(minButtonWidth)

            val totalWidth = finalColumns * finalButtonWidth + (finalColumns - 1) * gapX
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
                val column = indexInCategory % finalColumns
                val row = indexInCategory / finalColumns

                val x = startX + column * (finalButtonWidth + gapX)
                val y = buttonsStartY + row * (buttonHeight + gapY)

                val label = toolButton.label?.takeIf { it.isNotBlank() } ?: "Button"
                val command = toolButton.command ?: ""

                addDrawableChild(
                    ButtonWidget.builder(Text.literal(label)) {
                        PodushkaToolsClient.sendCommand(command)
                        close()
                    }
                        .dimensions(x, y, finalButtonWidth, buttonHeight)
                        .build()
                )
            }

            val rows = if (buttons.isEmpty()) {
                0
            } else {
                (buttons.size + finalColumns - 1) / finalColumns
            }

            currentY = buttonsStartY + rows * (buttonHeight + gapY) + categoryGapBottom
        }
    }

    private fun calculateButtonWidth(buttons: List<ToolButton>): Int {
        val maxTextWidth = buttons
            .map { button -> button.label?.takeIf { it.isNotBlank() } ?: "Button" }
            .maxOfOrNull { label -> textRenderer.getWidth(label) }
            ?: 40

        return (maxTextWidth + 36).coerceIn(minButtonWidth, maxButtonWidth)
    }

    private fun calculateColumns(buttonCount: Int, buttonWidth: Int): Int {
        if (buttonCount <= 0) {
            return 1
        }

        val configColumns = (ConfigManager.config.columns ?: 2).coerceIn(1, 6)
        val availableWidth = (width - horizontalScreenPadding).coerceAtLeast(minButtonWidth)

        var columns = configColumns.coerceAtMost(buttonCount)

        while (
            columns > 1 &&
            columns * buttonWidth + (columns - 1) * gapX > availableWidth
        ) {
            columns--
        }

        return columns.coerceAtLeast(1)
    }

    private fun contentBottomY(): Int {
        return if (pageCount > 1) {
            height - 66
        } else {
            height - 46
        }
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

        if (pageCount > 1) {
            context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("${pageIndex + 1} / $pageCount"),
                width / 2,
                height - 53,
                0xFFFFFFFF.toInt()
            )
        }

        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Profile: ${ConfigManager.getActiveProfileName()}"),
            8,
            height - 14,
            0xFFAAAAAA.toInt()
        )
    }

    override fun shouldPause(): Boolean {
        return false
    }
}