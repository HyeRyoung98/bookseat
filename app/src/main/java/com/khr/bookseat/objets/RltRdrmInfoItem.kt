package com.khr.bookseat.objets

import com.squareup.moshi.Json

data class RltRdrmInfoItem(
    @field:Json(name = "bldgFlrExpln")
    val bldgFlrExpln: String,
    @field:Json(name = "lclgvNm")
    val lclgvNm: String,
    @field:Json(name = "nowVstrCnt")
    val nowVstrCnt: String,
    @field:Json(name = "pblibId")
    val pblibId: String,
    @field:Json(name = "pblibNm")
    val pblibNm: String,
    @field:Json(name = "rdrmId")
    val rdrmId: String,
    @field:Json(name = "rdrmNm")
    val rdrmNm: String,
    @field:Json(name = "rdrmNo")
    val rdrmNo: String,
    @field:Json(name = "rdrmTypeNm")
    val rdrmTypeNm: String,
    @field:Json(name = "rmndSeatCnt")
    val rmndSeatCnt: String,
    @field:Json(name = "rsvtSeatCnt")
    val rsvtSeatCnt: String,
    @field:Json(name = "stdgCd")
    val stdgCd: String,
    @field:Json(name = "totDt")
    val totDt: String,
    @field:Json(name = "tseatCnt")
    val tseatCnt: String,
    @field:Json(name = "useSeatCnt")
    val useSeatCnt: String
)