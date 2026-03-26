package com.khr.bookseat.objets.kakao

import com.squareup.moshi.Json

data class Coord2regioncodeRes(
    @param:Json(name = "meta")
    val meta: Meta,
    @param:Json(name = "documents")
    val documents: List<Document>
)