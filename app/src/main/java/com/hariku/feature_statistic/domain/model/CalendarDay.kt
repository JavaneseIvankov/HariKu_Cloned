package com.hariku.feature_statistic.domain.model

import com.hariku.feature.mood.domain.model.MoodType

data class CalendarDay(
    val day: Int,
    val mood: MoodType? = null,
    val isEmpty: Boolean = false
)
