package com.zawa8.xnglopxd

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface

/**
 * Loads and caches Typefaces from assets/fonts/, and reads/writes the
 * user's selected font preference (same approach as xNglobord's
 * FontManager). There is always one xNglo font selected; the default
 * is [LocalFonts.DEFAULT_FONT_ID].
 */
object FontManager {
    private const val PREFS_NAME = "xnglopxd_prefs"
    private const val PREF_KEY_FONT = "user-local-font"

    private val cache: MutableMap<String, Typeface?> = mutableMapOf()

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedFontId(context: Context): String =
        prefs(context).getString(PREF_KEY_FONT, LocalFonts.DEFAULT_FONT_ID)
            ?: LocalFonts.DEFAULT_FONT_ID

    fun setSelectedFontId(context: Context, fontId: String) {
        prefs(context).edit().putString(PREF_KEY_FONT, fontId).apply()
    }

    fun getSelectedOption(context: Context): LocalFontOption =
        LocalFonts.byId(getSelectedFontId(context))
            ?: LocalFonts.ALL.first { it.id == LocalFonts.DEFAULT_FONT_ID }

    /** Typeface for [option], or null if the asset is missing/unreadable (caller falls back to default rendering). */
    fun loadTypeface(context: Context, option: LocalFontOption): Typeface? {
        cache[option.assetFileName]?.let { return it }
        return try {
            Typeface.createFromAsset(context.assets, "fonts/${option.assetFileName}")
                .also { cache[option.assetFileName] = it }
        } catch (e: Exception) {
            null
        }
    }
}
