package org.apache.cordova.camera;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class CameraViewActivity extends Activity {
    private static final String LOG_TAG = "CameraViewActivity";
    private int flashMode = 0;

    private int dpToPixels(int dipValue) {
      int value = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
        (float)dipValue,
        this.getResources().getDisplayMetrics()
      );

      return value;
    }

    @Override
    public void onBackPressed() {
      Log.d(LOG_TAG, "onBackPressed Called");
      setResult(Activity.RESULT_CANCELED);
      finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      requestWindowFeature(Window.FEATURE_NO_TITLE);
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);

      // Main container layout
      RelativeLayout main = new RelativeLayout(this);
      main.setLayoutParams(new RelativeLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT));
      main.setBackgroundColor(Color.BLACK);
      setContentView(main);

      CameraManager cameraManager = (CameraManager)this.getSystemService(Context.CAMERA_SERVICE);
      Display display = this.getWindowManager().getDefaultDisplay();
      CameraPreview cameraPreview = new CameraPreview(this, cameraManager, display);
      cameraPreview.setId(Integer.valueOf(1));
      main.addView(cameraPreview);

      DisplayMetrics metrics = this.getResources().getDisplayMetrics();
      int height = metrics.heightPixels;

      RelativeLayout topToolbar = new RelativeLayout(this);
      RelativeLayout.LayoutParams topToolbarParams = new RelativeLayout.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        this.dpToPixels(48)
      );
      topToolbarParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
      topToolbar.setLayoutParams(topToolbarParams);
      topToolbar.setVerticalGravity(Gravity.CENTER_VERTICAL);
      topToolbar.setHorizontalGravity(Gravity.LEFT);
      topToolbar.bringToFront();

      RelativeLayout toolbar = new RelativeLayout(this);
      int buttonsLayoutHeight = height - cameraPreview.getViewHeight() - this.dpToPixels(49);
      RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        buttonsLayoutHeight
      );
      layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
      toolbar.setLayoutParams(layoutParams);
      toolbar.setPadding(this.dpToPixels(2), buttonsLayoutHeight / 3,
        this.dpToPixels(2), this.dpToPixels(2));
      toolbar.setVerticalGravity(Gravity.CENTER_VERTICAL);
      toolbar.setHorizontalGravity(Gravity.LEFT);
      toolbar.bringToFront();

      Resources activityRes = this.getResources();
      String packageName = getApplication().getPackageName();

      ImageButton flashButton = new ImageButton(this);
      RelativeLayout.LayoutParams flashLayoutParams = new RelativeLayout.LayoutParams(this.dpToPixels(44), this.dpToPixels(44));
      flashLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
      flashLayoutParams.setMargins(this.dpToPixels(10), 0, 0, 0);
      flashButton.setLayoutParams(flashLayoutParams);
      flashButton.setContentDescription("Flash Button");
      flashButton.setId(Integer.valueOf(5));
      flashButton.setBackground(null);
      flashButton.setImageDrawable(activityRes.getDrawable(
        activityRes.getIdentifier("ic_action_flash_auto", "drawable", packageName),
        null));
      flashButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
      flashButton.getAdjustViewBounds();
      flashButton.setOnTouchListener(new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
          switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
              flashButton.setColorFilter(Color.GRAY, android.graphics.PorterDuff.Mode.SRC_IN);
              return false;
            case MotionEvent.ACTION_UP:
              flashButton.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
              return false;
          }
          return false;
        }
      });
      flashButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          flashMode = flashMode + 1;

          if (flashMode >= 2) {
            flashMode = -1;
          }

          cameraPreview.setFlash(flashMode);

          if (flashMode == -1) {
            flashButton.setImageDrawable(activityRes.getDrawable(
              activityRes.getIdentifier("ic_action_flash_off", "drawable", packageName),
              null));
          } else if (flashMode == 0) {
            flashButton.setImageDrawable(activityRes.getDrawable(
              activityRes.getIdentifier("ic_action_flash_auto", "drawable", packageName),
              null));
          } else if (flashMode == 1) {
            flashButton.setImageDrawable(activityRes.getDrawable(
              activityRes.getIdentifier("ic_action_flash_on", "drawable", packageName),
              null));
          }
        }
      });
      topToolbar.addView(flashButton);

      ImageButton closeButton = new ImageButton(this);
      RelativeLayout.LayoutParams closeLayoutParams = new RelativeLayout.LayoutParams(this.dpToPixels(44), this.dpToPixels(44));
      closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
      closeLayoutParams.setMargins(this.dpToPixels(10), this.dpToPixels(22), 0, 0);
      closeButton.setLayoutParams(closeLayoutParams);
      closeButton.setContentDescription("Close Button");
      closeButton.setId(Integer.valueOf(2));
      closeButton.setBackground(activityRes.getDrawable(
        activityRes.getIdentifier("circular_button", "drawable", packageName),
        null));
      closeButton.setImageDrawable(activityRes.getDrawable(
        activityRes.getIdentifier("ic_action_close", "drawable", packageName),
        null));
      closeButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
      closeButton.getAdjustViewBounds();
      closeButton.setOnTouchListener(new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
          switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
              closeButton.setColorFilter(Color.GRAY, android.graphics.PorterDuff.Mode.SRC_IN);
              return false;
            case MotionEvent.ACTION_UP:
              closeButton.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
              return false;
          }
          return false;
        }
      });
      closeButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          setResult(Activity.RESULT_CANCELED);
          finish();
        }
      });
      toolbar.addView(closeButton);

      ImageButton takeButton = new ImageButton(this);
      RelativeLayout.LayoutParams takeLayoutParams = new RelativeLayout.LayoutParams(this.dpToPixels(96), this.dpToPixels(96));
      takeLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
      takeButton.setLayoutParams(takeLayoutParams);
      takeButton.setContentDescription("Take Button");
      takeButton.setId(Integer.valueOf(3));
      takeButton.setBackground(null);
      takeButton.setImageDrawable(activityRes.getDrawable(
        activityRes.getIdentifier("ic_action_take_picture", "drawable", packageName),
        null));
      takeButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
      takeButton.getAdjustViewBounds();
      takeButton.setOnTouchListener(new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
          switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
              takeButton.setColorFilter(Color.GRAY, android.graphics.PorterDuff.Mode.SRC_IN);
              return false;
            case MotionEvent.ACTION_UP:
              takeButton.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
              return false;
          }
          return false;
        }
      });
      takeButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          TakePictureCallback callback = new TakePictureCallback() {
            @Override
            public void onFinished(Bitmap img) {
              Intent intent = new Intent();
//              intent.putExtra("data", img);
              setResult(Activity.RESULT_OK, intent);
              finish();
            }
          };
          String imageFilePath = getIntent().getStringExtra("imageFilePath");
          cameraPreview.takePicture(imageFilePath, callback);
        }
      });
      toolbar.addView(takeButton);

      try {
        String[] ids = cameraManager.getCameraIdList();
        if (ids.length > 1) {
          ImageButton flipButton = new ImageButton(this);
          RelativeLayout.LayoutParams flipLayoutParams = new RelativeLayout.LayoutParams(this.dpToPixels(64), this.dpToPixels(64));
          flipLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
          flipLayoutParams.setMargins(0, this.dpToPixels(18), this.dpToPixels(10), 0);
          flipButton.setLayoutParams(flipLayoutParams);
          flipButton.setContentDescription("Flip Camera Button");
          flipButton.setId(Integer.valueOf(4));
          flipButton.setBackground(null);
          flipButton.setImageDrawable(activityRes.getDrawable(
            activityRes.getIdentifier("ic_action_camera_flip", "drawable", packageName),
            null));
          flipButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
          flipButton.getAdjustViewBounds();
          flipButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
              switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                  flipButton.setColorFilter(Color.GRAY, android.graphics.PorterDuff.Mode.SRC_IN);
                  return false;
                case MotionEvent.ACTION_UP:
                  flipButton.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
                  return false;
              }
              return false;
            }
          });
          flipButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
              boolean isFlashSupported = cameraPreview.swichCameras();

              if (!isFlashSupported) {
                flashButton.setVisibility(View.INVISIBLE);
              } else {
                flashButton.setVisibility(View.VISIBLE);
              }
            }
          });
          toolbar.addView(flipButton);
        }
      }
      catch(CameraAccessException ce){
        ce.printStackTrace();
        Log.e(LOG_TAG, ce.toString());
      }

      main.addView(toolbar);
      main.addView(topToolbar);
    }
}
