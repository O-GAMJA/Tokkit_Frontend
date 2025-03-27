package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 툴바 설정
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // DrawerLayout 설정
        drawerLayout = findViewById(R.id.drawerLayout)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // NavigationView 설정
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        navigationView.setNavigationItemSelectedListener(this)

        // 초기 프래그먼트 설정
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            navigationView.setCheckedItem(R.id.nav_home)
        }

        // 바텀 네비게이션 설정
        setupBottomNavigation()

        // FAB 이벤트 설정
        setupSearchFab()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // 바텀 네비게이션 배경 제거
        bottomNavigationView.background = null

        // 가운데 아이템 비활성화
        bottomNavigationView.menu.getItem(2).isEnabled = false

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.fragment_home -> {
                    replaceFragment(HomeFragment())
                    findViewById<NavigationView>(R.id.navigationView).setCheckedItem(R.id.nav_home)
                }
                R.id.fragment_search -> {
                    replaceFragment(SearchFragment())
                    findViewById<NavigationView>(R.id.navigationView).setCheckedItem(R.id.nav_search)
                }
                R.id.fragment_review -> {
                    replaceFragment(ReviewFragment())
                    findViewById<NavigationView>(R.id.navigationView).setCheckedItem(R.id.nav_review)
                }
                R.id.fragment_settings -> {
                    replaceFragment(MypageFragment())
                    findViewById<NavigationView>(R.id.navigationView).setCheckedItem(R.id.nav_mypage)
                }
            }
            true
        }
    }

    private fun setupSearchFab() {
        val searchFab = findViewById<FloatingActionButton>(R.id.searchFab)
        searchFab.setOnClickListener {
            // FAB 클릭 시 EasyOCRActivity 실행
            val intent = Intent(this, EasyOCRActivity::class.java)
            startActivity(intent)
        }
    }


    // 프래그먼트 교체 함수
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // NavigationView 아이템 클릭 이벤트 처리
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                replaceFragment(HomeFragment())
                findViewById<BottomNavigationView>(R.id.bottomNavigationView).selectedItemId = R.id.fragment_home
            }
            R.id.nav_search -> {
                replaceFragment(SearchFragment())
                findViewById<BottomNavigationView>(R.id.bottomNavigationView).selectedItemId = R.id.fragment_search
            }
            R.id.nav_review -> {
                replaceFragment(ReviewFragment())
                findViewById<BottomNavigationView>(R.id.bottomNavigationView).selectedItemId = R.id.fragment_review
            }
            R.id.nav_mypage -> {
                replaceFragment(MypageFragment())
                findViewById<BottomNavigationView>(R.id.bottomNavigationView).selectedItemId = R.id.fragment_settings
            }
            R.id.nav_settings -> {
                // 설정 화면으로 이동하는 코드 (필요시 추가)
            }
            R.id.nav_logout -> {
                // 로그아웃 기능 구현 (필요시 추가)
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}