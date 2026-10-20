package org.tribhasa.sodhamitra.ui.theme

import androidx.annotation.FontRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import org.tribhasa.sodhamitra.R

/** The bundled fonts. Nothing is downloaded: every file is inside the app (res/font). */
object ScriptFonts {
    private data class Faces(@FontRes val regular: Int, @FontRes val medium: Int, @FontRes val bold: Int)

    private val faces: Map<Script, Faces> = mapOf(
        Script.LATIN to Faces(R.font.ubuntu_regular, R.font.ubuntu_medium, R.font.ubuntu_bold),
        Script.LATIN_EXT to Faces(R.font.noto_latin_regular, R.font.noto_latin_medium, R.font.noto_latin_bold),
        Script.DEVANAGARI to Faces(R.font.noto_devanagari_regular, R.font.noto_devanagari_medium, R.font.noto_devanagari_bold),
        Script.BENGALI to Faces(R.font.noto_bengali_regular, R.font.noto_bengali_medium, R.font.noto_bengali_bold),
        Script.GUJARATI to Faces(R.font.noto_gujarati_regular, R.font.noto_gujarati_medium, R.font.noto_gujarati_bold),
        // Noto Sans Odia has no Medium cut: Regular stands in for it.
        Script.ODIA to Faces(R.font.noto_oriya_regular, R.font.noto_oriya_regular, R.font.noto_oriya_bold),
        Script.TAMIL to Faces(R.font.noto_tamil_regular, R.font.noto_tamil_medium, R.font.noto_tamil_bold),
        Script.TELUGU to Faces(R.font.noto_telugu_regular, R.font.noto_telugu_medium, R.font.noto_telugu_bold),
        Script.KANNADA to Faces(R.font.noto_kannada_regular, R.font.noto_kannada_medium, R.font.noto_kannada_bold),
        Script.MALAYALAM to Faces(R.font.noto_malayalam_regular, R.font.noto_malayalam_medium, R.font.noto_malayalam_bold),
    )

    @FontRes
    fun resource(script: Script, weight: Int): Int {
        val f = faces.getValue(script)
        return when { weight >= 600 -> f.bold; weight >= 450 -> f.medium; else -> f.regular }
    }

    private val families: Map<Script, FontFamily> = faces.mapValues { (_, f) ->
        FontFamily(
            Font(f.regular, FontWeight.Normal),
            Font(f.medium, FontWeight.Medium),
            Font(f.bold, FontWeight.Bold),
        )
    }

    fun family(script: Script): FontFamily = families.getValue(script)

    /** Ubuntu: the font of all English interface text. */
    val ubuntu: FontFamily get() = family(Script.LATIN)

    /** Text with one font span per script run. Latin runs use Ubuntu; Indian scripts use their own bundled Noto face. */
    fun annotate(text: String): AnnotatedString {
        val runs = ScriptRuns.runs(text)
        return buildAnnotatedString {
            append(text)
            for (r in runs) addStyle(SpanStyle(fontFamily = family(r.script)), r.start, r.end)
        }
    }
}

/** Gives text typed into a field the right bundled font per script, without changing a single character of it. */
object ScriptVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(ScriptFonts.annotate(text.text), OffsetMapping.Identity)
}

@Composable
fun rememberScriptText(text: String): AnnotatedString = remember(text) { ScriptFonts.annotate(text) }
