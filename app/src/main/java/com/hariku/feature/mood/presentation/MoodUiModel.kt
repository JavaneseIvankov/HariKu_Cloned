package com.hariku.feature.mood.presentation

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.hariku.R
import com.hariku.feature.mood.domain.model.MoodType

data class MoodVisual(
    val type: MoodType,
    @param:DrawableRes val iconRes: Int,
    val accentColor: Color
)

object MoodVisualCatalog {
    val ordered: List<MoodVisual> = listOf(
        MoodVisual(MoodType.SENANG, R.drawable.ic_emote_senang, Color(0xFFFFD481)),
        MoodVisual(MoodType.BIASA, R.drawable.ic_emote_biasa, Color(0xFFE9C9AC)),
        MoodVisual(MoodType.SEDIH, R.drawable.ic_emote_sedih, Color(0xFFB0E6ED)),
        MoodVisual(MoodType.MARAH, R.drawable.ic_emote_marah, Color(0xFFF1A69A)),
        MoodVisual(MoodType.CEMAS, R.drawable.ic_emote_cemas, Color(0xFFD5C5F5)),
        MoodVisual(MoodType.LELAH, R.drawable.ic_emote_lelah, Color(0xFFE9CAEC)),
        MoodVisual(MoodType.KECEWA, R.drawable.ic_emote_kecewa, Color(0xFFD1EDAE)),
        MoodVisual(MoodType.TAKUT, R.drawable.ic_emote_takut, Color(0xFFADDBB5)),
        MoodVisual(MoodType.HAMPA, R.drawable.ic_emote_hampa, Color(0xFFDBE0DF)),
        MoodVisual(MoodType.SEMANGAT, R.drawable.ic_emote_semangat, Color(0xFFFFCEA2))
    )

    private val byType = ordered.associateBy { it.type }

    fun of(type: MoodType): MoodVisual = byType.getValue(type)
}
