package com.sportynix.app.domain.repository

import com.sportynix.app.core.network.ApiResult
import com.sportynix.app.domain.model.Announcement

interface AnnouncementRepository {
    suspend fun getAnnouncements(): ApiResult<List<Announcement>>
}
