package org.apache.cordova.camera;

import android.graphics.Bitmap;

public abstract class TakePictureCallback {
  public abstract void onFinished(Bitmap img);
}
