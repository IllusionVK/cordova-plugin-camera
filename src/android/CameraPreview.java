package org.apache.cordova.camera;

import android.content.Context;
import android.util.TypedValue;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import android.hardware.camera2.CameraManager;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
  private static final String LOG_TAG = "CameraPreview";

  private SurfaceHolder mHolder;
  private CameraController cameraController;
  private int viewWidth;
  private int viewHeight;
  private Display display;

  public CameraPreview(Context context, CameraManager cameraManager, Display d, int w, int h) {
    super(context);

    display = d;
    viewWidth = w;
    viewHeight = h;

    // Install a SurfaceHolder.Callback so we get notified when the
    // underlying surface is created and destroyed.
    mHolder = getHolder();
    mHolder.addCallback(this);
    // deprecated setting, but required on Android versions prior to 3.0
    mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    cameraController = new CameraController(cameraManager, this, display);
  }

  public void surfaceCreated(SurfaceHolder holder) {
    // The Surface has been created, now tell the camera where to draw the preview.
    cameraController.startCameraPreview();
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    // empty. Take care of releasing the Camera preview in your activity.
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    // If your preview can change or rotate, take care of those events here.
    // Make sure to stop the preview before resizing or reformatting it.

    if (mHolder.getSurface() == null){
      // preview surface does not exist
      return;
    }
  }

  public int dpToPixels(int dipValue) {
    int value = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
      (float)dipValue,
      this.getResources().getDisplayMetrics()
    );

    return value;
  }

  @Override
  public void onLayout(boolean changed, int left, int top, int right, int bottom) {
    if (changed) {
      (this).layout(0, 0, viewWidth, viewHeight);
    }
  }

  public void setSize(int w, int h) {
    viewWidth = w;
    viewHeight = h;

    (this).layout(0, 0, viewWidth, viewHeight);
  }

  public int getViewWidth() {
    return viewWidth;
  }

  public int getViewHeight() {
    return viewWidth;
  }

  public void takePicture(String imageFilePath, TakePictureCallback callback) {
    this.cameraController.takePicture(imageFilePath, callback);
  }

  public boolean swichCameras() {
    return this.cameraController.switchCameras();
  }

  public boolean isFlashSupported() {
    return this.cameraController.isFlashSupported();
  }

  public void setFlash(int mFlashMode) {
    this.cameraController.setFlash(mFlashMode);
  }
}
