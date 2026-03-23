package com.khr.bookseat.objets

import com.squareup.moshi.Json

data class PrstInfoItem(
    @field:Json(name = "lclgvNm")
    val lclgvNm: String,
    @field:Json(name = "operSttsNm")
    val operSttsNm: String,
    @field:Json(name = "pblibId")
    val pblibId: String,
    @field:Json(name = "pblibNm")
    val pblibNm: String,
    @field:Json(name = "rsvtPsbltyYn")
    val rsvtPsbltyYn: String,
    @field:Json(name = "seatUsgrt")
    val seatUsgrt: String,
    @field:Json(name = "stdgCd")
    val stdgCd: String,
    @field:Json(name = "tdyUseSeatCnt")
    val tdyUseSeatCnt: String,
    @field:Json(name = "tdyVstrCnt")
    val tdyVstrCnt: String,
    @field:Json(name = "totCrtrYmd")
    val totCrtrYmd: String,
    @field:Json(name = "utztnPsbltyRdrmCnt")
    val utztnPsbltyRdrmCnt: String
)