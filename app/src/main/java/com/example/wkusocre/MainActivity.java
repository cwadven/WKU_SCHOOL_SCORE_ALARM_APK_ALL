package com.example.wkusocre;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Observer;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.VolumeShaper;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static MainActivity instance;

    EditText username;
    EditText password;
    EditText start_password;
    Button start_btn;
    Button end_btn;
    TextView help_text;

    Drawable drawable;

    String filename = "";
    String filepath = "";

    String str_username;
    String str_password;

    String OTP = "20201228";

    PeriodicWorkRequest periodicWorkRequest;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        instance = this;


        // 워커 적용하기
//        Data data = new Data.Builder()
//                .putInt("number", 10)
//                .build();

//        Constraints constraints = new Constraints.Builder()
//                .setRequiredNetworkType(NetworkType.CONNECTED)
////                .setRequiresCharging(true)
////                .setRequiresDeviceIdle(true)
//        .build();

//        워커 설정
//        OneTimeWorkRequest downloadRequest = new OneTimeWorkRequest.Builder(SampleWorker.class)
//                .setInputData(data)
//                .setConstraints(constraints)
////                .setInitialDelay(5, TimeUnit.HOURS)
//                .addTag("download")
////                .setBackoffCriteria()
//                .build();

//        WorkManager.getInstance(this).enqueue(downloadRequest);

//        워커 정지
//        WorkManager.getInstance(this).cancelWorkById(periodicWorkRequest.getId());



        // 연결 시켜주기
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        start_password = findViewById(R.id.start_password);
        start_btn = findViewById(R.id.start_btn);
        end_btn = findViewById(R.id.end_btn);
        help_text = findViewById(R.id.help_text);

        end_btn.setEnabled(false);

        // 만약 권한이 없을 경우
        if (!isExternalStorageAvailableForRW()){
            start_btn.setEnabled(false);
        }

        // 실행 클릭시
        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String str_username = username.getText().toString();
                String str_password = password.getText().toString();
                String str_start_password = start_password.getText().toString();

                // 워커 정보 전송
                Data data = new Data.Builder()
                        .putInt("number", 10)
                        .putString("str_username", str_username)
                        .putString("str_password", str_password)
                        .build();

                // 워커에 실행할 request 적용
                periodicWorkRequest = new PeriodicWorkRequest.Builder(SampleWorker.class, 15, TimeUnit.MINUTES)
                        .setInputData(data)
//                        .setConstraints(constraints)
                        .addTag("Perioidic")
//                        .setInitialDelay(5, TimeUnit.SECONDS)
                        .build();

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    CreateFolder();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                }

                // 새로운 Thread를 가져와서 값 찾아오기 t.join()으로 끝날 때 까지 기다리기
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Document check_passwd = Jsoup.connect("https://www.imagestory.shop/board/detail/test/425")
                                    .get();
                            Elements passwd = check_passwd.select("div.information p");
                            OTP = passwd.text();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                try {
                    t.start();
                    t.join();
                    t.interrupt();
                } catch (InterruptedException e) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "인터넷에 문제가 있습니다!", Toast.LENGTH_SHORT).show();
                            // thread 에서 버튼 바꾸기
                            start_btn.setEnabled(true);
                            end_btn.setEnabled(false);
                            setUseableEditText(username, true);
                            setUseableEditText(password, true);
                            setUseableEditText(start_password, true);
                        }
                    });
                    e.printStackTrace();
                }

                // 만약 실행 비밀번호가 같을 경우
                if (str_start_password.equals(OTP)){
                    // 알람 적용 했다는 것을 보여주기
                    Toast.makeText(MainActivity.this, "알람을 적용 했습니다!", Toast.LENGTH_SHORT).show();
                    // 15분에 한번 씩 워커 실행
                    WorkManager.getInstance(MainActivity.this).enqueue(periodicWorkRequest);
                    // 버튼 클릭 못하도록
                    setUseableEditText(username, false);
                    setUseableEditText(password, false);
                    setUseableEditText(start_password, false);
                    start_btn.setEnabled(false);
                    end_btn.setEnabled(true);
                } else {
                    Toast.makeText(MainActivity.this, "실행 비밀번호가 틀렸습니다", Toast.LENGTH_SHORT).show();
                    setUseableEditText(username, true);
                    setUseableEditText(password, true);
                    setUseableEditText(start_password, true);
                }

            }

        });

        // 종료 클릭시
        end_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTask();
                start_btn.setEnabled(true);
                end_btn.setEnabled(false);
                setUseableEditText(username, true);
                setUseableEditText(password, true);
                setUseableEditText(start_password, true);
            }
        });

    }

    // 종료 클릭시
    public void stopTask() {
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(periodicWorkRequest.getId()).observe(this,
                new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        Log.d(TAG, "onChanged: WorkStatus:"+workInfo.getState());
                    }
                });
        WorkManager.getInstance(this).cancelAllWork();

        Toast.makeText(MainActivity.this, "실행이 종료 되었습니다!", Toast.LENGTH_SHORT).show();
    }

    // 파일을 쓸 수 있는지 확인
    private boolean isExternalStorageAvailableForRW() {
        String extStorageState = Environment.getExternalStorageState();
        if (extStorageState.equals(Environment.MEDIA_MOUNTED)){
            return true;
        }
        return false;
    }

    // 권한 이용
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permission, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permission, grantResults);

        if (requestCode == 100 && (grantResults.length>0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
            CreateFolder();
        }
    }

    // 실행 하는 순간 폴더 만들기
    private void CreateFolder() {
        // 폴더 만들기
        filename = "score.txt";
        filepath = "wkuScore";

        // 테스트
        File myExternalFile = new File(getExternalFilesDir(filepath), filename);
        if (!myExternalFile.exists()){
            FileOutputStream foos = null;
            try {
                foos = new FileOutputStream(myExternalFile);
                foos.write("ready".getBytes());
                foos.close();
            } catch (FileNotFoundException e){
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setUseableEditText(EditText et, boolean useable) {
        et.setClickable(useable);
        et.setEnabled(useable);
        et.setFocusable(useable);
        et.setFocusableInTouchMode(useable);
        if (useable) {
            drawable = getResources().getDrawable(R.drawable.edit_text_background);
        } else {
            drawable = getResources().getDrawable(R.drawable.edit_text_background_disable);
        }
        et.setBackground(drawable);
    }

    public static MainActivity getInstance() {
        return instance;
    }
}