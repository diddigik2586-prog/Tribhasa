package org.tribhasa.sodhamitra.i18n

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/** The ten interface languages, in the approved order. Each is listed by its own name in its own script. */
enum class AppLanguage(val code: String, val nativeName: String) {
    ENGLISH("en", "English"),
    KANNADA("kn", "ಕನ್ನಡ"),
    SANSKRIT("sa", "संस्कृतम्"),
    TELUGU("te", "తెలుగు"),
    HINDI("hi", "हिन्दी"),
    TAMIL("ta", "தமிழ்"),
    BENGALI("bn", "বাংলা"),
    GUJARATI("gu", "ગુજરાતી"),
    MALAYALAM("ml", "മലയാളം"),
    ODIA("or", "ଓଡ଼ିଆ");   // Android's language code for Odia is "or"

    val locale: Locale get() = Locale(code)

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

/** The chosen interface language, kept in a tiny private preferences file. */
object LanguageStore {
    private const val FILE = "settings"
    private const val KEY = "language"

    fun get(context: Context): AppLanguage =
        AppLanguage.fromCode(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null))

    fun set(context: Context, language: AppLanguage) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, language.code).commit()
    }

    /** A context whose resources use the chosen language (all strings are bundled in the app; nothing is downloaded). */
    fun wrap(base: Context): Context = wrap(base, get(base))

    fun wrap(base: Context, language: AppLanguage): Context {
        val locale = language.locale
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }
}
