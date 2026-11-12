package com.aboezzat.remote

import android.content.Context

class RemoteProfileRepository(private val context: Context) {
    fun load(assetPath: String): RemoteProfile {
        val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        return RemoteProfileParser.parse(json)
    }

    fun loadJvcRcEz38s(): RemoteProfile =
        load("remotes/jvc_rc_ez38s.json")
}
