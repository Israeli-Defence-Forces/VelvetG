package me.eldodebug.soar.lean;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.regex.*;

/** Key material is deliberately absent from all status and exception messages. */
public final class KeyVault {
  private static volatile String key = "";
  private static long checked;
  private static volatile boolean rejected;
  private static volatile String state = "not checked";
  private static final Object IO_LOCK = new Object();
  private static long sent, cooldown, headerUntil;
  private static int attempts;
  private static Object connection;
  private static final Pattern KEY = Pattern.compile("pika_[A-Za-z0-9_-]{16,128}");

  private KeyVault() {}

  private static File file() {
    try {
      return new File(
          me.eldodebug.soar.Glide.getInstance().getFileManager().getGlideDir(), "pika-api-key.txt");
    } catch (RuntimeException e) {
      return null;
    }
  }

  public static boolean valid(String k) {
    return k != null
        && k.length() >= 1
        && k.length() <= 512
        && k.matches("[!-~]+")
        && !k.contains("\r")
        && !k.contains("\n");
  }

  /** Only worker I/O shares IO_LOCK; tick/chat state never waits for filesystem operations. */
  public static String get() {
    synchronized (IO_LOCK) {
      long now = System.nanoTime();
      if (now - checked < 5000000000L && checked != 0) return key;
      checked = now;
      File f = file();
      if (f == null) return key;
      try {
        if (f.isFile() && f.length() <= 1024) {
          try (BufferedReader r =
              new BufferedReader(
                  new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String value = r.readLine();
            value = value == null ? "" : value.trim();
            if (valid(value)) {
              synchronized (KeyVault.class) {
                if (!value.equals(key)) {
                  key = value;
                  rejected = false;
                  state = "key configured";
                }
                if (rejected && !state.equals("waiting for server key response"))
                  state = "key rejected";
                return key;
              }
            }
          }
        }
      } catch (IOException ignored) {
      }
      synchronized (KeyVault.class) {
        key = "";
        state = "key missing";
        return key;
      }
    }
  }

  public static boolean save(String value) {
    if (!valid(value)) return false;
    synchronized (IO_LOCK) {
      File f = file();
      if (f == null) return false;
      Path temp = null;
      try {
        Path path = f.toPath().toAbsolutePath();
        Files.createDirectories(path.getParent());
        temp = Files.createTempFile(path.getParent(), "pika-", ".tmp");
        protect(temp);
        Files.write(temp, (value + "\n").getBytes(StandardCharsets.UTF_8));
        try {
          Files.move(
              temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
          Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
        checked = System.nanoTime();
        synchronized (KeyVault.class) {
          key = value;
          rejected = false;
          state = "key saved";
        }
        return true;
      } catch (IOException | SecurityException e) {
        state = "key save failed";
        return false;
      } finally {
        if (temp != null)
          try {
            Files.deleteIfExists(temp);
          } catch (IOException ignored) {
          }
      }
    }
  }

  private static void protect(Path p) throws IOException {
    PosixFileAttributeView posix = Files.getFileAttributeView(p, PosixFileAttributeView.class);
    if (posix != null) {
      posix.setPermissions(
          EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
      return;
    }
    AclFileAttributeView acl = Files.getFileAttributeView(p, AclFileAttributeView.class);
    if (acl != null) {
      UserPrincipal owner = Files.getOwner(p);
      AclEntry e =
          AclEntry.newBuilder()
              .setType(AclEntryType.ALLOW)
              .setPrincipal(owner)
              .setPermissions(EnumSet.allOf(AclEntryPermission.class))
              .build();
      acl.setAcl(Collections.singletonList(e));
      return;
    }
    throw new IOException("Owner-only key storage unavailable");
  }

  public static synchronized void response(int code) {
    response(code, key);
  }

  public static synchronized void response(int code, String sentKey) {
    if (!key.equals(sentKey)) return;
    if (code == 200) {
      rejected = false;
      state = "authenticated";
    }
    if (code == 401 || code == 403) {
      rejected = true;
      state = "authentication rejected; manual entry or automatic key capture needed";
    }
  }

  public static synchronized boolean usable(String candidate) {
    return !rejected && !key.isEmpty() && key.equals(candidate);
  }

  public static String status() {
    return state;
  }

  public static boolean pika(String host) {
    if (host == null) return false;
    host = host.toLowerCase(Locale.ROOT).replaceFirst(":\\d+$", "");
    return host.equals("pika-network.net")
        || host.endsWith(".pika-network.net")
        || host.equals("pika.host")
        || host.endsWith(".pika.host");
  }

  /** Explicit user /apikey remains usable even when automatic fetching is disabled. */
  public static synchronized boolean manual(String server, long now) {
    if (!pika(server)) return false;
    sent = now;
    headerUntil = 0;
    cooldown = now + 60000;
    state = "waiting for server key response";
    return true;
  }

  public static synchronized boolean request(Object conn, String host, long now, boolean auto) {
    if (connection != conn) {
      connection = conn;
      attempts = 0;
      sent = 0;
      headerUntil = 0;
      cooldown = now + 3000;
    }
    if (conn == null
        || !pika(host)
        || !auto
        || now < cooldown
        || attempts >= 3
        || (!key.isEmpty() && !rejected)) return false;
    sent = now;
    cooldown = now + 60000;
    attempts++;
    state = "waiting for server key response";
    return true;
  }

  /**
   * Exact server-shaped response, in a locally opened 10 second window. Player chat wrappers fail
   * the anchored match.
   */
  public static synchronized String capture(String text, String host, int chatType, long now) {
    if (!pika(host)
        || sent == 0
        || now < sent
        || now - sent > 10000
        || chatType == 2
        || text == null) return null;
    String s = text.replaceAll("\u00a7[0-9A-FK-ORa-fk-or]", "").trim();
    Matcher m =
        Pattern.compile("^\\(!\\) Your stats API key:\\s*(pika_[A-Za-z0-9_-]{16,128})\\s*$")
            .matcher(s);
    if (m.matches()) {
      sent = 0;
      headerUntil = 0;
      return m.group(1);
    }
    if (s.equals("(!) Your stats API key:")) {
      headerUntil = now + 10000;
      return null;
    }
    if (headerUntil >= now && headerUntil != 0 && KEY.matcher(s).matches()) {
      sent = 0;
      headerUntil = 0;
      return s;
    }
    return null;
  }

  public static String redact(String text) {
    if (text == null) return "";
    String clean = KEY.matcher(text).replaceAll("[API key hidden]");
    String current = key;
    return current.isEmpty() ? clean : clean.replace(current, "[API key hidden]");
  }

  public static String mode(String m) {
    if (m == null) return "ALL_MODES";
    m = m.trim().toUpperCase(Locale.ROOT);
    if (m.equals("QUADS")) return "QUAD";
    if (m.equals("ALL_MODES") || m.equals("SOLO") || m.equals("DOUBLES") || m.equals("QUAD"))
      return m;
    return "ALL_MODES";
  }
}
