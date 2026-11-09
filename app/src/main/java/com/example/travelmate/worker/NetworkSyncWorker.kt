package com.example.travelmate.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.travelmate.data.local.ExpenseDao
import com.example.travelmate.data.local.ReviewDao
import com.example.travelmate.data.local.SyncStatus
import com.example.travelmate.data.local.TripDao
import com.example.travelmate.data.model.toDomainModel
import com.example.travelmate.domain.repository.ReviewRepository
import com.example.travelmate.domain.repository.TripRepository
import com.example.travelmate.util.NetworkResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class NetworkSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val tripDao: TripDao,
    private val reviewDao: ReviewDao,
    private val expenseDao: ExpenseDao,
    private val tripRepository: TripRepository,
    private val reviewRepository: ReviewRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        android.util.Log.d("NetworkSyncWorker", "Starting offline sync...")
        var allSuccess = true

        // 1. Sync Trips
        val pendingTrips = tripDao.getPendingSyncTrips()
        for (tripEntity in pendingTrips) {
            when (tripEntity.syncStatus) {
                SyncStatus.PENDING_CREATE -> {
                    val trip = tripEntity.toDomainModel()
                    val result = tripRepository.createTrip(trip).first { it !is NetworkResult.Loading }
                    if (result is NetworkResult.Success) {
                        tripDao.updateSyncStatus(trip.id, SyncStatus.SYNCED)
                    } else {
                        allSuccess = false
                    }
                }
                SyncStatus.PENDING_UPDATE -> {
                    val trip = tripEntity.toDomainModel()
                    val result = tripRepository.updateTrip(trip).first { it !is NetworkResult.Loading }
                    if (result is NetworkResult.Success) {
                        tripDao.updateSyncStatus(trip.id, SyncStatus.SYNCED)
                    } else {
                        allSuccess = false
                    }
                }
                SyncStatus.PENDING_DELETE -> {
                    val result = tripRepository.deleteTrip(tripEntity.id).first { it !is NetworkResult.Loading }
                    if (result is NetworkResult.Success) {
                        tripDao.deleteTrip(tripEntity.id)
                    } else {
                        allSuccess = false
                    }
                }
                else -> {}
            }
        }

        // 2. Sync Reviews
        val pendingReviews = reviewDao.getPendingSyncReviews()
        for (reviewEntity in pendingReviews) {
            if (reviewEntity.syncStatus == SyncStatus.PENDING_CREATE) {
                val review = reviewEntity.toDomainModel()
                
                // Upload local images first if any
                val uploadedUrls = review.imageUrls.toMutableList()
                var uploadFailed = false
                
                for (localUri in reviewEntity.localImageUris) {
                    try {
                        // Normally we would read the file bytes here.
                        // Assuming we have a way to get bytes from localUri
                        // For simplicity, we just skip if we can't read it.
                        // In a full implementation, we'd use contentResolver.
                        val uri = android.net.Uri.parse(localUri)
                        val bytes = applicationContext.contentResolver.openInputStream(uri)?.readBytes()
                        if (bytes != null) {
                            val path = "reviews/${review.destinationId}/${System.currentTimeMillis()}.jpg"
                            val uploadResult = tripRepository.uploadImage(path, bytes).first { it !is NetworkResult.Loading }
                            if (uploadResult is NetworkResult.Success) {
                                uploadedUrls.add(uploadResult.data)
                            } else {
                                uploadFailed = true
                                break
                            }
                        }
                    } catch (e: Exception) {
                        uploadFailed = true
                    }
                }
                
                if (!uploadFailed) {
                    val finalReview = review.copy(imageUrls = uploadedUrls)
                    val result = reviewRepository.addReview(finalReview).first { it !is NetworkResult.Loading }
                    if (result is NetworkResult.Success) {
                        reviewDao.updateSyncStatus(reviewEntity.id, SyncStatus.SYNCED)
                    } else {
                        allSuccess = false
                    }
                } else {
                    allSuccess = false
                }
            }
        }

        // 3. Sync Expenses
        val pendingExpenses = expenseDao.getPendingSyncExpenses()
        for (expenseEntity in pendingExpenses) {
            when (expenseEntity.syncStatus) {
                SyncStatus.PENDING_CREATE -> {
                    val expense = expenseEntity.toDomainModel()
                    val result = tripRepository.addExpense(expense).first { it !is NetworkResult.Loading }
                    if (result is NetworkResult.Success) {
                        expenseDao.updateSyncStatus(expense.id, SyncStatus.SYNCED)
                    } else {
                        allSuccess = false
                    }
                }
                SyncStatus.PENDING_UPDATE -> {
                    val expense = expenseEntity.toDomainModel()
                    val result = tripRepository.updateExpense(expense).first { it !is NetworkResult.Loading }
                    if (result is NetworkResult.Success) {
                        expenseDao.updateSyncStatus(expense.id, SyncStatus.SYNCED)
                    } else {
                        allSuccess = false
                    }
                }
                SyncStatus.PENDING_DELETE -> {
                    val result = tripRepository.deleteExpense(expenseEntity.id).first { it !is NetworkResult.Loading }
                    if (result is NetworkResult.Success) {
                        expenseDao.deleteExpense(expenseEntity.id)
                    } else {
                        allSuccess = false
                    }
                }
                else -> {}
            }
        }

        android.util.Log.d("NetworkSyncWorker", "Offline sync completed. allSuccess=$allSuccess")
        return if (allSuccess) Result.success() else Result.retry()
    }
}
