package com.ken.view.viewpager;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.Scroller;


import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;


public class AutoPlayViewPager extends ViewPager {

    private Paint paint;
    private int position;
    private int size = 15;
    private int selected = Color.RED;
    private int background = 0x66FFFFFF;

    private int displayTime = Integer.MAX_VALUE;
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private final AutoPlayScroller autoPlayScroller;

    private PagerAdapter adapter;
    private final AtomicBoolean dataSetObserverRegistered = new AtomicBoolean(false);


    public AutoPlayViewPager(@NonNull Context context) {
        this(context, null);
    }

    public AutoPlayViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        addOnPageChangeListener(mListener);

        paint = new Paint();
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

        autoPlayScroller = new AutoPlayScroller(context);
        setField("android.support.v4.view.ViewPager", this, "mScroller", autoPlayScroller);
    }


    @Override
    public void setAdapter(@Nullable PagerAdapter adapter) {

        if (adapter == null) {
            super.setAdapter(null);
            return;
        }

        // 1. 包装原 adapter, 使其支持无限轮播
        super.setAdapter(new AutoPlayAdapter(adapter));
        this.adapter = adapter;

        // 防止重复设置 Adapter 导致注册报错
        if (dataSetObserverRegistered.get()) {
            adapter.unregisterDataSetObserver(mDataSetObserver);
        }
        adapter.registerDataSetObserver(mDataSetObserver);
        dataSetObserverRegistered.set(true);

        // 2. 确保默认选中的是第一个页面和第一个圆点
        if (adapter.getCount() > 0) {
            int currentItem = Integer.MAX_VALUE >> 1;
            currentItem = currentItem - currentItem % adapter.getCount();
            setCurrentItem(currentItem, false);
        }
    }


    /**
     * 开始自动播放
     * @param displayTime : 页面显示时间
     */
    public void startPlay(int displayTime) {

        this.displayTime =  displayTime;
        if (adapter == null || adapter.getCount() <= 1)
            return;

        stopPlay();
        isPlaying.set(true);
        postDelayed(player, this.displayTime);
    }

    /**
     * 停止播放
     */
    public void stopPlay() {
        removeCallbacks(player);
        isPlaying.set(false);
    }


    // 循环播放消息
    private final Runnable player = new Runnable() {

        @Override
        public void run() {
            if (isPlaying.get()) {
                setCurrentItem(getCurrentItem() + 1, true);
                postDelayed(player, displayTime);
            }
        }
    };

    /**
     * 设置切换时间
     * @param duration 切换时间(ms), 默认 600ms
     */
    public void setSwitchDuration(int duration) {
        if (duration >= displayTime) {
            throw new IllegalArgumentException("The augment duration must less than displayTime!");
        }
        autoPlayScroller.setDuration(duration);
    }


    /**
     * 设置圆点样式
     * @param size       圆点大小
     * @param background 圆点背景色
     * @param selected   圆点前景色
     */
    public void setPointStyle(int size, int background, int selected) {
        this.size = size;
        this.selected = selected;
        this.background = background;
        invalidate();
    }


    /**
     * 绘制圆点指示器
     * @param canvas 画布
     */
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        final int count = adapter == null ? 0 : adapter.getCount();
        if (count < 1) return;

        canvas.save();
        canvas.translate(getScrollX(), getScrollY());

        float x = (getWidth() - size * count * 2) / 2 + size;
        float y = getHeight() - size;

        paint.setColor(background);
        for (int i = 0; i < count; i++) {
            canvas.drawCircle(x + i * size * 2, y, size >> 1, paint);
        }

        paint.setColor(selected);
        canvas.drawCircle(x + position * size * 2, y, size >> 1, paint);

        canvas.restore();
    }


    /**
     * 防止用户手动滑动后, 马上又播放下一张
     */
    private final OnPageChangeListener mListener = new ViewPager.SimpleOnPageChangeListener() {

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                startPlay(displayTime);
            } else if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                stopPlay();
            }
        }

        @Override
        public void onPageSelected(int pos) {
            if (adapter.getCount() > 0) {
                position = pos % adapter.getCount();
                invalidate();
            }
        }
    };



    /**
     * 用于支持 Adapter.notifyDataSetChanged()
     */
    private final DataSetObserver mDataSetObserver = new DataSetObserver() {

        @Override
        public void onChanged() {
            update();
        }

        @Override
        public void onInvalidated() {
            update();
        }

        private void update() {

            if (getAdapter() == null)
                return;

            getAdapter().notifyDataSetChanged();
            selectFirstItem();

            if (displayTime != Integer.MAX_VALUE) {
                startPlay(displayTime);
            }
        }

        private void selectFirstItem() {

            stopPlay();

            // 1. 设置 mFirstLayout = true
            setField("android.support.v4.view.ViewPager", AutoPlayViewPager.this, "mFirstLayout", true);

            // 2. 重新设置 adapter
            setAdapter(null);
            setAdapter(adapter);

            if (adapter.getCount() > 0) {
                int currentItem = Integer.MAX_VALUE >> 1;
                currentItem = currentItem - currentItem % adapter.getCount();
                setCurrentItem(currentItem, false);
            }
        }
    };

    @Override
    protected void onDetachedFromWindow() {
        stopPlay();
        removeOnPageChangeListener(mListener);
        if (adapter != null && dataSetObserverRegistered.get()) {
            adapter.unregisterDataSetObserver(mDataSetObserver);
            dataSetObserverRegistered.set(false);
        }
        super.onDetachedFromWindow();
    }


    // 反射设置类字段
    public static boolean setField(String className, Object object, String filedName, Object filedValue) {
        try {
            Class clazz = Class.forName(className);
            Field field = clazz.getDeclaredField(filedName);
            field.setAccessible(true);
            field.set(object, filedValue);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }



    private static class AutoPlayAdapter extends PagerAdapter {

        private final PagerAdapter adapter;

        AutoPlayAdapter(PagerAdapter adapter) {
            if (adapter == null)
                throw new NullPointerException("adapter is null!");
            this.adapter = adapter;
        }

        @Override
        public int getCount() {
            if (adapter.getCount() <= 1)
                return adapter.getCount();
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return adapter.isViewFromObject(view, object);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            if (adapter.getCount() > 0) {
                return adapter.instantiateItem(container, position % adapter.getCount());
            }
            return null;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            if (adapter.getCount() != 0) {
                adapter.destroyItem(container, position % adapter.getCount(), object);
            }
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            if (adapter.getCount() != 0) {
                return adapter.getPageTitle(position % adapter.getCount());
            }
            return "";
        }
    }


    private static class AutoPlayScroller extends Scroller {

        private int duration = 600;

        AutoPlayScroller(Context context) {
            super(context, interpolator);
        }

        private static final Interpolator interpolator = (t) -> {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        };

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            super.startScroll(startX, startY, dx, dy, Math.max(duration, this.duration));
        }

        void setDuration(int duration) {
            this.duration = duration;
        }
    }
}
