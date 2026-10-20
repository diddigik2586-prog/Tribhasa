package org.tribhasa.sodhamitra.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.OutputStream
import org.tribhasa.sodhamitra.ui.theme.Script

/** Where finished pages go: a real PDF file, a throw-away counter, or (in tests) bitmaps that can be looked at. */
interface PageSink {
    fun begin(width: Int, height: Int): Canvas
    fun end()
}

class PdfFileSink(private val doc: PdfDocument) : PageSink {
    private var page: PdfDocument.Page? = null
    private var n = 0
    override fun begin(width: Int, height: Int): Canvas {
        page = doc.startPage(PdfDocument.PageInfo.Builder(width, height, ++n).create())
        return page!!.canvas
    }
    override fun end() { doc.finishPage(page) }
}

/** the first pass: lays everything out only to learn how many pages there will be */
class CountingSink : PageSink {
    var pages = 0
    private val bmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    override fun begin(width: Int, height: Int): Canvas { pages++; return Canvas(bmp) }
    override fun end() {}
}

/** renders each page to a bitmap (scale 1 = 72 dpi); used by the automated tests to inspect the layout */
class BitmapSink(private val scale: Float = 1.5f) : PageSink {
    val pages = ArrayList<Bitmap>()
    override fun begin(width: Int, height: Int): Canvas {
        val b = Bitmap.createBitmap((width * scale).toInt(), (height * scale).toInt(), Bitmap.Config.ARGB_8888)
        pages.add(b)
        return Canvas(b).apply { drawColor(Color.WHITE); scale(scale, scale) }
    }
    override fun end() {}
}

/**
 * Lays a [PdfDoc] out on A4 pages and draws it. Every page reserves room at the bottom for the footer, so tables and text
 * can never run into it; every page gets its page number and the two footer lines. All units are PDF points (1/72 inch).
 */
class PdfRenderer(private val fonts: PdfFonts, private val doc: PdfDoc, private val sink: PageSink, private val totalPages: Int) {
    private val pageW = if (doc.landscape) 842f else 595f
    private val pageH = if (doc.landscape) 595f else 842f
    private val margin = 40f
    private val top = 40f
    private val footerReserve = 78f
    private val bottom = pageH - footerReserve
    private val contentW = pageW - 2 * margin
    private val ink = Color.BLACK
    private val accent = 0xFF1D1673.toInt()
    private val muted = 0xFF555555.toInt()
    private val line = Paint().apply { color = 0xFF9A9A9A.toInt(); style = Paint.Style.STROKE; strokeWidth = 0.5f }
    private val fillHead = Paint().apply { color = 0xFFE5E2FF.toInt(); style = Paint.Style.FILL }
    private val fillLabel = Paint().apply { color = 0xFFF3EDE0.toInt(); style = Paint.Style.FILL }

    private lateinit var canvas: Canvas
    private var y = top
    private var pageNo = 0
    private var open = false

    fun render() {
        newPage()
        drawFirstPageHeader()
        for (b in doc.blocks) when (b) {
            is H1 -> heading(b.text, 15f, accent, rule = true, before = 14f)
            is H2 -> heading(b.text, 12.5f, accent, rule = false, before = 10f)
            is H3 -> heading(b.text, 10.5f, ink, rule = false, before = 8f)
            is Para -> paragraph(b.text)
            is Grid -> grid(b)
            is RecordTables -> b.records.forEach { recordTable(it) }
        }
        finishPage()
    }

    // ---- pages -----------------------------------------------------------------------------------------------
    private fun newPage() {
        if (open) finishPage()
        canvas = sink.begin(pageW.toInt(), pageH.toInt())
        open = true; pageNo++; y = top
    }

    private fun finishPage() {
        drawFooter(); sink.end(); open = false
    }

    private fun drawFooter() {
        val rule = pageH - footerReserve + 6f
        canvas.drawLine(margin, rule, pageW - margin, rule, line)
        var yy = rule + 3f
        // the page number stands on its own line; the two exact footer lines follow below it
        for ((text, size) in listOf(
            String.format(doc.pageOfFormat, pageNo, totalPages) to 8.5f, doc.footerLine1 to 8.5f, doc.footerLine2 to 8.5f,
        )) {
            val l = layout(text, contentW, size, muted, align = Layout.Alignment.ALIGN_CENTER)
            canvas.save(); canvas.translate(margin, yy); l.draw(canvas); canvas.restore()
            yy += l.height + 1f
        }
    }

    private fun ensure(h: Float) { if (y + h > bottom && y > top + 1f) newPage() }

    // ---- text -----------------------------------------------------------------------------------------------
    private fun layout(text: String, width: Float, size: Float, color: Int = ink, weight: Int = 400, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply { textSize = size; this.color = color; typeface = fonts.typeface(Script.LATIN, weight) }
        val s = fonts.spannable(text, weight)
        return StaticLayout.Builder.obtain(s, 0, s.length, paint, width.toInt().coerceAtLeast(1))
            .setAlignment(align).setLineSpacing(0f, 1.1f).setIncludePad(false).build()
    }

    private fun heading(text: String, size: Float, color: Int, rule: Boolean, before: Float) {
        val l = layout(text, contentW, size, color, 700)
        // a heading is never left alone at the bottom of a page: keep room for a few lines after it
        ensure(before + l.height + 40f)
        if (y > top) y += before
        canvas.save(); canvas.translate(margin, y); l.draw(canvas); canvas.restore()
        y += l.height + 3f
        if (rule) { canvas.drawLine(margin, y, pageW - margin, y, Paint().apply { this.color = accent; strokeWidth = 1f }); y += 5f }
    }

    /** running text that may be longer than a page: drawn page by page, line by line */
    private fun paragraph(text: String) {
        if (text.isBlank()) return
        val l = layout(text, contentW, 10f)
        flow(l, margin)
        y += 6f
    }

    private fun flow(l: StaticLayout, x: Float) {
        var first = 0
        while (first < l.lineCount) {
            if (bottom - y < l.getLineBottom(first) - l.getLineTop(first) && y > top + 1f) newPage()
            val avail = bottom - y
            val startTop = l.getLineTop(first)
            var last = first
            while (last < l.lineCount && l.getLineBottom(last) - startTop <= avail) last++
            if (last == first) last = first + 1               // safety: always make progress
            val h = (l.getLineBottom(last - 1) - startTop).toFloat()
            canvas.save()
            canvas.clipRect(x, y, x + l.width, y + h)
            canvas.translate(x, y - startTop)
            l.draw(canvas)
            canvas.restore()
            y += h
            first = last
            if (first < l.lineCount) newPage()
        }
    }

    // ---- first-page header (profile) and title ---------------------------------------------------------------
    private fun drawFirstPageHeader() {
        val p = doc.profile
        if (p != null && (p.lines.isNotEmpty() || p.photo != null)) {
            val photoW = if (p.photo != null) 62f else 0f
            val gap = if (p.photo != null) 12f else 0f
            var textH = 0f
            val ls = p.lines.mapIndexed { i, t -> layout(t, contentW - photoW - gap, if (i == 0) 11f else 9f, ink, if (i == 0) 700 else 400) }
            for (l in ls) textH += l.height + 1f
            val h = maxOf(textH, if (p.photo != null) 62f else 0f)
            ensure(h)
            p.photo?.let {
                val r = RectF(margin, y, margin + photoW, y + 62f)
                canvas.drawBitmap(it, null, r, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
                canvas.drawRect(r, line)
            }
            var yy = y
            for (l in ls) { canvas.save(); canvas.translate(margin + photoW + gap, yy); l.draw(canvas); canvas.restore(); yy += l.height + 1f }
            y += h + 6f
            canvas.drawLine(margin, y, pageW - margin, y, line); y += 12f
        }
        val t = layout(doc.title, contentW, 19f, accent, 700)
        flow(t, margin); y += 2f
        val s = layout(doc.subtitle, contentW, 10f, muted)
        flow(s, margin); y += 8f
    }

    // ---- tables -----------------------------------------------------------------------------------------------
    private val pad = 3f

    private fun gridFont(cols: Int) = when { cols >= 14 -> 6.4f; cols >= 11 -> 6.8f; cols >= 8 -> 7.6f; else -> 8.6f }

    private fun grid(g: Grid) {
        val total = g.weights.sum()
        val widths = g.weights.map { it / total * contentW }
        val size = gridFont(g.headers.size)
        val header = { g.headers.mapIndexed { i, t -> layout(t, widths[i] - 2 * pad, size, ink, 700) } }
        table(widths, header, g.rows.map { r -> r.mapIndexed { i, t -> layout(t, widths[i] - 2 * pad, size) } })
    }

    private fun recordTable(r: RecordTable) {
        val widths = listOf(contentW * 0.32f, contentW * 0.68f)
        val size = 9f
        val header = { listOf(layout(r.caption, contentW - 2 * pad, 10f, accent, 700)) }
        val rows = r.rows.map { (k, v) -> listOf(layout(k, widths[0] - 2 * pad, size, ink, 500), layout(v, widths[1] - 2 * pad, size)) }
        table(widths, header, rows, fillFirstColumn = true, spanHeader = true)
        y += 10f
    }

    private fun table(
        widths: List<Float>, header: () -> List<StaticLayout>, rows: List<List<StaticLayout>>,
        fillFirstColumn: Boolean = false, spanHeader: Boolean = false,
    ) {
        val hdrCells = header()
        fun drawHeader() {
            val ws = if (spanHeader) listOf(widths.sum()) else widths
            drawRowChunk(ws, hdrCells, IntArray(hdrCells.size), IntArray(hdrCells.size) { hdrCells[it].lineCount }, fillHead, false)
        }
        val headerH = rowHeight(hdrCells)
        ensure(headerH + (rows.firstOrNull()?.let { minOf(rowHeight(it), 60f) } ?: 0f))
        drawHeader()
        for (cells in rows) {
            val h = rowHeight(cells)
            if (y + h <= bottom) { drawRowChunk(widths, cells, IntArray(cells.size), IntArray(cells.size) { cells[it].lineCount }, null, fillFirstColumn); continue }
            if (h <= bottom - top - headerH) { newPage(); drawHeader(); drawRowChunk(widths, cells, IntArray(cells.size), IntArray(cells.size) { cells[it].lineCount }, null, fillFirstColumn); continue }
            // a single row taller than a whole page: split it line by line across pages
            val start = IntArray(cells.size)
            while (start.indices.any { start[it] < cells[it].lineCount }) {
                if (bottom - y < 2 * pad + 12f) { newPage(); drawHeader() }
                val avail = bottom - y - 2 * pad
                val end = IntArray(cells.size) { i ->
                    var e = start[i]
                    val l = cells[i]
                    if (e < l.lineCount) { val t = l.getLineTop(e); while (e < l.lineCount && l.getLineBottom(e) - t <= avail) e++ }
                    e
                }
                if (end.indices.none { end[it] > start[it] }) {                 // nothing fits: force one line to guarantee progress
                    val i = start.indices.first { start[it] < cells[it].lineCount }; end[i] = start[i] + 1
                }
                drawRowChunk(widths, cells, start, end, null, fillFirstColumn)
                for (i in start.indices) start[i] = end[i]
                if (start.indices.any { start[it] < cells[it].lineCount }) { newPage(); drawHeader() }
            }
        }
    }

    private fun rowHeight(cells: List<StaticLayout>) = (cells.maxOfOrNull { it.height } ?: 0) + 2 * pad

    /** draws lines [start, end) of every cell of a row at the current y and moves y below it */
    private fun drawRowChunk(widths: List<Float>, cells: List<StaticLayout>, start: IntArray, end: IntArray, fill: Paint?, labelColumn: Boolean) {
        var h = 0f
        for (i in cells.indices) {
            if (end[i] > start[i]) h = maxOf(h, (cells[i].getLineBottom(end[i] - 1) - cells[i].getLineTop(start[i])).toFloat())
        }
        h += 2 * pad
        var x = margin
        for (i in cells.indices) {
            val w = widths[i]
            if (fill != null) canvas.drawRect(x, y, x + w, y + h, fill)
            else if (labelColumn && i == 0) canvas.drawRect(x, y, x + w, y + h, fillLabel)
            canvas.drawRect(x, y, x + w, y + h, line)
            if (end[i] > start[i]) {
                val l = cells[i]
                val t = l.getLineTop(start[i])
                canvas.save()
                canvas.clipRect(x + pad, y + pad, x + w - pad, y + h - pad)
                canvas.translate(x + pad, y + pad - t)
                l.draw(canvas)
                canvas.restore()
            }
            x += w
        }
        y += h
    }
}
