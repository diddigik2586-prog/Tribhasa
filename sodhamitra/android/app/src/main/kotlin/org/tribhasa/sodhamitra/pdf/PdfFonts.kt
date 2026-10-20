package org.tribhasa.sodhamitra.pdf

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import org.tribhasa.sodhamitra.ui.theme.Script
import org.tribhasa.sodhamitra.ui.theme.ScriptFonts
import org.tribhasa.sodhamitra.ui.theme.ScriptRuns

/** The same bundled fonts as on screen, as Typefaces for drawing into a PDF. */
class PdfFonts(private val context: Context) {
    private val cache = HashMap<Pair<Script, Int>, Typeface>()

    fun typeface(script: Script, weight: Int): Typeface {
        val bucket = if (weight >= 600) 700 else if (weight >= 450) 500 else 400
        return cache.getOrPut(script to bucket) { context.resources.getFont(ScriptFonts.resource(script, bucket)) }
    }

    private class FaceSpan(private val face: Typeface) : MetricAffectingSpan() {
        override fun updateDrawState(tp: TextPaint) { tp.typeface = face }
        override fun updateMeasureState(tp: TextPaint) { tp.typeface = face }
    }

    /** text with one typeface span per script run: Ubuntu for English, the matching Noto face for each Indian script */
    fun spannable(text: String, weight: Int = 400): SpannableString {
        val s = SpannableString(text)
        for (r in ScriptRuns.runs(text)) s.setSpan(FaceSpan(typeface(r.script, weight)), r.start, r.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return s
    }
}
