package com.codewithkael.firebasevideocall.webrtc;

import static com.serenegiant.usb.UVCCamera.DEFAULT_PREVIEW_FPS;
import static com.serenegiant.usb.UVCCamera.DEFAULT_PREVIEW_FRAME_FORMAT;
import static com.serenegiant.usb.UVCCamera.DEFAULT_PREVIEW_HEIGHT;
import static com.serenegiant.usb.UVCCamera.DEFAULT_PREVIEW_WIDTH;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.hardware.usb.UsbDevice;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
/*
import com.jiangdg.ausbc.callback.IPreviewDataCallBack;
import com.jiangdg.ausbc.camera.CameraUVC;
import com.jiangdg.ausbc.camera.CameraUvcStrategy;
import com.jiangdg.ausbc.camera.bean.CameraRequest;
import com.jiangdg.ausbc.render.env.RotateType;*/

import com.herohan.uvcapp.ICameraHelper;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usb.UVCParam;

import org.webrtc.CameraVideoCapturer;
import org.webrtc.CapturerObserver;
import org.webrtc.EglRenderer;
import org.webrtc.NV21Buffer;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class UvcCapturer implements VideoCapturer, CameraVideoCapturer, USBMonitor.OnDeviceConnectListener, IFrameCallback {
    private static final String TAG = "===>>UvcCapturer";
    private Context context;

    private SurfaceViewRenderer svVideoRender;
    private SurfaceTextureHelper surfaceTextureHelper;
    private CapturerObserver capturerObserver;
    private Executor executor = Executors.newSingleThreadExecutor();
    private int UVC_PREVIEW_WIDTH = 640; //UVCCamera.DEFAULT_PREVIEW_WIDTH
    private int UVC_PREVIEW_HEIGHT = 480;// UVCCamera.DEFAULT_PREVIEW_HEIGHT
    private int UVC_PREVIEW_FPS = 30;
    private UVCCamera camera;
    private USBMonitor monitor;
    private boolean statusDOpen;

//    CameraUvcStrategy mUvcStrategy; // From Jiang Dongguo's AUSBC library

    public UvcCapturer(Context context, SurfaceViewRenderer svVideoRender) {
        this.context = context;
        this.svVideoRender = svVideoRender;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                monitor = new USBMonitor(context, ContextCompat.RECEIVER_EXPORTED, UvcCapturer.this);
                monitor.register();

            }
        });

    }


    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context context, CapturerObserver capturerObserver) {
        this.context = context;
        this.surfaceTextureHelper = surfaceTextureHelper;
        this.capturerObserver = capturerObserver;
    }

    @Override
    public void startCapture(int w, int h, int fps) {
//        if(mUvcStrategy != null) {
//          UVC_PREVIEW_WIDTH = w;
//          UVC_PREVIEW_HEIGHT = h;
//          UVC_PREVIEW_FPS = fps;
//          mUvcStrategy.addPreviewDataCallBack(new IPreviewDataCallBack() {
//              @Override
//              public void onPreviewData(@Nullable byte[] bytes, int i, int i1, @NonNull DataFormat dataFormat) {
//                  NV21Buffer nv21Buffer = new NV21Buffer(bytes,UVC_PREVIEW_WIDTH,UVC_PREVIEW_HEIGHT, null);
//                  VideoFrame frame = new VideoFrame(nv21Buffer, 0, System.nanoTime());
//                  capturerObserver.onFrameCaptured(frame);
//              }
//          });
//          mUvcStrategy.startPreview(getCameraRequest(), svVideoRender.getHolder());
//      }
        Log.d(TAG, "startCapture: ");
        capturerObserver.onCapturerStarted(true);
      svVideoRender.addFrameListener(new EglRenderer.FrameListener() {
          @Override
          public void onFrame(Bitmap bitmap) {
              Log.d(TAG, "onFrame: ====");
          }
      },0);
    }

    @Override
    public void stopCapture() throws InterruptedException {
        if (camera != null) {
            camera.stopPreview();
            camera.close();
            camera.destroy();
            svVideoRender.release();
            capturerObserver.onCapturerStarted(false);
        }
    }

    @Override
    public void changeCaptureFormat(int i, int i1, int i2) {
        Log.d(TAG, "changeCaptureFormat: i = [" + i + "], i1 = [" + i1 + "], i2 = [" + i2 + "]");
    }

    @Override
    public void dispose() {
        Log.d(TAG, "dispose() called");
        monitor.unregister();
        monitor.destroy();
        surfaceTextureHelper.dispose();
    }

    @Override
    public boolean isScreencast() {
        return false;
    }


    @Override
    public void switchCamera(CameraSwitchHandler cameraSwitchHandler) {
        Log.d(TAG, "switchCamera: ");
    }

    @Override
    public void switchCamera(CameraSwitchHandler cameraSwitchHandler, String s) {
        Log.d(TAG, "switchCamera() called with: cameraSwitchHandler = [" + cameraSwitchHandler + "], s = [" + s + "]");
    }




    @Override
    public void onAttach(UsbDevice device) {
        Log.d(TAG, "onAttach: ");
        monitor.requestPermission(device);
    }



    @Override
    public void onDetach(UsbDevice device) {
        Log.d(TAG, "onDetach: ");
        statusDOpen = false;
    }
    @Override
    public void onFrame(ByteBuffer frame) {
//[Size(2592x1944@15,type:7), Size(2048x1536@15,type:7), Size(1600x1200@15,type:7), Size(1920x1080@30,type:7), Size(1280x960@30,type:7), Size(1280x720@30,type:7), Size(1024x768@30,type:7), Size(960x720@30,type:7), Size(800x600@30,type:7), Size(640x480@30,type:7), Size(320x240@30,type:7), Size(2592x1944@2,type:5), Size(2048x1536@3,type:5), Size(1600x1200@5,type:5), Size(1920x1080@5,type:5), Size(1280x960@5,type:5), Size(1280x720@10,type:5), Size(1024x768@15,type:5), Size(960x720@15,type:5), Size(800x600@20,type:5), Size(640x480@30,type:5), Size(320x240@30,type:5)]
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: Frame:--->");
                byte[] imageArray = new byte[frame.remaining()];
                frame.get(imageArray);
                long imageTime = System.nanoTime();
                NV21Buffer nV21Buffer =
                        new NV21Buffer(imageArray, UVC_PREVIEW_WIDTH, UVC_PREVIEW_HEIGHT, new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "run: ===================>>>>>>>>>>>>w=640, h=480,>>>>>>>>>>>>>>>>>>>>>>");
                            }
                        });
                VideoFrame videoFrame = new VideoFrame(nV21Buffer, 0, imageTime);
                capturerObserver.onFrameCaptured(videoFrame);
                svVideoRender.onFrame(videoFrame);


            }
        });
    }

    private void onSetPreviewMJEG() {
        List<Size> mjpeg_camera_sizes = camera.getSupportedSizeList();
        Log.d(TAG, "onDeviceOpen: " + mjpeg_camera_sizes);
        // Pick the size that is closest to our required resolution
        int required_width = UVC_PREVIEW_WIDTH;  //640
        int required_height = UVC_PREVIEW_HEIGHT; //480
        int required_area = required_width * required_height;

        int preview_width = 0;
        int preview_height = 0;
        int error = Integer.MAX_VALUE; // trying to get this as small as possible

        for (Size s : mjpeg_camera_sizes) {
            // calculate the area for each camera size
            int s_area = s.width * s.height;
            // calculate the difference between this size and the target size
            int abs_error = Math.abs(s_area - required_area);
            // check if the abs_error is smaller than what we have already
            // then use the new size
            if (abs_error < error) {
                preview_width = s.width;
                preview_height = s.height;
                error = abs_error;
            }
        }
        Log.d(TAG, "run:MJPEG ===> height: " + preview_width + " width: " + preview_height);

        try {

            camera.setPreviewSize(preview_width, preview_height, UVCCamera.FRAME_FORMAT_MJPEG);
        } catch (final IllegalArgumentException e) {

        }
    }

    private void onSetPreviewYUV() {
        int required_width = 640;
        int required_height = 480;
        int required_area = required_width * required_height;
        // find closest matching size
        // Pick the size that is closest to our required resolution
        int yuv_preview_width = 0;
        int yuv_preview_height = 0;
        int yuv_error = Integer.MAX_VALUE; // trying to get this as small as possible
        List<Size> yuv_camera_sizes = camera.getSupportedSizeList();//UVCCamera.FRAME_FORMAT_YUYV
        for (Size s : yuv_camera_sizes) {
            // calculate the area for each camera size
            int s_area = s.width * s.height;
            // calculate the difference between this size and the target size
            int abs_error = Math.abs(s_area - required_area);
            // check if the abs_error is smaller than what we have already
            // then use the new size
            if (abs_error < yuv_error) {
                yuv_preview_width = s.width;
                yuv_preview_height = s.height;
                yuv_error = abs_error;
            }
        }
        Log.d(TAG, "run:YUV===> height: " + yuv_preview_height + " width: " + yuv_preview_width);
        camera.setPreviewSize(yuv_preview_width, yuv_preview_height, UVCCamera.FRAME_FORMAT_YUYV);
    }

    @Override
    public void onDeviceOpen(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
        Log.d(TAG, "onDeviceOpen() called with: device = [" + device + "], ctrlBlock = [" + ctrlBlock + "], createNew = [" + createNew + "]");

        if (!statusDOpen) {
            statusDOpen = true;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    camera = new UVCCamera(new UVCParam(new Size(DEFAULT_PREVIEW_FRAME_FORMAT, DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT, DEFAULT_PREVIEW_FPS, new ArrayList<>(DEFAULT_PREVIEW_FPS)),0));
                    camera.open(ctrlBlock);
                    try {
                       onSetPreviewMJEG();
                         //onSetPreviewYUV();
                    //camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG);
                    } catch (final IllegalArgumentException e) {
                        try {
                            Log.d(TAG, "run: After exception================================================================");
                            camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, DEFAULT_PREVIEW_FRAME_FORMAT);
                        } catch (final IllegalArgumentException e1) {
                            Log.d(TAG, "run: Error =>" + e1.getMessage());
                    camera.destroy();
//                        camera = null;

                        }
                    }
                    if (camera != null) {
                        Log.d(TAG, "run: setPreviewDisplay================================");
                        camera.setPreviewDisplay(svVideoRender.getHolder());
                        camera.setFrameCallback(UvcCapturer.this, UVCCamera.PIXEL_FORMAT_NV21);
                        camera.startPreview();

                    } else {
                        Log.d(TAG, "run: nulll");
                    }
                }
            });
        }
    }

    @Override
    public void onDeviceClose(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
        Log.d(TAG, "onDeviceClose: ");
        statusDOpen = false;
    }

    @Override
    public void onCancel(UsbDevice device) {
        Log.d(TAG, "onCancel: ");
    }

    @Override
    public void onError(UsbDevice device, USBMonitor.USBException e) {
        Log.d(TAG, "onError: ===========================");
        USBMonitor.OnDeviceConnectListener.super.onError(device, e);
    }

}
