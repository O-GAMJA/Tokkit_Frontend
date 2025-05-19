package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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
import kotlinx.coroutines.launch

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
                    Toast.makeText(this, "페이지를 열 수 없습니다", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "이 항목은 조회할 수 없습니다", Toast.LENGTH_SHORT).show()
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
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}

// 네비게이션 구조를 위한 데이터 클래스
data class FolderItem(val name: String, val subItems: List<Any>)
data class PageItem(val name: String, val pageId: String?)