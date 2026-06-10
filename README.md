# EPUB Okuyucu — Jetpack Compose + Material 3

Sıfır harici kütüphaneyle (sadece AndroidX) yazılmış mobil EPUB okuyucu.

## Özellikler

### Okuma deneyimi
- Metne tek dokunuşla üst/alt çubukları gizle (dikkat dağıtmayan tam ekran okuma)
- Bölüm içi kaydırma konumu kaydedilir — kitabı kapatıp açınca kaldığın satıra döner
- Alt çubukta ince ilerleme çubuğu + "bölüm 3/12 • %42" göstergesi
- Bölüm sonunda "Sonraki Bölüm" butonu (elini yukarı uzatmadan devam et)
- İçindekiler açılınca mevcut bölüme kaydırılmış gelir, aktif bölüm vurgulu

### Ayarlar (hepsi kalıcı)
- Yazı boyutu (12–28 sp) ve satır aralığı (1.2–2.2)
- Yazı tipi: Sans / Serif
- Tema: Sistem / Açık / Koyu (Android 12+ Material You dinamik renkler)
- İki yana yaslama seçeneği

### Kütüphane
- SAF belge seçici ile EPUB açma (depolama izni gerekmez)
- "Okumaya devam et" kartı: son kitabını tek dokunuşla aç
- Son okunanlar listesi (yazar adıyla), tek dokunuşla listeden kaldırma
- Kalıcı URI izniyle dosyalar tekrar açılabilir

## Kurulum
1. Android Studio (Ladybug veya üstü) ile klasörü aç
2. Gradle sync bekle
3. Çalıştır — minSdk 26 (Android 8.0+)

## Mimari
```
MainActivity         -> tek aktivite, book == null ise Library, değilse Reader
ReaderViewModel      -> kitap durumu, bölüm + kaydırma ilerlemesi, ayarlar, son okunanlar
epub/EpubParser      -> ZipFile + XmlPullParser: container.xml -> OPF -> NCX -> bölümler
util/HtmlToAnnotated -> XHTML -> Spanned -> AnnotatedString (bold/italic/başlık korunur)
ui/LibraryScreen     -> devam et kartı + son okunanlar + dosya seçici
ui/ReaderScreen      -> okuyucu, gizlenebilir çubuklar, TOC ve ayar sheet'leri
```

## Geliştirme fikirleri
- Görseller (Coil ile InlineTextContent veya WebView render)
- Kitap içi arama
- Sayfa çevirme animasyonlu HorizontalPager modu
- Sepya okuma teması
