package com.fungast.app.routes

// TODO: implement with CLLocationManager background updates ("location" background mode) on macOS
class IosRouteRecorder : RouteRecorder {
    override suspend fun start() = Unit
    override fun stop() = Unit
}
