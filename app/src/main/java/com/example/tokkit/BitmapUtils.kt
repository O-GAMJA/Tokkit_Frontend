package com.example.tokkit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.content.Context
import java.io.IOException

/**
 * 비트맵 처리를 위한 유틸리티 클래스
 */
object BitmapUtils {

    /**
     * Uri에서 비트맵 로드 및 회전 보정
     */
    fun loadBitmapFromUri(context: Context, uri: Uri, maxSize: Int = 1024): Bitmap? {
        return try {
            // 먼저 이미지 크기 정보만 가져옴
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            // 적절한 샘플링 비율 계산
            val sampleSize = calculateInSampleSize(options, maxSize, maxSize)

            // 실제 비트맵 로드
            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inJustDecodeBounds = false
            }

            val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, loadOptions)
            } ?: return null

            // 이미지 회전 처리
            correctImageOrientation(context, uri, bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 이미지 회전 보정
     */
    private fun correctImageOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        var rotationDegrees = 0

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                rotationDegrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // 회전이 필요하면 회전 적용
        return if (rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    /**
     * 이미지 크기에 맞는 샘플링 비율 계산
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // 원본 이미지 크기
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // 대상 크기에 가장 가까운 샘플링 비율 계산
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * 비트맵 크기 조정
     */
    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // 이미 적절한 크기면 원본 반환
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        // 비율 계산
        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight

        // 가로세로 비율 유지하면서 크기 조정
        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth / ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }
}