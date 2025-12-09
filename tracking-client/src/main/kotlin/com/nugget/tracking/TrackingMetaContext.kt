package com.nugget.tracking

/**
 * Thread-local context for enriching outbound monitoring logs with ad-hoc metadata.
 */
object TrackingMetaContext {
    private val attributeStore: ThreadLocal<MutableMap<String, Any?>> =
        ThreadLocal.withInitial { mutableMapOf<String, Any?>() }

    fun put(key: String, value: Any?) {
        attributeStore.get()[key] = value
    }

    fun remove(key: String) {
        attributeStore.get().remove(key)
    }

    fun clear() {
        attributeStore.get().clear()
    }

    fun snapshot(): Map<String, Any?>? {
        val snapshot = attributeStore.get()
        return if (snapshot.isEmpty()) null else snapshot.toMap()
    }
}
