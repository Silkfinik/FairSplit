package com.silkfinik.fairsplit.core.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.silkfinik.fairsplit.core.data.mapper.asDomainModel
import com.silkfinik.fairsplit.core.data.mapper.asDto
import com.silkfinik.fairsplit.core.domain.repository.UserRepository
import com.silkfinik.fairsplit.core.model.User
import com.silkfinik.fairsplit.core.network.model.UserDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.silkfinik.fairsplit.core.common.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import androidx.core.graphics.scale


@Singleton
class FirebaseUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    @param:ApplicationContext private val context: Context
) : UserRepository {

    override fun getUser(uid: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        close() 
                    } else {
                        close(e)
                    }
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val dto = snapshot.toObject(UserDto::class.java)
                    trySend(dto?.asDomainModel())
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createOrUpdateUser(user: User) {
        val dto = user.asDto()

        val updates = mutableMapOf<String, Any?>()
        updates["uid"] = dto.uid
        updates["display_name"] = dto.displayName
        updates["photo_url"] = dto.photoUrl
        updates["is_anonymous"] = dto.isAnonymous
        updates["updated_at"] = dto.updatedAt
        
        if (dto.email != null) updates["email"] = dto.email
        if (dto.linkedGhostIds != null) updates["linked_ghost_ids"] = dto.linkedGhostIds
        if (dto.fcmToken != null) updates["fcm_token"] = dto.fcmToken

        if (dto.createdAt != null) {
            updates["created_at"] = dto.createdAt
        }

        firestore.collection("users").document(user.id)
            .set(updates, SetOptions.merge())
            .await()
    }

    override suspend fun userExists(uid: String): Boolean {
        val snapshot = firestore.collection("users").document(uid).get().await()
        return snapshot.exists()
    }

    override suspend fun updateFcmToken(uid: String, token: String?) {
        val updates = mapOf(
            "fcm_token" to token,
            "updated_at" to System.currentTimeMillis()
        )
        firestore.collection("users").document(uid)
            .set(updates, SetOptions.merge())
            .await()
    }

    override suspend fun deleteUser(uid: String) {
        firestore.collection("users").document(uid).delete().await()
    }

    override suspend fun uploadUserAvatar(uid: String, imageUri: String): Result<String> {
        return try {
            val uri = imageUri.toUri()

            val compressedBytes = withContext(Dispatchers.IO) {
                compressImage(uri)
            } ?: return Result.Error("Не удалось обработать изображение")

            val filename = "avatars/${uid}_${System.currentTimeMillis()}.webp"
            val ref = storage.reference.child(filename)

            ref.putBytes(compressedBytes).await()
            val downloadUrl = ref.downloadUrl.await().toString()

            Result.Success(downloadUrl)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Ошибка загрузки изображения", e)
        }
    }

    private fun compressImage(uri: Uri): ByteArray? {
        return try {
            val contentResolver = context.contentResolver

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            options.inSampleSize = calculateInSampleSize(options, 600, 600)
            options.inJustDecodeBounds = false

            var bitmap = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null

            val orientation = contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL

            bitmap = rotateBitmap(bitmap, orientation)

            if (max(bitmap.width, bitmap.height) > 600) {
                val ratio = 600.0 / max(bitmap.width, bitmap.height)
                val width = (bitmap.width * ratio).toInt()
                val height = (bitmap.height * ratio).toInt()
                bitmap = bitmap.scale(width, height)
            }

            val outputStream = ByteArrayOutputStream()
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }

            bitmap.compress(format, 75, outputStream)
            bitmap.recycle()

            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) bitmap.recycle()
            rotated
        } catch (e: OutOfMemoryError) {
            bitmap
        }
    }
}
