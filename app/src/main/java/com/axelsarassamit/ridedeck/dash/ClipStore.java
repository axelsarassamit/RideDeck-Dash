package com.axelsarassamit.ridedeck.dash;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Protected markers outlive process death. Only finalized unprotected video is recycled. */
public final class ClipStore {
    public static final long QUOTA = 2L * 1024 * 1024 * 1024;
    public static final long RESERVE = 256L * 1024 * 1024;
    private final File root;
    public ClipStore(Context context) { root = new File(context.getFilesDir(), "recordings"); root.mkdirs(); }
    public File root() { return root; }
    public static boolean protectedClip(File f) { return new File(f.getPath() + ".protected").exists(); }
    public static boolean finalized(File f) { return new File(f.getPath() + ".ready").exists(); }
    public static boolean canRecycle(File f) { return finalized(f) && !protectedClip(f); }
    public static void mark(File f, String suffix) throws IOException {
        File marker = new File(f.getPath() + suffix);
        if (!marker.exists() && !marker.createNewFile()) throw new IOException("Cannot save clip status");
    }
    public List<File> clips() {
        List<File> result = new ArrayList<>();
        File[] sessions = root.listFiles();
        if (sessions != null) for (File dir : sessions) {
            File[] files = dir.listFiles((d, n) -> n.endsWith(".mp4"));
            if (files != null) java.util.Collections.addAll(result, files);
        }
        result.sort(Comparator.comparingLong(File::lastModified).reversed());
        return result;
    }
    public synchronized void reserve(long expectedBytes) throws IOException {
        List<File> files = clips();
        long used = 0;
        for (File f : files) used += f.length();
        files.sort(Comparator.comparingLong(File::lastModified));
        for (File f : files) {
            if (used + expectedBytes <= QUOTA && root.getUsableSpace() >= RESERVE + expectedBytes) return;
            if (!canRecycle(f)) continue;
            long length = f.length();
            if (f.delete()) { used -= length; new File(f.getPath() + ".ready").delete(); }
        }
        if (used + expectedBytes > QUOTA || root.getUsableSpace() < RESERVE + expectedBytes)
            throw new IOException("Storage full. Export protected clips to free space.");
    }
}
