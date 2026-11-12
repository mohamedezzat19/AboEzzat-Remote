package com.aboezzat.remote

import android.content.Context

class RemoteProfileRepository(private val context: Context) {
    fun load(assetPath: String): RemoteProfile {
        val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        return RemoteProfileParser.parse(json)
    }

    fun loadGvcCassette(): RemoteProfile =
        load("remotes/gvc_cassette.json")
}
