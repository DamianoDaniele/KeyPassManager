package com.personal.keypassmanager.drive

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface GoogleDriveApi {
    @Multipart
    @POST("upload/drive/v3/files?uploadType=multipart")
    suspend fun uploadFile(
        @Part("metadata") metadata: RequestBody, // JSON metadata
        @Part file: MultipartBody.Part, // file binario
        @Header("Authorization") auth: String
    ): Response<ResponseBody>

    @GET("drive/v3/files")
    suspend fun listFiles(
        @Query("q") query: String? = null, // ricerca avanzata
        @Query("spaces") spaces: String? = null,
        @Query("fields") fields: String = "files(id,name,modifiedTime,size)",
        @Query("pageSize") pageSize: Int = 100,
        @Query("pageToken") pageToken: String? = null,
        @Query("orderBy") orderBy: String = "modifiedTime desc",
        @Header("Authorization") auth: String,
        @Header("X-RateLimit-Limit") rateLimit: Int = 1000
    ): Response<ResponseBody>

    @GET("drive/v3/files/{fileId}?alt=media")
    suspend fun downloadFile(
        @Path("fileId") fileId: String,
        @Header("Authorization") auth: String,
        @Header("Range") range: String? = null
    ): Response<ResponseBody>

    @DELETE("drive/v3/files/{fileId}")
    suspend fun deleteFile(
        @Path("fileId") fileId: String,
        @Header("Authorization") auth: String
    ): Response<ResponseBody>
}
