package com.khr.bookseat.objets

data class LibraryBottomSheetData(
    val info: InfoItem,
    val rltRdrmInfo: List<RltRdrmInfoItem>? = null
)