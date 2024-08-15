package com.example.controlderefaccionesvestible.api

import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    //https://quintaesencia.website/api/v1/notificacion
    @GET("127.0.0.1/api/v1/notificacion")
    fun getResponse(): Call<ApiResponse>
}
