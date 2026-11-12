package com.aboezzat.remote

import android.content.Context

class RemoteProfileRepository(private val context: Context) {
    private val importedProfiles by lazy { ImportedProfileStore(context) }

    fun load(assetPath: String): RemoteProfile {
        val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        return RemoteProfileParser.parse(json)
    }

    fun loadJvcCassette(): RemoteProfile =
        load("remotes/jvc_cassette.json")

    fun importProfile(json: String): RemoteProfile =
        importedProfiles.importJson(json)

    fun listImportedProfiles(): List<RemoteProfile> =
        importedProfiles.listProfiles()

    fun loadImportedProfile(id: String): RemoteProfile =
        importedProfiles.load(id)

    fun exportImportedProfile(id: String): String =
        importedProfiles.exportJson(id)

    fun renameImportedProfile(id: String, displayName: String): RemoteProfile =
        importedProfiles.rename(id, displayName)

    fun moveImportedButton(id: String, buttonId: String, direction: Int): RemoteProfile =
        importedProfiles.moveButton(id, buttonId, direction)

    fun renameImportedGroup(id: String, group: String, newGroup: String): RemoteProfile =
        importedProfiles.renameGroup(id, group, newGroup)

    fun moveImportedGroup(id: String, group: String, direction: Int): RemoteProfile =
        importedProfiles.moveGroup(id, group, direction)

    fun deleteImportedProfile(id: String): Boolean =
        importedProfiles.delete(id)
}
