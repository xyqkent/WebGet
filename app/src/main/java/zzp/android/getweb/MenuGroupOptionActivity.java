package zzp.android.getweb;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import zzp.android.getweb.OwnClass.DBManger;
import zzp.android.getweb.OwnClass.UserFunction;

/**
 * Created by Xiang on 2015/5/22.
 */
public class MenuGroupOptionActivity extends ListActivity {
    ListView LV;
    RelativeLayout group_edit, group_new, group_cancel;
    String[][] GroupList;
    Context mContext;
    Toast toast;
    UserFunction uf=new UserFunction();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_group_option);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        mContext = this;
        // 关联Layout中的ListView
        LV = (ListView) findViewById(android.R.id.list);
        group_edit = (RelativeLayout) findViewById(R.id.group_edit);
        group_new = (RelativeLayout) findViewById(R.id.group_new);
        group_cancel = (RelativeLayout) findViewById(R.id.group_cancel);
        group_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                group_new.setVisibility(View.GONE);
                group_edit.setVisibility(View.GONE);
                group_cancel.setVisibility(View.VISIBLE);
                ShowGroupList(true);
            }
        });
        group_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                group_new.setVisibility(View.VISIBLE);
                group_edit.setVisibility(View.VISIBLE);
                group_cancel.setVisibility(View.GONE);
                ShowGroupList(false);
            }
        });
        group_new.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText et = new EditText(mContext);
                final String g_id = String.valueOf(Integer.parseInt(GroupList[GroupList.length - 1][0])+1);
                Log.i("ID",g_id);
                new AlertDialog.Builder(mContext)
                        .setTitle("请输入分组名称")
                        .setView(et)
                        .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                uf.HideKeyboard(mContext,et);
                                DBManger dbM = new DBManger();
                                SQLiteDatabase db = dbM.getDB(mContext);
                                db.execSQL("insert into type values ("+ g_id +",'"+ uf.sqliteEscape(et.getText().toString()) +"')");
                                showTextToast("新建分组成功");
                                ShowGroupList(false);
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
        ShowGroupList(false);
    }

    private void ShowGroupList(boolean delFlag) {
        getGroupList();//刷新GroupList
        // 生成动态数组，加入数据
        ArrayList<HashMap<String, Object>> remoteWindowItem = new ArrayList<HashMap<String, Object>>();
        for (int i = 0; i < GroupList.length; i++) {
            String str_tmp = GroupList[i][1] + "(" + GroupList[i][2] + ")";
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("ItemIcon", GroupList[i][0]); //图像资源的ID
            if (!delFlag) {
                map.put("ItemName", str_tmp);
            } else {
                map.put("ItemName", GroupList[i][1]);
            }
            if (GroupList[i][0].equals("0") || GroupList[i][0].equals("1")) {
                map.put("ItemDel", false);
            } else {
                map.put("ItemDel", delFlag);
            }
            remoteWindowItem.add(map);
        }
        // 生成适配器的Item和动态数组对应的元素
        lvButtonAdapter listItemAdapter = new lvButtonAdapter(
                this,
                remoteWindowItem, //数据源
                R.layout.menu_group_option_item, //ListItem的XML实现
                //动态数组与ImageItem对应的子项
                new String[]{"ItemIcon", "ItemName", "ItemDel"},
                //ImageItem的XML文件里面的一个ImageView,两个TextView ID
                new int[]{R.id.ItemIcon, R.id.ItemName, R.id.ItemDel}
        );

        if (!delFlag) {
            LV.setOnItemClickListener(sv_show_ClickListener);
        } else {
            LV.setOnItemClickListener(sv_edit_ClickListener);
        }
        LV.setAdapter(listItemAdapter);
    }

    private void getGroupList() {
        DBManger dbM = new DBManger();
        SQLiteDatabase db = dbM.getDB(this);
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

    //非编辑状态下的item侦听器
    private AdapterView.OnItemClickListener sv_show_ClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
            Message msg1 = Message.obtain();
            msg1.what = 3;
            msg1.obj = position;
            SaveViewActivity.mHandler.sendMessage(msg1);
            finish();
        }
    };
    //编辑状态下的item侦听器
    private AdapterView.OnItemClickListener sv_edit_ClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
            if (position == 0 || position == 1) {
                showTextToast("无法删除和修改系统初始分组");
            } else {
                final EditText et = new EditText(mContext);
                final String g_id = GroupList[position][0];
                et.setText(GroupList[position][1]);
                et.setSelection(GroupList[position][1].length());
                new AlertDialog.Builder(mContext)
                        .setTitle("请输入分组名称")
                        .setView(et)
                        .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                uf.HideKeyboard(mContext,et);
                                DBManger dbM = new DBManger();
                                SQLiteDatabase db = dbM.getDB(mContext);
                                db.execSQL("update type set g_name='" + uf.sqliteEscape(et.getText().toString()) + "' where g_id=" + g_id);
                                showTextToast("重命名成功");
                                ShowGroupList(true);
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        }
    };

    public class lvButtonAdapter extends BaseAdapter {
        private class buttonViewHolder {
            ImageView appIcon;
            TextView appName;
            ImageButton buttonDel;
        }

        private ArrayList<HashMap<String, Object>> mAppList;
        private LayoutInflater mInflater;
        private Context mContext;
        private String[] keyString;
        private int[] valueViewID;
        private buttonViewHolder holder;

        public lvButtonAdapter(Context c, ArrayList<HashMap<String, Object>> appList, int resource, String[] from, int[] to) {
            mAppList = appList;
            mContext = c;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            keyString = new String[from.length];
            valueViewID = new int[to.length];
            System.arraycopy(from, 0, keyString, 0, from.length);
            System.arraycopy(to, 0, valueViewID, 0, to.length);
        }

        @Override
        public int getCount() {
            return mAppList.size();
        }

        @Override
        public Object getItem(int position) {
            return mAppList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void removeItem(int position) {
            mAppList.remove(position);
            this.notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView != null) {
                holder = (buttonViewHolder) convertView.getTag();
            } else {
                convertView = mInflater.inflate(R.layout.menu_group_option_item, null);
                holder = new buttonViewHolder();
                holder.appIcon = (ImageView) convertView.findViewById(valueViewID[0]);
                holder.appName = (TextView) convertView.findViewById(valueViewID[1]);
                holder.buttonDel = (ImageButton) convertView.findViewById(valueViewID[2]);
                convertView.setTag(holder);
            }

            HashMap<String, Object> appInfo = mAppList.get(position);
            if (appInfo != null) {
                String aname = (String) appInfo.get(keyString[1]);
                holder.appName.setText(aname);
                holder.buttonDel.setOnClickListener(new lvButtonListener(position));
            }
            if (!(Boolean) appInfo.get(keyString[2])) {
                holder.buttonDel.setVisibility(View.GONE);
            }
            if (position == 0 || position == 1) {
                holder.appName.setTextColor(Color.argb(255, 170, 170, 170));
            }
            return convertView;
        }

        class lvButtonListener implements View.OnClickListener {
            private int position;

            lvButtonListener(int pos) {
                position = pos;
            }

            @Override
            public void onClick(View v) {
                int vid = v.getId();
                //注：分组中文件数量不为0则无法删除！
                if (vid == holder.buttonDel.getId()) {
                    if (GroupList[position][2].equals("0")) {
                        final String g_id = GroupList[position][0];
                        new AlertDialog.Builder(mContext)
                                .setTitle("警告！")
                                .setMessage("请问是否确定删除此分组？")
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        DBManger dbM = new DBManger();
                                        SQLiteDatabase db = dbM.getDB(mContext);
                                        db.execSQL("delete from type where g_id=" + g_id);
                                        showTextToast("删除分组成功");
                                        ShowGroupList(true);
                                    }
                                })
                                .setNegativeButton("取消", null)
                                .show();
                    } else {
                        new AlertDialog.Builder(mContext)
                                .setTitle("警告！")
                                .setMessage("无法删除文件数量不为0的分组！\n请先转移分组内文件！")
                                .setPositiveButton("确定", null)
                                .show();
                    }
                }
            }
        }
    }

    private void showTextToast(String msg) {
        if (toast == null) {
            toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
        } else {
            toast.setText(msg);
        }
        toast.cancel();
        toast.show();
    }

}
