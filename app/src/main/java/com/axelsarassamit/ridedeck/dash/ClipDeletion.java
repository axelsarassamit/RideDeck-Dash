package com.axelsarassamit.ridedeck.dash;
import java.io.*;import java.util.*;
/** Only removes selected internal MP4s and their markers, never ride data or exported copies. */
final class ClipDeletion {
 static final class Result {final Set<String> removed=new LinkedHashSet<>();int failed;}
 static Result delete(File root,List<File> chosen){Result result=new Result();Set<String> seen=new HashSet<>();
  for(File file:chosen){if(!seen.add(file.getPath()))continue;try{
   String rootPath=root.getCanonicalPath()+File.separator;
   if(!file.getCanonicalPath().startsWith(rootPath)||!file.getName().endsWith(".mp4")||file.isDirectory()){result.failed++;continue;}
   if(file.exists()&&!file.delete()){result.failed++;continue;}
   new File(file.getPath()+".ready").delete();new File(file.getPath()+".protected").delete();result.removed.add(file.getPath());
  }catch(IOException|SecurityException error){result.failed++;}}
  return result;
 }
}
