package org.jestron.arsa.model;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Bir arsa üyesinin rütbesi ve rütbeden bağımsız özel izin ayarları.
 * Özel ayar yoksa rütbe varsayılanı (ve ziyaretçi izinleri) geçerlidir.
 */
public final class ArsaUye {

    private Rutbe rutbe;
    private final EnumSet<Izin> acik = EnumSet.noneOf(Izin.class);
    private final EnumSet<Izin> kapali = EnumSet.noneOf(Izin.class);

    public ArsaUye(Rutbe rutbe) {
        this.rutbe = rutbe == null ? Rutbe.UYE : rutbe;
    }

    public Rutbe rutbe() {
        return rutbe;
    }

    /** Rütbe değişince özel ayarlar temizlenir; yeni rütbenin varsayılanları geçerli olur. */
    public void rutbeAyarla(Rutbe yeni) {
        this.rutbe = yeni == null ? Rutbe.UYE : yeni;
        acik.clear();
        kapali.clear();
    }

    /** @return TRUE/FALSE özel ayar varsa, yoksa null */
    public Boolean ozel(Izin izin) {
        if (acik.contains(izin)) return Boolean.TRUE;
        if (kapali.contains(izin)) return Boolean.FALSE;
        return null;
    }

    public void izinAyarla(Izin izin, boolean deger) {
        acik.remove(izin);
        kapali.remove(izin);
        (deger ? acik : kapali).add(izin);
    }

    public void ozelSifirla(Izin izin) {
        acik.remove(izin);
        kapali.remove(izin);
    }

    public void ozelleriSifirla() {
        acik.clear();
        kapali.clear();
    }

    public boolean ozelAyarVar() {
        return !acik.isEmpty() || !kapali.isEmpty();
    }

    public Set<Izin> acikIzinler() {
        return Collections.unmodifiableSet(acik);
    }

    public Set<Izin> kapaliIzinler() {
        return Collections.unmodifiableSet(kapali);
    }

    public void ozelleriYukle(Collection<Izin> acikListe, Collection<Izin> kapaliListe) {
        acik.clear();
        kapali.clear();
        for (Izin i : acikListe) {
            if (!kapaliListe.contains(i)) acik.add(i);
        }
        kapali.addAll(kapaliListe);
    }
}
