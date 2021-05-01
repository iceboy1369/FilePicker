package com.developer.filepicker.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.developer.filepicker.model.FileListItem;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

    public static boolean isTablet(Context context) {
        boolean xlarge = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4);
        boolean large = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
        return (xlarge || large);
    }

    @SuppressLint("WrongConstant")
    public static int[] get_Device_height_width(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager)context.getSystemService("window");
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        return new int[]{displayMetrics.heightPixels, displayMetrics.widthPixels};
    }

    public static String getFileSize(long bytes){
        return String.format(Locale.ENGLISH, "%.2f MB", bytes / (1024.00 * 1024.00));
    }

    public static ArrayList<FileListItem> prepareFileListEntries(ArrayList<FileListItem> internalList,
                           File inter, ExtensionFilter filter, boolean show_hidden_files) {
        try {
            File[] files  = inter.listFiles(filter);
            if (files!=null) {
                for (File name : files) {
                    if (name.canRead()) {
                        if (name.getName().startsWith(".") && !show_hidden_files) continue;
                        FileListItem item = new FileListItem();
                        item.setFilename(name.getName());
                        item.setSize(name.length());
                        item.setDirectory(name.isDirectory());
                        item.setLocation(name.getAbsolutePath());
                        item.setTime(name.lastModified());
                        internalList.add(item);
                    }
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
        List<String> results = new ArrayList<>();
        File[] externalDirs = context.getExternalFilesDirs(null);
        for (File file : externalDirs) {
            if (file.getPath().contains("/Android")){
                String path = file.getPath().split("/Android")[0];
                results.add(path);
            }
        }
        return results.toArray(new String[0]);
    }
}
