package com.wang.zxing;

import android.widget.Toast;

import com.wang.zxinglibrary.zXing.CaptureActivity;

/**
 * To Change The World
 * 2016/9/15 18:10
 * Created by Mr.Wang
 * 需要继承自CaptureActivity
 * 在自定义布局中需要包含一个surfaceView组件和ViewFindView组件
 * 三个抽象方法的返回值不能为0
 */
public class MainActivity extends CaptureActivity {

    @Override
    public int layoutID() {
        return R.layout.capture;
    }

    @Override
    public int surfaceViewID() {
        return R.id.preview_view;
    }

    @Override
    public int viewFindViewID() {
        return R.id.viewfinder_view;
    }

    @Override
    public void handleQrContent(String qrContent) {
        Toast.makeText(this, qrContent, Toast.LENGTH_LONG).show();
    }
}
