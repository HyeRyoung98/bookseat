package com.khr.bookseat

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk

class MapApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // https://apis.map.kakao.com/android_v2/docs/getting-started/quickstart/#2-%EB%84%A4%EC%9D%B4%ED%8B%B0%EB%B8%8C-%EC%95%B1-%ED%82%A4-%EC%B6%94%EA%B0%80
        KakaoMapSdk.init(this, KAKAO_MAP_KEY)
    }
}