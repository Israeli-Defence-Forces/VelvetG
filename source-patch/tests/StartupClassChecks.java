import java.util.*;
import java.util.zip.*;

/** JVM-verify inherited startup classes without initializing game, GL or networking. */
public final class StartupClassChecks {
    public static void main(String[] args) throws Exception {
        int checked=0;
        List<String> failures=new ArrayList<String>();
        try(ZipFile z=new ZipFile(args[0])) {
            for(ZipEntry e:Collections.list(z.entries())) {
                String name=e.getName();
                if(!name.endsWith(".class") || !(name.startsWith("me/eldodebug/soar/scout/") || name.startsWith("me/eldodebug/soar/management/mods/impl/")))continue;
                String binary=name.substring(0,name.length()-6).replace('/','.');
                try {
                    Class<?> type=Class.forName(binary,false,StartupClassChecks.class.getClassLoader());
                    type.getDeclaredMethods();type.getDeclaredConstructors();type.getDeclaredFields();checked++;
                }catch(LinkageError ex){failures.add(binary+": "+ex);}
            }
        }
        if(!failures.isEmpty())throw new AssertionError(String.join("\n",failures));
        System.out.println("PASS: "+checked+" inherited Scout/module startup classes JVM-verified without game initialization");
    }
}
