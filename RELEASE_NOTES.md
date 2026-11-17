## GelismisArsa 3.1.0

### Yeni / New
- **İki dil / Two languages:** `/arsa` → Türkçe, `/plot` → English. Oyuncunun son kullandığı komut; tüm GUI'leri, mesajları, koruma uyarılarını ve bildirimleri o dilde gösterir ve tercih kaydedilir. Tüm metinler `lang/tr.yml` ve `lang/en.yml` içinde düzenlenebilir.
- **Tek arsa kuralı / One-plot rule:** Bir oyuncunun adı bir arsada geçiyorsa (sahip veya üye), başka bir arsaya üye olamaz, yetki alamaz ve başka arsa kiralayamaz (`tek-arsa-kurali`).
- **Admin arsa listesi / Admin plot list:** `/arsa liste` artık 54 slotluk, sayfalı bir GUI. Filtre (tümü / sahipli / sahipsiz), özet, her arsa için sahip, tüm üyeler, rütbeleri ve izinlerinin tek tek gösterildiği detay ekranı, ışınlanma ve yönetim paneline geçiş.

### Düzeltme / Fixes
- Süresi geçmiş sohbet istemi artık oyuncunun normal sohbet mesajını yutmaz.

### Yükseltme / Upgrade
Jar'ı değiştirmen yeterli. `config.yml` içindeki yeni ayarlar (`varsayilan-dil`, `tek-arsa-kurali`) yoksa varsayılanlar kullanılır. Dil dosyaları ilk açılışta oluşturulur.
