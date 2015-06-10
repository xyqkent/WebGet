package zzp.android.getweb;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;

import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

import android.view.View;

import android.widget.RelativeLayout;


/**
 * Created by Xiang on 2015/6/9.
 */
public class WifiDirectHelpActivity extends Activity implements ViewPager.OnPageChangeListener {
    private ViewPager viewPager;
    int mViews[];


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_direct_help);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        viewPager = (ViewPager) findViewById(R.id.vPager);

        mViews = new int[]{R.id.LL1, R.id.LL2, R.id.LL3, R.id.LL4, R.id.LL5};
        //设置Adapter
        viewPager.setAdapter(new MyAdapter());
        //设置监听，主要是设置点点的背景
        viewPager.setOnPageChangeListener(this);
        //设置ViewPager的默认项, 设置为长度的100倍，这样子开始就能往左滑动
        viewPager.setCurrentItem(0);
    }

    public class MyAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mViews.length;
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
            View view = View.inflate(WifiDirectHelpActivity.this, R.layout.wifi_direct_help_content, null);
//            RelativeLayout ll = (RelativeLayout) view.findViewById(mViews[position]);
            View v = view.findViewById(mViews[position]);
            ((ViewPager) container).addView(v, 0);
            return v;
        }


    }

    @Override
    public void onPageScrollStateChanged(int arg0) {

    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {

    }

    @Override
    public void onPageSelected(int arg0) {

    }

    public int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }
}
