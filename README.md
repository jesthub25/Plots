# GelismisArsa

Koordinat tabanlı, kiralamalı ve **izin bazında detaylı** arsa koruma eklentisi.
Taban sürüm **Minecraft 1.20.1**, üst sürümlerde (1.20.x, 1.21.x) de çalışır. Spigot ve Paper uyumludur.

- Tek komut: `/arsa` (tüm işlemler alt komut + tab tamamlama ile)
- 11 ayrı izin, rütbe varsayılanları + üye başına özel ayar
- Ziyaretçi (üye olmayan) izinleri: varsayılan **tam koruma**, isteğe göre açılabilir
- Dolgu camı olmayan, holder tabanlı güvenli GUI
- Kira sistemi (Vault), şablon tabanlı arsa sıfırlama (WorldEdit / FAWE)
- Patlama, ateş, sıvı, piston, huni, dağıtıcı, mob tahribatı ve ağaç büyümesi koruması

## Kurulum

1. [Releases](../../releases) sayfasından `GelismisArsa-x.y.z.jar` dosyasını indirip `plugins/` klasörüne at.
2. İsteğe bağlı: **Vault** + bir ekonomi eklentisi (kiralama için), **WorldEdit** veya **FastAsyncWorldEdit** (seçimden oluşturma ve sıfırlama için).
3. Sunucuyu başlat. Eski sürüm verisi (`config.yml` içindeki `arsalar` + `kiralama.yml`) otomatik olarak `arsalar.yml` dosyasına taşınır, eski dosyalar `config-eski.yml` / `kiralama-eski.yml` olarak yedeklenir.

## Komutlar

| Komut | Açıklama |
|---|---|
| `/arsa kirala <süre>` | Bulunduğun sahipsiz arsayı kirala (`1w`, `2w`, `3d`) |
| `/arsa terket onayla` | Arsayı bırak, kalan süre orantılı iade edilir |
| `/arsa sure <arttır\|azalt> <süre>` | Kira süresini uzat / kısalt (`12h`, `2d`, `1w`) |
| `/arsa bilgi` | Arsa bilgileri ve **senin** o arsadaki izinlerin |
| `/arsa sahip` | Arsanın sahibi |
| `/arsa gui` | Yönetim paneli (sahip ve MOD) |
| `/arsa yetkiver <oyuncu>` | Üye olarak davet et |
| `/arsa yetkiver <oyuncu> <rütbe>` | Rütbe ver (`uye`, `guvenilir`, `yetkili`, `mod`) |
| `/arsa yetkiver <oyuncu> <izin\|hepsi>` | Tek bir izni / tüm izinleri aç |
| `/arsa yetkial <oyuncu>` | Arsadan at |
| `/arsa yetkial <oyuncu> <izin\|hepsi>` | Tek bir izni / tüm izinleri kapat |
| `/arsa ziyaretci [izin\|hepsi] [aç\|kapat]` | Ziyaretçi izinleri (argümansız: menü) |
| `/arsa izinler` | Tüm izinlerin listesi |

Arsanın içinde değilsen ve tek bir arsan varsa `gui`, `sure`, `yetkiver`, `yetkial`, `ziyaretci` o arsada çalışır.

### Admin

| Komut | Açıklama |
|---|---|
| `/arsa olustur` | WorldEdit seçiminden arsa oluştur |
| `/arsa olustur <x1> <y1> <z1> <x2> <y2> <z2>` | Koordinatla oluştur (`~` desteklenir) |
| `/arsa sil onayla` | Bulunduğun arsayı sil |
| `/arsa liste [sayfa]` | Tüm arsalar (konsoldan da çalışır) |
| `/arsa sablon` | Arsanın şu anki halini sıfırlama şablonu olarak kaydet |
| `/arsa bypass` | Koruma atlatmayı aç/kapat |
| `/arsa yenile` | `config.yml` yeniden yükle |

> **OP'ler korumayı otomatik atlatmaz.** Atlatma sadece `/arsa bypass` ile açıkken geçerlidir; böylece OP hesabıyla test ederken de korumayı görürsün.

## İzinler (arsa içi)

| Anahtar | İzin | Kapsam |
|---|---|---|
| `blokkoy` | Blok Koyma | Blok koyma, tabela düzenleme, kova boşaltma, çerçeve/tablo asma |
| `blokkir` | Blok Kırma | Blok kırma, kova doldurma, tarla ezme, ateş söndürme |
| `sandik` | Sandık / Depo | Sandık, varil, shulker, fırınlar, huni, dağıtıcı, demleme standı, kürsüden kitap alma |
| `kapi` | Kapı | Tüm kapılar, tuzak kapılar, çit kapıları (bakır kapılar dahil) |
| `mekanizma` | Mekanizma | Düğme, şalter, basınç plakası, tel, tekrarlayıcı, karşılaştırıcı, nota bloğu, zil |
| `etkilesim` | İstasyon | Çalışma masası, örs, büyü masası, yatak, saksı, kazan, kamp ateşi, mum vb. |
| `esyakullan` | Eşya Kullanma | Çakmak, kemik tozu, balta/çapa/kürek dönüştürme, spawn yumurtası, boya, fırça |
| `varlik` | Varlık | Hayvan, köylü, zırh askısı, eşya çerçevesi, tablo, araçlar |
| `itemtopla` | Yerden Eşya Alma | Arsa içindeki yere düşmüş eşyaları toplama (olta ile çekme dahil) |
| `itemat` | Yere Eşya Atma | Arsa içinde eşya atma |
| `davet` | Davet | Yeni üye davet etme (Üye rütbesiyle) |

### Rütbeler

| Rütbe | Varsayılan |
|---|---|
| Üye | Sadece arsaya girebilir |
| Güvenilir Üye | blokkoy, blokkir, sandik, kapi, mekanizma, itemtopla, itemat |
| Yetkili | Güvenilir + etkilesim, esyakullan, varlik |
| Mod | Tüm izinler + üye yönetimi |

**MOD kuralları:** MOD, arsa sahibi gibi üyeleri yönetir (davet, at, izin aç/kapat, Yetkili'ye kadar rütbe). Ancak **arsa sahibine ve diğer MOD'lara hiçbir işlem yapamaz** ve **kimseye MOD rütbesi veremez**.

Rütbe varsayılanları `config.yml → rutbe-izinleri` ile değiştirilebilir.

## Yetki düğümleri

| Düğüm | Varsayılan |
|---|---|
| `gelismisarsa.kiralama.kirala` / `.terket` / `.sure` | herkes |
| `gelismisarsa.gui` | herkes |
| `gelismisarsa.admin` (tümü) | op |
| `gelismisarsa.admin.olustur` / `.sil` / `.liste` / `.sablon` / `.yenile` | op |
| `gelismisarsa.admin.bypass` | op (yine de `/arsa bypass` ile açılmalı) |
| `gelismisarsa.admin.yonet` | op — her arsanın panelini sahibi gibi kullanma |

## Derleme

```bash
mvn clean package
```
Çıktı: `target/GelismisArsa-<sürüm>.jar`. Java 17 gerekir. `v*` etiketi gönderildiğinde GitHub Actions jar'ı derleyip sürüm olarak yayınlar.
