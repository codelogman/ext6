package tar.eof.ext6.fileoperations;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import info.guardianproject.iocipher.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import info.guardianproject.iocipher.FileOutputStream;
import info.guardianproject.iocipher.IOCipherFileChannel;
import info.guardianproject.iocipher.VirtualFileSystem;
import info.guardianproject.libcore.io.IoBridge;
import tar.eof.ext6.Constants;
import tar.eof.ext6.NavigationHelper;
import tar.eof.ext6.R;
import tar.eof.ext6.interfaces.IFuncPtr;
import tar.eof.ext6.models.FileItem;
import tar.eof.ext6.utils.FileUtils;
import tar.eof.ext6.utils.Filemerge;
import tar.eof.ext6.utils.IOUtils;
import tar.eof.ext6.utils.UIUpdateHelper;
import tar.eof.ext6.utils.UIUtils;

/**
 * Created by Aditya on 4/15/2017.
 */
public class FileIO {

    ExecutorService executor;
    Handler mUIUpdateHandler;
    Context mContext;
    UIUpdateHelper mHelper;
    NavigationHelper mNavigationHelper;

    public FileIO(NavigationHelper mNavigationHelper, Handler mUIUpdateHandler, Context mContext) {
        this.mUIUpdateHandler = mUIUpdateHandler;
        this.mContext = mContext;
        this.mNavigationHelper = mNavigationHelper;
        mHelper = new UIUpdateHelper(mNavigationHelper, mContext);
        executor = Executors.newFixedThreadPool(1);
    }

    public void createDirectory(final File path) {
        if (path.getParentFile() != null && path.getParentFile().canWrite()) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        FileUtils.forceMkdir(path);
                        mUIUpdateHandler.post(mHelper.updateRunner());
                    } catch (IOException e) {
                        e.printStackTrace();
                        mUIUpdateHandler.post(mHelper.errorRunner(mContext.getString(R.string.folder_creation_error)));
                    }
                }
            });
        } else {
            UIUtils.ShowToast(mContext.getString(R.string.permission_error), mContext);
        }
    }

    public void deleteItems(final List<FileItem> selectedItems) {
        if (selectedItems != null && selectedItems.size() > 0) {
            new MaterialAlertDialogBuilder(mContext)
                    .setMessage(mContext.getString(R.string.delete_dialog_message, selectedItems.size()))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with delete
                            final ProgressDialog progressDialog = new ProgressDialog(mContext);
                            progressDialog.setTitle(mContext.getString(R.string.delete_progress));
                            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                            progressDialog.setCancelable(false);
                            progressDialog.setProgress(0);
                            progressDialog.setMessage("");
                            progressDialog.show();

                            executor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    int i = 0;
                                    float TOTAL_ITEMS = selectedItems.size();
                                    try {
                                        for (; i < selectedItems.size(); i++) {
                                            mUIUpdateHandler.post(mHelper.progressUpdater(progressDialog, (int) ((i / TOTAL_ITEMS) * 100), "File: " + selectedItems.get(i).getFile().getName()));
                                            if (selectedItems.get(i).getFile().isDirectory()) {
                                                removeDir(selectedItems.get(i).getFile());
                                            } else {
                                                FileUtils.forceDelete(selectedItems.get(i).getFile());
                                            }
                                        }
                                        mUIUpdateHandler.post(mHelper.toggleProgressBarVisibility(progressDialog));
                                        mUIUpdateHandler.post(mHelper.updateRunner());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        mUIUpdateHandler.post(mHelper.toggleProgressBarVisibility(progressDialog));
                                        mUIUpdateHandler.post(mHelper.errorRunner(mContext.getString(R.string.delete_error)));
                                    }
                                }
                            });
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                            dialog.dismiss();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            UIUtils.ShowToast(mContext.getString(R.string.no_items_selected), mContext);
        }
    }

    public void pasteFiles(final File destination) {

        final Operations op = Operations.getInstance(mContext);
        final List<FileItem> selectedItems = op.getSelectedFiles();
        final Operations.FILE_OPERATIONS operation = op.getOperation();
        if (destination.canWrite()) {
            if (selectedItems != null && selectedItems.size() > 0) {
                final ProgressDialog progressDialog = new ProgressDialog(mContext);
                String title = mContext.getString(R.string.wait);
                progressDialog.setTitle(title);
                if (operation == Operations.FILE_OPERATIONS.COPY)
                    progressDialog.setTitle(mContext.getString(R.string.copying, title));
                else if (operation == Operations.FILE_OPERATIONS.CUT)
                    progressDialog.setTitle(mContext.getString(R.string.moving, title));

                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(false);
                progressDialog.setMessage("");
                progressDialog.setProgress(0);
                progressDialog.show();
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        int i = 0;
                        float TOTAL_ITEMS = selectedItems.size();
                        try {
                            for (; i < selectedItems.size(); i++) {
                                mUIUpdateHandler.post(mHelper.progressUpdater(progressDialog, (int) ((i / TOTAL_ITEMS) * 100), "File: " + selectedItems.get(i).getFile().getName()));
                                if (selectedItems.get(i).getFile().isDirectory()) {
                                    if (operation == Operations.FILE_OPERATIONS.CUT)
                                        FileUtils.moveDirectory(selectedItems.get(i).getFile(), new File(destination, selectedItems.get(i).getFile().getName()));
                                    else if (operation == Operations.FILE_OPERATIONS.COPY)
                                        FileUtils.copyDirectory(selectedItems.get(i).getFile(), new File(destination, selectedItems.get(i).getFile().getName()));
                                } else {
                                    if (operation == Operations.FILE_OPERATIONS.CUT)
                                        FileUtils.moveFile(selectedItems.get(i).getFile(), new File(destination, selectedItems.get(i).getFile().getName()));
                                    else if (operation == Operations.FILE_OPERATIONS.COPY)
                                        FileUtils.copyFile(selectedItems.get(i).getFile(), new File(destination, selectedItems.get(i).getFile().getName()));
                                }
                            }
                            mUIUpdateHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    op.resetOperation();
                                }
                            });
                            mUIUpdateHandler.post(mHelper.toggleProgressBarVisibility(progressDialog));
                            mUIUpdateHandler.post(mHelper.updateRunner());
                        } catch (IOException e) {
                            e.printStackTrace();
                            op.resetOperation();
                            mUIUpdateHandler.post(mHelper.toggleProgressBarVisibility(progressDialog));
                            mUIUpdateHandler.post(mHelper.errorRunner(mContext.getString(R.string.pasting_error)));
                        }
                    }
                });
            } else {
                UIUtils.ShowToast(mContext.getString(R.string.no_items_selected), mContext);
            }
        } else {
            UIUtils.ShowToast(mContext.getString(R.string.permission_error), mContext);
        }
    }

    public void exportDrive(final String dest) {

        final java.io.File destination = new java.io.File(dest);
            final java.io.File virtualfile = new java.io.File(VirtualFileSystem.get().getContainerPath());
                final ProgressDialog progressDialog = new ProgressDialog(mContext);
                String title = "exporting drive";
                progressDialog.setTitle(title);

                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setCancelable(false);
                progressDialog.setMessage("  wait a moment ...");
                progressDialog.setIndeterminate(true);
                progressDialog.show();
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                                try (InputStream in = new FileInputStream(virtualfile)) {
                                    try (OutputStream out = new java.io.FileOutputStream(destination)) {
                                        byte[] buf = new byte[1024];
                                        int len;
                                        while ((len = in.read(buf)) > 0) {
                                            out.write(buf, 0, len);
                                        }
                                    }
                                }
                            mUIUpdateHandler.post(mHelper.toggleProgressBarVisibility(progressDialog));
                            mUIUpdateHandler.post(mHelper.toggleToast(dest));
                            mUIUpdateHandler.post(mHelper.updateRunner());
                        } catch (IOException e) {
                            e.printStackTrace();
                            mUIUpdateHandler.post(mHelper.toggleProgressBarVisibility(progressDialog));
                            mUIUpdateHandler.post(mHelper.errorRunner(mContext.getString(R.string.pasting_error)));
                        }
                    }
                });
    }

    public void exportFiles(final String dest) {

        final Operations op = Operations.getInstance(mContext);
        final List<FileItem> selectedItems = op.getSelectedFiles();
        final java.io.File destination = new java.io.File(dest);
        if (destination.canWrite()) {
            if (selectedItems != null && selectedItems.size() > 0) {
                final ProgressDialog progressDialog = new ProgressDialog(mContext);
                String title = mContext.getString(R.string.wait);
                progressDialog.setTitle(title);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(false);
                progressDialog.setMessage("");
                progressDialog.setProgress(0);
                progressDialog.show();
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        int i = 0;
                        float TOTAL_ITEMS = selectedItems.size();
                        try {
                            for (; i < selectedItems.size(); i++) {
                                mUIUpdateHandler.post(mHelper.progressUpdater(progressDialog, (int) ((i / TOTAL_ITEMS) * 100), "File: " + selectedItems.get(i).getFile().getName()));
                                if (selectedItems.get(i).getFile().isDirectory()) {
                                        Filemerge.copyDirectory(selectedItems.get(i).getFile(), new java.io.File(dest + "/" + selectedItems.get(i).getFile().getName()));
                                } else {
                                        //Filemerge.copyFile(selectedItems.get(i).getFile(), new java.io.File(dest, selectedItems.get(i).getFile().getName()));

                                    exportToDisk(selectedItems.get(i).getFile(), new java.io.File(dest, selectedItems.get(i).getFile().getName()));
                                }
                            }
                            mUIUpdateHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    op.resetOperation();
                                }
                            });
                            mUIUpdateHandler.post(mHelper.toggleProgressBarVisibility(progressDialog));
                            mUIUpdateHandler.post(mHelper.updateRunner());
                        } catch (IOException e) {
                            e.printStackTrace();
                            mUIUpdateHandler.post(mHelper.toggleProgressBarVisibility(progressDialog));
                            mUIUpdateHandler.post(mHelper.errorRunner(mContext.getString(R.string.pasting_error)));
                        }
                    }
                });
            } else {
                UIUtils.ShowToast(mContext.getString(R.string.no_items_selected), mContext);
            }
        } else {
            UIUtils.ShowToast(mContext.getString(R.string.permission_error), mContext);
        }
    }

    private void exportToDisk (info.guardianproject.iocipher.File fileIn, java.io.File fileout) throws IOException
    {
        info.guardianproject.iocipher.FileInputStream fis = new info.guardianproject.iocipher.FileInputStream(fileIn);
        java.io.FileOutputStream fos = new java.io.FileOutputStream(fileout);

        byte[] b = new byte[4096];
        int len;
        while ((len = fis.read(b))!=-1)
        {
            fos.write(b, 0, len);
        }

        fis.close();
        fos.flush();
        fos.close();

    }

    //covenant delete
    public void pasteFiles0x84(final File destination, String[] files, boolean alsodelete) {

        if (destination.canWrite()) {
            if (files.length > 0) {
                final ProgressDialog progressDialog = new ProgressDialog(mContext);
                String title = mContext.getString(R.string.wait);
                progressDialog.setTitle("encrypting");

                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(false);
                progressDialog.setMessage("");
                progressDialog.setProgress(0);
                progressDialog.show();
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        int i = 0;
                        float TOTAL_ITEMS = files.length;
                        try {
                            for (; i < files.length; i++) {
                                String filename = files[i].substring(files[i].lastIndexOf("/")+1);
                                mUIUpdateHandler.post(mHelper.progressUpdater(progressDialog, (int) ((i / TOTAL_ITEMS) * 100), "File: " + filename));
                                //copyTox084Drive(destination.getPath() + "/" + filename, files[i] );
                                //copyToDrive(destination.getPath() + "/" + filename, files[i] );
                                copytoDrive(new java.io.File(files[i]), new File(destination.getPath() + "/" + filename), mContext, alsodelete);
                            }

                            mUIUpdateHandler.post(mHelper.toggleProgressBarVisibility(progressDialog));
                            mUIUpdateHandler.post(mHelper.updateRunner());
                        } catch (Exception e) {
                            e.printStackTrace();
                            mUIUpdateHandler.post(mHelper.toggleProgressBarVisibility(progressDialog));
                            mUIUpdateHandler.post(mHelper.errorRunner(mContext.getString(R.string.pasting_error)));
                        }
                    }
                });
            } else {
                UIUtils.ShowToast(mContext.getString(R.string.no_items_selected), mContext);
            }
        } else {
            UIUtils.ShowToast(mContext.getString(R.string.permission_error), mContext);
        }
    }

    private void copyTox084Drive(String fileName, String localFile) {
        try {

            java.io.File filesource = new java.io.File(localFile);
            InputStream is = new FileInputStream(filesource);

            info.guardianproject.iocipher.FileOutputStream fos = new info.guardianproject.iocipher.FileOutputStream(fileName);

            ReadableByteChannel sourceFileChannel = Channels.newChannel(is);
            IOCipherFileChannel destinationFileChannel = fos.getChannel();
            destinationFileChannel.transferFrom(sourceFileChannel, 0, is.available());

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void copytoDrive(java.io.File  srcFile, File destFile, Context context, boolean alsodelete) throws IOException {
        if (destFile.exists() && destFile.isDirectory()) {
            throw new IOException("Destination '" + destFile + "' exists but is a directory");
        }

        final long ONE_KB = 1024;
        final long ONE_MB = ONE_KB * ONE_KB;
        final long FILE_COPY_BUFFER_SIZE = ONE_MB * 30;
        VirtualFileSystem.get().beginTransaction();
        FileInputStream fis = null;
        info.guardianproject.iocipher.FileOutputStream fos = null;
        FileChannel input = null;
        IOCipherFileChannel output = null;
        try {
            fis = new java.io.FileInputStream(srcFile);
            fos = new info.guardianproject.iocipher.FileOutputStream(destFile);
            input = fis.getChannel();
            output = fos.getChannel();
            long size = input.size();
            long pos = 0;
            long count = 0;
            while (pos < size) {
                if (size - pos > FILE_COPY_BUFFER_SIZE) count = FILE_COPY_BUFFER_SIZE;
                else count = size - pos;
                pos += output.transferFrom(input, pos, count);
            }
        } finally {
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(fos);
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(fis);
        }

        if (srcFile.length() != destFile.length()) {
            throw new IOException("Failed to copy full contents from '" +
                    srcFile + "' to '" + destFile + "'");
        }else{
            if(alsodelete) {
                //also delete activated
                srcFile.delete();
                if (srcFile.exists()) {
                    srcFile.getCanonicalFile().delete();
                    if (srcFile.exists()) {
                        context.deleteFile(srcFile.getName());
                    }
                }
            }
        }

        VirtualFileSystem.get().completeTransaction();
    }

    public void renameFile(final FileItem fileItem) {
        UIUtils.showEditTextDialog(mContext, fileItem.getFile().getName(), "Rename Directory/File", new IFuncPtr() {
            @Override
            public void execute(final String val) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (fileItem.getFile().isDirectory())
                                FileUtils.moveDirectory(fileItem.getFile(), new File(fileItem.getFile().getParentFile(), val.trim()));
                            else
                                FileUtils.moveFile(fileItem.getFile(), new File(fileItem.getFile().getParentFile(), val.trim()));
                            mUIUpdateHandler.post(mHelper.updateRunner());
                        } catch (Exception e) {
                            e.printStackTrace();
                            mUIUpdateHandler.post(mHelper.errorRunner(mContext.getString(R.string.rename_error)));
                        }
                    }
                });
            }
        });
    }

    public void getProperties(List<FileItem> selectedItems) {

        StringBuilder msg = new StringBuilder();
        try {
            if (selectedItems.size() == 1) {
                boolean isDirectory = false;
                File f = selectedItems.get(0).getFile().getCanonicalFile();
                isDirectory = (f.isDirectory());
                String type = isDirectory ? mContext.getString(R.string.directory) : mContext.getString(R.string.file);
                String size = FileUtils.byteCountToDisplaySize(isDirectory ? getDirSize(f) : FileUtils.sizeOf(f));
                String lastModified = new SimpleDateFormat(Constants.DATE_FORMAT).format(new Date());
                msg.append(mContext.getString(R.string.file_type, type));
                msg.append(mContext.getString(R.string.file_size, size));
                msg.append(mContext.getString(R.string.file_modified, lastModified));
                msg.append(mContext.getString(R.string.file_path, selectedItems.get(0).getFile().getAbsolutePath()));
            } else {
                long totalSize = 0;
                for (int i = 0; i < selectedItems.size(); i++) {
                    File f = selectedItems.get(0).getFile().getCanonicalFile();
                    boolean isDirectory = (f.isDirectory());
                    totalSize += isDirectory ? getDirSize(f) : FileUtils.sizeOf(f);
                }
                msg.append(mContext.getString(R.string.file_type_plain) + " " + mContext.getString(R.string.file_type_multiple));
                msg.append(mContext.getString(R.string.file_size, FileUtils.byteCountToDisplaySize(totalSize)));
            }
        } catch (Exception e) {
            e.printStackTrace();
            msg.append(mContext.getString(R.string.property_error));
        }
        UIUtils.ShowMsg(msg.toString(), mContext.getString(R.string.properties_title), mContext);
    }

    public void shareMultipleFiles(List<FileItem> filesToBeShared) {

        ArrayList<Uri> uris = new ArrayList<>();
        for (FileItem file : filesToBeShared) {
            uris.add(Uri.fromFile(file.getFile()));
        }
        final Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("*/*");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        PackageManager manager = mContext.getPackageManager();
        List<ResolveInfo> infos = manager.queryIntentActivities(intent, 0);
        if (infos.size() > 0) {
            mContext.startActivity(Intent.createChooser(intent, mContext.getString(R.string.share)));
        } else {
            UIUtils.ShowToast(mContext.getString(R.string.sharing_no_app), mContext);
        }
    }

    private boolean removeDir(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                removeDir(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    private long getDirSize(File root) {
        if (root == null) {
            return 0;
        }
        if (root.isFile()) {
            return root.length();
        }
        try {
            if (isFileASymLink(root)) {
                return 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

        long length = 0;
        File[] files = root.listFiles();
        if (files == null) {
            return 0;
        }
        for (File file : files) {
            length += getDirSize(file);
        }

        return length;
    }

    private static boolean isFileASymLink(File file) throws IOException {
        File canon;
        if (file.getParent() == null) {
            canon = file;
        } else {
            File canonDir = file.getParentFile().getCanonicalFile();
            canon = new File(canonDir, file.getName());
        }
        return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
    }
}
