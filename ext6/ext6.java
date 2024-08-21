package tar.eof.ext6;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.roughike.bottombar.BottomBar;
import com.simplecityapps.recyclerview_fastscroll.interfaces.OnFastScrollStateChangeListener;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.VirtualFileSystem;
import tar.eof.ext6.adapters.CustomAdapter;
import tar.eof.ext6.adapters.CustomAdapterItemClickListener;
import tar.eof.ext6.fileoperations.FileIO;
import tar.eof.ext6.fileoperations.Operations;
import tar.eof.ext6.interfaces.IContextSwitcher;
import tar.eof.ext6.interfaces.IFuncPtr;
import tar.eof.ext6.listeners.OnFileChangedListener;
import tar.eof.ext6.listeners.TabChangeListener;
import tar.eof.ext6.models.FileItem;
import tar.eof.ext6.utils.Permissions;
import tar.eof.ext6.utils.PreferenceStorage;
import tar.eof.ext6.utils.UIUtils;

public class ext6 extends AppCompatActivity implements OnFileChangedListener, IContextSwitcher {

    private Context mContext;

    private CustomAdapter mAdapter;
    private FastScrollRecyclerView.LayoutManager mLayoutManager;
    private FastScrollRecyclerView mFilesListView;

    private BottomBar mBottomView;
    private BottomBar mTopStorageView;
    private TabChangeListener mTabChangeListener;

    private TextView mCurrentPath;
    private NavigationHelper mNavigationHelper;
    private Operations op;
    public FileIO io;

    //Action Mode for filebrowser_toolbar
    private static ActionMode mActionMode;
    private static final int APP_PERMISSION_REQUEST = 0;

    private List<FileItem> mFileList = new ArrayList<>();

    private VirtualFileSystem vfs;
    public static final String AB = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    public static SecureRandom rnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        //file storage stuff
        vfs = VirtualFileSystem.get();
        // Get File Storage Permission
        Intent in = new Intent(this, Permissions.class);
        Bundle bundle = new Bundle();
        bundle.putStringArray(Constants.APP_PREMISSION_KEY, Constants.APP_PREMISSIONS);
        in.putExtras(bundle);
        startActivityForResult(in, APP_PERMISSION_REQUEST);

        // Initialize Stuff
        mNavigationHelper = new NavigationHelper(mContext);
        mNavigationHelper.setmChangeDirectoryListener(this);
        io = new FileIO(mNavigationHelper, new Handler(Looper.getMainLooper()), mContext);
        op = Operations.getInstance(mContext);

        //file storage stuff too
        rnd = new SecureRandom();

        //set file filter (i.e display files with the given extension)
        String filterFilesWithExtension = null;
        if (filterFilesWithExtension != null && !filterFilesWithExtension.isEmpty()) {
            Set<String> allowedFileExtensions = new HashSet<String>(Arrays.asList(filterFilesWithExtension.split(";")));
            mNavigationHelper.setAllowedFileExtensionFilter(allowedFileExtensions);
        }

    }


    public static String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vfs.isMounted())
            vfs.unmount();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_PERMISSION_REQUEST) {
            if (resultCode != Activity.RESULT_OK)
                Toast.makeText(mContext, mContext.getString(R.string.error_no_permissions), Toast.LENGTH_LONG).show();

            if(PreferenceStorage.getBaseDrive().equals("maria")){
             //first run
                rnd = new SecureRandom();
                Calendar c = Calendar.getInstance();
                SimpleDateFormat df = new SimpleDateFormat("ddMMMyyyy");
                String formattedDate = df.format(c.getTime());
                final String FileName = getDir("vfs", MODE_PRIVATE).getAbsolutePath() + "/" + randomString(6) + formattedDate + ".vfs";
                java.io.File nameDrive = new java.io.File(FileName);
                UIUtils.BaseDriveDialog(this, nameDrive.getName(), new IFuncPtr(){
                    @Override
                    public void execute(final String val) {
                        PreferenceStorage.storeBaseDrive(FileName);
                        PreferenceStorage.storeCurrentDrive(PreferenceStorage.getBaseDrive());
                        vfs.setContainerPath(PreferenceStorage.getCurrentDrive());
                        vfs.createNewContainer(vfs.getContainerPath(),val);
                        loadUi(true, val);
                    }
                });
            }else {
                vfs.setContainerPath(PreferenceStorage.getCurrentDrive());
                //secuential with current

                String valid_until = PreferenceStorage.getExpiration();

                Date dateconverted = new Date(Long.parseLong(valid_until.toLowerCase(), 16)*100000);

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

                String dateString = sdf.format(dateconverted);

                Date strDate = null;
                try {
                    strDate = sdf.parse(dateString);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if ((new Date().after(strDate))) {
                    UIUtils.LicenceDialog(this, "pachuli70hpta", false,  new IFuncPtr(){
                        @Override
                        public void execute(final String key) {
                        }
                    });
                }

                getpass();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_default_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_newfolder) {
            UIUtils.showEditTextDialog(this, "", "Create Directory", new IFuncPtr(){
                @Override
                public void execute(final String val) {
                    io.createDirectory(new File(mNavigationHelper.getCurrentDirectory(),val.trim()));
                }
            });
        }
        else if (item.getItemId() == R.id.action_paste) {
            if (op.getOperation() == Operations.FILE_OPERATIONS.NONE) {
                UIUtils.ShowToast(mContext.getString(R.string.no_operation_error), mContext);
            }else if (op.getSelectedFiles() == null) {
                UIUtils.ShowToast(mContext.getString(R.string.no_files_paste), mContext);
            }else {
                io.pasteFiles(mNavigationHelper.getCurrentDirectory());
            }
        }

        return false;
    }
    @Override
    public void onBackPressed() {

        if (mAdapter.getChoiceMode() == Constants.CHOICE_MODE.MULTI_CHOICE) {
            switchMode(Constants.CHOICE_MODE.SINGLE_CHOICE);
            return;
        }

        if (!mNavigationHelper.navigateBack()) {
            super.onBackPressed();
        }
    }

    @Override
    public void changeBottomNavMenu(Constants.CHOICE_MODE multiChoice) {
        if (multiChoice == Constants.CHOICE_MODE.SINGLE_CHOICE) {
            mBottomView.setItems(R.xml.bottom_nav_items);
            mBottomView.getTabWithId(R.id.menu_none).setVisibility(View.GONE);
            mTopStorageView.getTabWithId(R.id.menu_none).setVisibility(View.GONE);
        } else {
            mBottomView.setItems(R.xml.bottom_nav_items_multiselect);
            mBottomView.getTabWithId(R.id.menu_none).setVisibility(View.GONE);
            mTopStorageView.getTabWithId(R.id.menu_none).setVisibility(View.GONE);
        }
    }

    @Override
    public void setNullToActionMode() {
        if (mActionMode != null)
            mActionMode = null;
    }

    @Override
    public void onResume() {
        super.onResume();
    }
        @Override
    public void reDrawFileList() {
        mFilesListView.setLayoutManager(null);
        mFilesListView.setAdapter(mAdapter);
        mFilesListView.setLayoutManager(mLayoutManager);
        mTabChangeListener.setmAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void switchMode(Constants.CHOICE_MODE mode) {
        if (mode == Constants.CHOICE_MODE.SINGLE_CHOICE) {
            if (mActionMode != null)
                mActionMode.finish();
        } else {
            if(mActionMode == null) {
                closeSearchView();
                ToolbarActionMode newToolBar = new ToolbarActionMode(this,this, mAdapter, Constants.APP_MODE.FILE_BROWSER, io);
                mActionMode = startSupportActionMode(newToolBar);
                mActionMode.setTitle(mContext.getString(R.string.select_multiple));
            }
        }
    }

    @Override
    public void onFileChanged(File updatedDirectory) {
        if (updatedDirectory != null && updatedDirectory.exists() && updatedDirectory.isDirectory()) {
            mFileList = mNavigationHelper.getFilesItemsInCurrentDirectory();
            mCurrentPath.setText(updatedDirectory.getAbsolutePath());
            mAdapter.notifyDataSetChanged();
            //mTopStorageView.getTabWithId(R.id.menu_internal_storage).setTitle(FileUtils.byteCountToDisplaySize(Constants.internalStorageRoot.getUsableSpace()) + "/" + FileUtils.byteCountToDisplaySize(Constants.internalStorageRoot.getTotalSpace()));
            //if (Constants.externalStorageRoot != null)
                //mTopStorageView.getTabWithId(R.id.menu_external_storage).setTitle(FileUtils.byteCountToDisplaySize(Constants.externalStorageRoot.getUsableSpace()) + "/" + FileUtils.byteCountToDisplaySize(Constants.externalStorageRoot.getTotalSpace()));
        }
    }

    private void getpass() {
        final View view = this.getLayoutInflater().inflate(R.layout.dialog_key2, null);
        final EditText pass = (EditText) view.findViewById(R.id.key_blc);

        Dialog passdialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (!pass.getText().toString().isEmpty()) {
                            ext6.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    final Handler handler = new Handler();
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            loadUi(true, pass.getText().toString());
                                        }
                                    }, 100);
                                }
                            });
                        } else {
                            Toast.makeText(mContext, "Invalid password", Toast.LENGTH_SHORT).show();
                            ext6.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    final Handler handler = new Handler();
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            loadUi(false, pass.getText().toString());
                                        }
                                    }, 1500);
                                }
                            });
                        }
                    }
                })
                .create();
        passdialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        passdialog.show();
    }

    private void loadUi(boolean scope, String passcode) {

        if(!scope) {
            System.exit(0);
        }

        //file storage stuff
        try {
            if (!vfs.isMounted())
                vfs.mount(passcode);

        }catch(IllegalArgumentException | IllegalStateException e){
          Toast.makeText(mContext, "invalid password", Toast.LENGTH_SHORT).show();
            Log.println(0,"VFS",e.toString());
          System.exit(0);
        }

        setContentView(R.layout.filebrowser_activity_main);
        mFileList = mNavigationHelper.getFilesItemsInCurrentDirectory();

        mCurrentPath = findViewById(R.id.currentPath);
        mFilesListView = findViewById(R.id.recycler_view);
        mAdapter = new CustomAdapter(mFileList,mContext);
        mFilesListView.setAdapter(mAdapter);
        mLayoutManager = new LinearLayoutManager(mContext);
        mFilesListView.setLayoutManager(mLayoutManager);
        final CustomAdapterItemClickListener onItemClickListener = new CustomAdapterItemClickListener(mContext, mFilesListView, new CustomAdapterItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                // TODO Handle item click
                if (mAdapter.getChoiceMode()== Constants.CHOICE_MODE.SINGLE_CHOICE) {
                    File f = mAdapter.getItemAt(position).getFile();
                    if (f.isDirectory()) {
                        closeSearchView();
                        mNavigationHelper.changeDirectory(f);
                    } else {
                        /*MimeTypeMap mimeMap = MimeTypeMap.getSingleton();
                        Intent openFileIntent = new Intent(Intent.ACTION_VIEW);
                        String mimeType = mimeMap.getMimeTypeFromExtension(FilenameUtils.getExtension(f.getName()));
                        Uri uri = FileProvider.getUriForFile(mContext, mContext.getString(R.string.filebrowser_provider), f);
                        openFileIntent.setDataAndType(uri,mimeType);
                        openFileIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        openFileIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try {
                            mContext.startActivity(openFileIntent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(mContext, mContext.getString(R.string.no_app_to_handle), Toast.LENGTH_LONG).show();
                        }*/
                        switchMode(Constants.CHOICE_MODE.MULTI_CHOICE);
                        mAdapter.selectItem(position);
                        mFilesListView.scrollToPosition(position);
                    }
                }else{
                    if(mAdapter.getItemAt(position).isSelected()) {
                        mAdapter.getItemAt(position).setSelected(false);
                        mAdapter.notifyDataSetChanged();
                    }
                    else {
                        mAdapter.selectItem(position);
                    }
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                switchMode(Constants.CHOICE_MODE.MULTI_CHOICE);
                mAdapter.selectItem(position);
                mFilesListView.scrollToPosition(position);
            }
        });
        mFilesListView.addOnItemTouchListener(onItemClickListener);

        mFilesListView.setOnFastScrollStateChangeListener(new OnFastScrollStateChangeListener() {
            @Override
            public void onFastScrollStart() {
                onItemClickListener.setmFastScrolling(true);
            }

            @Override
            public void onFastScrollStop() {
                onItemClickListener.setmFastScrolling(false);
            }
        });

        Toolbar toolbar = findViewById(R.id.filebrowser_tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setIcon(R.drawable.ic_quom);
        getSupportActionBar().setTitle("    x84");
        getSupportActionBar().setSubtitle("   Encrypted FileSystem Suite");

        toolbar.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                UIUtils.LicenceDialog(mContext, "pachuli70hpta", true,  new IFuncPtr(){
                    @Override
                    public void execute(final String key) {
                    }
                });
                return true;
            }
        });

        mBottomView = findViewById(R.id.bottom_navigation);
        mTopStorageView = findViewById(R.id.currPath_Nav);

        mTabChangeListener = new TabChangeListener(this, mNavigationHelper, mAdapter, io,this);

        mBottomView.setOnTabSelectListener(mTabChangeListener);
        mBottomView.setOnTabReselectListener(mTabChangeListener);

        mTopStorageView.setOnTabSelectListener(mTabChangeListener);
        mTopStorageView.setOnTabReselectListener(mTabChangeListener);

        mBottomView.getTabWithId(R.id.menu_none).setVisibility(View.GONE);
        mTopStorageView.getTabWithId(R.id.menu_none).setVisibility(View.GONE);

        onFileChanged(mNavigationHelper.getCurrentDirectory());

        //switch to initial directory if given
        String initialDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/";
        if (initialDirectory != null && !initialDirectory.isEmpty() ) {
            File initDir = new File(initialDirectory);
            if (initDir.exists())
                mNavigationHelper.changeDirectory(initDir);
        }
    }

    private void closeSearchView() {
    }

}