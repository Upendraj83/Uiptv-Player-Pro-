package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ThemePalette
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class UserProfileRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureProfileExists_validUser_createsAndReturnsProfile() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = UserProfileRepository(firestore, auth)

    val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureProfileExists(ALICE_EMAIL, "Alice Viewer", null)
    }

    assertEquals(uid, profile.userId)
    assertEquals(ALICE_EMAIL, profile.email)
    val fetched = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getProfile(uid) }
    assertNotNull(fetched)
    assertEquals(uid, fetched?.userId)
  }

  @Test
  fun updatePreferences_authenticatedOwner_updatesThemeFavoritesAndRecents() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = UserProfileRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureProfileExists(ALICE_EMAIL, "Alice Viewer", null)
    }

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updatePreferences(
        theme = ThemePalette.AMOLED,
        favorites = listOf("bbc_news", "nasa_tv"),
        recentlyViewed = listOf("nasa_tv"),
        allowAdultChannels = true,
      )
    }

    val updated = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getProfile(uid) }
    assertNotNull(updated)
    assertEquals("amoled", updated?.theme)
    assertEquals(listOf("bbc_news", "nasa_tv"), updated?.favorites)
    assertEquals(listOf("nasa_tv"), updated?.recentlyViewed)
    assertTrue(updated?.allowAdultChannels == true)
  }

  @Test
  fun observeProfile_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = UserProfileRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureProfileExists(ALICE_EMAIL, "Alice Viewer", null)
    }
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updatePreferences(favorites = listOf("france24"))
    }

    val observed = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeProfile(uid).first { it != null && it.favorites.contains("france24") }
    }
    assertNotNull(observed)
    assertTrue(observed?.favorites?.contains("france24") == true)
  }

  @Test
  fun getProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = UserProfileRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.ensureProfileExists(ALICE_EMAIL, "Alice Viewer", null)
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = UserProfileRepository(firestore, auth)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getProfile(aliceUid) }
      fail("Expected PERMISSION_DENIED when Bob reads Alice's profile")
    } catch (e: Exception) {
      val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
        "Expected FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }
  }

  @Test
  fun observeProfile_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = UserProfileRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeProfile("some_uid_123").first() }
      fail("Expected PERMISSION_DENIED when unauthenticated user observes profile")
    } catch (e: Exception) {
      val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
        "Expected nested FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice_tiwee@test.com"
    const val BOB_EMAIL = "bob_tiwee@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
