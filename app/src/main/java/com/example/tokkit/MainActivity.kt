package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.example.tokkit.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navigationContainer: LinearLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 상태바 투명하게 설정
        setStatusBarTransparent()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 커스텀 네비게이션 드로어 설정
        setupCustomNavigationDrawer()

        // 햄버거 메뉴 버튼 클릭 이벤트 설정
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // 버블차트 버튼 클릭 이벤트 설정
        binding.btnBubble.setOnClickListener{
            val intent = Intent(this, BubbleChartActivity::class.java)
            startActivity(intent)
        }

        // 초기 프래그먼트 설정
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        // 바텀 네비게이션 설정
        setupBottomNavigation()

        // FAB 이벤트 설정
        setupSearchFab()

        // 🔥 FCM 토큰 가져와서 로그로 출력
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "🔥 FCM 토큰 가져오기 실패", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d("FCM", "🔥 현재 FCM 토큰: $token")
        }
    }

    private fun setupCustomNavigationDrawer() {
        // 커스텀 네비게이션 드로어 레이아웃 인플레이트
        val navigationView = binding.navigationView
        val customNavView = layoutInflater.inflate(R.layout.layout_custom_navigation, navigationView, false)
        navigationView.addView(customNavView)

        navigationContainer = customNavView.findViewById(R.id.navigationItemsContainer)

        // 데이터 통신 구조 폴더, 하위 폴더, 페이지
        addMainFolder("데이터 통신", listOf(
            FolderItem("TCP/IP", listOf(
                PageItem("인터넷 계층", null),
                PageItem("전송 계층", null)
            ))
        ))

        // 자료 구조 폴더, 하위 폴더 페이지
        addMainFolder("자료 구조", listOf(
            FolderItem("선형 자료구조", listOf(
                PageItem("스택", null),
                PageItem("큐", null),
                PageItem("스택", null),
                PageItem("큐", null),
                PageItem("스택", null),
                PageItem("큐", null),
                PageItem("스택", null),
                PageItem("큐", null),
                PageItem("스택", null),
                PageItem("큐", null),
                PageItem("스택", null),
                PageItem("큐", null),
                PageItem("스택", null),
                PageItem("큐", null),
                PageItem("스택", null),
                PageItem("큐", null)
            )),
            FolderItem("비선형 자료구조", listOf(
                PageItem("트리", null)
            )),
            FolderItem("연습노트", emptyList())
        ))

        // 컴퓨터 구조 폴더
        addMainFolder("컴퓨터 구조", emptyList())
    }


    private fun addMainFolder(folderName: String, subItems: List<Any>) {
        val folderView = layoutInflater.inflate(R.layout.item_folder, navigationContainer, false)
        val folderNameTv = folderView.findViewById<TextView>(R.id.tvFolderName)
        val expandIcon = folderView.findViewById<ImageView>(R.id.ivExpandIcon)
        val folderIcon = folderView.findViewById<ImageView>(R.id.ivFolderIcon)

        folderNameTv.text = folderName
        folderIcon.setImageResource(R.drawable.ic_folder)

        // 폴더에 하위 항목이 없으면 화살표 아이콘 대신 공백 표시
        if (subItems.isEmpty()) {
            expandIcon.setImageResource(android.R.color.transparent)
        } else {
            expandIcon.setImageResource(R.drawable.ic_expand)
        }

        val subItemsContainer = LinearLayout(this)
        subItemsContainer.orientation = LinearLayout.VERTICAL
        subItemsContainer.visibility = View.GONE

        // 하위 아이템 추가
        for (item in subItems) {
            when (item) {
                is FolderItem -> addSubFolder(item, subItemsContainer)
                is PageItem -> addPage(item, subItemsContainer)
            }
        }

        // 확장/축소를 위한 클릭 리스너 추가
        folderView.setOnClickListener {
            if (subItems.isNotEmpty()) {  // 하위 항목이 있을 때만 토글 기능 활성화
                if (subItemsContainer.visibility == View.VISIBLE) {
                    subItemsContainer.visibility = View.GONE
                    expandIcon.setImageResource(R.drawable.ic_expand)
                } else {
                    subItemsContainer.visibility = View.VISIBLE
                    expandIcon.setImageResource(R.drawable.ic_collapse)
                }
            }
        }

        navigationContainer.addView(folderView)
        navigationContainer.addView(subItemsContainer)
    }

    private fun addSubFolder(folder: FolderItem, container: LinearLayout) {
        val folderView = layoutInflater.inflate(R.layout.item_folder, container, false)
        val folderNameTv = folderView.findViewById<TextView>(R.id.tvFolderName)
        val expandIcon = folderView.findViewById<ImageView>(R.id.ivExpandIcon)
        val folderIcon = folderView.findViewById<ImageView>(R.id.ivFolderIcon)

        // 들여쓰기를 위해 마진 추가
        val params = folderView.layoutParams as LinearLayout.LayoutParams
        params.marginStart = 24.dpToPx()
        folderView.layoutParams = params

        folderNameTv.text = folder.name
        folderIcon.setImageResource(R.drawable.ic_folder)

        // 폴더에 하위 항목이 없으면 화살표 아이콘 대신 공백 표시
        if (folder.subItems.isEmpty()) {
            expandIcon.setImageResource(android.R.color.transparent)
        } else {
            expandIcon.setImageResource(R.drawable.ic_expand)
        }

        val subItemsContainer = LinearLayout(this)
        subItemsContainer.orientation = LinearLayout.VERTICAL
        subItemsContainer.visibility = View.GONE

        // 하위 아이템 추가
        for (item in folder.subItems) {
            when (item) {
                is FolderItem -> addSubFolder(item, subItemsContainer)
                is PageItem -> addPage(item, subItemsContainer)
            }
        }

        // 확장/축소를 위한 클릭 리스너 추가
        folderView.setOnClickListener {
            if (folder.subItems.isNotEmpty()) {
                if (subItemsContainer.visibility == View.VISIBLE) {
                    subItemsContainer.visibility = View.GONE
                    expandIcon.setImageResource(R.drawable.ic_expand)
                } else {
                    subItemsContainer.visibility = View.VISIBLE
                    expandIcon.setImageResource(R.drawable.ic_collapse)
                }
            }
        }

        container.addView(folderView)
        container.addView(subItemsContainer)
    }

    // dp를 픽셀로 변환하는 확장 함수
    private fun Int.dpToPx(): Int {
        val scale = resources.displayMetrics.density
        return (this * scale + 0.5f).toInt()
    }

    private fun addPage(page: PageItem, container: LinearLayout) {
        val pageView = layoutInflater.inflate(R.layout.item_page, container, false)
        val pageNameTv = pageView.findViewById<TextView>(R.id.tvPageName)
        val pageIcon = pageView.findViewById<ImageView>(R.id.ivPageIcon)

        pageNameTv.text = page.name
        pageIcon.setImageResource(R.drawable.ic_page)
        pageView.setOnClickListener {
            // 페이지 클릭 처리 - 페이지로 이동
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            // 여기에 페이지 이동 로직 추가 예정
        }

        container.addView(pageView)
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

// 네비게이션 구조를 위한 데이터 클래스
data class FolderItem(val name: String, val subItems: List<Any>)
data class PageItem(val name: String, val pageId: String?)