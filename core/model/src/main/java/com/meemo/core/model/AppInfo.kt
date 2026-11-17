package com.meemo.core.model

data class AppInfo(
    val packageName: String,
    val label: String,
    val isLaunchable: Boolean = true
)
