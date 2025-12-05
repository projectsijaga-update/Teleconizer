package com.teleconizer.app.data.firestore

/*
 * FirestoreService - DISABLED
 * 
 * This service has been disabled in favor of Firebase Realtime Database.
 * All Firestore functionality has been migrated to RealtimeDatabaseService.
 * 
 * The original Firestore implementation has been commented out to ensure
 * no Firestore dependencies remain in the codebase.
 * 
 * If you need to re-enable Firestore, you would need to:
 * 1. Add firebase-firestore-ktx dependency back to build.gradle.kts
 * 2. Uncomment the original implementation code
 * 3. Update ViewModels to use FirestoreService instead of RealtimeDatabaseService
 * 
 * DEPRECATED - DO NOT USE
 */

// Original implementation commented out - using Realtime Database instead
/*
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.teleconizer.app.data.model.Patient
import com.teleconizer.app.data.model.DeviceStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreService {
    // All methods commented out - using RealtimeDatabaseService instead
}
*/
