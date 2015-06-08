package zzp.android.getweb.OwnClass;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import zzp.android.getweb.R;

/**
 * Created by yingqiang on 2015/5/22.
 */
//适配器设定
public class LVAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private Bitmap directory;
    private List<ListData> mList = null;
    public SparseBooleanArray map;//SparseBooleanArray代替HashMap或者Map
    private Boolean setCKFlg;
    private Context mContext;

    // 参数初始化
    public LVAdapter(Context context, List<ListData> list, Boolean flag) {
        mList = list;
        setCKFlg = flag;
        mContext = context;
        directory = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_list_item);
        // 缩小图片
        directory = small(directory, 0.5f);
        inflater = LayoutInflater.from(context);
        map = new SparseBooleanArray();

        // 初始化数据
        initDate();
    }

    private void initDate() {
        for (int i = 0; i < mList.size(); i++) {
            map.put(i, false);
        }
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.save_view_list, null);
            holder = new ViewHolder();
            holder.text2 = (TextView) convertView.findViewById(R.id.textView);
            holder.text1 = (TextView) convertView.findViewById(R.id.textTimes);
            holder.image = (ImageView) convertView.findViewById(R.id.imageView);
            holder.cb = (CheckBox) convertView.findViewById(R.id.item_cb);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        LinearLayout left_LL = (LinearLayout) convertView.findViewById(R.id.left_LL);
        if (setCKFlg) {
            holder.cb.setVisibility(View.VISIBLE);
            left_LL.setPadding(0, 0, dip2px(mContext, 28), 0);
        } else {
            holder.cb.setVisibility(View.GONE);
            left_LL.setPadding(0, 0, 0, 0);
        }
        holder.text1.setText(sdf.format((mList.get(position).getDate())));
        holder.text2.setText(mList.get(position).getName());
        holder.image.setImageBitmap(directory);
        holder.cb.setChecked(map.get(position));
        return convertView;
    }

    public class ViewHolder {
        private TextView text1;
        private TextView text2;
        private ImageView image;
        public CheckBox cb;
    }

    public void set_setCKFlg(Boolean flag) {
        this.setCKFlg = flag;
    }

    public Boolean get_setCKFlg() {
        return this.setCKFlg;
    }

    private Bitmap small(Bitmap map, float num) {
        Matrix matrix = new Matrix();
        matrix.postScale(num, num);
        return Bitmap.createBitmap(map, 0, 0, map.getWidth(), map.getHeight(), matrix, true);
    }

    private static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }
}
