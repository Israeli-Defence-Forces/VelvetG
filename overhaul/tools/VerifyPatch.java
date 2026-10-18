import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/**
 * Offline integrity verification: no Minecraft, HTTP, account, native, or credential
 * initialization.
 */
public final class VerifyPatch {
  static String hex(byte[] b) throws Exception {
    MessageDigest d = MessageDigest.getInstance("SHA-256");
    StringBuilder s = new StringBuilder();
    for (byte x : d.digest(b)) s.append(String.format(java.util.Locale.ROOT, "%02x", x & 255));
    return s.toString();
  }

  static byte[] read(InputStream in) throws IOException {
    try (InputStream i = in;
        ByteArrayOutputStream o = new ByteArrayOutputStream()) {
      byte[] b = new byte[8192];
      int n;
      while ((n = i.read(b)) != -1) o.write(b, 0, n);
      return o.toByteArray();
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 3)
      throw new IllegalArgumentException(
          "Usage: VerifyPatch original.jar patched.jar entries.sha256");
    if (!hex(Files.readAllBytes(Paths.get(args[0])))
        .equals("3e4b0190e26b4a1540ac23ff018a8c8280b8ab447c34cce0fc5bf8cc6c2a6813"))
      throw new IOException("Unexpected original build");
    Map<String, String> expected = new HashMap<>();
    for (String line :
        Files.readAllLines(Paths.get(args[2]), java.nio.charset.StandardCharsets.UTF_8)) {
      if (line.trim().isEmpty()) continue;
      int pos = line.indexOf("  ");
      if (pos != 64) throw new IOException("Invalid entry manifest");
      expected.put(line.substring(pos + 2), line.substring(0, pos));
    }
    int untouched = 0, modified = 0;
    try (ZipFile old = new ZipFile(args[0]);
        ZipFile now = new ZipFile(args[1])) {
      Set<String> seen = new HashSet<>();
      for (ZipEntry e : Collections.list(now.entries())) {
        String n = e.getName();
        if (!seen.add(n)) throw new IOException("Duplicate ZIP entry");
        byte[] data = read(now.getInputStream(e));
        String hash = expected.get(n);
        if (hash != null) {
          if (!hex(data).equals(hash))
            throw new IOException("Modified entry failed verification: " + n);
          if (n.endsWith(".class") && (((data[6] & 255) << 8) | (data[7] & 255)) > 52)
            throw new IOException("Java 8 incompatible class");
          modified++;
        } else {
          ZipEntry original = old.getEntry(n);
          if (original == null || !Arrays.equals(data, read(old.getInputStream(original))))
            throw new IOException("Unexpected entry change: " + n);
          untouched++;
        }
      }
      for (ZipEntry e : Collections.list(old.entries()))
        if (!seen.contains(e.getName())) throw new IOException("Baseline entry missing");
      if (modified != expected.size()) throw new IOException("Expected modifications missing");
    }
    System.out.println(
        "Verified offline: "
            + modified
            + " modified/new entries, "
            + untouched
            + " original entries unchanged; Java 8 class versions valid. This is not a Minecraft"
            + " launch test.");
  }
}
