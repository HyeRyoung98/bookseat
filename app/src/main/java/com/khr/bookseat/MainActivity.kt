package com.khr.bookseat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTransition
import com.kakao.vectormap.label.Transition
import com.khr.bookseat.objets.ApiResponse
import com.khr.bookseat.objets.InfoItem
import com.khr.bookseat.objets.PrstInfoItem
import com.khr.bookseat.objets.RltRdrmInfoItem
import com.khr.bookseat.objets.kakao.Coord2regioncodeRes
import com.khr.bookseat.services.api.RetrofitInstance
import com.khr.bookseat.services.api.kakaoMap.KakaoRetrofit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        val thisContext = this
        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .background(Color.Gray)
            ) {

                KakaoMapScreen(getLifeCycleCallback(thisContext), getReadyCallback(thisContext))
                ResearchButton(
                    onClick = { onClickResearchButton() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                MyLocationButton(
                    onClick = { fetchCurrentLocation() },
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }

        requestLocationAndFetch()

        //retrofitWork()
        //requestLocationAndFetch()
    }


    private val startZoomLevel = 15
    private val startPosition = LatLng.from(37.394660, 127.111182) // 판교역
    private var currentPosition: LatLng? = null    // 현재 위치
    private var dongSearchJob: Job? = null
    private var infoList: List<InfoItem>? = null
    private var prstInfoList : List<PrstInfoItem>? = null
    private var rltRdrmInfoList : List<RltRdrmInfoItem>? = null



    /** 현재 위치 가져오기(구글) */
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    // 권한 요청 런처 (Activity Result API)
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                    (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)

            if (!granted) {
                Toast.makeText(this, "위치 권한이 없어 현재 위치를 가져올 수 없어요.", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            // 권한 허용됨 -> 위치 가져오기
            fetchCurrentLocation()
        }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation() {
        // 마지막으로 알려진 위치(빠르고 배터리 적음). 단, null 일 수 있음.
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location == null) {
                    Toast.makeText(
                        this,
                        "현재 위치를 가져오지 못했어요. GPS를 켜고 다시 시도해 주세요.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }
                val lat = location.latitude
                val lng = location.longitude

                Toast.makeText(this, "현재 위치: $lat, $lng", Toast.LENGTH_SHORT).show()
                currentPosition = LatLng.from(lat, lng)
                currentPosition?.let { moveCameraTo(it) }
            }
            .addOnFailureListener {
                Toast.makeText(this, "위치 조회 실패: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun requestLocationAndFetch() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            fetchCurrentLocation()
        } else {
            // 권한 요청
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }


    /** 카카오맵 */
    private var kakaoMapRef: KakaoMap? = null
    private var labelLayer: LabelLayer? = null
    private fun moveCameraTo(pos: LatLng) {
        val cameraUpdate = CameraUpdateFactory.newCenterPosition(pos, startZoomLevel)
        kakaoMapRef?.moveCamera(cameraUpdate)
    }

    private fun getReadyCallback(context: Context): KakaoMapReadyCallback {
        return object : KakaoMapReadyCallback() {
            override fun onMapReady(kakaoMap: KakaoMap) {
                kakaoMapRef = kakaoMap
                labelLayer = kakaoMap.getLabelManager()!!.getLayer()
                // 전달받은 context를 사용
                Toast.makeText(context, "지도가 준비되었습니다.", Toast.LENGTH_SHORT).show()
                currentPosition?.let { moveCameraTo(it) }

//                // 초기 중심 좌표도 한 번 조회
//                val initialPosition = kakaoMap.cameraPosition!!.position
//                updateRegionByMapCenter(initialPosition)
//
                // 사용자가 지도 이동을 마쳤을 때 현재 화면 중앙 좌표 기준으로 동 조회
                kakaoMap.setOnCameraMoveEndListener { _, cameraPosition, _ ->
                    currentPosition = cameraPosition.position
                    //updateRegionByMapCenter(center)
                }
            }

            override fun getPosition(): LatLng {
                return startPosition
            }

            override fun getZoomLevel(): Int {
                return startZoomLevel
            }
        }
    }

    private fun getLifeCycleCallback(context: Context): MapLifeCycleCallback {
        return object : MapLifeCycleCallback() {
            override fun onMapResumed() {
                super.onMapResumed()
            }

            override fun onMapPaused() {
                super.onMapPaused()
            }

            override fun onMapDestroy() {
            }

            override fun onMapError(error: Exception) {
            }
        }
    }


    private fun onClickResearchButton() {
        infoList = null
        labelLayer?.removeAll()
        getRegionByMapCenter()
    }

    fun getTodayYyyyMMdd(): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return sdf.format(Date())
    }

    /** api */
    private fun getInfoData(pStdgCd: String) {
        if (pStdgCd.isEmpty()) return
        val siCode = pStdgCd.take(4) + "000000"
        Log.d("#############pStdgCd###siCode##", "$pStdgCd//$siCode")
        val service = RetrofitInstance.retrofitService
        service.getInfoData(DATA_API_KEY, "1", "100", "JSON", siCode)
            .enqueue(object : retrofit2.Callback<ApiResponse<InfoItem>> {
                override fun onResponse(
                    call: Call<ApiResponse<InfoItem>>,
                    response: Response<ApiResponse<InfoItem>>
                ) {
                    if (response.isSuccessful) {
                        val resultCode = response.body()?.header?.resultCode
                        if (resultCode.equals("K0")) {
                            Log.d("[getInfoData]request:success", response.body().toString())

                            infoList = response.body()?.body?.item
                            if (infoList != null) {

                                val styles = kakaoMapRef?.getLabelManager()
                                    ?.addLabelStyles(
                                        LabelStyles.from(
                                            LabelStyle.from(R.drawable.pink_marker)
                                                .setIconTransition(
                                                    LabelTransition.from(
                                                        Transition.None,
                                                        Transition.None
                                                    )
                                                )
                                        )
                                    )

                                for (lib in infoList) {
                                    val pos = LatLng.from(lib.lat.toDouble(), lib.lot.toDouble())
                                    labelLayer?.addLabel(
                                        LabelOptions.from(lib.pblibId, pos).setStyles(styles)
                                    )
                                }

                                kakaoMapRef?.setOnLodLabelClickListener { kakaoMap, lodLabelLayer, lodLabel ->
                                    lodLabel?.texts?.forEach { it ->
                                        Log.e("", "lodLabel $it, ${it.length}")
                                        //clickLabelName = it
                                    }

                                    false;
                                }

                            }
                        } else if (resultCode.equals("K3")) {
                            Toast.makeText(this@MainActivity, "조회된 정보가 없습니다.", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            Toast.makeText(this@MainActivity, "에러가 발생했습니다.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Log.d(
                            "[getInfoData]request:fail",
                            "Response not successful: ${response.errorBody()?.string()}"
                        )
                    }
                }

                override fun onFailure(call: Call<ApiResponse<InfoItem>>, t: Throwable) {
                    Log.e("[getInfoData]request:error", t.message.toString())
                }
            })
    }

    private fun getPrstInfoData(pStdgCd: String) {
        if (pStdgCd.isEmpty()) return
        val siCode = pStdgCd.take(4) + "000000"

        val service = RetrofitInstance.retrofitService
        service.getPrstInfoData(DATA_API_KEY, "1", "100", "JSON", siCode, getTodayYyyyMMdd(), getTodayYyyyMMdd())
            .enqueue(object : retrofit2.Callback<ApiResponse<PrstInfoItem>> {
                override fun onResponse(
                    call: Call<ApiResponse<PrstInfoItem>>,
                    response: Response<ApiResponse<PrstInfoItem>>
                ) {
                    if (response.isSuccessful) {
                        prstInfoList = response.body()?.body?.item
                        //Log.d("#############1 result##", result.toString())
                    } else {
                        Log.d(
                            "[getPrstInfoData]request:fail",
                            "Response not successful: ${response.errorBody()?.string()}"
                        )
                    }
                }

                override fun onFailure(call: Call<ApiResponse<PrstInfoItem>>, t: Throwable) {
                    Log.e("[getPrstInfoData]request:error", t.message.toString())
                }
            })
    }

    private fun getRltRdrmInfoData(pStdgCd: String) {
        if (pStdgCd.isEmpty()) return
        val siCode = pStdgCd.take(4) + "000000"

        val service = RetrofitInstance.retrofitService
        service.getRltRdrmInfoData(DATA_API_KEY, "1", "100", "JSON", siCode)
            .enqueue(object : retrofit2.Callback<ApiResponse<RltRdrmInfoItem>> {
                override fun onResponse(
                    call: Call<ApiResponse<RltRdrmInfoItem>>,
                    response: Response<ApiResponse<RltRdrmInfoItem>>
                ) {
                    if (response.isSuccessful) {
                        rltRdrmInfoList = response.body()?.body?.item
                        //Log.d("#############2 result##", result.toString())
                    } else {
                        Log.d(
                            "[getRltRdrmInfoData]request:fail",
                            "Response not successful: ${response.errorBody()?.string()}"
                        )
                    }
                }

                override fun onFailure(call: Call<ApiResponse<RltRdrmInfoItem>>, t: Throwable) {
                    Log.e("[getRltRdrmInfoData]request:error", t.message.toString())
                }
            })
    }

    private fun getRegionByMapCenter() {
        val service = KakaoRetrofit.kakaoRetrofitService

        dongSearchJob?.cancel()
        dongSearchJob = lifecycleScope.launch {
            delay(250)
            service.coordToRegionCode(
                "KakaoAK $KAKAO_API_KEY",
                currentPosition?.longitude ?: startPosition.longitude,
                currentPosition?.latitude ?: startPosition.latitude
            )
                .enqueue(object : retrofit2.Callback<Coord2regioncodeRes> {
                    override fun onResponse(
                        call: Call<Coord2regioncodeRes>,
                        response: Response<Coord2regioncodeRes>
                    ) {
                        if (response.isSuccessful) {
                            val result = response.body()?.documents?.filter { it.regionType == "B" }
                            getInfoData(result?.get(0)?.code ?: "")
                            getPrstInfoData(result?.get(0)?.code ?: "")
                            getRltRdrmInfoData(result?.get(0)?.code ?: "")
                            Log.d("[getRegionByMapCenter]request:success", result.toString())
                        } else {
                            Log.d(
                                "[getRegionByMapCenter]request:fail",
                                "Response not successful: ${response.errorBody()?.string()}"
                            )
                        }
                    }

                    override fun onFailure(call: Call<Coord2regioncodeRes>, t: Throwable) {
                        Log.e("[getRegionByMapCenter]request:error", t.message.toString())
                    }
                })
        }
    }
}

/** 컴포넌트 */
@Composable
fun KakaoMapScreen(lifeCycleCallback: MapLifeCycleCallback, readyCallback: KakaoMapReadyCallback) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // MapView를 한 번만 생성해서 재사용
    val mapView = remember { MapView(context) }

    // Compose Lifecycle ↔ MapView Lifecycle 연결
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
//                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.start(lifeCycleCallback, readyCallback)
//                Lifecycle.Event.ON_RESUME -> mapView.onResume()
//                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
//                Lifecycle.Event.ON_STOP -> mapView.onStop()
//                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    //val currentPos = currentPosition
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { mapView },
        update = { mv ->
            // 지도 준비/표시 (필요하면 여기서 컨트롤)
//            mv.getMapAsync { kakaoMap ->
//                // 예: 기본 설정(원하면 여기 커스텀)
//                // kakaoMap.set...()
//            }
        }
    )
}

@Composable
fun ResearchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(20.dp)) {
        Box(
            modifier = Modifier
                .border(
                    width = 3.dp,
                    color = Color(0xFF1976D2),
                    shape = RoundedCornerShape(20.dp)
                )
                .size(150.dp, 40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    color = Color.White.copy(alpha = 0.6f),
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "이 위치에서 검색",
                    tint = Color(0xFF1976D2) // 파란색
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "이 위치에서 검색",
                    color = Color.DarkGray,//Color(0xFF1976D2),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun MyLocationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(20.dp)) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(
                    color = Color.White,
                    shape = CircleShape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                //imageVector = Icons.Default.LocationOn,
                painter = painterResource(id = R.drawable.outline_my_location_black_24),
                contentDescription = "내 위치 이동",
                tint = Color(0xFF1976D2) // 파란색
            )
        }
    }
}