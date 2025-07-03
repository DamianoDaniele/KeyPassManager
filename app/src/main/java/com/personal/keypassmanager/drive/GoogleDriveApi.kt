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
        @Part metadata: MultipartBody.Part,
        @Part file: MultipartBody.Part,
        @Header("Authorization") auth: String
    ): Response<ResponseBody>

    @GET("drive/v3/files")
    suspend fun listFiles(
        @Query("spaces") spaces: String = "appDataFolder",
        @Query("fields") fields: String = "files(id,name)",
        @Header("Authorization") auth: String
    ): Response<ResponseBody>

    @GET("drive/v3/files/{fileId}?alt=media")
    suspend fun downloadFile(
        @Path("fileId") fileId: String,
        @Header("Authorization") auth: String
    ): Response<ResponseBody>
}
