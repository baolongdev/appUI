package com.example.appui.ui.screen.home.content

/**
 * View mode for agents list
 */
enum class ViewMode {
    List,
    Card
}

/**
 * Filter tab options
 */
enum class FilterTab {
    ALL,
    LAST7D
}

/**
 * Sort options for agents
 */
enum class SortBy {
    Newest,
    Oldest,
    Name;

    val displayName: String
        get() = when (this) {
            Newest -> "Mới nhất"
            Oldest -> "Cũ nhất"
            Name -> "Tên"
        }
}
