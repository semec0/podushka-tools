package semeco.podushkatools.client

data class ProfileSettings(
    val activeProfile: String? = "default"
)

data class ToolConfig(
    val title: String? = null,
    val columns: Int? = 2,

    // Старые поля оставляем только для совместимости со старыми JSON.
    // Новая версия меню их не использует и при сохранении убирает.
    val buttonWidth: Int? = null,
    val buttonHeight: Int? = null,

    val categories: List<ToolCategory>? = listOf()
)

data class ToolCategory(
    val name: String? = "",
    val buttons: List<ToolButton>? = listOf()
)

data class ToolButton(
    val label: String? = "",
    val command: String? = ""
)