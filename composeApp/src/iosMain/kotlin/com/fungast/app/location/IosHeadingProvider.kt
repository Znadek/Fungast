package com.fungast.app.location

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// TODO: implement with CLLocationManager heading updates when the iOS app is set up (requires macOS to build)
class IosHeadingProvider : HeadingProvider {
    override val hasCompass: Boolean = false
    override fun headingUpdates(): Flow<Heading> = emptyFlow()
}
