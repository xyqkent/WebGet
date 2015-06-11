package zzp.android.getweb;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;

import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Xiang on 2015/6/9.
 */
public class WifiDirectHelpActivity extends Activity implements ViewPager.OnPageChangeListener {
    private ViewPager viewPager;
    int xmls[];
    LinearLayout underPager;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_direct_help);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        viewPager = (ViewPager) findViewById(R.id.vPager);
        xmls = new int[]{R.layout.wifi_direct_help_1, R.layout.wifi_direct_help_1_1, R.layout.wifi_direct_help_1_2
                , R.layout.wifi_direct_help_2, R.layout.wifi_direct_help_2_1, R.layout.wifi_direct_help_2_2
                , R.layout.wifi_direct_help_3, R.layout.wifi_direct_help_3_1, R.layout.wifi_direct_help_3_2, R.layout.wifi_direct_help_3_3
                , R.layout.wifi_direct_help_3_4, R.layout.wifi_direct_help_3_5, R.layout.wifi_direct_help_3_6
                , R.layout.wifi_direct_help_4, R.layout.wifi_direct_help_4_1
                , R.layout.wifi_direct_help_5, R.layout.wifi_direct_help_5_1, R.layout.wifi_direct_help_5_2
                , R.layout.wifi_direct_help_6, R.layout.wifi_direct_help_6_1, R.layout.wifi_direct_help_6_2
        };

        //滑动指示点
        underPager = (LinearLayout) findViewById(R.id.underPager);

        viewPager.setAdapter(new MyAdapter());
        //设置Adapter
        //设置监听，主要是设置点点的背景
        viewPager.setOnPageChangeListener(this);
        initUnderPager();
        setUnderPager(0);
    }

    public class MyAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return xmls.length;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            ((ViewPager) container).removeView((View) object);
        }

        /**
         * 载入图片进去，用当前的position 除以 图片数组长度取余数是关键
         */
        @Override
        public Object instantiateItem(View container, int position) {
            LayoutInflater mInflater = getLayoutInflater();
            View v = mInflater.inflate(xmls[position], null);

            ((ViewPager) container).addView(v, 0);
            return v;
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {
    }

    @Override
    public void onPageScrolled(int i, float v, int distant) {
    }

    @Override
    public void onPageSelected(int arg0) {
        setUnderPager(arg0);
    }

    public int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    private void initUnderPager() {
        //加入点击功能
        for (int i = 0; i < xmls.length; i++) {
            Button bt = new Button(this);
            bt.setLayoutParams(new ViewGroup.LayoutParams(dip2px(this, 18), ViewGroup.LayoutParams.MATCH_PARENT));
            bt.setBackgroundResource(R.drawable.wifi_direct_dot_nomal);
            bt.setTag(i);
            bt.setOnClickListener(ringClick);
            underPager.addView(bt);
        }
    }

    private void setUnderPager(int position) {
        for (int i = 0; i < xmls.length; i++) {
            Button currentBt = (Button) underPager.getChildAt(i);
            currentBt.setBackgroundResource(R.drawable.wifi_direct_dot_nomal);
        }
        Button setBTN = (Button) underPager.getChildAt(position);
        setBTN.setBackgroundResource(R.drawable.wifi_direct_dot_selected);
    }

    private View.OnClickListener ringClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i("ringClick", v.getTag().toString());
            int position = (int) v.getTag();
            viewPager.setCurrentItem(position);
        }
    };
}
