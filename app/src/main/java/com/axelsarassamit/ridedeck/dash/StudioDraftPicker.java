package com.axelsarassamit.ridedeck.dash;

import android.app.*;
import android.widget.Toast;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

/** Shared saved-edit browser. Deleting recipes never touches their source media. */
final class StudioDraftPicker {
    static String id(File file){return file.getName().replace(".draft","");}
    static String[] labels(List<File> files){String[] labels=new String[files.size()];for(int i=0;i<labels.length;i++)labels[i]=new SimpleDateFormat("dd MMM yyyy HH:mm:ss",Locale.getDefault()).format(new Date(files.get(i).lastModified()));return labels;}
    static void show(Activity a,Consumer<String> open,Consumer<List<String>> deleted,Runnable saveNew){
        List<File> files=StudioDrafts.list(a);
        AlertDialog.Builder b=new AlertDialog.Builder(a).setTitle("Saved edits").setNegativeButton("Close",null);
        if(files.isEmpty())b.setMessage("No saved edits yet.");
        else b.setItems(labels(files),(d,i)->open.accept(id(files.get(i)))).setPositiveButton("Delete drafts",(d,w)->select(a,open,deleted,saveNew));
        if(saveNew!=null)b.setNeutralButton("Save as new draft",(d,w)->saveNew.run());
        b.show();
    }
    static void select(Activity a,Consumer<String> open,Consumer<List<String>> deleted,Runnable saveNew){
        List<File> files=StudioDrafts.list(a);boolean[] chosen=new boolean[files.size()];
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Select drafts to delete").setMultiChoiceItems(labels(files),chosen,(d,i,value)->{chosen[i]=value;int count=0;for(boolean yes:chosen)if(yes)count++;AlertDialog box=(AlertDialog)d;box.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(count>0);box.getButton(AlertDialog.BUTTON_POSITIVE).setText("Delete ("+count+")");}).setNeutralButton("Select all",null).setPositiveButton("Delete (0)",null).setNegativeButton("Cancel",(d,w)->show(a,open,deleted,saveNew)).create();
        dialog.setOnShowListener(d->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{Arrays.fill(chosen,true);for(int i=0;i<chosen.length;i++)dialog.getListView().setItemChecked(i,true);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(chosen.length>0);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Delete ("+chosen.length+")");});
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{List<String> ids=new ArrayList<>();for(int i=0;i<chosen.length;i++)if(chosen[i])ids.add(id(files.get(i)));
                new AlertDialog.Builder(a).setTitle("Delete "+ids.size()+" draft"+(ids.size()==1?"":"s")+"?").setMessage("Only saved edits will be removed. Original recordings and exported videos will be kept.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(confirm,w)->{List<String> removed=new ArrayList<>();for(String id:ids)try{StudioDrafts.delete(a,id);removed.add(id);}catch(Exception ignored){}dialog.dismiss();deleted.accept(removed);Toast.makeText(a,removed.size()==ids.size()?"Drafts deleted":"Some drafts could not be deleted",Toast.LENGTH_LONG).show();if(!a.isFinishing())show(a,open,deleted,saveNew);}).show();});
        });dialog.show();
    }
}
