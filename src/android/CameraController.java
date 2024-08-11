package org.apache.cordova.camera;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CameraController {

  private final String TAG = this.getClass().getSimpleName();
  private CameraManager cameraManager;
  private SurfaceView cameraPreview;
  private CameraDeviceCallback cameraDeviceCallback;
  private Handler handler;
  private Display display;
  private boolean mFlashSupported;
  private int mFlashMode = 0;

  private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
  static {
    ORIENTATIONS.append(Surface.ROTATION_0, 90);
    ORIENTATIONS.append(Surface.ROTATION_90, 0);
    ORIENTATIONS.append(Surface.ROTATION_180, 270);
    ORIENTATIONS.append(Surface.ROTATION_270, 180);
  }

  public CameraController(CameraManager cameraManager, SurfaceView cameraPreview, Display display){
    this.cameraManager = cameraManager;
    this.cameraPreview = cameraPreview;
    this.display = display;
  }

  @SuppressLint("MissingPermission")
  public void startCameraPreview() {
    Log.i(TAG, "Starting Camera Preview");
    handler = new Handler();
    cameraDeviceCallback = new CameraDeviceCallback(cameraPreview, handler);

    //we want to use the backfacing camera
    String backfacingId = getBackfacingCameraId(cameraManager);
    if (backfacingId == null) {
      Log.e(TAG, "Can not open Camera because no backfacing Camera was found");
      return;
    }

    //we have a backfacing camera, so we can start the preview
    try {
      cameraManager.openCamera(backfacingId, cameraDeviceCallback, handler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  @SuppressLint("MissingPermission")
  public boolean switchCameras() {
    final CameraDevice cameraDevice = cameraDeviceCallback.getCameraDevice();
    String cameraId = cameraDevice.getId();

    try {
      String[] ids = this.cameraManager.getCameraIdList();
      for (int i = 0; i < ids.length; i++) {
        if (ids[i] != cameraId) {
          try {
            cameraDevice.close();

            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(ids[i]);
            // Check if the flash is supported.
            Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            mFlashSupported = available == null ? false : available;

            cameraManager.openCamera(ids[i], cameraDeviceCallback, handler);
          } catch (CameraAccessException e) {
            e.printStackTrace();
          }
          return mFlashSupported;
        }
      }
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }

    return false;
  }

  private String getBackfacingCameraId(CameraManager cameraManager){
    try {
      String[] ids = cameraManager.getCameraIdList();
      for (int i = 0; i < ids.length; i++) {
        Log.i(TAG, "Found Camera ID: " + ids[i]);
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(ids[i]);
        int cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING);
        // Check if the flash is supported.
        Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        mFlashSupported = available == null ? false : available;

        if (cameraDirection == CameraCharacteristics.LENS_FACING_BACK) {
          Log.i(TAG, "Found back facing camera");
          return ids[i];
        }
      }
      return null;
    }
    catch(CameraAccessException ce){
      ce.printStackTrace();
      return null;
    }
  }

  private boolean mAutoFocusSupported = false;

  protected void takePicture(String imageFilePath, TakePictureCallback callback) {
    final CameraDevice cameraDevice = cameraDeviceCallback.getCameraDevice();
    if(null == cameraDevice) {
      Log.e(TAG, "cameraDevice is null");
      return;
    }
    CameraManager manager = this.cameraManager;

    try {
      CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
      Size[] jpegSizes = null;
      if (characteristics != null) {
        jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);

        int[] afAvailableModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);

        if (afAvailableModes.length == 0 || (afAvailableModes.length == 1
          && afAvailableModes[0] == CameraMetadata.CONTROL_AF_MODE_OFF)) {
          mAutoFocusSupported = false;
        } else {
          mAutoFocusSupported = true;
        }
      }

      int width = 640;
      int height = 480;
      if (jpegSizes != null && 0 < jpegSizes.length) {
        width = jpegSizes[0].getWidth();
        height = jpegSizes[0].getHeight();
      }
      final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
      List<Surface> outputSurfaces = new ArrayList<Surface>(2);
      outputSurfaces.add(reader.getSurface());
      outputSurfaces.add(cameraPreview.getHolder().getSurface());
      CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
      captureBuilder.addTarget(reader.getSurface());

      Log.e(TAG, "mFlashMode: " + mAutoFocusSupported);
      Log.e(TAG, "mFlashMode: " + mFlashMode);

      if (mAutoFocusSupported) {
        // Use the same AE and AF modes as the preview.
        captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        // This is how to tell the camera to lock focus.
        captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
      }

      if (mFlashSupported) {
        switch (mFlashMode) {
          case 0://FLASH_AUTO
//            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
//            captureBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH);
            captureBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
            captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT);
            break;
          case 1://FLASH_ON
//            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
//            captureBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            captureBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
            captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT);
            break;
          case -1://FLASH_OFF
//            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
//            captureBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            captureBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
            captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT);
            break;
        }
      }

      int rotation = display.getRotation();
      captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
      final File file = new File(imageFilePath);

      final Bitmap[] bitmap = {null};
      ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
          Image image = null;
          try {
            image = reader.acquireLatestImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);

            bitmap[0] = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap[0] == null) {
              Log.e(TAG, "bitmap is null");
            }

            Log.e(TAG, "File path: " + file.getAbsolutePath());
            Log.e(TAG, "File length: " + Long.toString(file.length()));

            try {
              CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
              int cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING);
              if (cameraDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                float[] mirrorY = { -1, 0, 0, 0, 1, 0, 0, 0, 1};
                Matrix rotate = new Matrix();
                Matrix matrixMirrorY = new Matrix();
                matrixMirrorY.setValues(mirrorY);

                rotate.postConcat(matrixMirrorY);
                rotate.preRotate(180);

                final Bitmap rImg = Bitmap.createBitmap(bitmap[0], 0, 0,
                  bitmap[0].getWidth(), bitmap[0].getHeight(), rotate, true);

                try (FileOutputStream out = new FileOutputStream(file)) {
                  rImg.compress(Bitmap.CompressFormat.JPEG, 100, out);
                } catch (IOException e) {
                  e.printStackTrace();
                }

                callback.onFinished(rImg);
                return;
              }
            }
            catch(CameraAccessException ce){
              ce.printStackTrace();
            }
            save(bytes);
            callback.onFinished(bitmap[0]);
          } catch (FileNotFoundException e) {
            e.printStackTrace();
          } catch (IOException e) {
            e.printStackTrace();
          } finally {
            if (image != null) {
              image.close();
            }
          }
        }

        private void save(byte[] bytes) throws IOException {
          OutputStream output = null;
          try {
            output = new FileOutputStream(file);
            output.write(bytes);
          } finally {
            if (null != output) {
              output.close();
            }
          }
        }
      };
      reader.setOnImageAvailableListener(readerListener, handler);
      final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
          super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
          super.onCaptureCompleted(session, request, result);
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
          super.onCaptureFailed(session, request, failure);
          Log.e("onCaptureFailed", "onCaptureFailed");
        }
      };

      cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
          try {
            session.capture(captureBuilder.build(), captureListener, handler);

          } catch (CameraAccessException e) {
            e.printStackTrace();
          }
        }
        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
          Log.e("onConfigureFailed", "onConfigureFailed");
        }
      }, handler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  public boolean isFlashSupported() {
    return mFlashSupported;
  }

  public void setFlash(int flashMode) {
    mFlashMode = flashMode;
  }
}
