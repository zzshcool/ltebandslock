package com.ltebandslock.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ltebandslock.data.model.RouterProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "router_profiles_prefs")

class ProfileRepository(private val context: Context) {

    private val gson = Gson()
    private val PROFILES_KEY = stringPreferencesKey("saved_router_profiles")
    private val ACTIVE_PROFILE_ID_KEY = stringPreferencesKey("active_profile_id")

    val profilesFlow: Flow<List<RouterProfile>> = context.dataStore.data.map { prefs ->
        val json = prefs[PROFILES_KEY] ?: ""
        if (json.isEmpty()) {
            val defaultList = listOf(
                RouterProfile(
                    name = "B818 Router (Default)",
                    ipAddress = "192.168.8.1",
                    username = "admin",
                    password = "admin",
                    isDefault = true
                )
            )
            defaultList
        } else {
            val type = object : TypeToken<List<RouterProfile>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        }
    }

    val activeProfileIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ACTIVE_PROFILE_ID_KEY]
    }

    suspend fun saveProfiles(profiles: List<RouterProfile>) {
        val json = gson.toJson(profiles)
        context.dataStore.edit { prefs ->
            prefs[PROFILES_KEY] = json
        }
    }

    suspend fun addOrUpdateProfile(profile: RouterProfile) {
        context.dataStore.edit { prefs ->
            val json = prefs[PROFILES_KEY] ?: ""
            val type = object : TypeToken<List<RouterProfile>>() {}.type
            val currentList: MutableList<RouterProfile> = if (json.isNotEmpty()) {
                gson.fromJson(json, type) ?: mutableListOf()
            } else {
                mutableListOf()
            }

            val index = currentList.indexOfFirst { it.id == profile.id }
            if (index >= 0) {
                currentList[index] = profile
            } else {
                currentList.add(profile)
            }

            prefs[PROFILES_KEY] = gson.toJson(currentList)
            prefs[ACTIVE_PROFILE_ID_KEY] = profile.id
        }
    }

    suspend fun deleteProfile(profileId: String) {
        context.dataStore.edit { prefs ->
            val json = prefs[PROFILES_KEY] ?: ""
            val type = object : TypeToken<List<RouterProfile>>() {}.type
            val currentList: MutableList<RouterProfile> = if (json.isNotEmpty()) {
                gson.fromJson(json, type) ?: mutableListOf()
            } else {
                mutableListOf()
            }

            currentList.removeAll { it.id == profileId }
            prefs[PROFILES_KEY] = gson.toJson(currentList)

            if (prefs[ACTIVE_PROFILE_ID_KEY] == profileId) {
                prefs[ACTIVE_PROFILE_ID_KEY] = currentList.firstOrNull()?.id ?: ""
            }
        }
    }

    suspend fun setActiveProfileId(id: String) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_PROFILE_ID_KEY] = id
        }
    }
}
