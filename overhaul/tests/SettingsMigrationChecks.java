package me.eldodebug.soar.lean;

import java.nio.file.*;
import java.util.*;

/** Starts before LeanSettings initializes, checking real on-disk migration and preservation. */
public final class SettingsMigrationChecks {
  public static void main(String[] args) throws Exception {
    Path old = Paths.get("config/velvetg-lean.properties");
    Files.createDirectories(old.getParent());
    byte[] original =
        "tabLimit=17\nunknownUserSetting=keep\napiKey=SECRET_CANARY_NOT_EXPORTED\n"
            .getBytes("ISO-8859-1");
    Files.write(old, original);
    if (!LeanSettings.get("tabLimit").equals("17"))
      throw new AssertionError("Old settings not read");
    LeanSettings.set("tabGameLimit", "8");
    if (!Arrays.equals(Files.readAllBytes(old), original))
      throw new AssertionError("Prior settings changed");
    Path current = Paths.get("config/velvetg-lean-v2.properties");
    String saved = new String(Files.readAllBytes(current), "ISO-8859-1");
    if (!saved.contains("unknownUserSetting=keep"))
      throw new AssertionError("Unknown local setting lost");
    LeanSettings.exportSettings();
    String exported =
        new String(
            Files.readAllBytes(Paths.get("config/velvetg-lean-export.properties")), "ISO-8859-1");
    if (exported.contains("SECRET_CANARY")
        || exported.contains("apiKey=")
        || exported.contains("unknownUserSetting"))
      throw new AssertionError("Unknown/secret exported");
    System.out.println(
        "PASS 4 original-setting migration, version separation, unknown-setting preservation and"
            + " secret-free export checks");
  }
}
