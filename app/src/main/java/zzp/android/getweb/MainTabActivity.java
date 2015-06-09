package zzp.android.getweb;

/**
 * Created by Xiang on 2015/5/18.
 */

import android.app.TabActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.TabWidget;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.Date;
import java.text.SimpleDateFormat;

import zzp.android.getweb.OwnClass.AnimationTabHost;
import zzp.android.getweb.OwnClass.DBManger;
import zzp.android.getweb.OwnClass.UserFunction;
import zzp.android.getweb.OwnClass.ZipUtils;

public class MainTabActivity extends TabActivity {
    private AnimationTabHost mTabHost;
    public static Handler mHandler;
    private RadioGroup main_radio;
    private RadioButton tab_btn_add, tab_btn_view, tab_btn_transfer;
    protected String extStorageAppBasePath;
    SearchView s_view;
    Boolean MenuBtnFlag = false;
    int tabNum;
    Toast toast;
    Context mContext;
    String ActionTitle[] = new String[]{"本地文库", "添加文章", "无线传输"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_tab);
        mContext = this;
        mHandler = setHandler();
        mTabHost = (AnimationTabHost) findViewById(android.R.id.tabhost);
        forceShowOverflowMenu();//强制显示menu菜单
        final TabWidget tabWidget = mTabHost.getTabWidget();
        tabWidget.setStripEnabled(false);// 圆角边线不启用
        // 视觉上,用单选按钮替代TabWidget
        main_radio = (RadioGroup) findViewById(R.id.main_radio);
        tab_btn_view = (RadioButton) findViewById(R.id.tab_btn_view);
        tab_btn_add = (RadioButton) findViewById(R.id.tab_btn_add);
        tab_btn_transfer = (RadioButton) findViewById(R.id.tab_btn_transfer);
        main_radio.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int id) {
                if (id == tab_btn_view.getId()) {
                    setTab(0, id, ActionTitle[0]);
                } else if (id == tab_btn_add.getId()) {
                    setTab(1, id, ActionTitle[1]);
                } else if (id == tab_btn_transfer.getId()) {
                    setTab(2, id, ActionTitle[2]);
                }
            }
        });

        SetSaveDir(this);
        // 打开静态数据库文件的输入流，并创建基础数据库
        String SaveDirPath = extStorageAppBasePath + File.separator + "Save";
        File SaveDir = new File(SaveDirPath);
        if (!SaveDir.exists()) SaveDir.mkdirs();//确保所有主要文件夹都存在
        String dbFilePath = extStorageAppBasePath + File.separator + "filelist.db";
        File dbFile = new File(dbFilePath);
        if (!dbFile.exists()) { // 如果不存在该数据库则创建
            try {
                InputStream is = this.getResources().openRawResource(R.raw.filelist);
                // 打开目标数据库文件的输出流
                FileOutputStream os = new FileOutputStream(dbFile);
                byte[] buffer = new byte[1024];
                int count = 0;
                // 将静态数据库文件拷贝到目的地
                while ((count = is.read(buffer)) > 0) {
                    os.write(buffer, 0, count);
                }
                os.flush();
                is.close();
                os.close();
                // 添加所有文件信息到数据库
                int id = 1;
                if (SaveDir.exists()) {
                    File[] files = SaveDir.listFiles();
                    //数据写入
                    DBManger dbM = new DBManger();
                    SQLiteDatabase db = dbM.getDB(mContext);
                    for (File f : files) {
                        if (f.isFile() && f.getName().endsWith("zip")) {
                            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            String Name = f.getName().substring(0, f.getName().length() - 4);
                            Date tmpDate = new Date(f.lastModified());
                            ContentValues cv = new ContentValues();
                            cv.put("id", id);
                            cv.put("name", Name);
                            cv.put("c_time", df.format(tmpDate));
                            cv.put("g_id", "1");
                            /* 插入数据 */
                            db.insert("list", null, cv);
                            id++;
                        }
                    }
                    db.close();
                }
            } catch (IOException e) {
                Log.i("Error", e.getMessage());
            }
        }
        //添加n个tab选项卡，定义他们的tab名，指示名，目标屏对应的类
        mTabHost.addTab(mTabHost.newTabSpec("TAG1").setIndicator("0").setContent(new Intent(this, SaveViewActivity.class)));
        mTabHost.addTab(mTabHost.newTabSpec("TAG2").setIndicator("1").setContent(new Intent(this, AddActivity.class)));
        mTabHost.addTab(mTabHost.newTabSpec("TAG3").setIndicator("3").setContent(new Intent(this, WiFiDirectActivity.class)));
        mTabHost.setOpenAnimation(true);
        //获取数据
        Intent intent = getIntent();
        if (intent.getStringExtra("url") != null) {
            setTab(1, tab_btn_add.getId(), ActionTitle[1]);
            Message.obtain(AddActivity.mHandler, 3, intent.getStringExtra("url")).sendToTarget();//对AddActivity传入url
        } else {
            setTab(0, tab_btn_view.getId(), ActionTitle[0]);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);//must store the new intent unless getIntent() will return the old one
        if (intent.getStringExtra("url") != null) {
            setTab(1, tab_btn_add.getId(), ActionTitle[1]);
            Message.obtain(AddActivity.mHandler, 3, intent.getStringExtra("url")).sendToTarget();//对AddActivity传入url
        }
    }

//    @Override
//    protected void onDestroy() {
//        Message.obtain(WiFiDirectActivity.mHandler, 2).sendToTarget();
//        Log.i("Destory","OnDestory");
//        super.onDestroy();
//
//    }

    private void setTab(int tabnum, int id, String title) {
        tabNum = tabnum;
        mTabHost.setCurrentTab(tabnum);
        invalidateOptionsMenu();
        main_radio.check(id);
        getActionBar().setTitle(title);
    }

    private Handler setHandler() {
        Handler tmpHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        //动态修改actionbar title
                        getActionBar().setTitle((String) msg.obj);
                        ActionTitle[msg.arg1] = (String) msg.obj;
                        break;
                    case 2:
                        //saveview进行多选后，隐藏下栏菜单
                        if ((Boolean) msg.obj) {
                            main_radio.setVisibility(View.VISIBLE);
                        } else {
                            main_radio.setVisibility(View.GONE);
                        }
                        break;
                    case 3:
                        //由AddActivity通知MainTab更新Menu
                        MenuBtnFlag = (boolean) msg.obj;
                        invalidateOptionsMenu();
                        break;
                }
            }
        };
        return tmpHandler;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return this.getCurrentActivity().onTouchEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_tab_menu, menu);
        MenuItem mi_group_option = menu.findItem(R.id.menu_group_option);
        MenuItem mi_search = menu.findItem(R.id.menu_search);
        MenuItem menu_add_save = menu.findItem(R.id.menu_add_save);
        MenuItem menu_add_refresh = menu.findItem(R.id.menu_add_refresh);
        MenuItem menu_p2p_search = menu.findItem(R.id.menu_p2p_search);
        MenuItem menu_p2p_help = menu.findItem(R.id.menu_p2p_help);
        s_view = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        for (int i = 0; i < menu.size(); i++) {
            if (menu.getItem(i).getItemId() != R.id.menu_app_exit)
                menu.getItem(i).setVisible(false);//隐藏所有item
        }
        switch (tabNum) {
            case 0:
                mi_search.setVisible(true);
                mi_group_option.setVisible(true);
                menu_p2p_help.setVisible(true);
                break;
            case 1:
                menu_add_save.setVisible(true);
                menu_add_refresh.setVisible(true);
                break;
            case 2:
                menu_p2p_search.setVisible(true);
                menu_p2p_help.setVisible(true);
                break;
        }
        if (MenuBtnFlag) {
            menu_add_refresh.setEnabled(true);
            menu_add_save.setEnabled(true);
        } else {
            menu_add_refresh.setEnabled(false);
            menu_add_save.setEnabled(false);
        }
        s_view.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                Message msg1 = Message.obtain();
                msg1.what = 1;
                SaveViewActivity.mHandler.sendMessage(msg1);
                return false;
            }
        });
        s_view.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                s_view.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //发送指令要求SaveView更新ListView
                Message.obtain(SaveViewActivity.mHandler, 2, newText).sendToTarget();
                return false;
            }
        });
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        /*
         * 将actionBar的HomeButtonEnabled设为ture， 将会执行此case
         */
            case R.id.menu_add_save:
                //通知AddActivity保存网页
                Message.obtain(AddActivity.mHandler, 6).sendToTarget();
                break;
            case R.id.menu_add_refresh:
                //通知AddActivity刷新网页
                Message.obtain(AddActivity.mHandler, 7).sendToTarget();
                break;
            case R.id.menu_group_option:
                Intent intent = new Intent();
                intent.setClass(MainTabActivity.this, MenuGroupOptionActivity.class);
                startActivity(intent);
                break;
            case R.id.menu_app_exit:
                finish();
                System.exit(0);
                break;
            case R.id.menu_p2p_search:
                Message.obtain(WiFiDirectActivity.mHandler, 1).sendToTarget();
                break;
            case R.id.menu_p2p_help:
                Intent intent2 = new Intent();
                intent2.setClass(MainTabActivity.this, WifiDirectHelpActivity.class);
                startActivity(intent2);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void SetSaveDir(Context context) {
        DBManger dbM = new DBManger();
        extStorageAppBasePath = dbM.getExtSDPath(context);
    }

    //如果设备有物理菜单按键，需要将其屏蔽才能显示OverflowMenu
    private void forceShowOverflowMenu() {
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //显示提示
    private void showTextToast(String msg) {
        if (toast == null) {
            toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
        } else {
            toast.setText(msg);
        }
        toast.show();
    }

    //压缩文件用，特殊
    private void zipDir() {
        String lis2tPath = extStorageAppBasePath + File.separator + "zipdir";
        File file2 = new File(lis2tPath);
        File[] files = file2.listFiles();
        UserFunction uf = new UserFunction();
        for (File f : files) {
            if (f.isDirectory() && !f.getName().equals("zip")) {
                String zipSave = lis2tPath + File.separator + "zip" + File.separator + uf.sqliteEscape(f.getName()) + ".zip";
                String savePath = f.getAbsolutePath();
                Log.i("xxx", zipSave);
                Log.i("xxx", savePath);
                //zip压缩
                try {
                    ZipUtils.zip(savePath, zipSave);
                    File dirFile = new File(savePath);
                    uf.delete(dirFile);//清除此次下载内容
                } catch (IOException e) {
                    showTextToast("压缩失败");
                    Log.i("压缩失败", f.getName() + e.getMessage());
                }

            }
        }
    }
}
