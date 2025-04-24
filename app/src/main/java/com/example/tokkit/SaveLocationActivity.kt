package com.example.tokkit

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.tokkit.databinding.ActivitySavelocationBinding

class SaveLocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavelocationBinding
    private lateinit var navigationContainer: LinearLayout

    private var selectedPath: String? = null
    private var selectedPageView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavelocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 저장 버튼
        binding.btnSave.setOnClickListener {
            // 선택된 폴더에 저장 처리
            val resultIntent = Intent().apply {
                putExtra("selectedPath", selectedPath)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        // 폴더 구조 설정
        setupFolderStructure()
    }

    private fun setupFolderStructure() {
        navigationContainer = binding.folderContainer

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
        addMainFolder("데이터 통신", emptyList())
        addMainFolder("데이터 통신", emptyList())
        addMainFolder("데이터 통신", emptyList())
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

    private fun addPage(page: PageItem, container: LinearLayout) {
        val pageView = layoutInflater.inflate(R.layout.item_page, container, false)
        val pageNameTv = pageView.findViewById<TextView>(R.id.tvPageName)
        val pageIcon = pageView.findViewById<ImageView>(R.id.ivPageIcon)

        pageNameTv.text = page.name
        pageIcon.setImageResource(R.drawable.ic_page)

        pageView.setOnClickListener {
            // 페이지 클릭 처리 - 이 페이지를 저장 위치로 선택
            selectedPath = page.name

            // 이전 선택된 뷰 초기화
            selectedPageView?.setBackgroundColor(Color.TRANSPARENT)

            // 현재 선택된 뷰 강조
            pageView.setBackgroundColor(ContextCompat.getColor(this, R.color.main))

            // 선택된 뷰 저장
            selectedPageView = pageView
        }

        container.addView(pageView)
    }

    // dp를 픽셀로 변환하는 확장 함수
    private fun Int.dpToPx(): Int {
        val scale = resources.displayMetrics.density
        return (this * scale + 0.5f).toInt()
    }
}