package ru.rustore.sdk.pushclient

/**
 * STUB IMPLEMENTATION
 * RuStoreTask interface for async operations
 */
interface RuStoreTask<T> {
    fun addOnSuccessListener(listener: (T) -> Unit): RuStoreTask<T>
    fun addOnFailureListener(listener: (Throwable) -> Unit): RuStoreTask<T>
}

/**
 * STUB IMPLEMENTATION
 * Simple implementation of RuStoreTask
 */
class RuStoreTaskImpl<T>(private val result: T) : RuStoreTask<T> {
    override fun addOnSuccessListener(listener: (T) -> Unit): RuStoreTask<T> {
        listener(result)
        return this
    }
    
    override fun addOnFailureListener(listener: (Throwable) -> Unit): RuStoreTask<T> {
        // No error in stub
        return this
    }
}
