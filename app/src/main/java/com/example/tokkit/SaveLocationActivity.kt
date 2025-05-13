package com.example.tokkit

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.tokkit.databinding.ActivitySavelocationBinding

// SaveLocationActivity.kt 수정

class SaveLocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavelocationBinding
    private lateinit var navigationContainer: LinearLayout

    private var selectedPath: String? = null
    private var selectedFolderView: View? = null
    // 선택된 폴더의 경로를 계층적으로 저장
    private var selectedFolderPath: MutableList<String> = mutableListOf()
    // 노트 제목 (Intent에서 받은 경우 사용)
    private var noteTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavelocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent에서 노트 제목 받기
        noteTitle = intent.getStringExtra("NOTE_TITLE")

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 저장 버튼
        binding.btnSave.setOnClickListener {
            // 선택된 폴더 경로에 노트 제목 추가
            val resultPath = if (selectedFolderPath.isNotEmpty()) {
                val folderPath = selectedFolderPath.joinToString(" > ")
                "$folderPath > \"$noteTitle\""
            } else {
                "\"$noteTitle\""
            }

            val resultIntent = Intent().apply {
                putExtra("selectedPath", resultPath)
                // 폴더 구조 정보도 함께 전달 (필요시)
                putStringArrayListExtra("folderStructure", ArrayList(selectedFolderPath))
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        // 폴더 구조 설정
        setupFolderStructure()
    }

    private fun setupFolderStructure() {
        navigationContainer = binding.folderContainer

        // 데이터 통신 구조 폴더, 하위 폴더
        addMainFolder("데이터 통신", listOf(
            FolderItem("TCP/IP", emptyList())
        ))

        // 자료 구조 폴더, 하위 폴더
        addMainFolder("자료 구조", listOf(
            FolderItem("선형 자료구조", emptyList()),
            FolderItem("비선형 자료구조", emptyList()),
            FolderItem("연습노트", emptyList())
        ))

        // 컴퓨터 구조 폴더
        addMainFolder("컴퓨터 구조", emptyList())
        addMainFolder("운영체제", emptyList())
        addMainFolder("알고리즘", emptyList())
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
                is FolderItem -> addSubFolder(item, subItemsContainer, mutableListOf(folderName))
            }
        }

        // 폴더 선택을 위한 클릭 리스너 추가
        folderView.setOnClickListener {
            // 폴더 열기/닫기 토글
            if (subItems.isNotEmpty()) {
                if (subItemsContainer.visibility == View.VISIBLE) {
                    subItemsContainer.visibility = View.GONE
                    expandIcon.setImageResource(R.drawable.ic_expand)
                } else {
                    subItemsContainer.visibility = View.VISIBLE
                    expandIcon.setImageResource(R.drawable.ic_collapse)
                }
            }

            // 폴더 선택 처리
            selectFolder(folderView, mutableListOf(folderName))
        }

        navigationContainer.addView(folderView)
        navigationContainer.addView(subItemsContainer)
    }

    private fun addSubFolder(folder: FolderItem, container: LinearLayout, parentPath: MutableList<String>) {
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

        // 하위 아이템 추가 (이제 PageItem 추가는 제거)
        for (item in folder.subItems) {
            if (item is FolderItem) {
                // 현재 경로에 현재 폴더 이름 추가
                val currentPath = ArrayList(parentPath)
                currentPath.add(folder.name)
                addSubFolder(item, subItemsContainer, currentPath)
            }
        }

        // 클릭 리스너: 폴더 열기/닫기 및 선택
        folderView.setOnClickListener {
            // 폴더 열기/닫기 토글
            if (folder.subItems.isNotEmpty()) {
                if (subItemsContainer.visibility == View.VISIBLE) {
                    subItemsContainer.visibility = View.GONE
                    expandIcon.setImageResource(R.drawable.ic_expand)
                } else {
                    subItemsContainer.visibility = View.VISIBLE
                    expandIcon.setImageResource(R.drawable.ic_collapse)
                }
            }

            // 현재 경로 복사 및 현재 폴더 이름 추가
            val currentPath = ArrayList(parentPath)
            currentPath.add(folder.name)

            // 폴더 선택 처리
            selectFolder(folderView, currentPath)
        }

        container.addView(folderView)
        container.addView(subItemsContainer)
    }

    // 폴더 선택 처리 메서드
    private fun selectFolder(folderView: View, folderPath: List<String>) {
        // 이전 선택 초기화
        selectedFolderView?.setBackgroundColor(Color.TRANSPARENT)

        // 현재 선택 저장
        selectedFolderView = folderView
        selectedFolderPath = folderPath.toMutableList()

        // 선택된 폴더 강조 표시
        folderView.setBackgroundColor(ContextCompat.getColor(this, R.color.main))

        // 로그
        Log.d("SaveLocation", "선택된 폴더 경로: ${folderPath.joinToString(" > ")}")
    }

    // dp를 픽셀로 변환하는 확장 함수
    private fun Int.dpToPx(): Int {
        val scale = resources.displayMetrics.density
        return (this * scale + 0.5f).toInt()
    }
}