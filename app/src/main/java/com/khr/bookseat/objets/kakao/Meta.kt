package com.khr.bookseat.objets.kakao

import com.squareup.moshi.Json

data class Meta(
    @param:Json(name = "total_count")
    val totalCount: Int
)