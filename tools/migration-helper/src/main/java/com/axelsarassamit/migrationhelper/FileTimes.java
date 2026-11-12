package com.axelsarassamit.migrationhelper;

import android.content.Context;
import android.os.Bundle;
import java.io.*;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;
import static com.axelsarassamit.migrationhelper.MigrationInstrumentation.*;

/** Existing Dash recording metadata only; never creates or writes recording contents. */
final class FileTimes {
    private final MigrationInstrumentation runner;
    FileTimes(MigrationInstrumentation runner){this.runner=runner;}
    private static final class Row {String path,hash;long bytes,modified;File file;}
    private static long integer(JSONObject object,String name)throws Exception{
        Object value=object.get(name);require(value instanceof Integer||value instanceof Long,"FILETIMES_SCHEMA");return ((Number)value).longValue();
    }
    private static File destination(File root,String path)throws Exception{
        require(safePath(path)&&RestorePolicy.fileTimesPath(path),"FILETIMES_PATH");
        File file=new File(root,path),parent=file;
        while(parent!=null&&!parent.equals(root)){require(!Files.isSymbolicLink(parent.toPath()),"TARGET_LINK");parent=parent.getParentFile();}
        require(parent!=null&&file.getCanonicalPath().startsWith(root.getPath()+File.separator),"FILETIMES_PATH");return file;
    }
    private static void validateFile(Row row,File root)throws Exception{
        File file=destination(root,row.path);
        require(file.isFile()&&file.length()==row.bytes&&sha(file).equals(row.hash),"FILETIMES_CONTENT");row.file=file;
    }
    void run(String operation,Bundle result)throws Exception{
        Context target=runner.getTargetContext();require(sourceFor(target.getPackageName()).equals(DASH),"FILETIMES_TARGET");
        String pin=runner.argument("fileTimesSha256");require(pin!=null&&pin.matches("[0-9a-f]{64}"),"FILETIMES_PIN");
        byte[] input=runner.fixedFileTimesInput();require(hex(MessageDigest.getInstance("SHA-256").digest(input)).equals(pin),"FILETIMES_PIN");
        JSONObject manifest=json(input);
        require(keys(manifest).equals(set("schema","targetPackage","files"))&&integer(manifest,"schema")==1
            &&manifest.get("targetPackage") instanceof String&&target.getPackageName().equals(manifest.getString("targetPackage"))
            &&manifest.get("files") instanceof JSONArray,"FILETIMES_SCHEMA");
        JSONArray rows=manifest.getJSONArray("files");require(rows.length()<=MAX_ENTRIES,"FILETIMES_LIMIT");
        List<Row> plan=new ArrayList<>();Set<String> seen=new HashSet<>();long total=0;
        File root=target.getDataDir().getCanonicalFile();
        for(int n=0;n<rows.length();n++){
            JSONObject value=rows.getJSONObject(n);require(keys(value).equals(set("path","bytes","sha256","modifiedMs"))
                &&value.get("path") instanceof String&&value.get("sha256") instanceof String,"FILETIMES_SCHEMA");
            Row row=new Row();row.path=value.getString("path");row.hash=value.getString("sha256");row.bytes=integer(value,"bytes");row.modified=integer(value,"modifiedMs");
            require(safePath(row.path)&&RestorePolicy.fileTimesPath(row.path)&&seen.add(row.path),"FILETIMES_PATH");
            require(row.bytes>=0&&row.bytes<=MAX_FILE&&row.hash.matches("[0-9a-f]{64}")&&RestorePolicy.fileTimeMillis(row.modified),"FILETIMES_LIMIT");
            total+=row.bytes;require(total<=MAX_TOTAL,"FILETIMES_LIMIT");plan.add(row);
        }
        File base=target.getNoBackupFilesDir();require(!Files.isSymbolicLink(base.toPath()),"TARGET_LINK");
        File work=new File(base,"ridedeck-migration-v1");require(!Files.isSymbolicLink(work.toPath())&&(work.isDirectory()||work.mkdir()),"WORKSPACE");
        try(RandomAccessFile file=new RandomAccessFile(new File(work,"lock"),"rw");FileLock held=file.getChannel().tryLock()){
            require(held!=null,"BUSY");require(!new File(work,"journal.json").exists()&&!new File(work,"provider-journal.json").exists(),"JOURNAL_PENDING");
            // Complete the entire schema/inventory/content preflight before changing
            // even one timestamp. No creation, content write or content deletion.
            for(Row row:plan)validateFile(row,root);
            result.putInt("fileTimesVerified",0);
            for(Row row:plan){
                validateFile(row,root);
                if(operation.equals("restoreFileTimes")&&row.file.lastModified()!=row.modified)require(row.file.setLastModified(row.modified),"FILETIMES_SET");
                require(row.file.lastModified()==row.modified,"FILETIMES_VERIFY");
                result.putInt("fileTimesVerified",result.getInt("fileTimesVerified")+1);
            }
            result.putString("status",operation.equals("restoreFileTimes")?"FILE_TIMES_RESTORED":"FILE_TIMES_VERIFIED");
            result.putBoolean("startupSuppressed",true);
        }
    }
}
