package com.example.tokkit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 초기 프래그먼트 설정
        replaceFragment(HomeFragment())

        // 바텀 네비게이션 설정
        setupBottomNavigation()

        // FAB 이벤트 설정
        setupSearchFab()
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // 바텀 네비게이션 배경 제거
        bottomNavigationView.background = null

        // 가운데 아이템 비활성화
        bottomNavigationView.menu.getItem(2).isEnabled = false

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.fragment_home -> replaceFragment(HomeFragment())
                R.id.fragment_search -> replaceFragment(SearchFragment())
                R.id.fragment_review-> replaceFragment(ReviewFragment())
                R.id.fragment_settings -> replaceFragment(MypageFragment())
            }
            true
        }
    }

    private fun setupSearchFab() {
        val searchFab = findViewById<FloatingActionButton>(R.id.searchFab)
        searchFab.setOnClickListener {
            // FAB 클릭 시 동작 구현
        }
    }

    // 프래그먼트 교체 함수
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}