package tar.eof.ext6.listeners;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.roughike.bottombar.OnTabReselectListener;
import com.roughike.bottombar.OnTabSelectListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import info.guardianproject.iocipher.VirtualFileSystem;
import tar.eof.ext6.Constants;
import tar.eof.ext6.NavigationHelper;
import tar.eof.ext6.R;
import tar.eof.ext6.adapters.CustomAdapter;
import tar.eof.ext6.ext6;
import tar.eof.ext6.fileoperations.FileIO;
import tar.eof.ext6.fileoperations.Operations;
import tar.eof.ext6.filepicker.controller.DialogSelectionListener;
import tar.eof.ext6.filepicker.model.DialogConfigs;
import tar.eof.ext6.filepicker.model.DialogProperties;
import tar.eof.ext6.filepicker.view.FilePickerDialog;
import tar.eof.ext6.filepicker.view.FilePickerPreference;
import tar.eof.ext6.interfaces.IContextSwitcher;
import tar.eof.ext6.interfaces.IFuncPtr;
import tar.eof.ext6.models.FileItem;
import tar.eof.ext6.utils.FileUtils;
import tar.eof.ext6.utils.PreferenceStorage;
import tar.eof.ext6.utils.UIUtils;

/**
 * Created by Aditya on 4/18/2017.
 */
public class TabChangeListener implements OnTabSelectListener, OnTabReselectListener {

    private NavigationHelper mNavigationHelper;
    private CustomAdapter mAdapter;
    private Activity mActivity;
    private FileIO io;
    private IContextSwitcher mIContextSwitcher;
    private Constants.SELECTION_MODES selectionMode;
    private Constants.APP_MODE appMode;

    public TabChangeListener(Activity mActivity, NavigationHelper mNavigationHelper, CustomAdapter mAdapter, FileIO io, IContextSwitcher mContextSwtcher) {
        this.mNavigationHelper = mNavigationHelper;
        this.mActivity = mActivity;
        this.mAdapter = mAdapter;
        this.io = io;
        this.mIContextSwitcher = mContextSwtcher;
        this.selectionMode = Constants.SELECTION_MODES.SINGLE_SELECTION;
        this.appMode = Constants.APP_MODE.FILE_CHOOSER;
    }


    public void onImport() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.alsodelete = true;

        FilePickerDialog dialog = new FilePickerDialog(mActivity,properties);
        dialog.setTitle("import to 0x84 drive");

        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files, boolean alsodelete) {
                io.pasteFiles0x84(mNavigationHelper.getCurrentDirectory(), files, alsodelete);
                mIContextSwitcher.switchMode(Constants.CHOICE_MODE.SINGLE_CHOICE);
             }

            @Override
            public void onCancelSelection() {
                Operations op = Operations.getInstance(mActivity);
                if (op != null) {
                    op.setOperation(Operations.FILE_OPERATIONS.NONE);
                }
            }
        });

        dialog.show();
    }

    public void onExport() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.DIR_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.alsodelete = DialogConfigs.ALSODELETE;

        FilePickerDialog dialog = new FilePickerDialog(mActivity,properties);
        dialog.setTitle("export to phone");

        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files, boolean alsodelete) {
                io.exportFiles(files[0]);
                mIContextSwitcher.switchMode(Constants.CHOICE_MODE.SINGLE_CHOICE);
            }
            @Override
            public void onCancelSelection()
            {
                Operations op = Operations.getInstance(mActivity);
                if (op != null) {
                    op.setOperation(Operations.FILE_OPERATIONS.NONE);
                }
                mIContextSwitcher.switchMode(Constants.CHOICE_MODE.SINGLE_CHOICE);
                mAdapter.unSelectAll();
            }
        });

        dialog.show();
    }

    @Override
    public void onTabSelected(@IdRes int tabId) {
        handleTabChange(tabId);
    }

    @Override
    public void onTabReSelected(@IdRes int tabId) {
        handleTabChange(tabId);
    }

    private void handleTabChange(int tabId) {

            if (tabId == R.id.menu_back) {
                mNavigationHelper.navigateBack();
            }
            else if (tabId == R.id.menu_internal_storage) {
                onImport();
                mAdapter.unSelectAll();
                mIContextSwitcher.switchMode(Constants.CHOICE_MODE.SINGLE_CHOICE);
            }
            else if (tabId == R.id.menu_export) {
                Operations op = Operations.getInstance(mActivity);
                if (op != null) {
                    op.setOperation(Operations.FILE_OPERATIONS.COPY);
                    op.setSelectedFiles(mAdapter.getSelectedItems());
                    onExport();
                    mAdapter.unSelectAll();
                    mIContextSwitcher.switchMode(Constants.CHOICE_MODE.SINGLE_CHOICE);
                }
            }else if (tabId == R.id.menu_import_drive) {
                UIUtils.CurrentDriveDialog(mActivity, new IFuncPtr(){
                    @Override
                    public void execute(final String val) {

                        PreferenceStorage.storeCurrentDrive(val);
                        Toast.makeText(mActivity, "rebooting filesystem", Toast.LENGTH_LONG).show();
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                System.exit(0);
                            }
                        }, 1300);
                    }
                });
            }else if (tabId == R.id.menu_external_storage) {

                LayoutInflater inflater = (LayoutInflater)mActivity.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
                View v = inflater.inflate(R.layout.empty_layout, null);
                TextView textdrive = (TextView) v.findViewById(R.id.textEmpty);

                textdrive.setVisibility(View.VISIBLE);
                textdrive.setText("Make a hard copy of your encrypted drive on phone? \n\nRemember your password please, you CANNOT decrypt files in drive without that ;)");

                Dialog dialog =  new MaterialAlertDialogBuilder(mActivity)
                        .setView(v)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Calendar c = Calendar.getInstance();
                                SimpleDateFormat df = new SimpleDateFormat("ddMMMyyyy");
                                String formattedDate = df.format(c.getTime());

                                String name = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + ext6.randomString(8) + formattedDate + ".vfs";
                                io.exportDrive(name);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                                dialog.dismiss();
                            }
                        })
                        .create();
                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        dialog.show();
                mAdapter.unSelectAll();
            }
            else if (tabId == R.id.menu_refresh) {
                mIContextSwitcher.switchMode(Constants.CHOICE_MODE.SINGLE_CHOICE);
                mNavigationHelper.triggerFileChanged();
            }
            else if (tabId == R.id.menu_filter) {
                UIUtils.showRadioButtonDialog(mActivity, mActivity.getResources().getStringArray(R.array.filter_options), mActivity.getString(R.string.filter_only), new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup radioGroup, int position) {
                        Operations op = Operations.getInstance(mActivity);
                        if (op != null) {
                            op.setmCurrentFilterOption(Constants.FILTER_OPTIONS.values()[position]);
                        }
                        mNavigationHelper.triggerFileChanged();
                    }
                });
            }
            else if (tabId == R.id.menu_sort) {
                UIUtils.showRadioButtonDialog(mActivity, mActivity.getResources().getStringArray(R.array.sort_options), mActivity.getString(R.string.sort_by), new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup radioGroup, int position) {
                        Operations op = Operations.getInstance(mActivity);
                        if (op != null) {
                            op.setmCurrentSortOption(Constants.SORT_OPTIONS.values()[position]);
                        }
                        mNavigationHelper.triggerFileChanged();
                    }
                });
            }
            else if (tabId == R.id.menu_delete) {
                List<FileItem> selectedItems = mAdapter.getSelectedItems();
                if (io != null) {
                    io.deleteItems(selectedItems);
                    mIContextSwitcher.switchMode(Constants.CHOICE_MODE.SINGLE_CHOICE);
                }
            }
            else if (tabId == R.id.menu_copy) {
                Operations op = Operations.getInstance(mActivity);
                if (op != null) {
                    op.setOperation(Operations.FILE_OPERATIONS.COPY);
                    op.setSelectedFiles(mAdapter.getSelectedItems());
                    mIContextSwitcher.switchMode(Constants.CHOICE_MODE.SINGLE_CHOICE);
                }
            }
            else if (tabId == R.id.menu_cut) {
                Operations op = Operations.getInstance(mActivity);
                if (op != null) {
                    op.setOperation(Operations.FILE_OPERATIONS.CUT);
                    op.setSelectedFiles(mAdapter.getSelectedItems());
                    mIContextSwitcher.switchMode(Constants.CHOICE_MODE.SINGLE_CHOICE);
                }
            }
            else if (tabId == R.id.menu_chooseitems) {
                {
                    List<FileItem> selItems = getmAdapter().getSelectedItems();
                    ArrayList<Uri> chosenItems = new ArrayList<>();
                    boolean hasInvalidSelections = false;
                    for (int i = 0; i < selItems.size(); i++) {
                        if (getAppMode() == Constants.APP_MODE.FOLDER_CHOOSER) {
                            if (selItems.get(i).getFile().isDirectory()) {
                                chosenItems.add(Uri.fromFile(selItems.get(i).getFile()));
                            } else {
                                hasInvalidSelections = true;
                            }
                        } else {
                            chosenItems.add(Uri.fromFile(selItems.get(i).getFile()));
                        }
                    }
                    if (hasInvalidSelections) {
                        UIUtils.ShowToast(mActivity.getString(R.string.invalid_selections),mActivity);
                        mActivity.finish();
                    }

                    mIContextSwitcher.switchMode(Constants.CHOICE_MODE.SINGLE_CHOICE);
                    if(getSelectionMode() == Constants.SELECTION_MODES.SINGLE_SELECTION) {
                        if(chosenItems.size() == 1) {
                            Intent data = new Intent();
                            data.setData(chosenItems.get(0));
                            mActivity.setResult(Activity.RESULT_OK, data);
                            mActivity.finish();
                        } else {
                            UIUtils.ShowToast(mActivity.getString(R.string.selection_error_single),mActivity);
                        }
                    } else {
                        Intent data = new Intent();
                        data.putParcelableArrayListExtra(Constants.SELECTED_ITEMS, chosenItems);
                        mActivity.setResult(Activity.RESULT_OK, data);
                        mActivity.finish();
                    }
                }
            } else if (tabId == R.id.menu_select) {
                Uri fileUri = Uri.fromFile(mNavigationHelper.getCurrentDirectory());
                Intent data = new Intent();
                data.setData(fileUri);
                mActivity.setResult(Activity.RESULT_OK, data);
                mActivity.finish();
            }
    }


    public static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    public CustomAdapter getmAdapter() {
        return mAdapter;
    }

    public void setmAdapter(CustomAdapter mAdapter) {
        this.mAdapter = mAdapter;
    }

    public Constants.SELECTION_MODES getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(Constants.SELECTION_MODES selectionMode) {
        this.selectionMode = selectionMode;
    }

    public Constants.APP_MODE getAppMode() {
        return appMode;
    }

    public void setAppMode(Constants.APP_MODE appMode) {
        this.appMode = appMode;
    }
}
