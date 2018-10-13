package com.ken.view.viewpager;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ken.androidkit.R;
import com.ken.base.BaseActivity;
import com.ken.util.UiUtil;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class TestAutoPlayViewPager extends BaseActivity {

    private AutoPlayViewPager viewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_auto_play_viewpager_layout);
        init();
    }

    private void init() {

        List<Map.Entry<Integer, String>> data = new ArrayList<>();
        data.add(new AbstractMap.SimpleEntry<>(R.mipmap.aaa, "Page-1"));
        data.add(new AbstractMap.SimpleEntry<>(R.mipmap.bbb, "Page-2"));
        data.add(new AbstractMap.SimpleEntry<>(R.mipmap.ccc, "Page-3"));

        PagerAdapter adapter = getPagerAdapter(data);
        viewPager = findViewById(R.id.view_page);

        viewPager.setAdapter(adapter);
        viewPager.setSwitchDuration(800);
        viewPager.setPointStyle(UiUtil.dp2px(10), 0x66FFFFFF, Color.RED);

        // 测试动态数据更新
        viewPager.postDelayed(() -> {
            data.add(new AbstractMap.SimpleEntry<>(R.mipmap.dddd, "Page-4"));
            adapter.notifyDataSetChanged();
        }, 4000);
    }

    @Override
    protected void onStart() {
        super.onStart();
        viewPager.startPlay(2000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        viewPager.stopPlay();                 // 停止播放, 节省资源
    }

    private PagerAdapter getPagerAdapter(final List<Map.Entry<Integer, String>> data) {

        return new PagerAdapter() {

            @Override
            public int getCount() {
                return data == null ? 0 : data.size();
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                return view == object;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {

                TextView view = new TextView(getBaseContext());
                view.setText(data.get(position).getValue());
                view.setBackgroundResource(data.get(position).getKey());

                container.addView(view);
                return view;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                if (container != null && object != null && object instanceof View) {
                    container.removeView((View) object);
                }
            }
        };
    }
}
