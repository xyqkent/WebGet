package zzp.android.getweb.OwnClass;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import java.io.File;

import zzp.android.getweb.R;

/**
 * Created by yingqiang on 2015/5/20.
 */
public class DBManger {
    public SQLiteDatabase getDB(Context context) {
        String extStorageAppBasePath = getExtSDPath(context);
        String dbDir = extStorageAppBasePath + File.separator + "filelist.db";
        SQLiteDatabase db = context.openOrCreateDatabase(dbDir, context.MODE_PRIVATE, null);
        return db;
    }

    public String getExtSDPath(Context context) {
        //返回SaveThisPage路径
        File extStorageAppBasePath = null;
        boolean isSdcard = false;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            isSdcard = true;
            File externalStorageDir = Environment.getExternalStorageDirectory();
            if (externalStorageDir != null) {
                extStorageAppBasePath = new File(externalStorageDir.getAbsolutePath() + File.separator + context.getResources().getString(R.string.app_name));
            }
        }
        if (!isSdcard) {
            // 新建缓存路径失败
            extStorageAppBasePath = context.getCacheDir();
        }
        return extStorageAppBasePath.getAbsolutePath();
    }

    public String getSavePath(Context context) {
        String SavePath = getExtSDPath(context) + File.separator + "Save";
        return SavePath;
    }
}
