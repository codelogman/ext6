package tar.eof.ext6.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.widget.Toast;

import tar.eof.ext6.NavigationHelper;

/**
 * Created by Aditya on 4/16/2017.
 */
public class UIUpdateHelper {

    Context mContext;
    NavigationHelper mNavigationeHelper;

    public UIUpdateHelper(NavigationHelper mNavigationeHelper, Context mContext) {
        this.mContext = mContext;
        this.mNavigationeHelper = mNavigationeHelper;
    }

    public Runnable updateRunner() {
        return new Runnable() {
            @Override
            public void run() {
                mNavigationeHelper.triggerFileChanged();
            }
        };
    }

    public Runnable errorRunner(final String msg) {
        return new Runnable() {
            @Override
            public void run() {
                    UIUtils.ShowToast(msg, mContext);
                    mNavigationeHelper.triggerFileChanged();
            }
        };
    }

    public Runnable progressUpdater(final ProgressDialog progressDialog, final int progress, final String msg) {
        return new Runnable() {
            @Override
            public void run() {
                if(progressDialog!=null) {
                    progressDialog.setProgress(progress);
                    progressDialog.setMessage(msg);
                }
            }
        };
    }

    public Runnable toggleProgressBarVisibility(final ProgressDialog progressDialog) {
        return new Runnable() {
            @Override
            public void run() {
                if(progressDialog!=null) {
                    progressDialog.dismiss();
                }
            }
        };
    }

    public Runnable toggleToast(String name) {
        return new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, "stored: " + name, Toast.LENGTH_LONG).show();
            }
        };
    }

}
