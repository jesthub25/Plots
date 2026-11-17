## GelismisArsa 3.0.0

Tam yeniden yazım. Taban sürüm **1.20.1**, üst sürümlerde de çalışır (Spigot / Paper).

### Kökünden düzeltilen koruma açıkları
- **Yetkisiz oyuncular kapı, tuzak kapı, sandık ve düğme kullanabiliyordu.** Ana el reddedilince sunucu işlemi yan elle tekrar deniyordu ve yan el hiç kontrol edilmiyordu. Artık her iki el de kontrol ediliyor, red en yüksek öncelikte kesinleştiriliyor ve sandıklar ayrıca envanter açılışında ikinci kez denetleniyor.
- **OP'ler korumayı otomatik atlatıyordu** (test sırasında koruma çalışmıyor gibi görünüyordu). Atlatma artık sadece `/arsa bypass` ile açılır.
- Basınç plakası, tarla ezme, ok ile düğme, eşya çerçevesi/zırh askısı, hayvan, araç, kova, tabela, kürsü, olta ile eşya çekme, huniyle depodan eşya çekme açıkları kapatıldı.
- GUI açıkken oyuncu envanterinin kullanılabilmesi düzeltildi (holder tabanlı tespit; shift-tık, sayı tuşu, sürükleme, çift tık dahil her şey engellenir).
- `/arsa yetkiver` "oyuncu bulunamadı" hatası düzeltildi.

### Yeni
- 11 ayrı izin, her üye için tek tek ayarlanabilir; rütbe varsayılanları config'den değiştirilebilir.
- MOD rütbesi: üyeleri sahibi gibi yönetir; sahibe ve diğer MOD'lara dokunamaz, MOD rütbesi veremez.
- Ziyaretçi izinleri (varsayılan tam koruma).
- Yeni GUI: dolgu camı yok, 27 slotluk üye izin ekranı, sayfalı üye listesi, sohbetten davet, onaylı arsadan atma, panelden kira uzatma.
- Tek komut `/arsa` + eksiksiz tab tamamlama.
- Çevre koruması: patlama, ateş, sıvı, piston, dağıtıcı, huni, mob tahribatı, ağaç büyümesi (hepsi config'den açılıp kapanır).
- Kira bitince/terk edilince arsa, oluşturulduğu (veya ilk kiralandığı) andaki **şablona** geri döner.
- Vault ve WorldEdit artık isteğe bağlı; yoksa koruma ve GUI yine çalışır.

### Performans
- Konum araması chunk yüklemeden, sınır kutusu + long anahtarlı indeks ile O(1).
- Blok/eşya izin eşlemesi açılışta bir kez hesaplanır (EnumMap).
- Kayıt ana thread'i hiç bekletmez: anlık görüntü + tek iş parçacığında atomik dosya yazımı.

### Taşıma
Eski `config.yml` içindeki arsalar ve `kiralama.yml` otomatik olarak `arsalar.yml` dosyasına taşınır; eski dosyalar yedeklenir.
