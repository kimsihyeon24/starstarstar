package com.example.starfinder

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class IntroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 사진이 상단 상태바 뒤까지 꽉 차게
        setContentView(R.layout.activity_intro)

        val dp = resources.displayMetrics.density

        // 제목이 아래에서 천천히 떠오르는 효과
        val title = findViewById<View>(R.id.introTitle)
        title.alpha = 0f
        title.translationY = 30f * dp
        title.animate().alpha(1f).translationY(0f).setDuration(900).start()

        // 부제목은 조금 늦게 나타남
//        val subtitle = findViewById<View>(R.id.introSubtitle)
//        subtitle.alpha = 0f
//        subtitle.animate().alpha(1f).setStartDelay(400).setDuration(900).start()

        // 시작하기 → 별 찾기 화면으로 이동
        findViewById<View>(R.id.startButton).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish() // 카메라 화면에서 뒤로가기 누르면 앱 종료

        }
    }
}