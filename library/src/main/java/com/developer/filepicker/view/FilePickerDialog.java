package com.developer.filepicker.view;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.developer.filepicker.R;
import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.controller.NotifyItemChecked;
import com.developer.filepicker.controller.adapters.FileListAdapter;
import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.model.FileListItem;
import com.developer.filepicker.model.MarkedItemList;
import com.developer.filepicker.utils.ExtensionFilter;
import com.developer.filepicker.utils.Utility;
import com.developer.filepicker.widget.MaterialCheckbox;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author akshay sunil masram
 */
public class FilePickerDialog extends Dialog implements AdapterView.OnItemClickListener {

    private final Context context;
    private ListView listView;
    private TextView dname, dir_path, title, select;
    private DialogProperties properties;
    private DialogSelectionListener callbacks;
    private ArrayList<FileListItem> internalList;
    private ExtensionFilter filter;
    private FileListAdapter mFileListAdapter;
    private String titleStr = null;
    private String positiveBtnNameStr = null;
    private String negativeBtnNameStr = null;

    public static final int EXTERNAL_READ_PERMISSION_GRANT = 112;

    public FilePickerDialog(Context context) {
        super(context);
        this.context = context;
        properties = new DialogProperties();
        filter = new ExtensionFilter(properties);
        internalList = new ArrayList<>();
    }

    public FilePickerDialog(Context context, DialogProperties properties, int themeResId) {
        super(context, themeResId);
        this.context = context;
        this.properties = properties;
        filter = new ExtensionFilter(properties);
        internalList = new ArrayList<>();
    }

    public FilePickerDialog(Context context, DialogProperties properties) {
        super(context);
        this.context = context;
        this.properties = properties;
        filter = new ExtensionFilter(properties);
        internalList = new ArrayList<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_main);
        listView = findViewById(R.id.fileList);
        select = findViewById(R.id.select);

        LinearLayout background = findViewById(R.id.background);
        ViewGroup.LayoutParams parentParams = background.getLayoutParams();

        int[] a = Utility.get_Device_height_width(context);
        if (a[0]<a[1]) {  //landscape
            parentParams.height = (a[0]/5)*4;
            parentParams.width = (a[1]/5)*2;
        }else {           // portrait

            if (Utility.isTablet(context)){
                parentParams.width = (a[1]/7)*3;
                parentParams.height = (a[0]/7)*3;
            }else {
                parentParams.width = (a[1]/6)*5;
                parentParams.height = (a[0]/7)*4;
            }
        }
        background.setLayoutParams(parentParams);



        int size = MarkedItemList.getFileCount();
        if (size == 0) {
            select.setEnabled(false);
            int color;
            color = context.getResources().getColor(R.color.colorAccent);
            select.setTextColor(color);
        }
        dname = findViewById(R.id.dname);
        title = findViewById(R.id.title);
        dir_path = findViewById(R.id.dir_path);
        TextView cancel = findViewById(R.id.cancel);
        if (negativeBtnNameStr != null) {
            cancel.setText(negativeBtnNameStr);
        }
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] paths = MarkedItemList.getSelectedPaths();
                if (callbacks != null) {
                    callbacks.onSelectedFilePaths(paths);
                }
                dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancel();
            }
        });
        mFileListAdapter = new FileListAdapter(internalList, context, properties);
        mFileListAdapter.setNotifyItemCheckedListener(new NotifyItemChecked() {
            @Override
            public void notifyCheckBoxIsClicked() {
                positiveBtnNameStr = positiveBtnNameStr == null ?
                        context.getResources().getString(R.string.choose_button_label) : positiveBtnNameStr;
                int size = MarkedItemList.getFileCount();
                if (size == 0) {
                    select.setEnabled(false);
                    int color;
                    color = context.getResources().getColor(R.color.colorAccent);
                    select.setTextColor(color);
                    select.setText(positiveBtnNameStr);
                } else {
                    select.setEnabled(true);
                    int color;
                    color = context.getResources().getColor(R.color.colorAccent);
                    select.setTextColor(color);
                    String button_label = positiveBtnNameStr + " (" + size + ") ";
                    select.setText(button_label);
                }
                if (properties.selection_mode == DialogConfigs.SINGLE_MODE) {
                    /*  If a single file has to be selected, clear the previously checked
                     *  checkbox from the list.
                     */
                    mFileListAdapter.notifyDataSetChanged();
                }
            }
        });
        listView.setAdapter(mFileListAdapter);

        //Title method added in version 1.0.5
        setTitle();
    }

    private void setTitle() {
        if (title == null || dname == null) {
            return;
        }
        if (titleStr != null) {
            if (title.getVisibility() == View.INVISIBLE) {
                title.setVisibility(View.VISIBLE);
            }
            title.setText(titleStr);
            if (dname.getVisibility() == View.VISIBLE) {
                dname.setVisibility(View.INVISIBLE);
            }
        } else {
            if (title.getVisibility() == View.VISIBLE) {
                title.setVisibility(View.INVISIBLE);
            }
            if (dname.getVisibility() == View.INVISIBLE) {
                dname.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        positiveBtnNameStr = (
                positiveBtnNameStr == null ?
                        context.getResources().getString(R.string.choose_button_label) :
                        positiveBtnNameStr
        );
        select.setText(positiveBtnNameStr);
        if (Utility.checkStorageAccessPermissions(context)) {
            File currLoc;
            internalList.clear();
            if (properties.offset.isDirectory() && validateOffsetPath()) {
                currLoc = new File(properties.offset.getAbsolutePath());
                FileListItem parent = new FileListItem();
                parent.setFilename(context.getString(R.string.label_parent_dir));
                parent.setDirectory(true);
                parent.setLocation(Objects.requireNonNull(currLoc.getParentFile())
                        .getAbsolutePath());
                parent.setTime(currLoc.lastModified());
                parent.setSize(currLoc.length());
                internalList.add(parent);
                internalList = Utility.prepareFileListEntries(internalList, currLoc, filter, properties.show_hidden_files);
                properties.offset = new File(DialogConfigs.DEFAULT_DIR);

            } else if (properties.root.exists() && properties.root.isDirectory()) {
                String[] allStorage = Utility.getStorageDirectories(context);
                for (int i = 0; i<allStorage.length; i++){
                    File storage = new File(allStorage[i]);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        if (Environment.isExternalStorageRemovable(storage)){
                            FileListItem parent2 = new FileListItem();
                            parent2.setFilename(context.getString(R.string.label_exsdcard_dir));
                            parent2.setDirectory(true);
                            parent2.setLocation(storage.getPath());
                            parent2.setTime(0);
                            parent2.setSize(0);
                            internalList.add(parent2);
                        }else {
                            FileListItem parent = new FileListItem();
                            parent.setFilename(context.getString(R.string.label_sdcard_dir));
                            parent.setDirectory(true);
                            parent.setLocation(storage.getPath());
                            parent.setTime(0);
                            parent.setSize(0);
                            internalList.add(parent);
                        }
                    }else {
                        FileListItem parent = new FileListItem();
                        parent.setFilename(context.getString(R.string.label_card_dir) + i);
                        parent.setDirectory(true);
                        parent.setLocation(storage.getPath());
                        parent.setTime(0);
                        parent.setSize(0);
                        internalList.add(parent);
                    }
                }


                currLoc = new File(DialogConfigs.DEFAULT_DIR);
            } else {
                currLoc = new File(properties.error_dir.getAbsolutePath());
                internalList = Utility.prepareFileListEntries(internalList, currLoc, filter, properties.show_hidden_files);
            }
            dname.setText(currLoc.getName());
            dir_path.setText(currLoc.getAbsolutePath());
            setTitle();
            mFileListAdapter.notifyDataSetChanged();
            listView.setOnItemClickListener(this);
        }
    }

    private boolean validateOffsetPath() {
        String offset_path = properties.offset.getAbsolutePath();
        String root_path = properties.root.getAbsolutePath();
        return !offset_path.equals(root_path) && offset_path.contains(root_path);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (internalList.size() > i) {
            FileListItem fitem = internalList.get(i);
            if (fitem.isDirectory()) {
                if (new File(fitem.getLocation()).canRead()) {
                    File currLoc = new File(fitem.getLocation());
                    dname.setText(currLoc.getName());
                    setTitle();
                    dir_path.setText(currLoc.getAbsolutePath());
                    internalList.clear();
                    if (!currLoc.getName().equals(properties.root.getName())) {
                        FileListItem parent = new FileListItem();
                        parent.setFilename(context.getString(R.string.label_parent_dir));
                        parent.setDirectory(true);
                        parent.setLocation(Objects.requireNonNull(currLoc
                                .getParentFile()).getAbsolutePath());
                        parent.setTime(currLoc.lastModified());
                        parent.setSize(currLoc.length());
                        internalList.add(parent);
                    }
                    internalList = Utility.prepareFileListEntries(internalList, currLoc, filter, properties.show_hidden_files);
                    mFileListAdapter.notifyDataSetChanged();
                } else {
                    onStart();
                }
            } else {
                MaterialCheckbox fmark = view.findViewById(R.id.file_mark);
                fmark.performClick();
            }
        }
    }

    public DialogProperties getProperties() {
        return properties;
    }

    public void setProperties(DialogProperties properties) {
        this.properties = properties;
        filter = new ExtensionFilter(properties);
    }

    public void setDialogSelectionListener(DialogSelectionListener callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public void setTitle(CharSequence titleStr) {
        if (titleStr != null) {
            this.titleStr = titleStr.toString();
        } else {
            this.titleStr = null;
        }
        setTitle();
    }

    public void setPositiveBtnName(CharSequence positiveBtnNameStr) {
        if (positiveBtnNameStr != null) {
            this.positiveBtnNameStr = positiveBtnNameStr.toString();
        } else {
            this.positiveBtnNameStr = null;
        }
    }

    public void setNegativeBtnName(CharSequence negativeBtnNameStr) {
        if (negativeBtnNameStr != null) {
            this.negativeBtnNameStr = negativeBtnNameStr.toString();
        } else {
            this.negativeBtnNameStr = null;
        }
    }

    public void markFiles(List<String> paths) {
        if (paths != null && paths.size() > 0) {
            if (properties.selection_mode == DialogConfigs.SINGLE_MODE) {
                File temp = new File(paths.get(0));
                switch (properties.selection_type) {
                    case DialogConfigs.DIR_SELECT:
                        if (temp.exists() && temp.isDirectory()) {
                            FileListItem item = new FileListItem();
                            item.setFilename(temp.getName());
                            item.setDirectory(temp.isDirectory());
                            item.setMarked(true);
                            item.setTime(temp.lastModified());
                            item.setSize(temp.length());
                            item.setLocation(temp.getAbsolutePath());
                            MarkedItemList.addSelectedItem(item);
                        }
                        break;

                    case DialogConfigs.FILE_SELECT:
                        if (temp.exists() && temp.isFile()) {
                            FileListItem item = new FileListItem();
                            item.setFilename(temp.getName());
                            item.setDirectory(temp.isDirectory());
                            item.setMarked(true);
                            item.setTime(temp.lastModified());
                            item.setSize(temp.length());
                            item.setLocation(temp.getAbsolutePath());
                            MarkedItemList.addSelectedItem(item);
                        }
                        break;

                    case DialogConfigs.FILE_AND_DIR_SELECT:
                        if (temp.exists()) {
                            FileListItem item = new FileListItem();
                            item.setFilename(temp.getName());
                            item.setDirectory(temp.isDirectory());
                            item.setMarked(true);
                            item.setTime(temp.lastModified());
                            item.setSize(temp.length());
                            item.setLocation(temp.getAbsolutePath());
                            MarkedItemList.addSelectedItem(item);
                        }
                        break;
                }
            } else {
                for (String path : paths) {
                    switch (properties.selection_type) {
                        case DialogConfigs.DIR_SELECT:
                            File temp = new File(path);
                            if (temp.exists() && temp.isDirectory()) {
                                FileListItem item = new FileListItem();
                                item.setFilename(temp.getName());
                                item.setDirectory(temp.isDirectory());
                                item.setMarked(true);
                                item.setTime(temp.lastModified());
                                item.setSize(temp.length());
                                item.setLocation(temp.getAbsolutePath());
                                MarkedItemList.addSelectedItem(item);
                            }
                            break;

                        case DialogConfigs.FILE_SELECT:
                            temp = new File(path);
                            if (temp.exists() && temp.isFile()) {
                                FileListItem item = new FileListItem();
                                item.setFilename(temp.getName());
                                item.setDirectory(temp.isDirectory());
                                item.setMarked(true);
                                item.setTime(temp.lastModified());
                                item.setSize(temp.length());
                                item.setLocation(temp.getAbsolutePath());
                                MarkedItemList.addSelectedItem(item);
                            }
                            break;

                        case DialogConfigs.FILE_AND_DIR_SELECT:
                            temp = new File(path);
                            if (temp.exists() && (temp.isFile() || temp.isDirectory())) {
                                FileListItem item = new FileListItem();
                                item.setFilename(temp.getName());
                                item.setDirectory(temp.isDirectory());
                                item.setMarked(true);
                                item.setTime(temp.lastModified());
                                item.setSize(temp.length());
                                item.setLocation(temp.getAbsolutePath());
                                MarkedItemList.addSelectedItem(item);
                            }
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void show() {
        if (!Utility.checkStorageAccessPermissions(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                    ((Activity) context).requestPermissions(new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.MANAGE_EXTERNAL_STORAGE},
                            EXTERNAL_READ_PERMISSION_GRANT);
                }else {
                    ((Activity) context).requestPermissions(new String[]{Manifest.permission
                            .READ_EXTERNAL_STORAGE}, EXTERNAL_READ_PERMISSION_GRANT);
                }
            }
        } else {
            super.show();
            positiveBtnNameStr = positiveBtnNameStr == null ?
                    context.getResources().getString(R.string.choose_button_label) : positiveBtnNameStr;
            select.setText(positiveBtnNameStr);
            int size = MarkedItemList.getFileCount();
            if (size == 0) {
                select.setText(positiveBtnNameStr);
            } else {
                String button_label = positiveBtnNameStr + " (" + size + ") ";
                select.setText(button_label);
            }
        }
    }

    @Override
    public void onBackPressed() {
        //currentDirName is dependent on dname
        String currentDirName = dname.getText().toString();
        if (internalList.size() > 0) {
            FileListItem fitem = internalList.get(0);
            File currLoc = new File(fitem.getLocation());

            if (!currLoc.canRead()){
                onStart();
            }else if (currentDirName.equals(properties.root.getName())) {
                super.onBackPressed();
            } else {
                dname.setText(currLoc.getName());
                dir_path.setText(currLoc.getAbsolutePath());
                internalList.clear();
                if (!currLoc.getName().equals(properties.root.getName())) {
                    FileListItem parent = new FileListItem();
                    parent.setFilename(context.getString(R.string.label_parent_dir));
                    parent.setDirectory(true);
                    parent.setLocation(Objects.requireNonNull(currLoc.getParentFile())
                            .getAbsolutePath());
                    parent.setTime(currLoc.lastModified());
                    parent.setSize(currLoc.length());
                    internalList.add(parent);
                }
                internalList = Utility.prepareFileListEntries(internalList, currLoc, filter,
                        properties.show_hidden_files);
                mFileListAdapter.notifyDataSetChanged();
            }
            setTitle();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void dismiss() {
        MarkedItemList.clearSelectionList();
        internalList.clear();
        super.dismiss();
    }
}
