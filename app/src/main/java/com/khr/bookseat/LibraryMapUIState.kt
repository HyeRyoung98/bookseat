package com.khr.bookseat

import com.kakao.vectormap.LatLng
import com.khr.bookseat.objets.InfoItem
import com.khr.bookseat.objets.LibraryBottomSheetData
import com.khr.bookseat.objets.RltRdrmInfoItem

data class LibraryMapUiState(
    val currentPosition: LatLng? = null,
    val infoList: List<InfoItem> = emptyList(),
    val rltRdrmInfoList: List<RltRdrmInfoItem> = emptyList(),
    val selectedLibraryData: LibraryBottomSheetData? = null,
    val showBottomSheet: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)