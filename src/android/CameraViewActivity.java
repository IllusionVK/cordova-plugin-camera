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
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.content.res.Configuration;
import android.os.Build;

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

  private ImageButton flashButton;
  private ImageButton libButton;
  private ImageButton closeButton;
  private ImageButton takeButton;
  private ImageButton flipButton;

  private int dp2 = 0;
  private int dp10 = 0;
  private int dp25 = 0;
  private int dp44 = 0;
  private int dp50 = 0;
  private int dp64 = 0;
  private int dp96 = 0;
  private int dp100 = 0;

  private int dpToPixels(int dipValue) {
    int value = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
      (float)dipValue,
      this.getResources().getDisplayMetrics()
    );

    return value;
  }

  private boolean isNavShowing() {
    // determine if navigation bar is going to be shown
    boolean isNavShowing = false;
    if (Build.VERSION.SDK_INT >= 13) {
      isNavShowing = ViewConfiguration.get(getApplication()).hasPermanentMenuKey();
    }

    return isNavShowing;
  }

  private boolean isNavAtBottom() {
    // determine where the navigation bar would be displayed
    boolean isNavAtBottom = false;
    if (Build.VERSION.SDK_INT >= 13) {
      isNavAtBottom = (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
        || (this.getResources().getConfiguration().smallestScreenWidthDp >= 600);
    }

    return isNavAtBottom;
  }

  private void calculateCameraSizes() {
    DisplayMetrics displayMetrics = new DisplayMetrics();
    this.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

    boolean isNavShowing = isNavShowing();
    boolean isNavAtBottom = isNavAtBottom();

    Log.d(LOG_TAG, "isNavShowing: " + isNavShowing);
    Log.d(LOG_TAG, "isNavAtBottom: " + isNavAtBottom);

    int screenWidth = displayMetrics.widthPixels;
    int screenHeight = displayMetrics.heightPixels;
    int navHeight = getNavigationBarHeight();

    if (!isNavShowing) {
      if (isNavAtBottom) {
        screenHeight = displayMetrics.heightPixels + navHeight;
      } else {
        screenWidth = displayMetrics.widthPixels + navHeight;
      }
    }

    if (screenWidth <= screenHeight) {
      viewHeight = (screenWidth * 4) / 3;
      viewWidth = screenWidth;
    } else {
      viewWidth = (screenHeight * 4) / 3;
      viewHeight = screenHeight;

      if (viewWidth > screenWidth) {
        viewWidth = screenWidth;
        viewHeight = (viewWidth * 4) / 3;
      }
    }
  }

  private int getNavigationBarHeight() {
    Resources resources = this.getResources();
    int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
    if (resourceId > 0) {
      return resources.getDimensionPixelSize(resourceId);
    }
    return 0;
  }

  private void setupLayout() {
    Display display = this.getWindowManager().getDefaultDisplay();
    int rotation = display.getRotation();
    DisplayMetrics metrics = this.getResources().getDisplayMetrics();
    int height = metrics.heightPixels;
    int width = metrics.widthPixels;
    int topMargin = 0;
    int leftMargin = 0;

    calculateCameraSizes();

    if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
      int maxH = height - dp50 - dp100;

      if (viewHeight < maxH) {
        topMargin = dp50;
      }
    } else {
      int maxW = width - dp50 - dp100;

      if (viewWidth < maxW) {
        leftMargin = dp50;
      }
    }

    LinearLayout.LayoutParams cameraLayoutParams = new LinearLayout.LayoutParams(
      viewWidth,
      viewHeight
    );
    cameraLayoutParams.topMargin = topMargin;
    cameraLayoutParams.leftMargin = leftMargin;
    cameraView.setLayoutParams(cameraLayoutParams);
    cameraView.setVerticalGravity(Gravity.TOP);
    cameraView.setHorizontalGravity(Gravity.LEFT);
    cameraPreview.setSize(viewWidth, viewHeight);

    Log.d(LOG_TAG, "rotation: " + rotation);

    if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
      main.setOrientation(LinearLayout.VERTICAL);

      LinearLayout.LayoutParams topToolbarParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        dp50
      );
      topToolbarParams.topMargin = -(viewHeight + topMargin);
      topToolbar.setBackgroundColor(Color.TRANSPARENT);
      topToolbar.setLayoutParams(topToolbarParams);
      topToolbar.setVerticalGravity(Gravity.CENTER_VERTICAL);
      topToolbar.setHorizontalGravity(Gravity.LEFT);
      topToolbar.bringToFront();

      RelativeLayout.LayoutParams flashLayoutParams = new RelativeLayout.LayoutParams(dp44, dp44);
      flashLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
      flashLayoutParams.setMargins(dp10, 0, 0, 0);
      flashButton.setLayoutParams(flashLayoutParams);

      RelativeLayout.LayoutParams libLayoutParams = new RelativeLayout.LayoutParams(dp44, dp44);
      libLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
      libLayoutParams.setMargins(0, 0, dp10, 0);
      libButton.setLayoutParams(libLayoutParams);

      LinearLayout.LayoutParams toolbarLayoutParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        dp100
      );
      toolbarLayoutParams.topMargin = height - dp100 - dp25 - topMargin;
      toolbar.setLayoutParams(toolbarLayoutParams);
      toolbar.setPadding(dp2, dp10, dp2, dp2);
      toolbar.setVerticalGravity(Gravity.CENTER_VERTICAL);
      toolbar.setHorizontalGravity(Gravity.TOP);
      toolbar.bringToFront();

      RelativeLayout.LayoutParams closeLayoutParams = new RelativeLayout.LayoutParams(dp44, dp44);
      closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
      closeLayoutParams.setMargins(dp10, this.dpToPixels(20), 0, 0);
      closeButton.setLayoutParams(closeLayoutParams);

      if (flipButton != null) {
        RelativeLayout.LayoutParams flipLayoutParams = new RelativeLayout.LayoutParams(dp64, dp64);
        flipLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        flipLayoutParams.setMargins(0, this.dpToPixels(15), this.dpToPixels(5), 0);
        flipButton.setLayoutParams(flipLayoutParams);
      }

      RelativeLayout.LayoutParams takeLayoutParams = new RelativeLayout.LayoutParams(dp96, dp96);
      takeLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
      takeButton.setLayoutParams(takeLayoutParams);
    } else {
      main.setOrientation(LinearLayout.HORIZONTAL);

      LinearLayout.LayoutParams topToolbarParams = new LinearLayout.LayoutParams(
        dp50,
        LinearLayout.LayoutParams.MATCH_PARENT
      );
      topToolbarParams.leftMargin = -(viewWidth + leftMargin);
      topToolbar.setBackgroundColor(Color.TRANSPARENT);
      topToolbar.setLayoutParams(topToolbarParams);
      topToolbar.setVerticalGravity(Gravity.TOP);
      topToolbar.setHorizontalGravity(Gravity.CENTER_VERTICAL);
      topToolbar.bringToFront();

      RelativeLayout.LayoutParams flashLayoutParams = new RelativeLayout.LayoutParams(dp44, dp44);
      flashLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
      flashLayoutParams.setMargins(0, 0, 0, dp10);
      flashButton.setLayoutParams(flashLayoutParams);

      RelativeLayout.LayoutParams libLayoutParams = new RelativeLayout.LayoutParams(dp44, dp44);
      libLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
      libLayoutParams.setMargins(0, 0, dp10, 0);
      libButton.setLayoutParams(libLayoutParams);

      LinearLayout.LayoutParams toolbarLayoutParams = new LinearLayout.LayoutParams(
        dp100,
        LinearLayout.LayoutParams.MATCH_PARENT
      );
      toolbarLayoutParams.leftMargin = width - dp100 - dp50;
      toolbar.setLayoutParams(toolbarLayoutParams);
      toolbar.setPadding(dp2, dp2, dp2, dp10);
      toolbar.setVerticalGravity(Gravity.TOP);
      toolbar.setHorizontalGravity(Gravity.RIGHT);
      toolbar.bringToFront();

      RelativeLayout.LayoutParams closeLayoutParams = new RelativeLayout.LayoutParams(dp44, dp44);
      closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
      closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
      closeLayoutParams.setMargins(0, 0, this.dpToPixels(22), this.dpToPixels(22));
      closeButton.setLayoutParams(closeLayoutParams);

      boolean isNavShowing = isNavShowing();
      boolean isNavAtBottom = isNavAtBottom();

      if (flipButton != null) {
        RelativeLayout.LayoutParams flipLayoutParams = new RelativeLayout.LayoutParams(dp64, dp64);
        if (!isNavShowing && !isNavAtBottom) {
          flipLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
          flipLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
          flipLayoutParams.setMargins(0, 0, this.dpToPixels(12), 0);
        } else {
          flipLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
          flipLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
          flipLayoutParams.setMargins(0, 0, this.dpToPixels(15), height - (dp64 / 2));
        }

        flipButton.setLayoutParams(flipLayoutParams);
      }

      RelativeLayout.LayoutParams takeLayoutParams = new RelativeLayout.LayoutParams(dp96, dp96);
      if (!isNavShowing && !isNavAtBottom) {
        takeLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
        takeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        takeLayoutParams.setMargins(0, 0, 0, 0);
      } else {
        takeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        takeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        takeLayoutParams.setMargins(0, 0, 0, (height / 2) - (dp96 / 4));
      }

      takeButton.setLayoutParams(takeLayoutParams);
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    setupLayout();
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
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

    dp2 = this.dpToPixels(2);
    dp10 = this.dpToPixels(10);
    dp25 = this.dpToPixels(25);
    dp44 = this.dpToPixels(44);
    dp50 = this.dpToPixels(50);
    dp64 = this.dpToPixels(64);
    dp96 = this.dpToPixels(96);
    dp100 = this.dpToPixels(100);

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

    calculateCameraSizes();

    cameraPreview = new CameraPreview(this, cameraManager, display, viewWidth, viewHeight);
    cameraPreview.setId(Integer.valueOf(1));

    cameraView = new RelativeLayout(this);
    cameraView.addView(cameraPreview);

    topToolbar = new RelativeLayout(this);

    Resources activityRes = this.getResources();
    String packageName = getApplication().getPackageName();

    flashButton = new ImageButton(this);

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
            flashButton.clearColorFilter();
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

    libButton = new ImageButton(this);

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
            libButton.clearColorFilter();
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

    closeButton = new ImageButton(this);

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
            closeButton.clearColorFilter();
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

    takeButton = new ImageButton(this);

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
            takeButton.clearColorFilter();
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
        flipButton = new ImageButton(this);

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
                flipButton.clearColorFilter();
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

    setupLayout();

    main.addView(cameraView);
    main.addView(topToolbar);
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
