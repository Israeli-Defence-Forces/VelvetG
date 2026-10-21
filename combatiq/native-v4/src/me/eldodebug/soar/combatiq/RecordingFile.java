package me.eldodebug.soar.combatiq;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Writer-thread-only segmented CSV. Unfinalized output keeps .partial suffix. */
final class RecordingFile {
    private final Path folder;
    private final String session;
    private final long limit;
    private final boolean rotate;
    private BufferedWriter out;
    private Path partial,complete;
    private MessageDigest digest;
    private int segment;
    private long bytes,lossBase,filteredBase;
    RecordingFile(Path folder,String session,long limit,boolean rotate)throws IOException {
        this.folder=folder;this.session=session;this.limit=limit;this.rotate=rotate;open();
    }
    private void open()throws IOException {
        complete=folder.resolve("combatiq-native-"+session+"-"+String.format("%04d",segment)+".csv");
        partial=complete.resolveSibling(complete.getFileName()+".partial");
        out=Files.newBufferedWriter(partial,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
        try {
        try{digest=MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException impossible){throw new IOException(impossible);}
        bytes=0;
        data("# CombatIQ native experimental schema 3; packet hooks are NOT server acceptance or actual socket writes\n");
        data("ns,kind,data,moveForward,packetClass,tick,epoch,thread,sequence,entity,flags,x,y,z,connection,phase,packetId\n");
        data("# SEGMENT,"+session+","+segment+"\n");
        } catch(IOException failure){abort();throw failure;}
    }
    private void data(String text)throws IOException {byte[] raw=text.getBytes(StandardCharsets.UTF_8);out.write(text);digest.update(raw);bytes+=raw.length;}
    boolean append(String row,long loss,long filtered)throws IOException {
        int length=row.getBytes(StandardCharsets.UTF_8).length;
        if(bytes+length>limit-512){
            if(!rotate)return false;
            finishSegment(loss,filtered,false,true);segment++;open();
            if(bytes+length>limit-512)throw new IOException("Single telemetry row exceeds segment budget");
        }
        data(row);return true;
    }
    private void finishSegment(long loss,long filtered,boolean capped,boolean continues)throws IOException {
        out.write("# LOSS,"+(loss-lossBase)+"\n# FILTERED,"+(filtered-filteredBase)+"\n");
        StringBuilder hex=new StringBuilder();for(byte b:digest.digest())hex.append(Character.forDigit((b>>4)&15,16)).append(Character.forDigit(b&15,16));
        out.write("# DATA_SHA256,"+hex+"\n");
        if(capped)out.write("# CAPPED recorder row/duration/size limit reached\n");
        if(continues)out.write("# CONTINUES,"+session+","+(segment+1)+"\n");
        out.write("# END\n");out.flush();out.close();out=null;
        // Files.move without REPLACE_EXISTING never overwrites another recording.
        Files.move(partial,complete);
        lossBase=loss;filteredBase=filtered;
    }
    void finish(long loss,long filtered,boolean capped)throws IOException {finishSegment(loss,filtered,capped,false);}
    void flush()throws IOException{if(out!=null)out.flush();}
    void abort(){if(out!=null)try{out.close();}catch(IOException ignored){}out=null;}
}
