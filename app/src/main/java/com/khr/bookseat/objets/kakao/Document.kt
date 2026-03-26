package com.khr.bookseat.objets.kakao

import com.squareup.moshi.Json

data class Document(
    @param:Json(name = "region_type")
    val regionType: String,     //H(행정동) 또는 B(법정동)
    @param:Json(name = "address_name")
    val addressName: String,
    @param:Json(name = "region_1depth_name")
    val region1depthName: String,
    @param:Json(name = "region_2depth_name")
    val region2depthName: String,
    @param:Json(name = "region_3depth_name")
    val region3depthName: String,
    @param:Json(name = "region_4depth_name")
    val region4depthName: String,
    @param:Json(name = "code")
    val code: String,
    @param:Json(name = "x")
    val x: Double,
    @param:Json(name = "y")
    val y: Double,
)