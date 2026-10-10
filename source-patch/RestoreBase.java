import java.io.*;import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.security.*;
/** Restores the four modified original classes and verifies all baseline entry bytes. */
public final class RestoreBase {
 static byte[] read(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);return out.toByteArray();}
 static String sha(byte[] data)throws Exception{StringBuilder s=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(data))s.append(String.format("%02x",b&255));return s.toString();}
 public static void main(String[] args)throws Exception{
  Map<String,byte[]> entries=new TreeMap<>();try(ZipFile zip=new ZipFile(args[0])){for(ZipEntry e:Collections.list(zip.entries()))if(!e.isDirectory()&&!e.getName().startsWith("me/movi/violet/")&&!e.getName().equals("mixins.violet.json")&&!e.getName().equals("violet-patch-report.txt"))entries.put(e.getName(),read(zip.getInputStream(e)));}
  try(ZipFile zip=new ZipFile(args[1])){for(ZipEntry e:Collections.list(zip.entries()))if(!e.isDirectory())entries.put(e.getName(),read(zip.getInputStream(e)));}
  int verified=0;for(String line:Files.readAllLines(Paths.get(args[2]),java.nio.charset.StandardCharsets.UTF_8)){if(line.isEmpty())continue;String name=line.substring(66);byte[] data=entries.get(name);if(data==null||!sha(data).equals(line.substring(0,64)))throw new IOException("Baseline entry differs: "+name);verified++;}
  if(verified!=entries.size())throw new IOException("Unexpected baseline entries");
  try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(Paths.get(args[3])))){for(Map.Entry<String,byte[]> e:entries.entrySet()){zip.putNextEntry(new ZipEntry(e.getKey()));zip.write(e.getValue());zip.closeEntry();}}
  System.out.println("Restored and verified "+verified+" baseline entries from the supplied PerfPatch.");
 }
}
