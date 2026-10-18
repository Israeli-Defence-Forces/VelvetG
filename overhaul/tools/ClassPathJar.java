import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

/** Avoid Windows command-line length limits; Manifest writes standards-compliant folded lines. */
public final class ClassPathJar {
  public static void main(String[] args) throws Exception {
    StringBuilder cp = new StringBuilder();
    for (String line :
        Files.readAllLines(Paths.get(args[0]), java.nio.charset.StandardCharsets.UTF_8)) {
      if (line.trim().isEmpty()) continue;
      Path p = Paths.get(line).toAbsolutePath();
      if (!Files.isRegularFile(p) || !p.toString().endsWith(".jar"))
        throw new java.io.IOException("Missing launcher dependency");
      cp.append(p.toUri().toASCIIString()).append(' ');
    }
    Manifest m = new Manifest();
    m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    m.getMainAttributes().put(Attributes.Name.CLASS_PATH, cp.toString().trim());
    try (JarOutputStream out =
        new JarOutputStream(
            Files.newOutputStream(Paths.get(args[1]), StandardOpenOption.CREATE_NEW), m)) {}
  }
}
