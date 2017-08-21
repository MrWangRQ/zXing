/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.langwing.zxinglibrary.zXing;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.langwing.zxinglibrary.R;
import com.langwing.zxinglibrary.core.ResultPoint;
import com.langwing.zxinglibrary.zXing.camera.CameraManager;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 80L;
    //    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
//    private static final int POINT_SIZE = 6;

    private CameraManager cameraManager;
    private final Paint paint;
    private int maskColor;
    //    private int resultColor;

    //    private int resultPointColor;
    private int scannerAlpha;
    private List<ResultPoint> possibleResultPoints;
//    private List<ResultPoint> lastPossibleResultPoints;

    boolean isFirst;

    private int slideTop;

    //四个边角的颜色
    private int borderColor;
    // 四个绿色边角对应的长度
    private int angleLenth;

    //移动的线的颜色
    private int lineColor;
    // 中间那条线每次刷新移动的距离
    private int lineSpeed;
    // 中间滑动线的最底端位置
    private int slideBottom;
    //扫描框中的中间线的与扫描框左右的间隙
    private static final int MIDDLE_LINE_PADDING = 5;
    // 扫描框中的中间线的宽度
    private static final int MIDDLE_LINE_WIDTH = 4;
    //字体距离扫描框下面的距离
    private float hintPaddintTop;
    //字体大小
    float textSize;
    // 四个边角对应的宽度
    private int angleWidth;
    //提示文字
    private String strHint;
    //提示文字的颜色
    private int hintTextColor;


    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
        strHint = resources.getString(R.string.scan_text);
        maskColor = 0xa5000000;
        lineColor = resources.getColor(R.color.viewfinder_laser);
        scannerAlpha = 0;
        possibleResultPoints = new ArrayList<>(5);
        getAttrs(context, attrs);
    }

    private void getAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.wang);
        strHint = typedArray.getString(R.styleable.wang_hint);
        if (strHint == null) {
            strHint = "";
        }
        textSize = typedArray.getDimension(R.styleable.wang_hintTextSize, 16);
        hintTextColor = typedArray.getColor(R.styleable.wang_hintTextColor, 0x000000);
        maskColor = typedArray.getInt(R.styleable.wang_mastColor, R.color.viewfinder_mask);
        borderColor = typedArray.getColor(R.styleable.wang_borderColor, 0xff5ee300);
        lineColor = typedArray.getColor(R.styleable.wang_lineColor, 0xff5ee300);
        hintPaddintTop = typedArray.getDimension(R.styleable.wang_hintPaddingTop, 20);
        angleLenth = (int) typedArray.getDimension(R.styleable.wang_angleLength, 30);
        angleWidth = (int) typedArray.getDimension(R.styleable.wang_angleWidth, 10);
        lineSpeed = typedArray.getInt(R.styleable.wang_lineSpeed, 8);
        typedArray.recycle();
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return; // not ready yet, early draw before done configuring
        }
        Rect frame = cameraManager.getFramingRect();
        Rect previewFrame = cameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }

        //初始化中间线滑动的最上边和最下边
        if (!isFirst) {
            isFirst = true;
            slideTop = frame.top;
            slideBottom = frame.bottom;
        }

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened

        paint.setColor(borderColor);
        //扫描框的外边框
        paint.setStyle(Paint.Style.STROKE);//设置空心
        paint.setStrokeWidth(3);
        canvas.drawRect(frame, paint);
        //画扫描框边上的角，总共8个部分
        paint.setStyle(Paint.Style.FILL);//设置实心
        canvas.drawRect(frame.left, frame.top, frame.left + angleLenth, frame.top + angleWidth, paint);
        canvas.drawRect(frame.left, frame.top, frame.left + angleWidth, frame.top + angleLenth, paint);
        canvas.drawRect(frame.right - angleLenth, frame.top, frame.right, frame.top + angleWidth, paint);
        canvas.drawRect(frame.right - angleWidth, frame.top, frame.right, frame.top + angleLenth, paint);
        canvas.drawRect(frame.left, frame.bottom - angleWidth, frame.left + angleLenth, frame.bottom, paint);
        canvas.drawRect(frame.left, frame.bottom - angleLenth, frame.left + angleWidth, frame.bottom, paint);
        canvas.drawRect(frame.right - angleLenth, frame.bottom - angleWidth, frame.right, frame.bottom, paint);
        canvas.drawRect(frame.right - angleWidth, frame.bottom - angleLenth, frame.right, frame.bottom, paint);
        //外边框的黑色透明部分
        paint.setColor(maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);
        paint.setColor(lineColor);
        paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
        scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;

        //绘制中间的线,每次刷新界面，中间的线往下移动SPEEN_DISTANCE
        slideTop += lineSpeed;
        if (slideTop >= frame.bottom) {
            slideTop = frame.top;
        }
        canvas.drawRect(frame.left + MIDDLE_LINE_PADDING, slideTop - MIDDLE_LINE_WIDTH / 2, frame.right - MIDDLE_LINE_PADDING, slideTop + MIDDLE_LINE_WIDTH / 2, paint);

        //画扫描框下面的字
        paint.setColor(hintTextColor);
        paint.setTextSize(textSize);
        //   paint.setTypeface(Typeface.create("System", Typeface.BOLD));

        float strWidth = paint.measureText(strHint);
        canvas.drawText(strHint, frame.right - frame.width() / 2 - strWidth / 2, frame.bottom + hintPaddintTop, paint);

//        float scaleX = frame.width() / (float) previewFrame.width();
//        float scaleY = frame.height() / (float) previewFrame.height();

//        List<ResultPoint> currentPossible = possibleResultPoints;
//        List<ResultPoint> currentLast = lastPossibleResultPoints;
//        int frameLeft = frame.left;
//        int frameTop = frame.top;
////        if (currentPossible.isEmpty()) {
////            lastPossibleResultPoints = null;
//        } else {
//            possibleResultPoints = new ArrayList<>(5);
//            lastPossibleResultPoints = currentPossible;
//            paint.setAlpha(CURRENT_POINT_OPACITY);
//            paint.setColor(resultPointColor);
//            synchronized (currentPossible) {
//                for (ResultPoint point : currentPossible) {
//                    canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
//                            frameTop + (int) (point.getY() * scaleY),
//                            POINT_SIZE, paint);
//                }
//            }
//        }
//        if (currentLast != null) {
//            paint.setAlpha(CURRENT_POINT_OPACITY / 2);
//            paint.setColor(resultPointColor);
//            synchronized (currentLast) {
//                float radius = POINT_SIZE / 2.0f;
//                for (ResultPoint point : currentLast) {
//                    canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
//                            frameTop + (int) (point.getY() * scaleY),
//                            radius, paint);
//                }
//            }
//        }

        //只刷新扫描框的内容，其他地方不刷新
        postInvalidateDelayed(ANIMATION_DELAY,
                frame.left + angleLenth,
                frame.top + angleLenth,
                frame.right - angleLenth,
                frame.bottom - angleLenth);
    }

    public void drawViewfinder() {
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }
}
