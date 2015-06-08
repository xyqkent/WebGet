package zzp.android.getweb.OwnClass;

import android.app.Application;

import java.util.ArrayList;

/**
 * Created by Xiang on 2015/6/4.
 */
public class PublicVariable {

    private static PublicVariable sInstance = null;

    private Application application = null;
    private ArrayList<ListData> DBList;

    public static PublicVariable getInstance() {
        if (sInstance == null) {
            sInstance = new PublicVariable();
        }
        return sInstance;
    }

    private PublicVariable() {
    }

    public void set_DBList(ArrayList<ListData> lists) {
        this.DBList = lists;
    }

    public ArrayList<ListData> get_DBList() {
        return this.DBList;
    }

}

