package com.example.tokkit

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.tokkit.data.remote.api.DirectoryApiService
import com.example.tokkit.data.remote.model.Directory
import com.example.tokkit.databinding.ActivitySavelocationBinding
import com.example.tokkit.databinding.DialogAddFolderBinding
import com.example.tokkit.util.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

class SaveLocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavelocationBinding
    private lateinit var navigationContainer: LinearLayout

    private var selectedPath: String? = null
    private var selectedFolderView: View? = null
    private var selectedDirectoryId: Int? = null

    // 선택된 폴더의 경로를 계층적으로 저장
    private var selectedFolderPath: MutableList<String> = mutableListOf()
    // 노트 제목 (Intent에서 받은 경우 사용)
    private var noteTitle: String? = null

    // 선택된 폴더 컨테이너와 확장 아이콘을 저장
    private var selectedFolderContainer: LinearLayout? = null
    private var selectedFolderExpandIcon: ImageView? = null

    // 디렉토리 API 서비스
    private val directoryApi by lazy { RetrofitClient.createService(DirectoryApiService::class.java) }

    // 로딩 중 표시
    private var isLoading = false

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

            Log.d("SaveLocation", "저장 시 선택된 디렉토리 ID: $selectedDirectoryId")

            val resultIntent = Intent().apply {
                putExtra("selectedPath", resultPath)
                putExtra("selectedDirectoryId", selectedDirectoryId)
                putStringArrayListExtra("folderStructure", ArrayList(selectedFolderPath))
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        // 폴더 추가 버튼
        binding.btnAddFolder.setOnClickListener {
            if (selectedFolderPath.isEmpty()) {
                // 선택된 폴더가 없으면 최상위 폴더 추가
                showAddFolderDialog(null, null)
            } else {
                // 선택된 폴더가 있으면 해당 폴더 하위에 추가
                showAddFolderDialog(selectedFolderContainer, selectedFolderPath)
            }
        }

        // 디렉토리 트리 로드
        loadDirectoryTree()
    }

    private fun loadDirectoryTree() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                // 임시로 memberId = 1 사용, 실제로는 로그인한 사용자 ID 사용
                val memberId = 1L
                val response = directoryApi.getDirectoryTree(memberId)

                if (response.isSuccess) {
                    // 성공적으로 디렉토리 트리를 가져왔을 때
                    Log.d("SaveLocation", "디렉토리 트리 로드 성공: ${response.result.size}개의 최상위 디렉토리")
                    setupDirectoryTree(response.result)
                } else {
                    // API 호출은 성공했지만 결과가 실패인 경우
                    Log.e("SaveLocation", "디렉토리 트리 로드 실패: ${response.message}")
                    Toast.makeText(this@SaveLocationActivity, "폴더 정보를 불러오지 못했습니다: ${response.message}", Toast.LENGTH_SHORT).show()
                    setupDefaultFolders()
                }
            } catch (e: HttpException) {
                // HTTP 에러 발생 시
                Log.e("SaveLocation", "디렉토리 트리 API 호출 에러: ${e.message()}", e)
                Toast.makeText(this@SaveLocationActivity, "서버 연결 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                setupDefaultFolders()
            } catch (e: Exception) {
                // 기타 예외 발생 시
                Log.e("SaveLocation", "디렉토리 트리 로드 중 예외 발생", e)
                Toast.makeText(this@SaveLocationActivity, "폴더 정보를 불러오는 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                setupDefaultFolders()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun setupDirectoryTree(directories: List<Directory>) {
        navigationContainer = binding.folderContainer
        navigationContainer.removeAllViews()  // 기존 뷰 제거

        if (directories.isEmpty()) {
            // 디렉토리가 없는 경우
            val emptyTextView = TextView(this).apply {
                text = "폴더가 없습니다. 새 폴더를 추가해주세요."
                textSize = 16f
                setPadding(24, 24, 24, 24)
                setTextColor(ContextCompat.getColor(context, R.color.gray500))
            }
            navigationContainer.addView(emptyTextView)
            return
        }

        // 최상위 디렉토리 표시
        for (directory in directories) {
            addDirectoryView(directory, navigationContainer, emptyList())
        }
    }

    private fun addDirectoryView(directory: Directory, container: LinearLayout, parentPath: List<String>) {
        val folderView = layoutInflater.inflate(R.layout.item_folder, container, false)
        val folderNameTv = folderView.findViewById<TextView>(R.id.tvFolderName)
        val expandIcon = folderView.findViewById<ImageView>(R.id.ivExpandIcon)
        val folderIcon = folderView.findViewById<ImageView>(R.id.ivFolderIcon)

        folderNameTv.text = directory.name
        folderIcon.setImageResource(R.drawable.ic_folder)

        // 하위 디렉토리가 있는지 확인
        val hasChildren = directory.children.isNotEmpty()

        // 하위 항목 컨테이너 생성
        val subItemsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        // 들여쓰기 설정 (하위 폴더인 경우)
        val indentLevel = parentPath.size // 깊이 레벨
        val params = folderView.layoutParams as? LinearLayout.LayoutParams
            ?: LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.marginStart = (24 * indentLevel).dpToPx()
        folderView.layoutParams = params

        // 폴더에 하위 항목이 없으면 화살표 아이콘 대신 공백 표시
        if (!hasChildren) {
            expandIcon.setImageResource(android.R.color.transparent)
        } else {
            expandIcon.setImageResource(R.drawable.ic_expand)

            // 하위 디렉토리 추가
            for (childDirectory in directory.children) {
                val newPath = parentPath.toMutableList().apply { add(directory.name) }
                addDirectoryView(childDirectory, subItemsContainer, newPath)
            }
        }

        // 클릭 리스너 설정
        folderView.setOnClickListener {
            // 하위 항목이 있으면 토글
            if (hasChildren) {
                if (subItemsContainer.visibility == View.VISIBLE) {
                    subItemsContainer.visibility = View.GONE
                    expandIcon.setImageResource(R.drawable.ic_expand)
                } else {
                    subItemsContainer.visibility = View.VISIBLE
                    expandIcon.setImageResource(R.drawable.ic_collapse)
                }
            }

            // 현재 폴더 경로 설정
            val currentPath = parentPath.toMutableList().apply { add(directory.name) }

            // 폴더 선택 처리
            selectFolder(folderView, subItemsContainer, expandIcon, currentPath, directory.directory_id)
        }

        // 컨테이너에 추가
        container.addView(folderView)
        container.addView(subItemsContainer)
    }

    // 폴더 선택 처리 메서드
    private fun selectFolder(folderView: View, container: LinearLayout, expandIcon: ImageView,
                             folderPath: List<String>, directoryId: Int) {
        // 이전 선택 초기화
        selectedFolderView?.setBackgroundColor(Color.TRANSPARENT)

        // 현재 선택 저장
        selectedFolderView = folderView
        selectedFolderPath = folderPath.toMutableList()
        selectedFolderContainer = container
        selectedFolderExpandIcon = expandIcon
        selectedDirectoryId = directoryId

        // 선택된 폴더 강조 표시
        folderView.setBackgroundColor(ContextCompat.getColor(this, R.color.main))

        // 로그
        Log.d("SaveLocation", "선택된 폴더 경로: ${folderPath.joinToString(" > ")}, 디렉토리 ID: $directoryId")
    }

    private fun setupDefaultFolders() {
        navigationContainer = binding.folderContainer
        navigationContainer.removeAllViews()  // 기존 뷰 제거

        // 기본 폴더 구조
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

    private fun showAddFolderDialog(parentContainer: LinearLayout?, parentPath: List<String>?) {
        val dialogBinding = DialogAddFolderBinding.inflate(LayoutInflater.from(this))
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogBinding.root)

        // 다이얼로그 제목 설정
        if (parentPath != null && parentPath.isNotEmpty()) {
            val parentFolderName = parentPath.last()
            dialogBinding.dialogTitle.text = "'$parentFolderName' 하위에 폴더 추가"
        } else {
            dialogBinding.dialogTitle.text = "새 폴더 추가"
        }

        val dialog = builder.create()
        dialog.show()

        // 취소 버튼
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // 추가 버튼
        dialogBinding.btnAdd.setOnClickListener {
            val folderName = dialogBinding.etFolderName.text.toString().trim()
            if (folderName.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        // API를 통해 폴더 추가
                        val parentId = if (selectedDirectoryId != null && parentPath != null) selectedDirectoryId else null
                        val response = directoryApi.createDirectory(folderName, parentId)

                        if (response.isSuccess) {
                            // 성공적으로 폴더가 추가됨
                            Toast.makeText(this@SaveLocationActivity, "폴더가 추가되었습니다", Toast.LENGTH_SHORT).show()

                            // 트리 다시 로드
                            loadDirectoryTree()
                        } else {
                            // API 호출은 성공했지만 결과가 실패인 경우
                            Toast.makeText(this@SaveLocationActivity, "폴더 추가 실패: ${response.message}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("SaveLocation", "폴더 추가 중 오류 발생", e)
                        Toast.makeText(this@SaveLocationActivity, "폴더 추가 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()

                        // 오프라인 모드에서는 임시로 UI에만 추가
                        if (parentContainer != null && parentPath != null) {
                            // 선택된 폴더 하위에 추가
                            addSubFolderToSelected(folderName, parentContainer, parentPath)
                        } else {
                            // 최상위 레벨에 추가
                            addMainFolder(folderName, emptyList())
                        }
                    }
                }
                dialog.dismiss()
            } else {
                Toast.makeText(this, "폴더명을 입력해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addSubFolderToSelected(folderName: String, parentContainer: LinearLayout, parentPath: List<String>) {
        // 하위 폴더 생성 및 추가
        val newFolder = FolderItem(folderName, emptyList())
        addSubFolder(newFolder, parentContainer, ArrayList(parentPath))

        // 부모 폴더가 접혀있다면 펼치기
        parentContainer.visibility = View.VISIBLE
        selectedFolderExpandIcon?.setImageResource(R.drawable.ic_collapse)
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
            if (subItemsContainer.childCount > 0 || subItems.isNotEmpty()) {
                if (subItemsContainer.visibility == View.VISIBLE) {
                    subItemsContainer.visibility = View.GONE
                    expandIcon.setImageResource(R.drawable.ic_expand)
                } else {
                    subItemsContainer.visibility = View.VISIBLE
                    expandIcon.setImageResource(R.drawable.ic_collapse)
                }
            }

            // 폴더 선택 처리 (임시 ID 사용)
            selectFolder(folderView, subItemsContainer, expandIcon, mutableListOf(folderName), -99)
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
        val indentLevel = parentPath.size // 부모 경로의 길이가 깊이 레벨
        val params = folderView.layoutParams as? LinearLayout.LayoutParams
            ?: LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.marginStart = (24 * indentLevel).dpToPx() // 깊이에 비례하여 마진 설정
        folderView.layoutParams = params

        folderNameTv.text = folder.name
        folderIcon.setImageResource(R.drawable.ic_folder)

        // 하위 항목 컨테이너 생성
        val subItemsContainer = LinearLayout(this)
        subItemsContainer.orientation = LinearLayout.VERTICAL
        subItemsContainer.visibility = View.GONE

        // 폴더에 하위 항목이 없으면 화살표 아이콘 대신 공백 표시
        if (folder.subItems.isEmpty()) {
            expandIcon.setImageResource(android.R.color.transparent)
        } else {
            expandIcon.setImageResource(R.drawable.ic_expand)

            // 하위 아이템 추가
            for (item in folder.subItems) {
                if (item is FolderItem) {
                    // 현재 경로에 현재 폴더 이름 추가
                    val currentPath = ArrayList(parentPath)
                    currentPath.add(folder.name)
                    addSubFolder(item, subItemsContainer, currentPath)
                }
            }
        }

        // 클릭 리스너: 폴더 열기/닫기 및 선택
        folderView.setOnClickListener {
            // 폴더 열기/닫기 토글
            if (subItemsContainer.childCount > 0 || folder.subItems.isNotEmpty()) {
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

            // 폴더 선택 처리 (임시 ID 사용)
            selectFolder(folderView, subItemsContainer, expandIcon, currentPath, -99)
        }

        container.addView(folderView)
        container.addView(subItemsContainer)
    }

    private fun showLoading(show: Boolean) {
        isLoading = show
        if (show) {
            // 로딩 표시
            binding.folderContainer.visibility = View.GONE
            // progressBar가 있다면 표시
            // binding.progressBar.visibility = View.VISIBLE
        } else {
            // 로딩 숨김
            binding.folderContainer.visibility = View.VISIBLE
            // progressBar가 있다면 숨김
            // binding.progressBar.visibility = View.GONE
        }
    }

    // dp를 픽셀로 변환하는 확장 함수
    private fun Int.dpToPx(): Int {
        val scale = resources.displayMetrics.density
        return (this * scale + 0.5f).toInt()
    }
}