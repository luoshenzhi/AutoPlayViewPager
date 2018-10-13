# AutoPlayViewPager
1. 代码入侵性小, 修改一下类名即可, 不需要更换适配器
2. 通过代码绘制指示器, 没有指示器布局文件和没有指示器资源文件
3. 支持设置 ViewPager 切换时间
4. 可通过 adapter.notifyDataSetChanged() 动态更新数据, 解决 setCurrentItem(int item) 卡顿

https://blog.csdn.net/zhishenluo/article/details/83041255
