package com.example.myapplication;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d("TAG", "OpenCv not loaded.");
        } else {
            Log.d("TAG", "OpenCv loaded.");
        }
    }

    JavaCameraView cameraView;
    CascadeClassifier detector;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference access = database.getReference("Access");
    SimpleDateFormat date_formatter = new SimpleDateFormat("MM-dd-yyyy");
    SimpleDateFormat time_formatter = new SimpleDateFormat("hh00");
    Date time = new Date();
    String dateString = date_formatter.format(time);
//    AsyncHttpClient client;
//    Handler mainHandler = new Handler(Looper.getMainLooper());
//    String url = "http://192.168.4.1/";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

//        client = new AsyncHttpClient();
        initClassifier();
        cameraView = findViewById(R.id.cameraView);
        cameraView.setCameraIndex(1);   // 0 for rear; 1 for front
        cameraView.setCvCameraViewListener(this);
        cameraView.enableView();
        cameraView.enableFpsMeter();
        cameraView.setMaxFrameSize(320, 240);
    }

    private void initClassifier() {
        try {
            InputStream is = getResources()
                    .openRawResource(R.raw.cascade);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "cascade.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            detector = new CascadeClassifier(cascadeFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.disableView();
        cameraView.disableFpsMeter();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    int counter = 0;

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat image = inputFrame.rgba();
        Mat grayImage = new Mat();
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_RGB2GRAY);

        MatOfRect objVectors = new MatOfRect();
        detector.detectMultiScale(grayImage, objVectors);
        if (objVectors.toArray().length == 0) {
            return image;
        }
        final Rect rect = objVectors.toArray()[0];
        int x = rect.x;
        int y = rect.y;

        counter++;

        if (counter >= 40) {
            final String timeString = time_formatter.format(time);

            access.child(dateString).child(timeString).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Map<String, Object> map = new HashMap<>();
                        map.put(dateString, Integer.parseInt(dataSnapshot.getValue().toString()) + 1);
                        access.child(dateString).child(timeString).updateChildren(map);
                    } else {
                        access.child(dateString).child(timeString).setValue(1);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            counter = 0;
        }

//        final String xs = Integer.toString(x);
//        final String ys = Integer.toString(y);
//
//        Runnable myRunnable = new Runnable() {
//            @Override
//            public void run() {
//                client.get(url + xs + "/" + ys, new AsyncHttpResponseHandler() {
//                    @Override
//                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
//
//                    }
//
//                    @Override
//                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
//
//                    }
//                });
//            }
//        };
//        mainHandler.post(myRunnable);



        Imgproc.rectangle(image, new Point(x, y),
                new Point(x + rect.width, y + rect.height),
                new Scalar(0, 255, 0), 3);
        return image;

    }

}