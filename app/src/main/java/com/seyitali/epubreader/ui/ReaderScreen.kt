package com.seyitali.epubreader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seyitali.epubreader.ReaderViewModel
import com.seyitali.epubreader.util.htmlToAnnotated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ReaderScreen(vm: ReaderViewModel) {
    val book = vm.book ?: return
    val chapter = book.chapters[vm.chapterIndex]
    var showToc by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    // Metne dokununca üst/alt çubuklar gizlenir -> dikkat dağıtmayan okuma
    var barsVisible by remember { mutableStateOf(true) }
    val scroll = rememberScrollState()

    // HTML -> AnnotatedString dönüşümünü arka planda yap (büyük bölümlerde UI donmasın)
    val annotated by produceState(AnnotatedString(""), chapter) {
        value = withContext(Dispatchers.Default) { htmlToAnnotated(chapter.html) }
    }

    // Bölüm yüklenince kaldığın kaydırma konumuna dön
    LaunchedEffect(annotated) {
        if (annotated.isNotEmpty()) {
            scroll.scrollTo(vm.savedScroll(vm.chapterIndex))
        }
    }

    // Kaydırma konumunu (yarım saniye sakinleşince) kaydet
    LaunchedEffect(vm.chapterIndex) {
        snapshotFlow { scroll.value }
            .debounce(500)
            .collect { vm.saveScroll(vm.chapterIndex, it) }
    }

    val readProgress =
        if (scroll.maxValue > 0) scroll.value.toFloat() / scroll.maxValue else 0f
    val fontFamily = if (vm.fontFamilyIndex == 1) FontFamily.Serif else FontFamily.SansSerif

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = barsVisible,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                book.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                chapter.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { vm.closeBook() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showToc = true }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "İçindekiler")
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Filled.Tune, contentDescription = "Okuma ayarları")
                        }
                    }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = barsVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Column {
                    LinearProgressIndicator(
                        progress = { readProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    BottomAppBar {
                        IconButton(
                            onClick = { vm.goToChapter(vm.chapterIndex - 1) },
                            enabled = vm.chapterIndex > 0
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = "Önceki bölüm"
                            )
                        }
                        Text(
                            "${vm.chapterIndex + 1} / ${book.chapters.size}  •  %${(readProgress * 100).toInt()}",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge
                        )
                        IconButton(
                            onClick = { vm.goToChapter(vm.chapterIndex + 1) },
                            enabled = vm.chapterIndex < book.chapters.lastIndex
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = "Sonraki bölüm"
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (annotated.isEmpty()) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            Column(
                Modifier
                    .padding(padding)
                    .then(if (!barsVisible) Modifier.statusBarsPadding() else Modifier)
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    // Metne tek dokunuş: çubukları gizle/göster
                    .pointerInput(Unit) {
                        detectTapGestures { barsVisible = !barsVisible }
                    }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    annotated,
                    fontSize = vm.fontSize.sp,
                    lineHeight = (vm.fontSize * vm.lineSpacing).sp,
                    fontFamily = fontFamily,
                    textAlign = if (vm.justify) TextAlign.Justify else TextAlign.Start
                )
                // Bölüm sonunda tek dokunuşla devam et
                if (vm.chapterIndex < book.chapters.lastIndex) {
                    Spacer(Modifier.height(24.dp))
                    FilledTonalButton(
                        onClick = { vm.goToChapter(vm.chapterIndex + 1) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sonraki Bölüm")
                        Spacer(Modifier.height(0.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "— Kitabın sonu —",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }

    // ---- İçindekiler ----
    if (showToc) {
        val tocState = rememberLazyListState(
            initialFirstVisibleItemIndex = (vm.chapterIndex - 2).coerceAtLeast(0)
        )
        ModalBottomSheet(onDismissRequest = { showToc = false }) {
            Text(
                "İçindekiler",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            LazyColumn(state = tocState, contentPadding = PaddingValues(bottom = 32.dp)) {
                itemsIndexed(book.chapters) { i, ch ->
                    ListItem(
                        headlineContent = {
                            Text(ch.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        },
                        leadingContent = {
                            Text("${i + 1}", style = MaterialTheme.typography.labelLarge)
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (i == vm.chapterIndex)
                                MaterialTheme.colorScheme.secondaryContainer
                            else Color.Transparent
                        ),
                        modifier = Modifier.clickable {
                            vm.goToChapter(i)
                            showToc = false
                        }
                    )
                }
            }
        }
    }

    // ---- Okuma ayarları ----
    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }) {
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("Okuma Ayarları", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                Text(
                    "Yazı boyutu: ${vm.fontSize.toInt()} sp",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = vm.fontSize,
                    onValueChange = { vm.setFontSize(it) },
                    valueRange = 12f..28f,
                    steps = 15
                )

                Text(
                    "Satır aralığı: ${"%.1f".format(vm.lineSpacing)}",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = vm.lineSpacing,
                    onValueChange = { vm.setLineSpacing(it) },
                    valueRange = 1.2f..2.2f,
                    steps = 9
                )

                Text("Yazı tipi", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("Sans", "Serif").forEachIndexed { i, label ->
                        SegmentedButton(
                            selected = vm.fontFamilyIndex == i,
                            onClick = { vm.setFontFamilyIndex(i) },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = 2)
                        ) { Text(label) }
                    }
                }
                Spacer(Modifier.height(16.dp))

                Text("Tema", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("Sistem", "Açık", "Koyu").forEachIndexed { i, label ->
                        SegmentedButton(
                            selected = vm.themeMode == i,
                            onClick = { vm.setThemeMode(i) },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = 3)
                        ) { Text(label) }
                    }
                }
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "İki yana yasla",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = vm.justify, onCheckedChange = { vm.setJustify(it) })
                }
            }
        }
    }
}
