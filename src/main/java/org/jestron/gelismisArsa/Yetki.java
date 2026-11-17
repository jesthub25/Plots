package org.jestron.gelismisArsa;

import org.bukkit.entity.Player;
import org.jestron.gelismisArsa.model.Arsa;
import org.jestron.gelismisArsa.model.ArsaUye;
import org.jestron.gelismisArsa.model.Izin;
import org.jestron.gelismisArsa.model.Rutbe;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tüm yetki kararlarının tek merkezi.
 *
 * Kurallar:
 *  - Arsa sahibi her şeyi yapar.
 *  - MOD tüm izinlere sahiptir ve üyeleri sahip gibi yönetir; ancak sahibe ve diğer MOD'lara
 *    dokunamaz, kimseye MOD rütbesi veremez.
 *  - DAVET izni olan üye sadece yeni üye ekleyebilir (Üye rütbesiyle).
 *  - Üye olmayan herkes ziyaretçidir; yalnızca arsada açılan ziyaretçi izinlerini kullanabilir.
 *  - Admin koruma atlatması sadece /arsa bypass ile AÇIKKEN geçerlidir (OP'ler otomatik atlatmaz).
 */
public final class Yetki {

    public static final String PERM_BYPASS = "gelismisarsa.admin.bypass";
    public static final String PERM_YONET = "gelismisarsa.admin.yonet";

    private final Set<UUID> bypass = new HashSet<>();

    // ── Koruma ──────────────────────────────────────────────────────────────

    public boolean izinli(Player p, Arsa arsa, Izin izin) {
        if (arsa == null) return true;
        UUID u = p.getUniqueId();
        if (arsa.sahipMi(u)) return true;
        if (!bypass.isEmpty() && bypassAcik(p)) return true;
        ArsaUye uye = arsa.uye(u);
        if (uye != null) return arsa.uyeIzni(uye, izin);
        return arsa.ziyaretciIzni(izin);
    }

    public boolean bypassAcik(Player p) {
        return bypass.contains(p.getUniqueId()) && p.hasPermission(PERM_BYPASS);
    }

    /** @return yeni durum */
    public boolean bypassDegistir(Player p) {
        UUID u = p.getUniqueId();
        if (bypass.remove(u)) return false;
        bypass.add(u);
        return true;
    }

    public void cikis(UUID uuid) {
        bypass.remove(uuid);
    }

    // ── Yönetim ─────────────────────────────────────────────────────────────

    /** Arsa sahibi veya yönetim izni olan admin. */
    public boolean sahipGibi(Player p, Arsa arsa) {
        return arsa.sahipMi(p.getUniqueId()) || p.hasPermission(PERM_YONET);
    }

    public boolean modMu(Player p, Arsa arsa) {
        ArsaUye u = arsa.uye(p.getUniqueId());
        return u != null && u.rutbe() == Rutbe.MOD;
    }

    /** Yönetim panelini (GUI) açabilir mi? */
    public boolean panel(Player p, Arsa arsa) {
        return sahipGibi(p, arsa) || modMu(p, arsa);
    }

    /** Hedef üyenin rütbe/izinlerini değiştirebilir veya atabilir mi? */
    public boolean yonetebilir(Player p, Arsa arsa, UUID hedef) {
        if (hedef.equals(p.getUniqueId())) return false;
        if (arsa.sahipMi(hedef)) return false;
        if (sahipGibi(p, arsa)) return true;
        if (modMu(p, arsa)) {
            ArsaUye h = arsa.uye(hedef);
            return h == null || h.rutbe() != Rutbe.MOD;
        }
        return false;
    }

    public boolean rutbeVerebilir(Player p, Arsa arsa, UUID hedef, Rutbe rutbe) {
        if (!yonetebilir(p, arsa, hedef)) return false;
        return rutbe != Rutbe.MOD || sahipGibi(p, arsa);
    }

    public boolean davetEdebilir(Player p, Arsa arsa) {
        if (!arsa.sahibiVar()) return false;
        if (panel(p, arsa)) return true;
        ArsaUye u = arsa.uye(p.getUniqueId());
        return u != null && arsa.uyeIzni(u, Izin.DAVET);
    }

    public boolean ziyaretciDuzenleyebilir(Player p, Arsa arsa) {
        return panel(p, arsa);
    }
}
