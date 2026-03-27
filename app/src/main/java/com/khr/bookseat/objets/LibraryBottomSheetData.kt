package com.khr.bookseat.objets

data class LibraryBottomSheetData(
    val info: InfoItem,
    val prstInfo: PrstInfoItem? = null,
    val rltRdrmInfo: RltRdrmInfoItem? = null
)