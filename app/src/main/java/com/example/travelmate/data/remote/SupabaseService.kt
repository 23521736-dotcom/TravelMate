package com.example.travelmate.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseService @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val bucketName = "Travelmate_images"

    suspend fun uploadImage(path: String, byteArray: ByteArray): String {
        val bucket = supabaseClient.storage.from(bucketName)
        bucket.upload(path, byteArray, upsert = true)
        return bucket.publicUrl(path)
    }
}
