package com.sportynix.app.data.remote.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

/** The team endpoints shared by the React client and Android client. */
interface TeamApiService {
    @GET("api/teams/") suspend fun myTeams(): Response<JsonElement>
    @GET("api/teams/discover/") suspend fun discoverTeams(
        @Query("limit") limit: Int = 100,
        @Query("latitude") latitude: String? = null,
        @Query("longitude") longitude: String? = null
    ): Response<JsonElement>
    @GET("api/teams/my_invitations/") suspend fun receivedInvitations(): Response<JsonElement>
    @GET("api/teams/my_sent_invitations/") suspend fun sentInvitations(): Response<JsonElement>
    @GET("api/teams/{id}/") suspend fun details(@Path("id") id: Int): Response<JsonElement>
    @POST("api/teams/{id}/get_team_chat/") suspend fun teamChat(@Path("id") id: Int): Response<JsonElement>
    @GET("api/teams/{id}/pending_members/") suspend fun pendingMembers(@Path("id") id: Int): Response<JsonElement>
    @GET("api/teams/{id}/search_members/") suspend fun searchMembers(@Path("id") id: Int, @Query("q") query: String): Response<JsonElement>
    @GET("api/teams/{id}/join_status/") suspend fun joinStatus(@Path("id") id: Int): Response<JsonElement>
    @POST("api/teams/{id}/request_join/") suspend fun requestJoin(@Path("id") id: Int): Response<JsonElement>
    @POST("api/teams/{id}/cancel_join_request/") suspend fun cancelJoin(@Path("id") id: Int): Response<JsonElement>
    @POST("api/teams/{id}/accept_invitation/") suspend fun acceptInvitation(@Path("id") id: Int, @Body body: JsonObject): Response<JsonElement>
    @POST("api/teams/{id}/reject_invitation/") suspend fun rejectInvitation(@Path("id") id: Int, @Body body: JsonObject): Response<JsonElement>
    @POST("api/teams/{id}/invite/") suspend fun invite(@Path("id") id: Int, @Body body: JsonObject): Response<JsonElement>
    @POST("api/teams/{id}/approve_membership/") suspend fun approveMembership(@Path("id") id: Int, @Body body: JsonObject): Response<JsonElement>
    @POST("api/teams/{id}/reject_membership/") suspend fun rejectMembership(@Path("id") id: Int, @Body body: JsonObject): Response<JsonElement>
    @POST("api/teams/{id}/remove_member/") suspend fun removeMember(@Path("id") id: Int, @Body body: JsonObject): Response<JsonElement>
    @POST("api/teams/{id}/add_admin/") suspend fun addAdmin(@Path("id") id: Int, @Body body: JsonObject): Response<JsonElement>
    @POST("api/teams/{id}/remove_admin/") suspend fun removeAdmin(@Path("id") id: Int, @Body body: JsonObject): Response<JsonElement>
    @POST("api/teams/{id}/leave/") suspend fun leave(@Path("id") id: Int): Response<JsonElement>
    @POST("api/teams/{id}/force_delete/") suspend fun forceDelete(@Path("id") id: Int): Response<JsonElement>
    @DELETE("api/teams/{id}/") suspend fun delete(@Path("id") id: Int): Response<JsonElement>
    @POST("api/teams/{id}/generate_invite_link/") suspend fun generateInviteLink(@Path("id") id: Int): Response<JsonElement>
    @GET("api/teams/invite-link/resolve/") suspend fun resolveInvite(@Query("token") token: String): Response<JsonElement>
    @POST("api/teams/invite-link/request_join/") suspend fun requestInviteJoin(@Body body: JsonObject): Response<JsonElement>

    @Multipart
    @POST("api/teams/")
    suspend fun create(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part logo: MultipartBody.Part? = null,
        @Part cover_image: MultipartBody.Part? = null
    ): Response<JsonElement>

    @Multipart
    @PATCH("api/teams/{id}/")
    suspend fun update(
        @Path("id") id: Int,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part logo: MultipartBody.Part? = null,
        @Part cover_image: MultipartBody.Part? = null
    ): Response<JsonElement>
}
