package com.khr.bookseat

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.khr.bookseat.objets.ApiResponse
import com.khr.bookseat.objets.InfoItem
import com.khr.bookseat.services.api.RetrofitInstance
import retrofit2.Call
import retrofit2.Response
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.kakao.vectormap.camera.CameraUpdateFactory

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
                MyLocationButton(onClick = {fetchCurrentLocation()}, modifier = Modifier.align(Alignment.BottomEnd))
            }
        }

        requestLocationAndFetch()

        retrofitWork()
        //requestLocationAndFetch()
    }


    private val startZoomLevel = 15
    private val startPosition = LatLng.from(37.394660, 127.111182) // 판교역
    private var currentPosition by mutableStateOf<LatLng?>(null)    // 현재 위치

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
    private fun moveCameraTo(pos: LatLng) {
        val cameraUpdate = CameraUpdateFactory.newCenterPosition(pos)
        kakaoMapRef?.moveCamera(cameraUpdate)
    }

    private fun getReadyCallback(context: Context): KakaoMapReadyCallback {
        return object : KakaoMapReadyCallback() {
            override fun onMapReady(kakaoMap: KakaoMap) {
                kakaoMapRef = kakaoMap
                // 전달받은 context를 사용
                Toast.makeText(context, "지도가 준비되었습니다.", Toast.LENGTH_SHORT).show()
                currentPosition?.let { moveCameraTo(it) }
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

    /** api */
    private fun retrofitWork() {
        val service = RetrofitInstance.retrofitService
        service.getInfoData(DATA_API_KEY, "1", "100", "JSON", "5214000000")
            .enqueue(object : retrofit2.Callback<ApiResponse<InfoItem>> {
                override fun onResponse(
                    call: Call<ApiResponse<InfoItem>>,
                    response: Response<ApiResponse<InfoItem>>
                ) {

                    if (response.isSuccessful) {
                        Log.d("##################", response.body().toString())
                        val result = response.body()?.body?.item?.get(0)?.lat
                        //35.9402673000

                        Log.d("################item##", result + "")
                    } else {
                        Log.d(
                            "##################",
                            "Response not successful: ${response.errorBody()?.string()}"
                        )
                    }
                }

                override fun onFailure(call: Call<ApiResponse<InfoItem>>, t: Throwable) {
                    Log.d("태그", t.message.toString())
                }
            })
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
fun MyLocationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(20.dp)) {
        Box(
            modifier = Modifier
                .size(50.dp)
//            .shadow(
//                elevation = 6.dp,
//                shape = CircleShape,
//                clip = false
//            )
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