package tar.eof.ext6;

import android.app.Application;
import android.content.Context;

public class AppContext extends Application {
    private static Context context;


    public void onCreate() {
        super.onCreate();
        AppContext.context = this;

    }



    public static Context getAppContext() {
        return AppContext.context;
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
