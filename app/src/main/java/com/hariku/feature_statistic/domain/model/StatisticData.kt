package com.hariku.feature_statistic.domain.model

import com.hariku.feature.mood.domain.model.MoodType

data class SentimentData(
    val negative: Float,
    val positive: Float,
    val neutral: Float
)

data class MoodStatItem(
    val moodType: MoodType,
    val count: Int,
    val percentage: Float
)

data class WeeklySentiment(
    val weekLabel: String,
    val negative: Float,
    val positive: Float,
    val neutral: Float
)
