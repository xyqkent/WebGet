package zzp.android.getweb;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiang on 2015/6/9.
 */
public class WifiDirectHelpActivity extends Activity implements ViewPager.OnPageChangeListener {
    private ViewPager viewPager;
    private List<View> lists = new ArrayList<View>();
    private View[] mViews;
    private int[] imgIdArray;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_direct_help);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        viewPager = (ViewPager) findViewById(R.id.vPager);
        imgIdArray = new int[]{R.drawable.help1, R.drawable.help2, R.drawable.help3, R.drawable.help4, R.drawable.help5
                , R.drawable.help6, R.drawable.help7, R.drawable.help8, R.drawable.help9, R.drawable.help10, R.drawable.help11
                , R.drawable.help12, R.drawable.help13, R.drawable.help14, R.drawable.help15};
        //将图片装载到数组中
        mViews = new ImageView[imgIdArray.length];
//        for (int i = 0; i < 3; i++) {
//            ImageView imageView = new ImageView(WifiDirectHelpActivity.this);
//            imageView.setAdjustViewBounds(true);
//            imageView.setImageResource(imgIdArray[i]);
//            mViews[i] = imageView;
//        }
//        for (int i = 0; i < imgIdArray.length; i++) {
//            ImageView imageView = new ImageView(this);
//            imageView.setAdjustViewBounds(true);
//            mViews[i] = imageView;
//            imageView.setImageResource(imgIdArray[i]);
////            ImageView imageView = new ImageView(this);
////            imageView.setAdjustViewBounds(true);
////            Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), imgIdArray[i]);
////            imageView.setImageBitmap(bitmap);
////            mViews[i] = imageView;
//        }
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
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            mViews[position] = null;
            ((ViewPager) container).removeView((View) object);
        }

        /**
         * 载入图片进去，用当前的position 除以 图片数组长度取余数是关键
         */
        @Override
        public Object instantiateItem(View container, int position) {
            for (int i = -1; i < 2; i++) {
                if (position + i >= 0) {
                    ImageView imageView = new ImageView(WifiDirectHelpActivity.this);
                    imageView.setAdjustViewBounds(true);
                    imageView.setImageResource(imgIdArray[position + i]);
                    mViews[position + i] = imageView;
                }
            }
            Log.i("instantiateItem", String.valueOf(position));
            ((ViewPager) container).addView(mViews[position], 0);
            return mViews[position];
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
