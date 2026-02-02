package com.app.idisplaynew.ui.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

// Create a singleton instance for DataStore
val Context.dataStore by preferencesDataStore(name = "user_prefs")

class DataStoreManager(private val context: Context) {

    companion object {

        private val IS_FIRST_LOGIN = booleanPreferencesKey("isFirstLogin")
        private val IS_FIRST_VISIT = booleanPreferencesKey("isFirstVisit")
        private val FULL_ROLE_NAME = stringPreferencesKey("fullRoleName")
        private val USER_ID = intPreferencesKey("userId")
        private val UserTypeId = intPreferencesKey("userTypeId")
        private val RoleId = intPreferencesKey("roleId")
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val FULL_NAME_KEY = stringPreferencesKey("full_name")
        private val EMAIL = stringPreferencesKey("email")
        private val PHONE_NO = stringPreferencesKey("phoneNo")
        private val PROFILE_IMAGE = stringPreferencesKey("profileImage")
        private val ROLE_NAME = stringPreferencesKey("roleName")
        private val SAVED_EMAIL = stringPreferencesKey("saved_email")
        private val SAVED_PASSWORD = stringPreferencesKey("saved_password")
        private val REMEMBER_ME = booleanPreferencesKey("remember_me")
    }

    // Save email and password
    suspend fun saveCredentials(email: String, password: String, rememberMe: Boolean = true) {
        context.dataStore.edit { preferences ->
            preferences[SAVED_EMAIL] = email
            preferences[SAVED_PASSWORD] = password
            preferences[REMEMBER_ME] = rememberMe
        }
    }

    // Save `isFirstVisit`
    suspend fun saveIsFirstVisit(isFirstVisit: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_VISIT] = isFirstVisit
        }
    }

    // Save `isFirstLogin`
    suspend fun saveIsFirstLogin(isFirstLogin: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_LOGIN] = isFirstLogin
        }
    }

    // Save `fullRoleName`
    suspend fun saveFullRoleName(fullRoleName: String) {
        context.dataStore.edit { preferences ->
            preferences[FULL_ROLE_NAME] = fullRoleName
        }
    }

    // Save User Id
    suspend fun saveUserId(userId: Int) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = userId
        }
    }

    suspend fun saveUserTypeId(userTypeId: Int) {
        context.dataStore.edit { preferences ->
            preferences[UserTypeId] = userTypeId
        }
    }
    suspend fun savetypeId(typeId: Int) {
        context.dataStore.edit { preferences ->
            preferences[RoleId] = typeId
        }
    }

    // Save Auth Token
    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = token
        }
    }

    // Save Full Name
    suspend fun saveFullName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[FULL_NAME_KEY] = name.trim() // Remove any leading/trailing spaces
        }
    }

    // Save Email
    suspend fun saveEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[EMAIL] = email.trim() // Remove any leading/trailing spaces
        }
    }

    // Save PhoneNo
    suspend fun savePhoneNo(phoneNo: String) {
        context.dataStore.edit { preferences ->
            preferences[PHONE_NO] = phoneNo.trim() // Remove any leading/trailing spaces
        }
    }

    // Save ProfileImage
    suspend fun saveProfileImage(profileImage: String) {
        context.dataStore.edit { preferences ->
            preferences[PROFILE_IMAGE] = profileImage.trim() // Remove any leading/trailing spaces
        }
    }

    // Save RoleName
    suspend fun saveRoleName(roleName: String) {
        context.dataStore.edit { preferences ->
            preferences[ROLE_NAME] = roleName.trim() // Remove any leading/trailing spaces
        }
    }

    // Load saved credentials
    val savedEmailFlow: Flow<String?> = context.dataStore.data
        .map { it[SAVED_EMAIL] }

    val savedPasswordFlow: Flow<String?> = context.dataStore.data
        .map { it[SAVED_PASSWORD] }

    val rememberMeFlow: Flow<Boolean> = context.dataStore.data
        .map { it[REMEMBER_ME] ?: false }

    // Retrieve `isFirstLogin`
    val isFirstLogin: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[IS_FIRST_LOGIN] ?: true } // default true

    // Retrieve `isFirstVisit`
    val isFirstVisit: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_FIRST_VISIT] ?: false
    }

    // Retrieve `fullRoleName`
    val fullRoleName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[FULL_ROLE_NAME]
    }


    // Retrieve User Id
    val userId: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID]
    }
    val userTypeId: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[UserTypeId]
    }
    val roleId: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[RoleId]
    }

    // Retrieve Auth Token
    val authToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[AUTH_TOKEN_KEY]
    }

    // Retrieve Full Name
    val fullName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[FULL_NAME_KEY]
    }

    // Retrieve Email
    val email: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[EMAIL]
    }

    // Retrieve PhoneNo
    val phoneNo: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PHONE_NO]
    }

    // Retrieve ProfileImage
    val profileImage: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PROFILE_IMAGE]
    }

    // Retrieve RoleName
    val roleName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ROLE_NAME]
    }

    suspend fun clearAllButKeepRememberedCredentials() {
        val rememberMe = rememberMeFlow.first()
        if (rememberMe) {
            val savedEmail = savedEmailFlow.firstOrNull()
            val savedPassword = savedPasswordFlow.firstOrNull()

            context.dataStore.edit { it.clear() }

            context.dataStore.edit { preferences ->
                savedEmail?.let { preferences[PreferencesKeys.SAVED_EMAIL] = it }
                savedPassword?.let { preferences[PreferencesKeys.SAVED_PASSWORD] = it }
                preferences[PreferencesKeys.REMEMBER_ME] = true
            }
        } else {
            context.dataStore.edit { it.clear() }
        }
    }

    // Clear All User Data
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

object PreferencesKeys {
    val SAVED_EMAIL = stringPreferencesKey("saved_email")
    val SAVED_PASSWORD = stringPreferencesKey("saved_password")
    val REMEMBER_ME = booleanPreferencesKey("remember_me")

    // Add other keys as needed
}

