package com.hariku.feature.chatbot.domain.model

import androidx.annotation.DrawableRes
import com.hariku.R

/**
 * Maps stable string avatar keys stored in Firestore to current-build drawable
 * resource IDs and guards UI rendering against missing or stale IDs.
 */
object ChatbotAvatarCatalog {
    const val DEFAULT_AVATAR_KEY = "avatar_hariku"

    @DrawableRes
    val DEFAULT_AVATAR_RES_ID: Int = R.drawable.ic_avatar_hariku

    val customAvatarResIds: List<Int> = listOf(
        R.drawable.ic_customize_cat_01,
        R.drawable.ic_customize_cat_02,
        R.drawable.ic_customize_cat_03,
        R.drawable.ic_customize_cat_04,
        R.drawable.ic_customize_cat_05,
        R.drawable.ic_customize_cat_06,
        R.drawable.ic_customize_cat_07,
        R.drawable.ic_customize_cat_08,
        R.drawable.ic_customize_cat_09,
        R.drawable.ic_customize_cat_10,
        R.drawable.ic_customize_cat_11,
        R.drawable.ic_customize_cat_12
    )

    private val keyToRes: Map<String, Int> = mapOf(
        "cat_01" to R.drawable.ic_customize_cat_01,
        "cat_02" to R.drawable.ic_customize_cat_02,
        "cat_03" to R.drawable.ic_customize_cat_03,
        "cat_04" to R.drawable.ic_customize_cat_04,
        "cat_05" to R.drawable.ic_customize_cat_05,
        "cat_06" to R.drawable.ic_customize_cat_06,
        "cat_07" to R.drawable.ic_customize_cat_07,
        "cat_08" to R.drawable.ic_customize_cat_08,
        "cat_09" to R.drawable.ic_customize_cat_09,
        "cat_10" to R.drawable.ic_customize_cat_10,
        "cat_11" to R.drawable.ic_customize_cat_11,
        "cat_12" to R.drawable.ic_customize_cat_12,
        "avatar_hariku" to R.drawable.ic_avatar_hariku,
        "avatar_hariku2" to R.drawable.ic_avatar_hariku2,
        "avatar_maman" to R.drawable.ic_avatar_maman,
        "avatar_noir" to R.drawable.ic_avatar_noir,
        "avatar_quesar" to R.drawable.ic_avatar_quesar
    )

    private val resToKey: Map<Int, String> = keyToRes.entries.associate { (k, v) -> v to k }

    private val presetNameToKey: Map<String, String> = mapOf(
        "hariku" to "cat_01",
        "luna" to "cat_02",
        "moy" to "cat_03",
        "mochi" to "cat_04",
        "midnight" to "cat_05"
    )

    fun keyForDrawable(@DrawableRes resId: Int): String? = resToKey[resId]

    fun keyForPresetName(botName: String?): String? =
        botName?.trim()?.lowercase()?.let(presetNameToKey::get)

    @DrawableRes
    fun resolveDrawableResId(
        avatarKey: String?,
        legacyResId: Int? = null,
        botName: String? = null
    ): Int {
        avatarKey?.takeIf { it.isNotBlank() }?.let { key ->
            keyToRes[key]?.let { return it }
        }
        if (legacyResId != null && legacyResId in resToKey) {
            return legacyResId
        }
        keyForPresetName(botName)?.let { presetKey ->
            keyToRes[presetKey]?.let { return it }
        }
        return DEFAULT_AVATAR_RES_ID
    }

    @DrawableRes
    fun safeDrawableResId(resId: Int?): Int =
        if (resId != null && resId in resToKey) resId else DEFAULT_AVATAR_RES_ID
}
