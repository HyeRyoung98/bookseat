package com.khr.bookseat.objets

import com.squareup.moshi.Json

data class ApiResponse<T>(
    @field:Json(name = "header")
    val header: Header,
    @field:Json(name = "body")
    val body: Body<T>

)