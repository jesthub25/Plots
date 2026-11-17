## Arsa 3.2.0

### Değişiklik / Change
- Eklentinin adı **GelismisArsa → Arsa** oldu. Jar: `Arsa-3.2.0.jar`, veri klasörü: `plugins/Arsa`.
  The plugin is now named **Arsa**. Jar: `Arsa-3.2.0.jar`, data folder: `plugins/Arsa`.
- Yetki düğümleri `gelismisarsa.*` → **`arsa.*`** (ör. `arsa.admin`, `arsa.admin.liste`, `arsa.kiralama.kirala`).
  Permission nodes renamed `gelismisarsa.*` → **`arsa.*`**.
- Komutlar aynı: `/arsa` (Türkçe), `/plot` (English).

### Yükseltme / Upgrade
1. Eski `GelismisArsa-*.jar` dosyasını sil, `Arsa-3.2.0.jar` dosyasını koy. / Remove the old jar, add the new one.
2. İlk açılışta `plugins/GelismisArsa` içindeki tüm veriler (arsalar, kira, dil tercihleri, şablonlar, ayarlar) otomatik olarak `plugins/Arsa` klasörüne kopyalanır. Eski klasör yedek olarak kalır.
   On first start all data is copied automatically from `plugins/GelismisArsa` to `plugins/Arsa`.
3. LuckPerms vb. yetki eklentisindeki `gelismisarsa.` düğümlerini `arsa.` ile değiştir. / Update `gelismisarsa.` nodes to `arsa.` in your permissions plugin.

### 3.1.0 ile gelenler / Included from 3.1.0
- İki dil (`/arsa` TR, `/plot` EN), tek arsa kuralı, 54 slotluk admin arsa listesi ve detay GUI'si.
