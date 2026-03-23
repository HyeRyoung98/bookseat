package com.khr.bookseat.services.api

import com.khr.bookseat.objets.ApiResponse
import com.khr.bookseat.objets.InfoItem
import com.khr.bookseat.objets.PrstInfoItem
import com.khr.bookseat.objets.RltRdrmInfoItem
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface RetrofitService {
    @GET("info_v2")
    fun getInfoData(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: String,
        @Query("numOfRows") numOfRows: String,
        @Query("type") type: String,
        @Query("stdgCd") stdgCd: String,
    ) : Call<ApiResponse<InfoItem>>

    @GET("prst_info_v2")
    fun getPrstInfoData(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: String,
        @Query("numOfRows") numOfRows: String,
        @Query("type") type: String,
        @Query("stdgCd") stdgCd: String,
        @Query("fromCrtrYmd") fromCrtrYmd: String,
        @Query("toCrtrYmd") toCrtrYmd: String
    ) : Call<ApiResponse<PrstInfoItem>>

    @GET("rlt_rdrm_info_v2")
    fun getRltRdrmInfoData(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: String,
        @Query("numOfRows") numOfRows: String,
        @Query("type") type: String,
        @Query("stdgCd") stdgCd: String,
    ) : Call<ApiResponse<RltRdrmInfoItem>>
}