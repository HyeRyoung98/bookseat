package com.khr.bookseat.objets

import com.squareup.moshi.Json

data class Body<T>(
    @field:Json(name = "item")
    val item: List<T>,
    @field:Json(name = "numOfRows")
    val numOfRows: Int,
    @field:Json(name = "pageNo")
    val pageNo: Int,
    @field:Json(name = "totalCount")
    val totalCount: Int
)