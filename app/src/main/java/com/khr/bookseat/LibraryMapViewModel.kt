package com.khr.bookseat

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakao.vectormap.LatLng
import com.khr.bookseat.objets.ApiResponse
import com.khr.bookseat.objets.InfoItem
import com.khr.bookseat.objets.LibraryBottomSheetData
import com.khr.bookseat.objets.RltRdrmInfoItem
import com.khr.bookseat.objets.kakao.Coord2regioncodeRes
import com.khr.bookseat.services.api.RetrofitInstance
import com.khr.bookseat.services.api.kakaoMap.KakaoRetrofit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LibraryMapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryMapUiState())
    val uiState: StateFlow<LibraryMapUiState> = _uiState.asStateFlow()

    fun updateCurrentPosition(position: LatLng) {
        _uiState.value = _uiState.value.copy(currentPosition = position)
    }

    fun dismissBottomSheet() {
        _uiState.value = _uiState.value.copy(
            showBottomSheet = false,
            selectedLibraryData = null
        )
    }

    fun showLibraryBottomSheetById(libraryId: String) {
        val info = _uiState.value.infoList.find { it.pblibId == libraryId } ?: return
        val rlt = _uiState.value.rltRdrmInfoList.filter { it.pblibId == libraryId }

        _uiState.value = _uiState.value.copy(
            selectedLibraryData = LibraryBottomSheetData(
                info = info,
                rltRdrmInfo = rlt
            ),
            showBottomSheet = true
        )
    }

    fun searchLibrariesByMapCenter(
        currentPosition: LatLng?,
        startPosition: LatLng
    ) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            infoList = emptyList(),
            rltRdrmInfoList = emptyList()
        )

        val service = KakaoRetrofit.kakaoRetrofitService
        service.coordToRegionCode(
            "KakaoAK $KAKAO_API_KEY",
            currentPosition?.longitude ?: startPosition.longitude,
            currentPosition?.latitude ?: startPosition.latitude
        ).enqueue(object : Callback<Coord2regioncodeRes> {
            override fun onResponse(
                call: Call<Coord2regioncodeRes>,
                response: Response<Coord2regioncodeRes>
            ) {
                if (!response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "지역 조회 실패"
                    )
                    return
                }

                val regionCode = response.body()
                    ?.documents
                    ?.firstOrNull { it.regionType == "B" }
                    ?.code
                    .orEmpty()

                if (regionCode.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "지역 코드를 찾지 못했어요."
                    )
                    return
                }

                getRltRdrmInfoData(regionCode)
            }

            override fun onFailure(call: Call<Coord2regioncodeRes>, t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = t.message ?: "지역 조회 중 오류"
                )
            }
        })
    }

    private fun getRltRdrmInfoData(pStdgCd: String) {
        val siCode = pStdgCd.take(4) + "000000"

        RetrofitInstance.retrofitService
            .getRltRdrmInfoData(DATA_API_KEY, "1", "100", "JSON", siCode)
            .enqueue(object : Callback<ApiResponse<RltRdrmInfoItem>> {
                override fun onResponse(
                    call: Call<ApiResponse<RltRdrmInfoItem>>,
                    response: Response<ApiResponse<RltRdrmInfoItem>>
                ) {
                    if (!response.isSuccessful) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "열람실 정보 조회 실패"
                        )
                        return
                    }

                    val roomList = response.body()?.body?.item ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        rltRdrmInfoList = roomList
                    )

                    getInfoData(siCode)
                }

                override fun onFailure(call: Call<ApiResponse<RltRdrmInfoItem>>, t: Throwable) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = t.message ?: "열람실 정보 조회 중 오류"
                    )
                }
            })
    }

    private fun getInfoData(siCode: String) {
        RetrofitInstance.retrofitService
            .getInfoData(DATA_API_KEY, "1", "100", "JSON", siCode)
            .enqueue(object : Callback<ApiResponse<InfoItem>> {
                override fun onResponse(
                    call: Call<ApiResponse<InfoItem>>,
                    response: Response<ApiResponse<InfoItem>>
                ) {
                    if (!response.isSuccessful) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "도서관 정보 조회 실패"
                        )
                        return
                    }

                    val resultCode = response.body()?.header?.resultCode
                    if (resultCode.equals("K0")) {
                        val infoList = response.body()?.body?.item ?: emptyList()

                        _uiState.value = _uiState.value.copy(
                            infoList = infoList,
                            isLoading = false,
                            errorMessage = null
                        )
                    } else if (resultCode.equals("K3")) {
                        _uiState.value = _uiState.value.copy(
                            infoList = emptyList(),
                            isLoading = false,
                            errorMessage = "조회된 정보가 없습니다."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "에러가 발생했습니다."
                        )
                    }
                }

                override fun onFailure(call: Call<ApiResponse<InfoItem>>, t: Throwable) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = t.message ?: "도서관 정보 조회 중 오류"
                    )
                }
            })
    }
}