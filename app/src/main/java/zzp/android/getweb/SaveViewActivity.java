package zzp.android.getweb;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import zzp.android.getweb.OwnClass.DBManger;
import zzp.android.getweb.OwnClass.LVAdapter;
import zzp.android.getweb.OwnClass.ListData;
import zzp.android.getweb.OwnClass.PublicVariable;
import zzp.android.getweb.OwnClass.UserFunction;
import zzp.android.getweb.OwnClass.ZipUtils;
import zzp.android.getweb.WiFiDirectClass.DeviceDetailFragment;

@SuppressLint({"InflateParams", "SimpleDateFormat"})
public class SaveViewActivity<SpinnerActivity> extends ListActivity {
    public static Handler mHandler;
    static final int GB_SP_DIFF = 160;
    // 存放国标一级汉字不同读音的起始区位码
    static final int[] secPosValueList = {1601, 1637, 1833, 2078, 2274, 2302, 2433, 2594, 2787, 3106, 3212, 3472, 3635, 3722, 3730, 3858, 4027, 4086, 4390, 4558, 4684, 4925, 5249, 5600};
    // 存放国标一级汉字不同读音的起始区位码对应读音
    static final char[] firstLetter = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'w', 'x', 'y', 'z'};
    //储存文件信息
    ArrayList<ListData> mList = new ArrayList<ListData>();
    ListView LV, sv_LV;
    LinearLayout group, sort, mselect, ms_all, ms_inv, ms_cancel, btn1, btn2, btnAction;
    RelativeLayout share, setgroup, delete, sendfiles;
    PopupWindow popupWindow;
    Toast toast = null;
    LVAdapter mAdapter;
    Context mContext;
    UserFunction uf = new UserFunction();
    int sortFlag = 1, GroupSel = 0, sort_sel = 0;
    String currPath = "";
    String[] mItems = {"时间（后->先）", "时间（先->后）", "名称（A->Z）", "名称（Z->A）"}, mLongItems = {"分享", "重命名", "设置分组", "删除"};
    String[][] GroupList;
    Boolean cbShow = false, SFFlag = false;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        //初始化文件路径
        SetSaveDir(mContext);
        setContentView(R.layout.save_view);
        LV = (ListView) findViewById(android.R.id.list);

        sort = (LinearLayout) findViewById(R.id.sort);
        group = (LinearLayout) findViewById(R.id.group);
        mselect = (LinearLayout) findViewById(R.id.mselect);
        ms_all = (LinearLayout) findViewById(R.id.ms_all);
        ms_inv = (LinearLayout) findViewById(R.id.ms_inv);

        ms_cancel = (LinearLayout) findViewById(R.id.ms_cancel);
        btn1 = (LinearLayout) findViewById(R.id.btn1);
        btn2 = (LinearLayout) findViewById(R.id.btn2);

        btnAction = (LinearLayout) findViewById(R.id.btnAction);
        share = (RelativeLayout) findViewById(R.id.share);
        setgroup = (RelativeLayout) findViewById(R.id.setgroup);
        delete = (RelativeLayout) findViewById(R.id.delete);
        sendfiles = (RelativeLayout) findViewById(R.id.sendfiles);

        sort.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(mContext)
                        .setTitle("选择排序方式")
                        .setSingleChoiceItems(mItems, sort_sel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        sort_sel = which;
                                        sortFlag = which + 1;
                                        refreshLV();
                                        dialog.dismiss();
                                    }
                                }
                        )
                        .setNegativeButton("取消", null)
                        .show();
            }
        });

        group.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showPopupWindow(v);
            }
        });
        mselect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //多选功能
                btn1.setVisibility(View.GONE);
                btn2.setVisibility(View.VISIBLE);
                btnAction.setVisibility(View.VISIBLE);
                //通知MainTab显示功能键
                Message.obtain(MainTabActivity.mHandler, 2, false).sendToTarget();
                showFileDir(currPath, sortFlag, GroupSel, true);
            }
        });

        ms_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < mList.size(); i++) {
                    mAdapter.map.put(i, true);
                }
                mAdapter.notifyDataSetChanged();
                showTextToast("已选中" + mList.size() + "项");
            }
        });

        ms_inv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int checksum = 0;
                for (int i = 0; i < mList.size(); i++) {
                    if (mAdapter.map.get(i)) {
                        mAdapter.map.put(i, false);
                    } else {
                        mAdapter.map.put(i, true);
                        checksum++;
                    }
                }
                mAdapter.notifyDataSetChanged();
                showTextToast("已选中" + checksum + "项");
            }
        });

        ms_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (toast != null) toast.cancel();
                btn1.setVisibility(View.VISIBLE);
                btn2.setVisibility(View.GONE);
                btnAction.setVisibility(View.GONE);
                //通知MainTab隐藏功能键
                Message.obtain(MainTabActivity.mHandler, 2, true).sendToTarget();
                showFileDir(currPath, sortFlag, GroupSel, false);
            }
        });
        //多选后显示的菜单
        share.setOnClickListener(btnActionBar);
        setgroup.setOnClickListener(btnActionBar);
        delete.setOnClickListener(btnActionBar);
        sendfiles.setOnClickListener(btnActionBar);

        LV.setOnItemClickListener(LV_ItemClickListener);
        LV.setOnItemLongClickListener(LV_LongClickListener);

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        //调用刷新LV
                        refreshLV();
                        break;
                    case 2:
                        //ActionBar搜索功能回传
                        List<ListData> obj = searchItem((String) msg.obj);
                        updateLayout(obj);
                        break;
                    case 3:
                        //用于GroupOption点击后显示对应分组
                        GroupSel = (int) msg.obj;
                        refreshLV();
                        break;
                    case 4:
                        //传入连接断开或接入的标记
                        SFFlag = (Boolean) msg.obj;
                        if (SFFlag) sendfiles.setVisibility(View.VISIBLE);
                        else sendfiles.setVisibility(View.GONE);
                }
            }
        };
        //默认使用时间（后->先），初始化数据
        refreshLV();
    }

    private void refreshLV() {
        getGroupList();//先获取分组信息
        //showFileDir(当前Save路径，排序标记，分组ID，搜索关键词，搜索标记,复选框标记)
        showFileDir(currPath, sortFlag, GroupSel, cbShow);
    }

    private void getGroupList() {
        SQLiteDatabase db = GetDB();
        Cursor cur = db.rawQuery("select * from TypeCount order by g_id", null);
        //初始化菜单数组
        GroupList = new String[cur.getCount()][3];
        int all_sum = 0;
        for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
            int i = cur.getPosition();
            GroupList[i][0] = cur.getString(0);
            GroupList[i][1] = cur.getString(1);
            GroupList[i][2] = cur.getString(2);
            all_sum += Integer.parseInt(GroupList[i][2]);
            if (cur.getPosition() == cur.getCount() - 1) {
                GroupList[0][2] = String.valueOf(all_sum);
            }
        }
        cur.close();
        db.close();
    }

    private void showFileDir(String path, int flag, int g_sel, boolean CKFLag) {
        cbShow = CKFLag;
        mList.clear();
        String g_id = GroupList[g_sel][0];
        String FilePath, Name, Link, id, ABTitle;
        if (g_sel != 0) {
            g_id = " where g_id=" + g_id;
        } else {
            g_id = "";
        }
        //获取ListView数据
        SQLiteDatabase db = GetDB();
        Cursor cur = db.rawQuery("select * from list" + g_id, null);
        for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date tmpDate = new java.util.Date();
            try {
                tmpDate = sdf.parse(cur.getString(2));
            } catch (Exception e) {
                Log.e("showFileDir", e.getMessage());
            }
            Name = cur.getString(1);
            FilePath = path + File.separator + Name + ".zip";
            Link = cur.getString(4);
            id = cur.getString(0);
            ListData tdp = new ListData(tmpDate, Name, FilePath, Link, id, false);
            mList.add(tdp);
        }
        //数据测试用
//        String abc[] = new String[]{"a", "b", "d", "e", "f", "g", "h", "j", "k", "l", "i", "q", "w", "r", "t", "y", "u", "o", "p"};
//        int j = 1;
//        for (int i = 0; i < 2000; i++) {
//            if (abc.length % j == 0) {
//                j = 1;
//            }
//            ListData tdp = new ListData(new Date(), abc[j] + String.valueOf(i), "", "", "", false);
//            mList.add(tdp);
//            j++;
//        }
        //结束

        ABTitle = GroupList[GroupSel][1] + "(" + GroupList[GroupSel][2] + ")";
        cur.close();
        db.close();
        //listview数据的排序，根据传入的flag判断哪种排序
        mListSort((int) Math.ceil((double) flag / 2), (int) Math.pow(-1, flag));
        mAdapter = new LVAdapter(mContext, mList, cbShow);
        LV.setAdapter(mAdapter);
        //写入ActionBar Title ,Handler,arg1,arg2,obj
        Message.obtain(MainTabActivity.mHandler, 1, 0, -1, ABTitle).sendToTarget();
        //将mList写入PublicVariable
        PublicVariable.getInstance().set_DBList(mList);
    }

    private void showPopupWindow(View view) {
        // 一个自定义的布局，作为显示的内容
        View contentView = LayoutInflater.from(mContext).inflate(R.layout.save_view_group, null);
        TextView empty = (TextView) findViewById(android.R.id.empty);
        //定义popupw中的listview（菜单）
        sv_LV = (ListView) contentView.findViewById(android.R.id.list);
        sv_LV.setAdapter(new ArrayAdapter<String>(mContext, R.layout.save_view_group_item, getData()));
        sv_LV.setOnItemClickListener(group_ItemClickListener);
        sv_LV.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        sv_LV.setItemChecked(GroupSel, true);
        int ppW_height = LV.getHeight(), ppW_width = LV.getWidth();
        //获取手机宽度的一半和LV高度的3/4
        if (ppW_width == 0) {
            ppW_height = empty.getHeight();
            ppW_width = empty.getWidth();
        }
        if (uf.dip2px(mContext, 40) * sv_LV.getCount() > LV.getHeight() * 3 / 4) {
            ppW_height = ppW_height * 3 / 4;
        } else {
            ppW_height = LayoutParams.WRAP_CONTENT;
        }
        ppW_width = ppW_width / 2;
        popupWindow = new PopupWindow(contentView, ppW_width, ppW_height, true);
        popupWindow.setTouchable(true);
        //设置点击窗口外边窗口消失
        popupWindow.setOutsideTouchable(true);
        // 设置此参数获得焦点，否则无法点击
        popupWindow.setFocusable(true);
        // 如果不设置PopupWindow的背景，无论是点击外部区域还是Back键都无法dismiss弹框
        // 我觉得这里是API的一个bug
        popupWindow.setBackgroundDrawable(new ColorDrawable(0));
        // 设置好参数之后再show
        popupWindow.showAsDropDown(view);
    }

    private View.OnClickListener btnActionBar = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            List<ListData> mAList = new ArrayList<ListData>();
            for (int i = 0; i < mList.size(); i++) {
                if (mAdapter.map.get(i)) {
                    ListData ld = mList.get(i);
                    mAList.add(ld);
                }
            }
            int mAcount = mAList.size();
            if (mAcount < 1) {
                showTextToast("未选择文件");
                return;
            }
            uf.setContextNListDate(mContext, mAList);
            switch (v.getId()) {
                case R.id.share:
                    uf.shareFile();
                    break;
                case R.id.setgroup:
                    uf.reGroup();
                    break;
                case R.id.delete:
                    uf.delFile();
                    break;
                case R.id.sendfiles:
                    uf.sendSelects();
                    break;
            }
        }
    };

    private AdapterView.OnItemClickListener group_ItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
            TextView tv = (TextView) v;
            GroupSel = position;
            refreshLV();
            //向MainTabActivity写入actionbar信息 ,Handler,arg1,arg2,obj
            Message.obtain(MainTabActivity.mHandler, 1, 0, -1, tv.getText()).sendToTarget();
            popupWindow.dismiss();
        }
    };

    private AdapterView.OnItemLongClickListener LV_LongClickListener = new AdapterView.OnItemLongClickListener() {
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            final int LC_posistion = position, select_offset;
            if (SFFlag) {
                select_offset = 0;
                mLongItems = new String[]{"发送", "分享", "重命名", "设置分组", "删除"};
            } else {
                mLongItems = new String[]{"分享", "重命名", "设置分组", "删除"};
                select_offset = 1;
            }
            new AlertDialog.Builder(mContext)
                    .setTitle("选择操作")
                    .setItems(mLongItems, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    ListData ld = mList.get(LC_posistion);
                                    List<ListData> mAList = new ArrayList<ListData>();
                                    mAList.add(ld);
                                    uf.setContextNListDate(mContext, mAList);
                                    switch (which + select_offset) {
                                        case 0:
                                            uf.sendSelects();
                                            break;
                                        case 1:
                                            uf.shareFile();
                                            break;
                                        case 2:
                                            uf.reName();
                                            break;
                                        case 3:
                                            uf.reGroup();
                                            break;
                                        case 4:
                                            uf.delFile();
                                            break;
                                    }
                                    dialog.dismiss();
                                }
                            }
                    )
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        }
    };

    private List<String> getData() {
        List<String> data = new ArrayList<String>();
        getGroupList();//刷新GroupList
        for (int i = 0; i < GroupList.length; i++) {
            String str_tmp = GroupList[i][1] + "(" + GroupList[i][2] + ")";
            data.add(str_tmp);
        }
        return data;
    }

    //搜索mList以达到搜索内容
    public List<ListData> searchItem(String name) {
        List<ListData> mSearchList = new ArrayList<ListData>();
        for (int i = 0; i < mList.size(); i++) {
            int index = mList.get(i).getName().indexOf(name);
            // 存在匹配的数据
            if (index != -1) {
                mSearchList.add(mList.get(i));
            }
        }
        if (mSearchList.size() == 0) {
            TextView empty = (TextView) findViewById(android.R.id.empty);
            empty.setText("无搜索结果");
        }
        return mSearchList;
    }

    public void updateLayout(List<ListData> mSearchList) {
        LV.setAdapter(new LVAdapter(mContext, mSearchList, cbShow));
    }

    //TODO 排序代码需要优化，超过2000条会卡顿
    private void mListSort(int flag, final int sflag) {
        switch (flag) {
            case 1:
                Collections.sort(mList, new Comparator<ListData>() {
                    @Override
                    public int compare(ListData tdp1, ListData tdp2) {
                        java.util.Date date1 = tdp1.getDate();
                        java.util.Date date2 = tdp2.getDate();
                        // 对日期字段进行升序，如果欲降序可采用after方法
                        if (date1.after(date2)) {
                            return sflag;
                        }
                        return -1 * sflag;
                    }
                });
                break;
            case 2:
                Collections.sort(mList, new Comparator<ListData>() {
                    @Override
                    public int compare(ListData tdp1, ListData tdp2) {
                        int minLen = 0;
                        char chr1[] = tdp1.getName().toCharArray();
                        char chr2[] = tdp2.getName().toCharArray();
                        // 对日期字段进行升序，如果欲降序可采用after方法
                        if (chr1.length <= chr2.length)
                            minLen = chr1.length;
                        else
                            minLen = chr2.length;
                        //字符排序，顺序对比字符
                        for (int i = 0; i < minLen; i++) {
                            if (getFirstLetter(chr1[i]).compareTo(getFirstLetter(chr2[i])) < 0) {
                                i = minLen;
                                return sflag;
                            }
                            if (getFirstLetter(chr1[i]).compareTo(getFirstLetter(chr2[i])) > 0) {
                                i = minLen;
                                return -1 * sflag;
                            }
                        }
                        return 0;
                    }
                });
                break;
        }
    }


    //设置savelistview的点击事件
    private AdapterView.OnItemClickListener LV_ItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (cbShow) {
                LVAdapter.ViewHolder holder = (LVAdapter.ViewHolder) view.getTag();
                // 改变CheckBox的状态
                holder.cb.toggle();
                // 将CheckBox的选中状况记录下来
                mAdapter.map.put(position, holder.cb.isChecked());
            } else {
                final int pis = position;
                final Context context = mContext;
                String path = mList.get(pis).getPath();
                final String file_id = mList.get(position).getID();
                final File file = new File(path);
                Log.i("thrd", String.valueOf(file.exists()));
                // 文件存在并可读
                if (file.exists() && file.canRead()) {
                    Thread thrd = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String viewPath = currPath + File.separator + "view";
                            File viewDir = new File(viewPath);
                            uf.delete(viewDir);
                            try {
                                ZipUtils.unzip(file.getAbsolutePath(), viewPath);
                            } catch (Exception e) {
                                showTextToast("尝试打开文件失败\n" + e.toString());
                                return;
                            }
                            File index = new File(currPath + File.separator + "view/index.html");
                            Intent intent = new Intent();
                            intent.setClass(context, WebViewActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putSerializable("ListData", mList.get(pis));
                            intent.putExtras(bundle);
                            intent.putExtra("SFFlag", SFFlag);
                            intent.setDataAndType(Uri.fromFile(index), "html");
                            startActivity(intent);
                            toast.cancel();
                        }
                    });
                    showTextToast("正在解压缩文件，请稍后");
                    thrd.start();
                }
                // 没有权限
                else {
                    new AlertDialog.Builder(context).
                            setTitle("注意").
                            setMessage("没有要打开的文件，请问是否删除数据库记录？").
                            setPositiveButton("删除", new OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    SQLiteDatabase db = GetDB();
                                    db.execSQL("delete from list where id=" + file_id);
                                    db.close();
                                    refreshLV();
                                    showTextToast("成功删除数据库记录");
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
            }
        }
    };


    public static Character getFirstLetter(char ch) {
        byte[] uniCode = null;
        //非汉字和出错则直接返回字符
        try {
            uniCode = String.valueOf(ch).getBytes("GBK");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return ch;
        }
        if ((uniCode[0] & 0xff) < 128 && (uniCode[0] & 0xff) > 0) { // 非汉字
            return ch;
        } else {
            return convert(uniCode);
        }
    }

    //汉字转换出拼音第一个字母
    static char convert(byte[] bytes) {
        char result = '-';
        int secPosValue = 0;
        int i;
        for (i = 0; i < bytes.length; i++) {
            bytes[i] -= GB_SP_DIFF;
        }
        secPosValue = bytes[0] * 100 + bytes[1];
        for (i = 0; i < 23; i++) {
            if (secPosValue >= secPosValueList[i] && secPosValue < secPosValueList[i + 1]) {
                result = firstLetter[i];
                break;
            }
        }
        return result;
    }

    private SQLiteDatabase GetDB() {
        DBManger dbM = new DBManger();
        SQLiteDatabase db = dbM.getDB(mContext);
        return db;
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

    //设定初始化目录
    private void SetSaveDir(Context context) {
        DBManger dbM = new DBManger();
        currPath = dbM.getSavePath(context);
    }


    //设置按返回键时显示再按一次退出
    private long exitTime;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i("keycode", String.valueOf(keyCode));
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 2000) // System.currentTimeMillis()无论何时调用，肯定大于2000
            {
                showTextToast("再按一次退出程序");
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}