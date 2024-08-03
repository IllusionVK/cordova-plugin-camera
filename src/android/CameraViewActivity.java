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
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.content.res.Configuration;

public class CameraViewActivity extends Activity {
    private static final String LOG_TAG = "CameraViewActivity";
    private static final int PHOTOLIBRARY = 18;
    private int flashMode = 0;

    private CameraPreview cameraPreview;
    private LinearLayout main;

    private int viewWidth;
    private int viewHeight;

    private RelativeLayout topToolbar;
    private RelativeLayout toolbar;
    private RelativeLayout cameraView;

    private RelativeLayout.LayoutParams flashLayoutParams;
    private RelativeLayout.LayoutParams libLayoutParams;

    private RelativeLayout.LayoutParams closeLayoutParams;
    private RelativeLayout.LayoutParams takeLayoutParams;
    private RelativeLayout.LayoutParams flipLayoutParams = null;

    private int dpToPixels(int dipValue) {
      int value = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
        (float)dipValue,
        this.getResources().getDisplayMetrics()
      );

      return value;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Display display = this.getWindowManager().getDefaultDisplay();
        int rotation = display.getRotation();
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        int height = metrics.heightPixels;
        int width = metrics.widthPixels;

        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
          int screenWidth = display.getWidth();
          int screenHeight = display.getHeight();
          viewHeight = 2 * (screenHeight / 3);
          viewWidth = screenWidth;
        } else {
          int screenWidth = display.getWidth();
          int screenHeight = display.getHeight();
          viewWidth = 2 * (screenWidth / 3);
          viewHeight = screenHeight;
        }

        cameraView.getLayoutParams().width = viewWidth;
        cameraView.getLayoutParams().height = viewHeight;
        cameraPreview.setSize(viewWidth, viewHeight);

        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
          main.setOrientation(LinearLayout.VERTICAL);

          topToolbar.getLayoutParams().width = RelativeLayout.LayoutParams.MATCH_PARENT;
          topToolbar.getLayoutParams().height = this.dpToPixels(48);
          topToolbar.requestLayout();
          topToolbar.setVerticalGravity(Gravity.CENTER_VERTICAL);
          topToolbar.setHorizontalGravity(Gravity.LEFT);

          flashLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
          flashLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
          flashLayoutParams.setMargins(this.dpToPixels(10), 0, 0, 0);

          libLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
          libLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
          libLayoutParams.setMargins(0, 0, this.dpToPixels(10), 0);

          int buttonsLayoutHeight = height - cameraPreview.getViewHeight() - this.dpToPixels(50);
          toolbar.getLayoutParams().height = buttonsLayoutHeight;
          toolbar.getLayoutParams().width = RelativeLayout.LayoutParams.MATCH_PARENT;

          toolbar.setPadding(this.dpToPixels(2), this.dpToPixels(2),
            this.dpToPixels(2), this.dpToPixels(20));
          toolbar.setVerticalGravity(Gravity.CENTER_VERTICAL);
          toolbar.setHorizontalGravity(Gravity.BOTTOM);

          closeLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
          closeLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
          closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
          closeLayoutParams.setMargins(this.dpToPixels(10), this.dpToPixels(22), 0, 0);

          if (flipLayoutParams != null) {
            flipLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
            flipLayoutParams.setMargins(0, this.dpToPixels(18), this.dpToPixels(10), 0);
          }

          takeLayoutParams.removeRule(RelativeLayout.CENTER_VERTICAL);
          takeLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
          takeLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        } else {
          main.setOrientation(LinearLayout.HORIZONTAL);

          topToolbar.getLayoutParams().width = this.dpToPixels(48);
          topToolbar.getLayoutParams().height = RelativeLayout.LayoutParams.MATCH_PARENT;
          topToolbar.requestLayout();
          topToolbar.setVerticalGravity(Gravity.TOP);
          topToolbar.setHorizontalGravity(Gravity.CENTER_VERTICAL);

          flashLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
          flashLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
          flashLayoutParams.setMargins(0, 0, 0, this.dpToPixels(10));

          libLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
          libLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
          libLayoutParams.setMargins(0, this.dpToPixels(10), 0, 0);

          int buttonsLayoutW = width - cameraPreview.getViewWidth() - this.dpToPixels(50);
          toolbar.getLayoutParams().width = buttonsLayoutW;
          toolbar.getLayoutParams().height = RelativeLayout.LayoutParams.MATCH_PARENT;
          toolbar.setVerticalGravity(Gravity.TOP);
          toolbar.setHorizontalGravity(Gravity.RIGHT);
          toolbar.setPadding(this.dpToPixels(2), this.dpToPixels(2),
            this.dpToPixels(20), this.dpToPixels(2));

          closeLayoutParams.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
          closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
          closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
          closeLayoutParams.setMargins(0, 0, this.dpToPixels(22), this.dpToPixels(10));

          if (flipLayoutParams != null) {
            flipLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            flipLayoutParams.setMargins(0, this.dpToPixels(10), this.dpToPixels(10), 0);
          }

          takeLayoutParams.removeRule(RelativeLayout.CENTER_HORIZONTAL);
          takeLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
          takeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        }
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
      main = new LinearLayout(this);
      main.setOrientation(LinearLayout.VERTICAL);
      main.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
      main.setBackgroundColor(Color.BLACK);
      setContentView(main);

      DisplayMetrics metrics = this.getResources().getDisplayMetrics();
      int height = metrics.heightPixels;

      CameraManager cameraManager = (CameraManager)this.getSystemService(Context.CAMERA_SERVICE);
      Display display = this.getWindowManager().getDefaultDisplay();
      int rotation = display.getRotation();

      if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
        int screenWidth = display.getWidth();
        int screenHeight = display.getHeight();
        viewHeight = 2 * (screenHeight / 3);
        viewWidth = screenWidth;
      } else {
        int screenWidth = display.getWidth();
        int screenHeight = display.getHeight();
        viewWidth = 2 * (screenWidth / 3);
        viewHeight = screenHeight;
      }

      cameraPreview = new CameraPreview(this, cameraManager, display, viewWidth, viewHeight);
      cameraPreview.setId(Integer.valueOf(1));

      cameraView = new RelativeLayout(this);
      RelativeLayout.LayoutParams cameraLayoutParams = new RelativeLayout.LayoutParams(
        viewWidth,
        viewHeight
      );
      cameraView.setLayoutParams(cameraLayoutParams);
      cameraView.setVerticalGravity(Gravity.TOP);
      cameraView.setHorizontalGravity(Gravity.LEFT);
      cameraView.addView(cameraPreview);

      topToolbar = new RelativeLayout(this);
      RelativeLayout.LayoutParams topToolbarParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.MATCH_PARENT,
        this.dpToPixels(48)
      );
      topToolbar.setLayoutParams(topToolbarParams);
      topToolbar.setVerticalGravity(Gravity.CENTER_VERTICAL);
      topToolbar.setHorizontalGravity(Gravity.LEFT);
      topToolbar.bringToFront();

      Resources activityRes = this.getResources();
      String packageName = getApplication().getPackageName();

      ImageButton flashButton = new ImageButton(this);
      flashLayoutParams = new RelativeLayout.LayoutParams(this.dpToPixels(44), this.dpToPixels(44));
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

      ImageButton libButton = new ImageButton(this);
      libLayoutParams = new RelativeLayout.LayoutParams(this.dpToPixels(44), this.dpToPixels(44));
      libLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
      libLayoutParams.setMargins(0, 0, this.dpToPixels(10), 0);
      libButton.setLayoutParams(libLayoutParams);
      libButton.setContentDescription("Lib Button");
      libButton.setId(Integer.valueOf(6));
      libButton.setBackground(null);
      libButton.setImageDrawable(activityRes.getDrawable(
        activityRes.getIdentifier("ic_action_photo_lib", "drawable", packageName),
        null));
      libButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
      libButton.getAdjustViewBounds();
      libButton.setOnTouchListener(new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
          switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
              libButton.setColorFilter(Color.GRAY, android.graphics.PorterDuff.Mode.SRC_IN);
              return false;
            case MotionEvent.ACTION_UP:
              libButton.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
              return false;
          }
          return false;
        }
      });
      libButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          CameraLauncher.isLibButtonClicked = true;
          String imageUri = getIntent().getStringExtra("requestCode");
          Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
          intent.setType("image/*");
          intent.setAction(Intent.ACTION_GET_CONTENT);
          intent.addCategory(Intent.CATEGORY_OPENABLE);
          intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
          intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
          startActivityForResult(Intent.createChooser(intent, "Select Picture"), PHOTOLIBRARY);
        }
      });
      topToolbar.addView(libButton);

      toolbar = new RelativeLayout(this);
      int buttonsLayoutHeight = height - cameraPreview.getViewHeight() - this.dpToPixels(50);
      RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        buttonsLayoutHeight
      );
      layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
      toolbar.setLayoutParams(layoutParams);
      toolbar.setPadding(this.dpToPixels(2), this.dpToPixels(2),
        this.dpToPixels(2), this.dpToPixels(20));
      toolbar.setVerticalGravity(Gravity.CENTER_VERTICAL);
      toolbar.setHorizontalGravity(Gravity.BOTTOM);
      toolbar.bringToFront();

      ImageButton closeButton = new ImageButton(this);
      closeLayoutParams = new RelativeLayout.LayoutParams(this.dpToPixels(44), this.dpToPixels(44));
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
      takeLayoutParams = new RelativeLayout.LayoutParams(this.dpToPixels(96), this.dpToPixels(96));
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
          flipLayoutParams = new RelativeLayout.LayoutParams(this.dpToPixels(64), this.dpToPixels(64));
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

      main.addView(topToolbar);
      main.addView(cameraView);
      main.addView(toolbar);
    }

  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (requestCode == PHOTOLIBRARY) {
      if (resultCode == Activity.RESULT_OK && intent != null) {
        intent.putExtra("reqCode", PHOTOLIBRARY);
        setResult(Activity.RESULT_OK, intent);
        finish();
      } else {
        setResult(Activity.RESULT_CANCELED);
        finish();
      }
    }
  }
}
