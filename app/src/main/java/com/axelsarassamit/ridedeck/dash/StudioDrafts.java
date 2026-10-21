package com.axelsarassamit.ridedeck.dash;

import android.content.Context;
import android.os.*;
import android.util.AtomicFile;
import java.io.*;
import java.util.*;

/** App-private versioned edit recipes, never copies or modifies source media. */
final class StudioDrafts {
    static File folder(Context c){File f=new File(c.getFilesDir(),"studio-drafts");f.mkdirs();return f;}
    static File file(Context c,String id)throws IOException{if(id==null||!id.matches("[a-zA-Z0-9-]{1,80}"))throw new IOException("Invalid draft");return new File(folder(c),id+".draft");}
    static void save(Context c,String id,Bundle project,Bundle recipe)throws IOException {
        Bundle b=new Bundle();b.putInt("schema",1);b.putBundle("project",project);b.putBundle("recipe",recipe);
        Parcel p=Parcel.obtain();byte[] bytes;try{p.writeBundle(b);bytes=p.marshall();}finally{p.recycle();}
        AtomicFile atomic=new AtomicFile(file(c,id));FileOutputStream out=null;try{out=atomic.startWrite();out.write(bytes);atomic.finishWrite(out);}catch(IOException e){if(out!=null)atomic.failWrite(out);throw e;}
    }
    static Bundle read(Context c,String id)throws IOException {
        AtomicFile f=new AtomicFile(file(c,id));byte[] bytes=f.readFully();if(bytes.length>1024*1024)throw new IOException("Draft too large");
        Parcel p=Parcel.obtain();try{p.unmarshall(bytes,0,bytes.length);p.setDataPosition(0);Bundle b=p.readBundle(StudioDrafts.class.getClassLoader());if(b==null||b.getInt("schema")!=1)throw new IOException("Unsupported draft");return b;}catch(RuntimeException e){throw new IOException("Unreadable draft",e);}finally{p.recycle();}
    }
    static List<File> list(Context c){File[] files=folder(c).listFiles((d,n)->n.endsWith(".draft"));List<File> result=new ArrayList<>(files==null?Collections.emptyList():Arrays.asList(files));result.sort((a,b)->Long.compare(b.lastModified(),a.lastModified()));return result;}
}
