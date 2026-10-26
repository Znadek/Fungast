package com.fungast.app.ui

import androidx.compose.runtime.Composable

// iOS has no system back button; navigation uses the on-screen back arrow
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
