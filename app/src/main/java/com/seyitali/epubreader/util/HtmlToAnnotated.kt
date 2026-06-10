package com.seyitali.epubreader.util

import android.graphics.Typeface
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.core.text.HtmlCompat

/**
 * XHTML bölüm içeriğini Compose'un AnnotatedString'ine çevirir.
 * HtmlCompat -> Spanned -> span'leri SpanStyle'a aktar.
 */
fun htmlToAnnotated(html: String): AnnotatedString {
    val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
    return buildAnnotatedString {
        append(spanned.toString())
        for (span in spanned.getSpans(0, spanned.length, Any::class.java)) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if (start < 0 || end <= start) continue
            when (span) {
                is StyleSpan -> when (span.style) {
                    Typeface.BOLD ->
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                    Typeface.ITALIC ->
                        addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    Typeface.BOLD_ITALIC ->
                        addStyle(
                            SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                            start, end
                        )
                }
                is UnderlineSpan ->
                    addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                // Başlıklar (h1, h2...) — em birimi kullanıcı yazı boyutuna göre ölçeklenir
                is RelativeSizeSpan ->
                    addStyle(
                        SpanStyle(fontSize = TextUnit(span.sizeChange, TextUnitType.Em)),
                        start, end
                    )
            }
        }
    }
}
