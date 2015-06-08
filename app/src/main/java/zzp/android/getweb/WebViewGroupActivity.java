package zzp.android.getweb;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import zzp.android.getweb.OwnClass.DBManger;

/**
 * Created by Xiang on 2015/5/21.
 */
public class WebViewGroupActivity extends Activity {
    private GridView gv;
    String GroupList[][],list_id[];
    Toast toast = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_view_group);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        //准备要添加的数据条目
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        DBManger dbM=new DBManger();
        SQLiteDatabase db=dbM.getDB(this);
        Cursor cur = db.rawQuery("select * from type where g_id<> 0", null);
        GroupList=new String[cur.getCount()][2];
        for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
            int i = cur.getPosition();
            GroupList[i][0]=cur.getString(0);
            GroupList[i][1]=cur.getString(1);
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("imageItem", R.drawable.icon_group_item);//添加图像资源的ID
            item.put("textItem", cur.getString(1));//按序号添加ItemText
            items.add(item);
        }
        cur.close();
        db.close();
        //获取上一个intent的发送过来的ID
        Intent intent = getIntent();
        Bundle b=this.getIntent().getExtras();
        list_id=b.getStringArray("list_id");
        //实例化一个适配器
        SimpleAdapter adapter = new SimpleAdapter(this, items, R.layout.web_view_group_item, new String[]{"imageItem", "textItem"}, new int[]{R.id.image_item, R.id.text_item});
        //获得GridView实例
        gv = (GridView) findViewById(R.id.groupgrid);
        //为GridView设置适配器
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String sqlstr = " where id in(";
                for (int i = 0; i < list_id.length; i++) {
                    sqlstr += list_id[i] + ",";
                }
                sqlstr = sqlstr.substring(0, sqlstr.length() - 1) + ")";
                DBManger dbM=new DBManger();
                SQLiteDatabase db=dbM.getDB(WebViewGroupActivity.this);
                String g_id=GroupList[position][0];
                db.execSQL("update list set g_id="+ g_id  + sqlstr);
                showTextToast("已成功设置分组");
                //更新SaveViewActivity
                Message msg1 = Message.obtain();
                msg1.what = 1;
                SaveViewActivity.mHandler.sendMessage(msg1);
                finish();
            }
        });
        gv.setAdapter(adapter);

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
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
