package me.eldodebug.soar.lean;

/** Render-owner text join cache. Changes in stats or native name/team formatting invalidate it. */
public final class NameLabel {
  public final String statistic;
  private String original, combined;

  public NameLabel(String statistic) {
    this.statistic = statistic;
  }

  public String beside(String name) {
    if (!name.equals(original)) {
      original = name;
      combined = name + " §7[" + statistic + "]";
    }
    return combined;
  }
}
