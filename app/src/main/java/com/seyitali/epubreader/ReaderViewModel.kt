package com.seyitali.epubreader

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seyitali.epubreader.epub.EpubBook
import com.seyitali.epubreader.epub.EpubParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RecentBook(val uri: String, val title: String, val author: String)

class ReaderViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)

    var book by mutableStateOf<EpubBook?>(null); private set
    var chapterIndex by mutableIntStateOf(0); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var recents by mutableStateOf(loadRecents()); private set

    // ---- Okuma ayarları (kalıcı) ----
    var fontSize by mutableFloatStateOf(prefs.getFloat("fontSize", 17f)); private set
    /** 0 = Sistem, 1 = Açık, 2 = Koyu */
    var themeMode by mutableIntStateOf(prefs.getInt("themeMode", 0)); private set
    /** 0 = Sans, 1 = Serif */
    var fontFamilyIndex by mutableIntStateOf(prefs.getInt("fontFamily", 0)); private set
    var lineSpacing by mutableFloatStateOf(prefs.getFloat("lineSpacing", 1.6f)); private set
    var justify by mutableStateOf(prefs.getBoolean("justify", false)); private set

    private var progressKey: String? = null

    fun openBook(uri: Uri) {
        if (loading) return
        loading = true
        error = null
        // Son okunanlardan tekrar açabilmek için kalıcı izin al
        try {
            getApplication<Application>().contentResolver
                .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Bazı sağlayıcılar kalıcı izin vermez; sorun değil
        }
        viewModelScope.launch {
            try {
                val parsed = withContext(Dispatchers.IO) {
                    EpubParser.parse(getApplication(), uri)
                }
                progressKey = "progress_${uri.toString().hashCode()}"
                chapterIndex = prefs.getInt(progressKey, 0)
                    .coerceIn(0, parsed.chapters.lastIndex)
                book = parsed
                addRecent(uri, parsed.title, parsed.author)
            } catch (e: Exception) {
                error = "EPUB açılamadı: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    fun closeBook() {
        book = null
    }

    fun goToChapter(i: Int) {
        val b = book ?: return
        chapterIndex = i.coerceIn(0, b.chapters.lastIndex)
        progressKey?.let { prefs.edit().putInt(it, chapterIndex).apply() }
    }

    // ---- Bölüm içi kaydırma konumu ----

    fun saveScroll(chapter: Int, value: Int) {
        progressKey?.let { prefs.edit().putInt("${it}_s$chapter", value).apply() }
    }

    fun savedScroll(chapter: Int): Int =
        progressKey?.let { prefs.getInt("${it}_s$chapter", 0) } ?: 0

    // ---- Ayarlar ----

    fun setFontSize(size: Float) {
        fontSize = size
        prefs.edit().putFloat("fontSize", size).apply()
    }

    fun setThemeMode(mode: Int) {
        themeMode = mode
        prefs.edit().putInt("themeMode", mode).apply()
    }

    fun setFontFamilyIndex(i: Int) {
        fontFamilyIndex = i
        prefs.edit().putInt("fontFamily", i).apply()
    }

    fun setLineSpacing(v: Float) {
        lineSpacing = v
        prefs.edit().putFloat("lineSpacing", v).apply()
    }

    fun setJustify(v: Boolean) {
        justify = v
        prefs.edit().putBoolean("justify", v).apply()
    }

    // ---- Son okunanlar ("uri \t başlık \t yazar" satırları) ----

    private fun loadRecents(): List<RecentBook> =
        prefs.getString("recents", "").orEmpty()
            .split('\n')
            .filter { it.contains('\t') }
            .map { line ->
                val parts = line.split('\t', limit = 3)
                RecentBook(
                    uri = parts[0],
                    title = parts.getOrElse(1) { "Adsız Kitap" },
                    author = parts.getOrElse(2) { "" }
                )
            }

    private fun addRecent(uri: Uri, title: String, author: String) {
        val updated = (listOf(RecentBook(uri.toString(), title, author)) +
                recents.filter { it.uri != uri.toString() }).take(8)
        saveRecents(updated)
    }

    fun removeRecent(uri: String) {
        saveRecents(recents.filter { it.uri != uri })
    }

    private fun saveRecents(list: List<RecentBook>) {
        recents = list
        prefs.edit()
            .putString("recents", list.joinToString("\n") { "${it.uri}\t${it.title}\t${it.author}" })
            .apply()
    }
}
