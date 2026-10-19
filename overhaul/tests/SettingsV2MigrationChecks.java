package me.eldodebug.soar.lean;

import java.nio.file.*;
import java.util.*;

public final class SettingsV2MigrationChecks {
  public static void main(String[] args) throws Exception {
    Path old = Paths.get("config/velvetg-lean.properties"),
        previous = Paths.get("config/velvetg-lean-v2.properties");
    Files.createDirectories(old.getParent());
    Files.write(old, "tabLimit=11\n".getBytes("ISO-8859-1"));
    byte[] v2 = "tabLimit=27\nanimation=BOUNCE\nunknown=keep\n".getBytes("ISO-8859-1");
    Files.write(previous, v2);
    if (!LeanSettings.get("tabLimit").equals("27"))
      throw new AssertionError("Overhaul2 priority migration");
    LeanSettings.set("hudLimit", "19");
    if (!Arrays.equals(Files.readAllBytes(previous), v2))
      throw new AssertionError("Overhaul2 settings changed");
    if (!new String(Files.readAllBytes(old), "ISO-8859-1").equals("tabLimit=11\n"))
      throw new AssertionError("Overhaul1 settings changed");
    if (!LeanSettings.animation().equals("BOUNCE")
        || !LeanSettings.get("presentationPreset").equals("CUSTOM"))
      throw new AssertionError("Custom values lost");
    System.out.println(
        "PASS 4 Overhaul2-first migration, prior-file preservation and custom-preset defaults"
            + " checks");
  }
}
