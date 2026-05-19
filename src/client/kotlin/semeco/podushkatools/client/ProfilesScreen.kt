package semeco.podushkatools.client

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text

class ProfilesScreen(
    private val requestedPage: Int = 0
) : Screen(Text.literal("Profiles")) {

    private var pageIndex = 0
    private var pageCount = 1

    override fun init() {
        clearChildren()

        ConfigManager.load()

        val profiles = ConfigManager.listProfiles()
        val activeProfile = ConfigManager.getActiveProfileName()
        val centerX = width / 2

        val listTop = 58
        val rowHeight = 24
        val pageSize = calculatePageSize(listTop, rowHeight)

        pageCount = if (profiles.isEmpty()) {
            1
        } else {
            ((profiles.size - 1) / pageSize) + 1
        }

        pageIndex = requestedPage.coerceIn(0, pageCount - 1)

        val startIndex = pageIndex * pageSize
        val visibleProfiles = profiles.drop(startIndex).take(pageSize)

        visibleProfiles.forEachIndexed { visibleIndex, profileName ->
            val rowY = listTop + visibleIndex * rowHeight
            val displayName = if (profileName == activeProfile) {
                "* $profileName"
            } else {
                profileName
            }

            addDrawableChild(
                ButtonWidget.builder(Text.literal(displayName)) {
                    ConfigManager.selectProfile(profileName)
                    client?.setScreen(CommandMenuScreen())
                }
                    .dimensions(centerX - 170, rowY, 250, 20)
                    .build()
            )

            val deleteButton = ButtonWidget.builder(Text.literal("Del")) {
                ConfigManager.deleteProfile(profileName)
                client?.setScreen(ProfilesScreen(pageIndex))
            }
                .dimensions(centerX + 88, rowY, 45, 20)
                .build()

            deleteButton.active = profileName != "default" && profiles.size > 1

            addDrawableChild(deleteButton)
        }

        if (pageCount > 1) {
            val previousButton = ButtonWidget.builder(Text.literal("<")) {
                client?.setScreen(ProfilesScreen(pageIndex - 1))
            }
                .dimensions(centerX - 86, height - 58, 50, 20)
                .build()

            previousButton.active = pageIndex > 0
            addDrawableChild(previousButton)

            val nextButton = ButtonWidget.builder(Text.literal(">")) {
                client?.setScreen(ProfilesScreen(pageIndex + 1))
            }
                .dimensions(centerX + 36, height - 58, 50, 20)
                .build()

            nextButton.active = pageIndex < pageCount - 1
            addDrawableChild(nextButton)
        }

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Create")) {
                client?.setScreen(CreateProfileScreen())
            }
                .dimensions(centerX - 155, height - 32, 145, 20)
                .build()
        )

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Back")) {
                client?.setScreen(CommandMenuScreen())
            }
                .dimensions(centerX + 10, height - 32, 145, 20)
                .build()
        )
    }

    private fun calculatePageSize(listTop: Int, rowHeight: Int): Int {
        val listBottom = height - 68
        return ((listBottom - listTop) / rowHeight).coerceAtLeast(1)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0xAA000000.toInt())

        super.render(context, mouseX, mouseY, delta)

        val centerX = width / 2

        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("Profiles"),
            centerX,
            10,
            0xFFFFD966.toInt()
        )

        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("Active: ${ConfigManager.getActiveProfileName()}"),
            centerX,
            34,
            0xFFFFFFFF.toInt()
        )

        if (pageCount > 1) {
            context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("${pageIndex + 1} / $pageCount"),
                centerX,
                height - 53,
                0xFFFFFFFF.toInt()
            )
        }
    }

    override fun shouldPause(): Boolean {
        return false
    }
}