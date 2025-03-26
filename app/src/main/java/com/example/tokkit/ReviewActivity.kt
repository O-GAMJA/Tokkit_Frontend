package com.example.tokkit

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlin.math.exp

class ReviewActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private lateinit var btnReview: Button
    private var reviewTimes = mutableListOf<Float>()
    private val baseLambda = 0.015f
    private val totalDuration = 1440f // 1일 = 1440분

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        lineChart = findViewById(R.id.lineChart)
        btnReview = findViewById(R.id.btnReview)

        reviewTimes.add(0f) // 최초 학습
        drawAllCurves()

        btnReview.setOnClickListener {
            val lastTime = reviewTimes.last()
            val newTime = lastTime + 180f // 3시간 후 복습
            reviewTimes.add(newTime)
            drawAllCurves()
        }
    }

    private fun drawAllCurves() {
        val dataSets = mutableListOf<ILineDataSet>()

        var previousEndValue = 100f // 초기 기억률

        for ((index, startTime) in reviewTimes.withIndex()) {
            val entries = ArrayList<Entry>()

            val lambda = baseLambda / (index + 1)
            val prevStart = if (index == 0) 0f else reviewTimes[index - 1]
            val prevLambda = baseLambda / (index)

            for (t in prevStart.toInt()..(startTime + totalDuration).toInt() step 30) {
                val dt = t - startTime

                val retention = when {
                    t < startTime -> {
                        // 이전 곡선 계산
                        val dtPrev = t - prevStart
                        1.0f * exp(-prevLambda * dtPrev)
                    }
                    else -> {
                        // 복습 이후 새 곡선 (항상 100에서 시작)
                        1.0f * exp(-lambda * (t - startTime))
                    }
                }

                entries.add(Entry(t.toFloat(), retention * 100))
            }

            val dataSet = LineDataSet(entries, "${index + 1}차 학습")

            val color = if (index == reviewTimes.size - 1) {
                Color.argb(255, 70 + index * 40, 100, 255 - index * 40)
            } else {
                Color.argb(100, 70 + index * 40, 100, 255 - index * 40)
            }

            dataSet.color = color
            dataSet.setDrawCircles(false)
            dataSet.lineWidth = 2f
            dataSet.setDrawValues(false)

            dataSets.add(dataSet)
        }

        val lineData = LineData(dataSets)
        lineChart.data = lineData

        lineChart.axisLeft.axisMaximum = 100f
        lineChart.axisLeft.axisMinimum = 0f
        lineChart.axisRight.isEnabled = false
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = true

        lineChart.invalidate()
    }

}
