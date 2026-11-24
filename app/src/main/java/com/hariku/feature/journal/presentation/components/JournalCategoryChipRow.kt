package com.hariku.feature.journal.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hariku.R
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.presentation.JournalDesignTokens

data class JournalCategoryVisual(
    val tag: JournalCategoryTag,
    @param:DrawableRes val iconRes: Int,
    @param:DrawableRes val bannerRes: Int
)

object JournalCategoryCatalog {
    fun of(tag: JournalCategoryTag): JournalCategoryVisual = when (tag) {
        JournalCategoryTag.AKADEMIK -> JournalCategoryVisual(
            tag = tag,
            iconRes = R.drawable.produktivitas_rencana_kerja,
            bannerRes = R.drawable.ic_kategori_strategi_coping
        )
        JournalCategoryTag.KECEMASAN -> JournalCategoryVisual(
            tag = tag,
            iconRes = R.drawable.cemas_tenangkan_diri,
            bannerRes = R.drawable.ic_kategori_kecemasan
        )
        JournalCategoryTag.STRES -> JournalCategoryVisual(
            tag = tag,
            iconRes = R.drawable.stress_awan_pikiran,
            bannerRes = R.drawable.ic_kategori_stres
        )
        JournalCategoryTag.TIDUR -> JournalCategoryVisual(
            tag = tag,
            iconRes = R.drawable.tidur_kualitas_tidur,
            bannerRes = R.drawable.ic_kategori_tidur
        )
        JournalCategoryTag.HUBUNGAN -> JournalCategoryVisual(
            tag = tag,
            iconRes = R.drawable.positivitas_kualitas_diri,
            bannerRes = R.drawable.ic_kategori_hubungan
        )
        JournalCategoryTag.PRODUKTIVITAS -> JournalCategoryVisual(
            tag = tag,
            iconRes = R.drawable.produktivitas_latihan_fokus,
            bannerRes = R.drawable.ic_kategori_motivasi_diri
        )
        JournalCategoryTag.POSITIVITAS -> JournalCategoryVisual(
            tag = tag,
            iconRes = R.drawable.positivitas_apresiasi,
            bannerRes = R.drawable.ic_kategori_motivasi_diri
        )
        JournalCategoryTag.UMUM -> JournalCategoryVisual(
            tag = tag,
            iconRes = R.drawable.positivitas_hal_baik,
            bannerRes = R.drawable.ic_kategori_kesadaran_penuh
        )
    }
}

@Composable
fun JournalCategoryChipRow(
    selectedCategory: JournalCategoryTag?,
    onSelectCategory: (JournalCategoryTag?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp)
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "all") {
            CategoryFilterChip(
                label = "Semua",
                iconRes = null,
                isSelected = selectedCategory == null,
                onClick = { onSelectCategory(null) }
            )
        }
        items(
            items = JournalCategoryTag.entries,
            key = { it.name }
        ) { category ->
            val visual = JournalCategoryCatalog.of(category)
            CategoryFilterChip(
                label = category.label,
                iconRes = visual.iconRes,
                isSelected = selectedCategory == category,
                onClick = { onSelectCategory(category) }
            )
        }
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    @DrawableRes iconRes: Int?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val backgroundColor = if (isSelected) {
        JournalDesignTokens.PeachHighlight
    } else {
        JournalDesignTokens.CardWhite
    }
    val borderColor = if (isSelected) {
        JournalDesignTokens.PrimaryCta
    } else {
        Color(0xFFE5DFDC)
    }

    Row(
        modifier = Modifier
            .clip(shape)
            .background(backgroundColor)
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = borderColor, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(7.dp))
        }
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = JournalDesignTokens.TextPrimary
        )
    }
}
