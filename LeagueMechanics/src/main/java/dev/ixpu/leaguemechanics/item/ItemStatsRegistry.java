package dev.ixpu.leaguemechanics.item;

public class ItemStatsRegistry {
    private String id;
    private String name;

    private double hp;
    private double hr;

    private double ad;
    private double ap;

    private double td;
    private double as;
    private double ar;
    private double mr;
    private double ls;
    private double cc;
    private double sr;

    private double ms;
    private boolean hasPassive;
    private String passiveId;

    private double apenFlat;
    private double apenPercent;
    private double mpenFlat;
    private double mpenPercent;

    private double ch;

    private double tn;

    public ItemStatsRegistry(String id, String name,
                             double hp, double hr, double ad, double ap, double td, double as,
                             double ar, double mr, double ls, double cc, double sr, double ms,
                             boolean hasPassive, String passiveId,
                             double apenFlat, double apenPercent, double mpenFlat, double mpenPercent,
                             double ch) {
        this(id, name, hp, hr, ad, ap, td, as, ar, mr, ls, cc, sr, ms, hasPassive, passiveId,
                apenFlat, apenPercent, mpenFlat, mpenPercent, ch, 0.0);
    }

    public ItemStatsRegistry(String id, String name,
                             double hp, double hr, double ad, double ap, double td, double as,
                             double ar, double mr, double ls, double cc, double sr, double ms,
                             boolean hasPassive, String passiveId,
                             double apenFlat, double apenPercent, double mpenFlat, double mpenPercent,
                             double ch, double tn) {
        this.id = id;
        this.name = name;

        this.hp = hp;
        this.hr = hr;

        this.ad = ad;
        this.ap = ap;

        this.td = td;
        this.as = as;
        this.ar = ar;
        this.mr = mr;
        this.ls = ls;
        this.cc = cc;
        this.sr = sr;

        this.ms = ms;
        this.hasPassive = hasPassive;
        this.passiveId = passiveId;

        this.apenFlat = apenFlat;
        this.apenPercent = apenPercent;
        this.mpenFlat = mpenFlat;
        this.mpenPercent = mpenPercent;

        this.ch = ch;
        this.tn = tn;
    }
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getHp() {
        return hp;
    }
    public void setHp(double hp) {
        this.hp = hp;
    }

    public double getHr() {
        return hr;
    }


    public double getAd() {
        return ad;
    }
    public void setAd(double ad) {
        this.ad = ad;
    }

    public double getAp() {
        return ap;
    }
    public void setAp(double ap) {
        this.ap = ap;
    }


    public double getTd() {
        return td;
    }

    public double getAs() {
        return as;
    }

    public double getAr() {
        return ar;
    }

    public double getMr() {
        return mr;
    }

    public double getLs() {
        return ls;
    }

    public double getCc() {
        return cc;
    }

    public double getSr() {
        return sr;
    }

    public double getMs() {
        return ms;
    }

    public boolean hasPassive() {
        return hasPassive;
    }

    public String getPassiveId() {
        return passiveId;
    }

    public double getApenFlat() {
        return apenFlat;
    }

    public double getApenPercent() {
        return apenPercent;
    }

    public double getMpenFlat() {
        return mpenFlat;
    }

    public double getMpenPercent() {
        return mpenPercent;
    }

    public double getCh() {
        return ch;
    }

    public void setCh(double ch) {
        this.ch = ch;
    }

    public double getTn() {
        return tn;
    }

    public void setTn(double tn) {
        this.tn = tn;
    }
}
