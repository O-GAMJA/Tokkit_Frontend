package com.example.tokkit

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tokkit.data.remote.api.DirectoryApiService
import com.example.tokkit.data.remote.model.Directory
import com.example.tokkit.databinding.ActivityMainBinding
import com.example.tokkit.util.RetrofitClient
import com.google.android.material.navigation.NavigationView
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import com.example.tokkit.util.CustomToastUtil

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navigationContainer: LinearLayout

    // Fragment 태그 상수
    companion object {
        private const val TAG_HOME = "HOME_FRAGMENT"
        private const val TAG_SEARCH = "SEARCH_FRAGMENT"
        private const val TAG_REVIEW = "REVIEW_FRAGMENT"
        private const val TAG_MYPAGE = "MYPAGE_FRAGMENT"
    }

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
            // 디렉토리 트리를 다시 로드
            loadDirectoryTreeFromApi()
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // 버블차트 버튼 클릭 이벤트 설정
        binding.btnBubble.setOnClickListener{
            val intent = Intent(this, BubbleChartActivity::class.java)
            startActivity(intent)
        }

        // 초기 프래그먼트 설정
        if (savedInstanceState == null) {
            showFragment(TAG_HOME)
        }

        // 바텀 네비게이션 설정
        setupBottomNavigation()

        // FAB 이벤트 설정
        setupSearchFab()

        // Intent에서 태그 검색 정보 확인
        handleTagSearchIntent(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // 새로운 Intent가 들어왔을 때도 처리
        handleTagSearchIntent(intent)
    }

    private fun handleTagSearchIntent(intent: Intent?) {
        intent?.let {
            val selectedTag = it.getStringExtra("selected_tag")
            val searchByTag = it.getBooleanExtra("search_by_tag", false)

            if (searchByTag && !selectedTag.isNullOrEmpty()) {
                Log.d("MainActivity", "버블 차트에서 태그 선택됨: $selectedTag")

                // HomeFragment 표시
                showFragment(TAG_HOME)
                binding.bottomNavigationView.selectedItemId = R.id.fragment_home

                // Fragment가 완전히 로드된 후 태그 검색 실행
                supportFragmentManager.executePendingTransactions()

                val homeFragment = supportFragmentManager.findFragmentByTag(TAG_HOME) as? HomeFragment
                homeFragment?.let { fragment ->
                    // Bundle로 태그 정보 설정
                    val bundle = Bundle().apply {
                        putString("search_tag", selectedTag)
                        putBoolean("is_tag_search", true)
                    }
                    fragment.arguments = bundle

                    // 태그 검색 실행
                    fragment.searchFromExternalTag(selectedTag)
                }
            }
        }
    }

    private fun showFragment(tag: String) {
        val fragmentManager = supportFragmentManager
        val currentFragment = fragmentManager.findFragmentById(R.id.fragment_container)
        val targetFragment = fragmentManager.findFragmentByTag(tag)

        // 이미 해당 Fragment가 표시중이면 리턴
        if (currentFragment != null && currentFragment.tag == tag) {
            return
        }

        val transaction = fragmentManager.beginTransaction()

        // 현재 Fragment가 있으면 숨기기
        currentFragment?.let {
            transaction.hide(it)
        }

        if (targetFragment != null) {
            // 이미 생성된 Fragment가 있으면 보이기
            transaction.show(targetFragment)
        } else {
            // 새로운 Fragment 생성하여 추가
            val newFragment = when (tag) {
                TAG_HOME -> HomeFragment()
                TAG_SEARCH -> SearchFragment()
                TAG_REVIEW -> ReviewFragment()
                TAG_MYPAGE -> MypageFragment()
                else -> HomeFragment()
            }
            transaction.add(R.id.fragment_container, newFragment, tag)
        }

        transaction.commit()
    }

    private fun setupCustomNavigationDrawer() {
        // 커스텀 네비게이션 드로어 레이아웃 인플레이트
        val navigationView = binding.navigationView
        val customNavView = layoutInflater.inflate(R.layout.layout_custom_navigation, navigationView, false)
        navigationView.addView(customNavView)

        navigationContainer = customNavView.findViewById(R.id.navigationItemsContainer)

        // API로부터 디렉토리 트리 로드
        loadDirectoryTreeFromApi()
    }

    private fun loadDirectoryTreeFromApi() {
        // 진행 상태 표시를 위한 로딩 표시자 추가 (옵션)
        val progressBar = ProgressBar(this)
        navigationContainer.addView(progressBar)

        // API 호출은 메인 스레드가 아닌 코루틴으로 실행
        lifecycleScope.launch {
            try {
                // 임시로 memberId = 1 사용, 실제로는 로그인한 사용자 ID 사용
                val memberId = 1L
                val directoryApi = RetrofitClient.createService(DirectoryApiService::class.java)
                val response = directoryApi.getDirectoryTree(memberId)

                // 로딩 표시자 제거
                navigationContainer.removeView(progressBar)

                if (response.isSuccess) {
                    // 성공적으로 디렉토리 트리를 가져왔을 때
                    Log.d("MainActivity", "디렉토리 트리 로드 성공: ${response.result.size}개의 최상위 디렉토리")
                    setupDirectoryTreeForNav(response.result)
                } else {
                    // API 호출은 성공했지만 결과가 실패인 경우
                    Log.e("MainActivity", "디렉토리 트리 로드 실패: ${response.message}")

                }
            } catch (e: Exception) {
                // 로딩 표시자 제거
                navigationContainer.removeView(progressBar)

                // 오류 시 기본 폴더 표시
                Log.e("MainActivity", "디렉토리 트리 로드 중 예외 발생", e)
            }
        }
    }

    private fun setupDirectoryTreeForNav(directories: List<Directory>) {
        // 기존 뷰 제거
        navigationContainer.removeAllViews()

        if (directories.isEmpty()) {
            // 디렉토리가 없는 경우
            val emptyTextView = TextView(this).apply {
                text = "폴더가 없습니다."
                textSize = 16f
                setPadding(24, 24, 24, 24)
                setTextColor(ContextCompat.getColor(context, R.color.gray500))
            }
            navigationContainer.addView(emptyTextView)
            return
        }

        // 전체 노트 ID 로깅
        Log.d("MainActivity", "로드된 디렉토리 트리 정보:")
        directories.forEach { directory ->
            logDirectoryContents(directory, "")
        }

        // 최상위 디렉토리 표시
        for (directory in directories) {
            addDirectoryViewForNav(directory, navigationContainer, emptyList())
        }
    }

    // 디렉토리 내용을 재귀적으로 로깅하는 함수
    private fun logDirectoryContents(directory: Directory, indent: String) {
        Log.d("MainActivity", "${indent}디렉토리: ${directory.name} (ID: ${directory.directory_id})")

        // 노트 정보 로깅
        directory.notes.forEachIndexed { index, note ->
            val noteId = note.note_id ?: note.id
            Log.d("MainActivity", "${indent}  - 노트 ${index+1}: '${note.title ?: "(제목 없음)"}' (ID: $noteId)")
        }

        // 하위 디렉토리 정보 로깅 (재귀 호출)
        directory.children.forEach { childDir ->
            logDirectoryContents(childDir, "$indent  ")
        }
    }

    private fun addDirectoryViewForNav(directory: Directory, container: LinearLayout, parentPath: List<String>) {
        val folderView = layoutInflater.inflate(R.layout.item_folder, container, false)
        val folderNameTv = folderView.findViewById<TextView>(R.id.tvFolderName)
        val expandIcon = folderView.findViewById<ImageView>(R.id.ivExpandIcon)
        val folderIcon = folderView.findViewById<ImageView>(R.id.ivFolderIcon)

        folderNameTv.text = directory.name
        folderIcon.setImageResource(R.drawable.ic_folder)

        // 북마크 폴더인 경우 디자인 변경
        if (directory.name == "bookmark") {
            folderIcon.setImageResource(R.drawable.ic_bookmark2)
            folderView.setBackgroundResource(R.drawable.bookmark_folder_background)
            val iconParams = folderIcon.layoutParams
            iconParams.width = 31.dpToPx()
            iconParams.height = 31.dpToPx()
            folderIcon.layoutParams = iconParams
           // folderNameTv.textSize = 18f
            folderNameTv.setTypeface(null, Typeface.BOLD)
            // 텍스트 색상도 조절
            // folderNameTv.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        }else {
            folderIcon.setImageResource(R.drawable.ic_folder)
            // 일반 폴더는 기본 크기 유지
            val iconParams = folderIcon.layoutParams
            iconParams.width = 24.dpToPx()
            iconParams.height = 24.dpToPx()
            folderIcon.layoutParams = iconParams
        }

        // 하위 디렉토리와 노트 확인
        val hasChildren = directory.children.isNotEmpty() || directory.notes.isNotEmpty()

        // 하위 항목 컨테이너 생성
        val subItemsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        // 들여쓰기 설정 (하위 폴더인 경우)
        val indentLevel = parentPath.size
        val params = folderView.layoutParams as? LinearLayout.LayoutParams
            ?: LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.marginStart = (24 * indentLevel).dpToPx()
        folderView.layoutParams = params

        // 폴더에 하위 항목이 없으면 화살표 아이콘 대신 공백 표시
        if (!hasChildren) {
            expandIcon.setImageResource(android.R.color.transparent)
        } else {
            expandIcon.setImageResource(R.drawable.ic_expand)

            // 노트 항목 추가
            for (note in directory.notes) {
                val noteId = note.note_id ?: note.id
                addPageViewForNav(note.title, noteId, subItemsContainer, parentPath.size + 1)
            }

            // 하위 디렉토리 추가
            for (childDirectory in directory.children) {
                val newPath = parentPath.toMutableList().apply { add(directory.name) }
                addDirectoryViewForNav(childDirectory, subItemsContainer, newPath)
            }
        }

        // 확장/축소를 위한 클릭 리스너 추가
        folderView.setOnClickListener {
            if (hasChildren) {
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

    private fun addPageViewForNav(pageTitle: String?, noteId: String?, container: LinearLayout, indentLevel: Int) {
        val pageView = layoutInflater.inflate(R.layout.item_page, container, false)
        val pageNameTv = pageView.findViewById<TextView>(R.id.tvPageName)
        val pageIcon = pageView.findViewById<ImageView>(R.id.ivPageIcon)

        // null 처리
        pageNameTv.text = pageTitle ?: "(제목 없음)"
        pageIcon.setImageResource(R.drawable.ic_page)

        // 들여쓰기 설정
        val params = pageView.layoutParams as? LinearLayout.LayoutParams
            ?: LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.marginStart = (24 * indentLevel).dpToPx()
        pageView.layoutParams = params

        // 노트 클릭 시 상세 화면으로 이동
        pageView.setOnClickListener {
            // 드로어 닫기
            binding.drawerLayout.closeDrawer(GravityCompat.START)

            if (noteId != null) {
                // 선택된 노트 ID 로그 출력
                Log.d("MainActivity", "선택된 노트 ID: $noteId")

                try {
                    // 노트 상세 화면으로 이동
                    val intent = Intent(this, SearchDetailActivity::class.java)
                    intent.putExtra("NOTE_ID", noteId)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "상세 화면 이동 중 오류", e)

                    CustomToastUtil.showToast(
                        context = this,
                        message = "페이지를 열 수 없습니다",
                        iconResId = R.drawable.ic_bot
                    )
                }
            } else {
                CustomToastUtil.showToast(
                    context = this,
                    message = "이 항목은 조회할 수 없습니다",
                    iconResId = R.drawable.ic_bot
                )
            }
        }

        container.addView(pageView)
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
                    showFragment(TAG_HOME)
                    binding.navigationView.setCheckedItem(R.id.nav_home)
                }
                R.id.fragment_search -> {
                    showFragment(TAG_SEARCH)
                    binding.navigationView.setCheckedItem(R.id.nav_search)
                }
                R.id.fragment_review -> {
                    showFragment(TAG_REVIEW)
                    binding.navigationView.setCheckedItem(R.id.nav_review)
                }
                R.id.fragment_settings -> {
                    showFragment(TAG_MYPAGE)
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

    // 프래그먼트 교체 함수 (기존 방식 - 필요시 사용)
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // NavigationView 아이템 클릭 이벤트 처리
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                showFragment(TAG_HOME)
                binding.bottomNavigationView.selectedItemId = R.id.fragment_home
            }
            R.id.nav_search -> {
                showFragment(TAG_SEARCH)
                binding.bottomNavigationView.selectedItemId = R.id.fragment_search
            }
            R.id.nav_review -> {
                showFragment(TAG_REVIEW)
                binding.bottomNavigationView.selectedItemId = R.id.fragment_review
            }
            R.id.nav_mypage -> {
                showFragment(TAG_MYPAGE)
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

    fun reloadDirectoryTree() {
        loadDirectoryTreeFromApi()
    }
}

// 네비게이션 구조를 위한 데이터 클래스
data class FolderItem(val name: String, val subItems: List<Any>)
data class PageItem(val name: String, val pageId: String?)