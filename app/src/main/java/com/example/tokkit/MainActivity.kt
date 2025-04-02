package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.example.tokkit.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 상태바 투명하게 설정
       setStatusBarTransparent()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 햄버거 메뉴 버튼 클릭 이벤트 설정
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // NavigationView 설정
        binding.navigationView.setNavigationItemSelectedListener(this)

        // 초기 프래그먼트 설정
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            binding.navigationView.setCheckedItem(R.id.nav_home)
        }

        // 바텀 네비게이션 설정
        setupBottomNavigation()

        // FAB 이벤트 설정
        setupSearchFab()
    }

    //상태 바 투명하게
    private fun setStatusBarTransparent() {
        window.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun setupBottomNavigation() {
        // 바텀 네비게이션 배경 제거
        binding.bottomNavigationView.background = null

        // 가운데 아이템 비활성화
        binding.bottomNavigationView.menu.getItem(2).isEnabled = false

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.fragment_home -> {
                    replaceFragment(HomeFragment())
                    binding.navigationView.setCheckedItem(R.id.nav_home)
                }
                R.id.fragment_search -> {
                    replaceFragment(SearchFragment())
                    binding.navigationView.setCheckedItem(R.id.nav_search)
                }
                R.id.fragment_review -> {
                    replaceFragment(ReviewFragment())
                    binding.navigationView.setCheckedItem(R.id.nav_review)
                }
                R.id.fragment_settings -> {
                    replaceFragment(MypageFragment())
                    binding.navigationView.setCheckedItem(R.id.nav_mypage)
                }
            }
            true
        }
    }

    private fun setupSearchFab() {
        binding.searchFab.setOnClickListener {
            // FAB 클릭 시 동작 구현
            val intent = Intent(this, AttachReferenceActivity::class.java)
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
                binding.bottomNavigationView.selectedItemId = R.id.fragment_home
            }
            R.id.nav_search -> {
                replaceFragment(SearchFragment())
                binding.bottomNavigationView.selectedItemId = R.id.fragment_search
            }
            R.id.nav_review -> {
                replaceFragment(ReviewFragment())
                binding.bottomNavigationView.selectedItemId = R.id.fragment_review
            }
            R.id.nav_mypage -> {
                replaceFragment(MypageFragment())
                binding.bottomNavigationView.selectedItemId = R.id.fragment_settings
            }
            R.id.nav_settings -> {
                // 설정 화면으로 이동하는 코드 (필요시 추가)
            }
            R.id.nav_logout -> {
                // 로그아웃 기능 구현 (필요시 추가)
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}