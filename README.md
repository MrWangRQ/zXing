# zXing
高自定义的二维码扫描库，基于google 的zxing


    <!--hint 提示-->
    <!--hintTextSize提示字体的大小-->
    <!--hintTextColor 提示字体的颜色-->
    <!--mastColor 透明遮罩的颜色-->
    <!--borderColor 扫描框 边框的颜色-->
    <!--lineColor 中间线的颜色-->
    <!--hintPaddingTop 提示语距离 扫描框的距离-->
    <!--angleLength 四个角的长度-->
    <!--angleWidth 四个角的宽度-->
    <!--lineSpeed 线的移动速度-->

    <com.wang.zxinglibrary.zXing.ViewfinderView
        android:id="@+id/viewfinder_view"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        wang:angleLength="30dp"
        wang:angleWidth="5dp"
        wang:borderColor="#00ff00"
        wang:hint="这里写提示"
        wang:hintPaddingTop="20dp"
        wang:hintTextColor="#00ff00"
        wang:hintTextSize="16sp"
        wang:lineColor="#00ff00"
        wang:mastColor="@color/viewfinder_mask" />
