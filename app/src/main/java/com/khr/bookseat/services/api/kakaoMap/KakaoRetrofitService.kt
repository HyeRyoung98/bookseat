package com.khr.bookseat.services.api.kakaoMap

import com.khr.bookseat.objets.kakao.Coord2regioncodeRes
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface KakaoRetrofitService {

    @GET("v2/local/geo/coord2regioncode.json")
    fun coordToRegionCode(
        @Header("Authorization") authorization: String,
        @Query("x") longitude: Double,
        @Query("y") latitude: Double,
        @Query("input_coord") inputCoord: String = "WGS84"
    ): Call<Coord2regioncodeRes>

}