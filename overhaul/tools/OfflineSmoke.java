import java.io.*;
import java.lang.reflect.*;
import java.net.*;

/** Executes only the installed pure mode-normalizer. No Glide, game, account or native init. */
public final class OfflineSmoke {
  public static void main(String[] args) throws Exception {
    if (args.length != 1) throw new IllegalArgumentException("Expected installed JAR");
    try (URLClassLoader loader =
        new URLClassLoader(new URL[] {new File(args[0]).toURI().toURL()}, null)) {
      Class<?> vault = Class.forName("me.eldodebug.soar.lean.KeyVault", true, loader);
      Method mode = vault.getMethod("mode", String.class);
      String[][] cases = {
        {"QUAD", "QUAD"},
        {"QUADS", "QUAD"},
        {" quad ", "QUAD"},
        {"ALL_MODES", "ALL_MODES"},
        {"SOLO", "SOLO"},
        {"DOUBLES", "DOUBLES"},
        {"invalid", "ALL_MODES"}
      };
      for (String[] c : cases)
        if (!c[1].equals(mode.invoke(null, c[0])))
          throw new IOException("Mode normalization failed");
      if (!"ALL_MODES".equals(mode.invoke(null, new Object[] {null})))
        throw new IOException("Null mode failed");
    }
    System.out.println(
        "PASS installed normalizer: 8 mode checks; QUAD retained; no HTTP or native"
            + " initialization.");
  }
}
