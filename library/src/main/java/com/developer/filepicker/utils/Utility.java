package com.developer.filepicker.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import com.developer.filepicker.model.FileListItem;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * @author akshay sunil masram
 */
public class Utility {

    public static boolean checkStorageAccessPermissions(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            String permission = "android.permission.READ_EXTERNAL_STORAGE";
            int res = context.checkCallingOrSelfPermission(permission);
            return (res == PackageManager.PERMISSION_GRANTED);
        }
        else {
            return true;
        }
    }

    public static String getFileSize(long bytes){
        return String.format(Locale.ENGLISH, "%.2f MB", bytes / (1024.00 * 1024.00));
    }

    public static ArrayList<FileListItem> prepareFileListEntries(ArrayList<FileListItem> internalList,
                           File inter, ExtensionFilter filter, boolean show_hidden_files) {
        try {
            File[] files  = inter.listFiles(filter);
            for (File name : files) {
                if (name.canRead()) {
                    if(name.getName().startsWith(".") && !show_hidden_files) continue;
                    FileListItem item = new FileListItem();
                    item.setFilename(name.getName());
                    item.setSize(name.length());
                    item.setDirectory(name.isDirectory());
                    item.setLocation(name.getAbsolutePath());
                    item.setTime(name.lastModified());
                    internalList.add(item);
                }
            }
            Collections.sort(internalList);
        }
        catch (NullPointerException e) {
            e.printStackTrace();
            internalList=new ArrayList<>();
        }
        return internalList;
    }

    public static String[] getStorageDirectories(Context context) {
        String [] storageDirectories;
        String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            List<String> results = new ArrayList<>();
            File[] externalDirs = context.getExternalFilesDirs(null);
            for (File file : externalDirs) {
                String path = file.getPath().split("/Android")[0];
                if((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Environment.isExternalStorageRemovable(file))
                        || rawSecondaryStoragesStr != null && rawSecondaryStoragesStr.contains(path)){
                    results.add(path);
                }
            }
            storageDirectories = results.toArray(new String[0]);
        }else{
            final Set<String> rv = new HashSet<>();

            if (!TextUtils.isEmpty(rawSecondaryStoragesStr)) {
                final String[] rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator);
                Collections.addAll(rv, rawSecondaryStorages);
            }
            storageDirectories = rv.toArray(new String[rv.size()]);
        }
        return storageDirectories;
    }
}
