package com.aca56.cahiersortiecodex.data.settings

enum class LauncherIconOption(
    val storageKey: String,
    val label: String,
    val aliasClassName: String,
) {
    DEFAULT(
        storageKey = "default",
        label = "Icône par défaut",
        aliasClassName = "com.aca56.cahiersortiecodex.DefaultLauncherAlias",
    ),
    FOREST(
        storageKey = "forest",
        label = "Forêt",
        aliasClassName = "com.aca56.cahiersortiecodex.ForestLauncherAlias",
    ),
    MINT(
        storageKey = "mint",
        label = "Menthe",
        aliasClassName = "com.aca56.cahiersortiecodex.MintLauncherAlias",
    ),
    DEEP(
        storageKey = "deep",
        label = "Vert profond",
        aliasClassName = "com.aca56.cahiersortiecodex.DeepLauncherAlias",
    );

    companion object {
        fun fromStorageKey(value: String?): LauncherIconOption {
            return entries.firstOrNull { it.storageKey == value } ?: DEFAULT
        }
    }
}
