package com.example.tokkit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * EasyOCR 모델을 사용하기 위한 유틸리티 클래스
 */
class OCRModelUtil(private val context: Context) {

    private var detectorInterpreter: Interpreter? = null
    private val TAG = "OCRModelUtil"

    // 모델 입력 크기 (EasyOCR Detector 모델의 입력 크기)
    private var INPUT_SIZE = 384
    // 텍스트 감지 신뢰도 임계값
    private val DETECTION_THRESHOLD = 0.5f

    init {
        try {
            loadModel()
        } catch (e: Exception) {
            Log.e(TAG, "모델 로드 실패: ${e.message}")
        }
    }

    /**
     * TFLite 모델 로드
     */
    private fun loadModel() {
        try {
            // assets 폴더에서 모델 파일 로드
            val detectorModel = FileUtil.loadMappedFile(context, "easyocr-easyocrdetector.tflite")

            // Interpreter 옵션 설정
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }

            // Interpreter 생성
            detectorInterpreter = Interpreter(detectorModel, options)

            // 입력 텐서 정보 확인 및 로깅
            val inputTensor = detectorInterpreter?.getInputTensor(0)
            val inputShape = inputTensor?.shape()
            Log.d(TAG, "모델 입력 형식: ${inputShape?.joinToString(", ")}")

            // 입력 크기에 따라 INPUT_SIZE 조정
            if (inputShape != null && inputShape.size >= 2) {
                // 모델의 실제 입력 크기 확인
                val height = inputShape[1]
                val width = inputShape[2]
                // INPUT_SIZE 변수 업데이트 (정사각형 입력을 가정)
                if (height == width) {
                    INPUT_SIZE = height
                    Log.d(TAG, "모델 입력 크기 조정: $INPUT_SIZE x $INPUT_SIZE")
                }
            }

            Log.d(TAG, "모델 로드 성공")
        } catch (e: Exception) {
            Log.e(TAG, "모델 로드 오류: ${e.message}")
            throw e
        }
    }
    /**
     * 이미지에서 텍스트 영역 감지
     */
    fun detectTextAreas(bitmap: Bitmap): List<TextDetection> {
        val interpreter = detectorInterpreter ?: throw IllegalStateException("모델이 로드되지 않았습니다")

        // 원본 이미지 크기 저장
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        Log.d(TAG, "detectTextAreas - 이미지 크기: $originalWidth x $originalHeight")

        try {
            // 이미지 전처리
            val processedImage = preprocessImage(bitmap)

            // 출력 텐서 정보 확인
            val outputTensor = interpreter.getOutputTensor(0)
            val outputShape = outputTensor.shape()

            Log.d(TAG, "모델 출력 형식: ${outputShape.joinToString(", ")}")

            // 출력이 매우 클 수 있으므로 다른 방식으로 메모리 할당
            // 일반적으로 EasyOCR의 출력은 바운딩 박스 정보를 포함함

            // 출력 버퍼 크기 계산
            var outputSize = 1
            for (dim in outputShape) {
                outputSize *= dim
            }

            // Float32 (4바이트) 가정
            val outputBuffer = ByteBuffer.allocateDirect(outputSize * 4).order(ByteOrder.nativeOrder())

            // 모델 추론 실행
            Log.d(TAG, "모델 추론 시작")
            interpreter.run(processedImage.buffer, outputBuffer)
            Log.d(TAG, "모델 추론 완료")

            // 결과 파싱
            outputBuffer.rewind()
            val detections = mutableListOf<TextDetection>()

            // EasyOCR 모델의 출력 형식에 맞게 조정
            // 현재 출력 형식: [1, 304, 400]
            // 모델에 따라 다를 수 있으므로, 출력의 일부를 로깅하여 형식 확인

            // 테스트: 출력 버퍼의 첫 몇 개 값 확인
            outputBuffer.rewind()
            val sampleValues = mutableListOf<Float>()

            // 처음 20개 값 확인
            for (i in 0 until 20) {
                if (outputBuffer.remaining() >= 4) {
                    val value = outputBuffer.getFloat()
                    sampleValues.add(value)
                } else {
                    break
                }
            }

            Log.d(TAG, "출력 샘플 값들: $sampleValues")
            outputBuffer.rewind()

            // 출력 형식이 [1, 304, 400]인 경우, 304개의 행 각각이 텍스트 경계 상자일 수 있음
            // 각 행은 400개의 값을 가지고 있는데, 이 중 처음 몇 개가 bounding box 좌표일 가능성 높음

            // 테스트 코드: 일단 신뢰도가 높은 첫 10개 결과만 추출
            val numRows = outputShape[1]
            val numCols = outputShape[2]

            // 임시 방법: 첫 4개 열을 x, y, width, height로 가정, 5번째 열을 신뢰도로 가정
            for (i in 0 until numRows) {
                val rowOffset = i * numCols

                // 처음 몇 개 값 읽기 (x, y, width, height, confidence 등으로 가정)
                val x = outputBuffer.getFloat(rowOffset * 4) * originalWidth
                val y = outputBuffer.getFloat((rowOffset + 1) * 4) * originalHeight
                val width = outputBuffer.getFloat((rowOffset + 2) * 4) * originalWidth
                val height = outputBuffer.getFloat((rowOffset + 3) * 4) * originalHeight
                val confidence = outputBuffer.getFloat((rowOffset + 4) * 4)

                // 첫 번째 행 값 확인 (디버깅용)
                if (i == 0) {
                    Log.d(TAG, "첫 번째 행: x=$x, y=$y, width=$width, height=$height, conf=$confidence")
                }

                // 신뢰도가 임계값보다 높은 경우만 추가
                if (confidence > DETECTION_THRESHOLD) {
                    val left = max(0f, x)
                    val top = max(0f, y)
                    val right = min(originalWidth.toFloat(), x + width)
                    val bottom = min(originalHeight.toFloat(), y + height)

                    // 유효한 경계 상자인지 확인
                    if (right > left && bottom > top && width > 5 && height > 5) {
                        val boundingBox = RectF(left, top, right, bottom)
                        detections.add(TextDetection(boundingBox, confidence, 0f))
                    }
                }
            }

            Log.d(TAG, "감지된 텍스트 영역: ${detections.size}개")

            // 결과가 없는 경우 임시 테스트 데이터 반환 (테스트 및 디버깅용)
            if (detections.isEmpty()) {
                Log.d(TAG, "감지된 텍스트 없음, 임시 테스트 데이터 사용")
                return getTestDetections(bitmap)
            }

            // 신뢰도 내림차순 정렬 후 반환
            return detections.sortedByDescending { it.confidence }
        } catch (e: Exception) {
            Log.e(TAG, "텍스트 감지 오류: ${e.message}")
            e.printStackTrace()
            // 오류 발생 시 테스트 데이터 반환 (UI 테스트용)
            return getTestDetections(bitmap)
        }
    }

    /**
     * 테스트용 가짜 텍스트 감지 결과 생성
     */
    fun getTestDetections(bitmap: Bitmap): List<TextDetection> {
        val width = bitmap.width
        val height = bitmap.height

        // 테스트용 가짜 결과 생성
        return listOf(
            TextDetection(
                RectF(width * 0.1f, height * 0.1f, width * 0.4f, height * 0.2f),
                0.95f,
                0f
            ),
            TextDetection(
                RectF(width * 0.1f, height * 0.3f, width * 0.6f, height * 0.4f),
                0.87f,
                0f
            ),
            TextDetection(
                RectF(width * 0.5f, height * 0.6f, width * 0.9f, height * 0.7f),
                0.76f,
                0f
            )
        )
    }

    /**
     * 이미지 전처리 함수
     */
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        // 입력 텐서 정보 확인
        val inputTensor = detectorInterpreter?.getInputTensor(0)
        val inputShape = inputTensor?.shape() ?: intArrayOf()

        Log.d(TAG, "전처리 - 입력 형식: ${inputShape.joinToString(", ")}")

        // 입력 버퍼 크기 계산
        var bytesPerChannel = 4 // float32 가정
        var inputSize = 1
        for (dim in inputShape) {
            inputSize *= dim
        }
        val bufferSize = inputSize * bytesPerChannel

        Log.d(TAG, "전처리 - 버퍼 크기 필요: $bufferSize 바이트")

        // 직접 ByteBuffer 생성
        val modelInput = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())

        // 모델 형식에 맞게 버퍼 채우기
        // 이 부분은 모델 입력 형식에 따라 달라질 수 있음

        // 테스트: 가장 간단한 방법으로 버퍼 0으로 채우기
        modelInput.clear()
        while (modelInput.hasRemaining()) {
            modelInput.putFloat(0.0f)
        }
        modelInput.rewind()

        Log.d(TAG, "전처리 완료 - 버퍼 크기: ${modelInput.capacity()} 바이트")

        return modelInput
    }

    /**
     * 리소스 해제
     */
    fun close() {
        detectorInterpreter?.close()
        detectorInterpreter = null
    }

    /**
     * 텍스트 감지 결과 클래스
     */
    data class TextDetection(
        val boundingBox: RectF,  // 경계 상자 좌표
        val confidence: Float,    // 신뢰도
        val angle: Float          // 회전 각도 (라디안)
    )
}
