package com.example.data.repository

import com.example.data.model.ThemePalette
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserProfileRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  fun observeProfile(userId: String): Flow<UserProfile?> {
    val path = "users/$userId"
    return db
      .collection("users")
      .document(userId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.GET, path, auth)
        }
        throw error
      }
  }

  suspend fun getProfile(userId: String): UserProfile? {
    val path = "users/$userId"
    return try {
      val snapshot = db.collection("users").document(userId).get().await()
      if (snapshot.exists()) {
        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      } else {
        null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path, auth)
      throw e
    }
  }

  suspend fun ensureProfileExists(
    email: String?,
    displayName: String?,
    photoUrl: String?,
  ): UserProfile {
    val uid = requireUserId()
    val path = "users/$uid"
    val docRef = db.collection("users").document(uid)
    return try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        existing.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          ?: UserProfile(id = uid, userId = uid, email = email ?: "$uid@tiwee.app")
      } else {
        val safeEmail = email?.takeIf { it.isNotBlank() }?.take(256) ?: "$uid@tiwee.app"
        val safeName = displayName?.take(100) ?: "UiPTv Viewer"
        val safePhoto = photoUrl?.take(1000) ?: ""
        val payload =
          mapOf(
            "userId" to uid,
            "email" to safeEmail,
            "displayName" to safeName,
            "photoURL" to safePhoto,
            "theme" to ThemePalette.DARK.value,
            "favorites" to emptyList<String>(),
            "recentlyViewed" to emptyList<String>(),
            "allowAdultChannels" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(payload).await()
        UserProfile(
          id = uid,
          userId = uid,
          email = safeEmail,
          displayName = safeName,
          photoURL = safePhoto,
          theme = ThemePalette.DARK.value,
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path, auth)
      throw e
    }
  }

  suspend fun updatePreferences(
    theme: ThemePalette? = null,
    favorites: List<String>? = null,
    recentlyViewed: List<String>? = null,
    allowAdultChannels: Boolean? = null,
  ) {
    val uid = requireUserId()
    val path = "users/$uid"
    val docRef = db.collection("users").document(uid)
    val updates =
      mutableMapOf<String, Any>(
        "updatedAt" to FieldValue.serverTimestamp()
      )
    if (theme != null) updates["theme"] = theme.value
    if (favorites != null) updates["favorites"] = favorites.distinct().take(200)
    if (recentlyViewed != null) updates["recentlyViewed"] = recentlyViewed.distinct().take(30)
    if (allowAdultChannels != null) updates["allowAdultChannels"] = allowAdultChannels

    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path, auth)
      throw e
    }
  }
}
