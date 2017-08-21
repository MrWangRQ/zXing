package com.langwing.zxinglibrary.zXing;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import com.langwing.zxinglibrary.R;
import com.langwing.zxinglibrary.core.BarcodeFormat;
import com.langwing.zxinglibrary.core.Result;
import com.langwing.zxinglibrary.core.client.result.ResultParser;
import com.langwing.zxinglibrary.zXing.camera.CameraManager;
import com.langwing.zxinglibrary.zXing.result.ResultHandler;
import com.langwing.zxinglibrary.zXing.result.TextResultHandler;

import java.io.IOException;
import java.util.Collection;

public abstract class CaptureActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = CaptureActivity.class.getSimpleName();

    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private boolean hasSurface;
    private Collection<BarcodeFormat> decodeFormats;
    private String characterSet;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private AmbientLightManager ambientLightManager;

    ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(layoutID());
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);
        ambientLightManager = new AmbientLightManager(this);
    }

    public abstract int layoutID();

    public abstract int surfaceViewID();

    public abstract int viewFindViewID();

    @Override
    protected void onResume() {
        super.onResume();
        cameraManager = new CameraManager(getApplication());

        viewfinderView = (ViewfinderView) findViewById(viewFindViewID());
        viewfinderView.setCameraManager(cameraManager);

        handler = null;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        beepManager.updatePrefs();
        ambientLightManager.start(cameraManager);

        inactivityTimer.onResume();
        decodeFormats = null;
        characterSet = null;

        SurfaceView surfaceView = (SurfaceView) findViewById(surfaceViewID());
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        beepManager.close();
        cameraManager.closeDriver();
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(surfaceViewID());
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;
            // Use volume up/down to turn on light
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                cameraManager.setTorch(false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                cameraManager.setTorch(true);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult   The contents of the barcode.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param barcode     A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        inactivityTimer.onActivity();
        ResultHandler resultHandler = new TextResultHandler(this, ResultParser.parseResult(rawResult));

        boolean fromLiveScan = barcode != null;
        if (fromLiveScan) {
            // Then not from history, so beep/vibrate and we have an image to draw on
            beepManager.playBeepSoundAndVibrate();
            //  drawResultPoints(barcode, scaleFactor, rawResult);
        }
        String content = resultHandler.getDisplayContents().toString();
        Log.e("content", content + "---" + rawResult.getText());
        handleQrContent(content);
    }

    public abstract void handleQrContent(String qrContent);

//    /**
//     * Superimpose a line for 1D or dots for 2D to highlight the key features of the barcode.
//     *
//     * @param barcode     A bitmap of the captured image.
//     * @param scaleFactor amount by which thumbnail was scaled
//     * @param rawResult   The decoded results which contains the points to draw.
//     */
//    private void drawResultPoints(Bitmap barcode, float scaleFactor, Result rawResult) {
//        ResultPoint[] points = rawResult.getResultPoints();
//        if (points != null && points.length > 0) {
//            Canvas canvas = new Canvas(barcode);
//            Paint paint = new Paint();
//            paint.setColor(getResources().getColor(R.color.result_points));
//            if (points.length == 2) {
//                paint.setStrokeWidth(4.0f);
//                drawLine(canvas, paint, points[0], points[1], scaleFactor);
//            } else if (points.length == 4 &&
//                    (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A ||
//                            rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
//                // Hacky special case -- draw two lines, for the barcode and metadata
//                drawLine(canvas, paint, points[0], points[1], scaleFactor);
//                drawLine(canvas, paint, points[2], points[3], scaleFactor);
//            } else {
//                paint.setStrokeWidth(10.0f);
//                for (ResultPoint point : points) {
//                    if (point != null) {
//                        canvas.drawPoint(scaleFactor * point.getX(), scaleFactor * point.getY(), paint);
//                    }
//                }
//            }
//        }
//    }

//    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b, float scaleFactor) {
//        if (a != null && b != null) {
//            canvas.drawLine(scaleFactor * a.getX(),
//                    scaleFactor * a.getY(),
//                    scaleFactor * b.getX(),
//                    scaleFactor * b.getY(),
//                    paint);
//        }
//    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, decodeFormats, DecodeHintManager.parseDecodeHints(getIntent()), characterSet, cameraManager);
//                handler = new CaptureActivityHandler(this, decodeFormats, characterSet, cameraManager);
            }
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
        builder.setOnCancelListener(new FinishListener(this));
        builder.show();
    }

    //重置相机
    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }
}