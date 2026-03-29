package ru.rustore.sdk.pushclient

import android.content.Context

/**
 * STUB IMPLEMENTATION
 * Replace with real RuStore Push SDK when building locally in Belarus/Russia.
 * 
 * Real SDK available from: https://repo.rustore.ru/
 * 
 * To use real SDK:
 * 1. Download pushclient-2.1.0.jar from RuStore
 * 2. Place in app/libs/ folder
 * 3. Remove this stub file
 * 4. Add to build.gradle: implementation files('libs/pushclient-2.1.0.jar')
 */
object RuStorePushClient {
    
    private var isInitialized = false
    private var projectId: String? = null
    
    @JvmStatic
    fun init(
        context: Context,
        projectId: String,
        internalPrefix: String = ""
    ) {
        this.projectId = projectId
        this.isInitialized = true
        android.util.Log.d("RuStorePushClient[STUB]", "Initialized with projectId: ${projectId.take(20)}...")
    }
    
    @JvmStatic
    fun getToken(): RuStoreTask<String?> {
        android.util.Log.d("RuStorePushClient[STUB]", "getToken() called")
        return RuStoreTaskImpl(null)
    }
    
    @JvmStatic
    fun deleteToken(): RuStoreTask<Unit> {
        android.util.Log.d("RuStorePushClient[STUB]", "deleteToken() called")
        return RuStoreTaskImpl(Unit)
    }
    
    @JvmStatic
    fun subscribeToTopic(topic: String): RuStoreTask<Unit> {
        android.util.Log.d("RuStorePushClient[STUB]", "subscribeToTopic: $topic")
        return RuStoreTaskImpl(Unit)
    }
    
    @JvmStatic
    fun unsubscribeFromTopic(topic: String): RuStoreTask<Unit> {
        android.util.Log.d("RuStorePushClient[STUB]", "unsubscribeFromTopic: $topic")
        return RuStoreTaskImpl(Unit)
    }
}
