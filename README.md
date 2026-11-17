# Arsa

Koordinat tabanlı, kiralamalı ve **izin bazında detaylı** arsa koruma eklentisi.
Taban sürüm **Minecraft 1.20.1**, üst sürümlerde (1.20.x, 1.21.x) de çalışır. Spigot ve Paper uyumludur.

- İki dil: `/arsa` → **Türkçe**, `/plot` → **English**. Oyuncunun son kullandığı komut; GUI'leri, mesajları, koruma uyarılarını ve bildirimleri o dile çevirir (tercih kaydedilir)
- Tek arsa kuralı: bir oyuncunun adı bir arsada geçiyorsa (sahip/üye) başka arsaya üye olamaz, yetki alamaz, başka arsa kiralayamaz
- Admin arsa listesi: 54 slotluk sayfalı GUI, filtre, her arsa için sahip / üyeler / rütbeler / tüm izinler detayı, ışınlanma
- 11 ayrı izin, rütbe varsayılanları + üye başına özel ayar
- Ziyaretçi (üye olmayan) izinleri: varsayılan **tam koruma**, isteğe göre açılabilir
- Dolgu camı olmayan, holder tabanlı güvenli GUI
- Kira sistemi (Vault), şablon tabanlı arsa sıfırlama (WorldEdit / FAWE)
- Patlama, ateş, sıvı, piston, huni, dağıtıcı, mob tahribatı ve ağaç büyümesi koruması

## Kurulum

1. [Releases](../../releases) sayfasından `Arsa-x.y.z.jar` dosyasını indirip `plugins/` klasörüne at.
2. İsteğe bağlı: **Vault** + bir ekonomi eklentisi (kiralama için), **WorldEdit** veya **FastAsyncWorldEdit** (seçimden oluşturma ve sıfırlama için).
3. **GelismisArsa'dan yükseltiyorsan:** eski `GelismisArsa-*.jar` dosyasını sil. `plugins/GelismisArsa` içindeki veriler ilk açılışta otomatik olarak `plugins/Arsa` klasörüne kopyalanır. Yetki eklentindeki `gelismisarsa.*` düğümlerini `arsa.*` olarak değiştir.
4. Sunucuyu başlat. Eski sürüm verisi (`config.yml` içindeki `arsalar` + `kiralama.yml`) otomatik olarak `arsalar.yml` dosyasına taşınır, eski dosyalar `config-eski.yml` / `kiralama-eski.yml` olarak yedeklenir.

## Dil / Language

`/arsa ...` yazan oyuncu Türkçe, `/plot ...` yazan oyuncu İngilizce görür. Tüm metinler `plugins/Arsa/lang/tr.yml` ve `lang/en.yml` içinde düzenlenebilir (`/arsa yenile`). Hiç komut kullanmamış oyuncular için `config.yml → varsayilan-dil` geçerlidir (`otomatik` = oyun istemcisinin dili).

| Türkçe (`/arsa`) | English (`/plot`) |
|---|---|
| `kirala <süre>` | `rent <time>` |
| `terket onayla` | `leave confirm` |
| `sure <arttır\|azalt> <süre>` | `time <add\|remove> <time>` |
| `bilgi` / `sahip` / `gui` | `info` / `owner` / `gui` |
| `yetkiver <oyuncu> [rütbe\|izin\|hepsi]` | `trust <player> [rank\|permission\|all]` |
| `yetkial <oyuncu> [izin\|hepsi]` | `untrust <player> [permission\|all]` |
| `ziyaretci [izin\|hepsi] [aç\|kapat]` | `visitor [permission\|all] [on\|off]` |
| `izinler` / `yardim` | `perms` / `help` |
| `olustur` / `sil onayla` / `liste` | `create` / `delete confirm` / `list` |
| `sablon` / `bypass` / `yenile` | `template` / `bypass` / `reload` |

Rütbe anahtarları: `uye, guvenilir, yetkili, mod` / `member, trusted, officer, mod`
İzin anahtarları: `blokkoy, blokkir, sandik, kapi, mekanizma, etkilesim, esyakullan, varlik, itemtopla, itemat, davet` /
`place, break, container, door, redstone, interact, useitem, entity, pickup, drop, invite`

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
| `/arsa liste` | Tüm arsalar: 54 slotluk sayfalı GUI (konsolda sohbet listesi) |
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
| `arsa.kiralama.kirala` / `.terket` / `.sure` | herkes |
| `arsa.gui` | herkes |
| `arsa.admin` (tümü) | op |
| `arsa.admin.olustur` / `.sil` / `.liste` / `.sablon` / `.yenile` | op |
| `arsa.admin.bypass` | op (yine de `/arsa bypass` ile açılmalı) |
| `arsa.admin.yonet` | op — her arsanın panelini sahibi gibi kullanma |

## Derleme

```bash
mvn clean package
```
Çıktı: `target/Arsa-<sürüm>.jar`. Java 17 gerekir. `v*` etiketi gönderildiğinde GitHub Actions jar'ı derleyip sürüm olarak yayınlar.
