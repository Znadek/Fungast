package com.fungast.app.location

// TODO: implement with NSURLSession when the iOS app is set up; until then only full links work
class IosLinkResolver : LinkResolver {
    override suspend fun resolve(url: String): String? = null
}
