package com.wang.zxing;

import android.widget.Toast;

import com.wang.zxinglibrary.zXing.CaptureActivity;

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
