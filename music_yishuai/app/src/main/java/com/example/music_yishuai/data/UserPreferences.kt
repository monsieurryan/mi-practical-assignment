package com.example.music_yishuai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {
    companion object {
        private val HAS_ACCEPTED_TERMS = booleanPreferencesKey("has_accepted_terms")
//        private val LAST_PLAYED_SONG_ID = stringPreferencesKey("last_played_song_id")
        private val FAVORITE_SONGS = stringSetPreferencesKey("favorite_songs")
    }

    val hasAcceptedTerms: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HAS_ACCEPTED_TERMS] ?: false
        }

//    val lastPlayedSongId: Flow<String> = context.dataStore.data
//        .map { preferences ->
//            preferences[LAST_PLAYED_SONG_ID] ?: ""
//        }
//
//    val favoriteSongs: Flow<Set<String>> = context.dataStore.data
//        .map { preferences ->
//            preferences[FAVORITE_SONGS] ?: emptySet()
//        }

    suspend fun setAcceptedTerms(accepted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_ACCEPTED_TERMS] = accepted
        }
    }

//    suspend fun saveLastPlayedSongId(songId: String) {
//        context.dataStore.edit { preferences ->
//            preferences[LAST_PLAYED_SONG_ID] = songId
//        }
//    }

    suspend fun isSongFavorite(songId: String): Boolean {
        return context.dataStore.data
            .map { preferences -> 
                preferences[FAVORITE_SONGS]?.contains(songId) ?: false 
            }
            .first()
    }

//    suspend fun addFavoriteSong(songId: String) {
//        context.dataStore.edit { preferences ->
//            val currentSet = preferences[FAVORITE_SONGS] ?: emptySet()
//            preferences[FAVORITE_SONGS] = currentSet + songId
//        }
//    }

//    suspend fun removeFavoriteSong(songId: String) {
//        context.dataStore.edit { preferences ->
//            val currentSet = preferences[FAVORITE_SONGS] ?: emptySet()
//            preferences[FAVORITE_SONGS] = currentSet - songId
//        }
//    }

    suspend fun toggleFavoriteSong(songId: String): Boolean {
        var isFavoriteNow = false
        context.dataStore.edit { preferences ->
            val currentSet = preferences[FAVORITE_SONGS] ?: emptySet()
            if (currentSet.contains(songId)) {
                preferences[FAVORITE_SONGS] = currentSet - songId
                isFavoriteNow = false
            } else {
                preferences[FAVORITE_SONGS] = currentSet + songId
                isFavoriteNow = true
            }
        }
        return isFavoriteNow
    }
} 