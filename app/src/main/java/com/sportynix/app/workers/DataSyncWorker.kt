package com.sportynix.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sportynix.app.domain.repository.BookingRepository
import com.sportynix.app.domain.repository.VenueRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class DataSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val venueRepository: VenueRepository,
    private val bookingRepository: BookingRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting background data sync...")
            venueRepository.fetchVenues()
            bookingRepository.fetchUserBookings()
            Timber.d("Background data sync completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Background sync failed.")
            Result.retry()
        }
    }
}
