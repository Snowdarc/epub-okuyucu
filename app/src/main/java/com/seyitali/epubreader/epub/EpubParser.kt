package com.seyitali.epubreader.epub

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader
import java.net.URLDecoder
import java.util.zip.ZipFile

/**
 * Basit ama sağlam bir EPUB 2/3 ayrıştırıcısı.
 * Akış: container.xml -> OPF (manifest + spine + metadata) -> NCX (bölüm adları) -> XHTML bölümler
 */
object EpubParser {

    fun parse(context: Context, uri: Uri): EpubBook {
        // SAF URI'sini doğrudan ZipFile ile açamayız; önce cache'e kopyala
        val cacheFile = File(context.cacheDir, "current.epub")
        context.contentResolver.openInputStream(uri)?.use { input ->
            cacheFile.outputStream().use { input.copyTo(it) }
        } ?: error("Dosya okunamadı")

        ZipFile(cacheFile).use { zip ->
            // 1) container.xml -> OPF dosyasının yolu
            val container = zip.readText("META-INF/container.xml")
                ?: error("container.xml bulunamadı (geçerli bir EPUB değil)")
            var opfPath: String? = null
            parseXml(container) { p ->
                if (p.eventType == XmlPullParser.START_TAG && p.localNameNoNs() == "rootfile") {
                    if (opfPath == null) opfPath = p.getAttributeValue(null, "full-path")
                }
            }
            val opf = opfPath ?: error("OPF yolu bulunamadı")
            val opfDir = opf.substringBeforeLast('/', "")
            val opfXml = zip.readText(opf) ?: error("OPF dosyası okunamadı")

            // 2) OPF: metadata + manifest + spine
            var title = ""
            var author = ""
            val manifest = LinkedHashMap<String, Pair<String, String>>() // id -> (href, media-type)
            val spine = mutableListOf<String>()
            var ncxId: String? = null

            parseXml(opfXml) { p ->
                if (p.eventType == XmlPullParser.START_TAG) {
                    when (p.localNameNoNs()) {
                        "title" -> if (title.isBlank()) title = p.safeText()
                        "creator" -> if (author.isBlank()) author = p.safeText()
                        "item" -> {
                            val id = p.getAttributeValue(null, "id")
                            val href = p.getAttributeValue(null, "href")
                            val media = p.getAttributeValue(null, "media-type") ?: ""
                            if (id != null && href != null) {
                                manifest[id] = href to media
                                if (media.contains("dtbncx")) ncxId = id
                            }
                        }
                        "spine" -> p.getAttributeValue(null, "toc")?.let { ncxId = it }
                        "itemref" -> p.getAttributeValue(null, "idref")?.let { spine.add(it) }
                    }
                }
            }

            // 3) NCX'ten bölüm başlıklarını çıkar (varsa)
            val tocTitles: Map<String, String> = ncxId?.let { manifest[it] }?.let { pair ->
                val ncxPath = resolvePath(opfDir, pair.first)
                val ncxDir = ncxPath.substringBeforeLast('/', "")
                zip.readText(ncxPath)?.let { parseNcx(it, ncxDir) }
            } ?: emptyMap()

            // 4) Spine sırasına göre bölümleri oku
            val chapters = spine.mapIndexedNotNull { index, id ->
                val pair = manifest[id] ?: return@mapIndexedNotNull null
                val (href, media) = pair
                if (media.isNotBlank() && !media.contains("html")) return@mapIndexedNotNull null
                val path = resolvePath(opfDir, href)
                val raw = zip.readText(path) ?: return@mapIndexedNotNull null
                val body = extractBody(raw)
                // Sadece resimden oluşan / boş sayfaları (kapak vb.) atla
                if (body.replace(Regex("<[^>]+>"), "").isBlank()) return@mapIndexedNotNull null
                val chTitle = tocTitles[path] ?: titleFromHtml(raw) ?: "Bölüm ${index + 1}"
                Chapter(chTitle, body)
            }

            if (chapters.isEmpty()) error("Okunabilir bölüm bulunamadı")
            return EpubBook(title.ifBlank { "Adsız Kitap" }, author, chapters)
        }
    }

    // ---------- yardımcılar ----------

    private fun ZipFile.readText(path: String): String? {
        val normalized = path.removePrefix("/")
        val entry = getEntry(normalized) ?: getEntry(path) ?: return null
        return getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun XmlPullParser.localNameNoNs(): String = name?.substringAfter(':') ?: ""

    private fun XmlPullParser.safeText(): String =
        try { nextText().trim() } catch (_: Exception) { "" }

    private inline fun parseXml(xml: String, onEvent: (XmlPullParser) -> Unit) {
        val p = XmlPullParserFactory.newInstance().newPullParser()
        p.setInput(StringReader(xml))
        while (p.eventType != XmlPullParser.END_DOCUMENT) {
            onEvent(p)
            p.next()
        }
    }

    /** NCX'teki navPoint'lerden: çözümlenmiş dosya yolu -> bölüm adı */
    private fun parseNcx(xml: String, ncxDir: String): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        var pendingLabel: String? = null
        parseXml(xml) { p ->
            if (p.eventType == XmlPullParser.START_TAG) {
                when (p.localNameNoNs()) {
                    "text" -> pendingLabel = p.safeText()
                    "content" -> {
                        val src = p.getAttributeValue(null, "src")?.substringBefore('#')
                        val label = pendingLabel
                        if (!src.isNullOrBlank() && !label.isNullOrBlank()) {
                            val key = resolvePath(ncxDir, src)
                            if (!map.containsKey(key)) map[key] = label
                        }
                    }
                }
            }
        }
        return map
    }

    /** "OEBPS" + "../text/ch1.xhtml" gibi göreli yolları normalize eder */
    private fun resolvePath(baseDir: String, href: String): String {
        val decoded = try { URLDecoder.decode(href, "UTF-8") } catch (_: Exception) { href }
        val combined = if (baseDir.isBlank()) decoded else "$baseDir/$decoded"
        val out = mutableListOf<String>()
        for (part in combined.split('/')) {
            when (part) {
                "", "." -> Unit
                ".." -> if (out.isNotEmpty()) out.removeAt(out.size - 1)
                else -> out.add(part)
            }
        }
        return out.joinToString("/")
    }

    /** <body> içeriğini al; script/style/svg/img gibi gösteremeyeceğimiz şeyleri temizle */
    private fun extractBody(html: String): String {
        val body = Regex("(?is)<body[^>]*>(.*)</body>").find(html)?.groupValues?.get(1) ?: html
        return body
            .replace(Regex("(?is)<(script|style|svg)[^>]*>.*?</\\1>"), " ")
            .replace(Regex("(?is)<img[^>]*>"), " ")
            .replace(Regex("(?is)<image[^>]*>"), " ")
    }

    /** NCX yoksa başlığı h1/h2/h3/title etiketinden tahmin et */
    private fun titleFromHtml(html: String): String? {
        for (tag in listOf("h1", "h2", "h3", "title")) {
            val m = Regex("(?is)<$tag[^>]*>(.*?)</$tag>").find(html) ?: continue
            val t = m.groupValues[1].replace(Regex("<[^>]+>"), "").trim()
            if (t.isNotBlank() && t.length <= 100) return t
        }
        return null
    }
}
