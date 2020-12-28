package com.example.wkusocre;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class SampleWorker extends Worker {
    private static final String TAG = "SampleWorker";

    public SampleWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data inputData = getInputData();
        int number = inputData.getInt("number", -1);

        String username = inputData.getString("str_username");
        String password = inputData.getString("str_password");

        String channelId = "channel";
        String channelName = "Channel Name";

        // 새로운 Thread를 가져와서 값 찾아오기 t.join()으로 끝날 때 까지 기다리기
        NotificationManager notifManager
                = (NotificationManager) getApplicationContext().getSystemService  (Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            notifManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(getApplicationContext(), channelId);

        Intent notificationIntent = new Intent(getApplicationContext()

                , MainActivity.class);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int requestID = (int) System.currentTimeMillis();

        PendingIntent pendingIntent
                = PendingIntent.getActivity(getApplicationContext()
                , requestID
                , notificationIntent
                , PendingIntent.FLAG_UPDATE_CURRENT);

        // Notification 뜨는 정보들
        builder.setContentTitle("\uD83D\uDE0E성적 나왔어요~!") // required
                .setContentText("성적이 최신화 되었습니다!")  // required
                .setDefaults(Notification.DEFAULT_ALL) // 알림, 사운드 진동 설정
                .setAutoCancel(true) // 알림 터치시 반응 후 삭제
                .setSmallIcon(R.mipmap.score)
                .setContentIntent(pendingIntent);

        Log.i("Test", "Timer start");

        // Window, Chrome의 User Agent.
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36";

        // 전송할 폼 데이터
        Map<String, String> data = new HashMap<>();
        data.put("userid", username);
        data.put("passwd", password);
        data.put("nextURL", "http://intra.wku.ac.kr/SWupis/V005/loginReturn.jsp");

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                // 크롤링 작업하기
                Connection.Response response = null;
                try {
                    // 로그인 하는 URL


                    // 쿠키 생성

                    // auth.wku에 연결하여 wkuToken 값 가져오기
                    try {
                        // 쿠키가 없을 경우
                        


                        // 폴더 및 파일 만들기
                        String filename = "score.txt";
                        String filepath = "wkuScore";

                        // 성적 파일 읽기
                        File savefile = new File(getApplicationContext().getExternalFilesDir(filepath), filename);

                        // 없을 경우 파일 만들기
                        if (!savefile.exists()) {
                            try {
                                FileOutputStream fos = new FileOutputStream(savefile);
                                fos.write("ready".getBytes());
                                fos.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            // 있을 경우 파일 조회해서 안에 있는 내용과 비교하기
                        } else {
                            try {
                                // 파일 안의 값 읽기
                                FileInputStream fis = new FileInputStream(savefile.getAbsolutePath());
                                BufferedReader bufferReader = new BufferedReader(new InputStreamReader(fis));
                                String pre_score = (String) bufferReader.readLine();
                                String get_score = (String) score_check.text();

                                // 전에 있던 파일의 내용과 다를 경우 즉, 성적이 변했을 경우
                                if (!pre_score.equals(get_score)) {
                                    FileOutputStream fos = new FileOutputStream(savefile);
                                    fos.write(score_check.text().getBytes());
                                    fos.close();

                                    // 다를 경우 Notification 출력! 맨처음에 ready라는 정보가 들어가는데,
                                    // 그게 ready일 경우 새로운 파일 만드는 것 뿐!
                                    // ready가 아니고 다른 글자일 경우 notify정보도 실행 할 수 있도록 설정
                                    if(!pre_score.equals("ready")) {
                                        notifManager.notify(0, builder.build());
                                        // 뜨면 끝내기
                                        fis.close();
                                        MainActivity.getInstance().runOnUiThread(new Runnable() {
                                            public void run() {
                                                Toast.makeText(MainActivity.getInstance(), "다시 실행 해주세요~!", Toast.LENGTH_SHORT).show();
                                                MainActivity.getInstance().start_btn.setEnabled(true);
                                                MainActivity.getInstance().end_btn.setEnabled(false);
                                            }
                                        });
                                        WorkManager.getInstance(MainActivity.getInstance()).cancelAllWork();
                                        MainActivity.getInstance().setUseableEditText(MainActivity.getInstance().username, true);
                                        MainActivity.getInstance().setUseableEditText(MainActivity.getInstance().password, true);
                                        MainActivity.getInstance().setUseableEditText(MainActivity.getInstance().start_password, true);
                                    }

                                }
                                fis.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e) {
                        // thread 에서 toast 띄우기
                        MainActivity.getInstance().runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(MainActivity.getInstance(), "아이디 혹은 비밀번호에 문제가 있습니다!", Toast.LENGTH_SHORT).show();
                                // thread 에서 버튼 바꾸기
                                MainActivity.getInstance().start_btn.setEnabled(true);
                                MainActivity.getInstance().end_btn.setEnabled(false);
                                MainActivity.getInstance().setUseableEditText(MainActivity.getInstance().username, true);
                                MainActivity.getInstance().setUseableEditText(MainActivity.getInstance().password, true);
                                MainActivity.getInstance().setUseableEditText(MainActivity.getInstance().start_password, true);
                            }
                        });
                        WorkManager.getInstance(MainActivity.getInstance()).cancelAllWork();
                        e.printStackTrace();
                    }

                } catch (IOException e) {
                    // thread 에서 toast 띄우기
                    MainActivity.getInstance().runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.getInstance(), "인터넷에 문제가 있어 보입니다!", Toast.LENGTH_SHORT).show();
                            // thread 에서 버튼 바꾸기
                            MainActivity.getInstance().start_btn.setEnabled(true);
                            MainActivity.getInstance().end_btn.setEnabled(false);
                            MainActivity.getInstance().setUseableEditText(MainActivity.getInstance().username, true);
                            MainActivity.getInstance().setUseableEditText(MainActivity.getInstance().password, true);
                            MainActivity.getInstance().setUseableEditText(MainActivity.getInstance().start_password, true);
                        }
                    });
                    WorkManager.getInstance(MainActivity.getInstance()).cancelAllWork();
                    e.printStackTrace();
                }
            }
        });
        // 스레드 끝나면 만료시키기 위해서
        t2.start();
        try {
            t2.join();
        } catch (InterruptedException e) {
            MainActivity.getInstance().runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.getInstance(), "인터넷에 문제가 있어 보입니다!", Toast.LENGTH_SHORT).show();
                    // thread 에서 버튼 바꾸기
                    MainActivity.getInstance().start_btn.setEnabled(true);
                    MainActivity.getInstance().end_btn.setEnabled(false);
                    MainActivity.getInstance().setUseableEditText(MainActivity.getInstance().username, true);
                    MainActivity.getInstance().setUseableEditText(MainActivity.getInstance().password, true);
                    MainActivity.getInstance().setUseableEditText(MainActivity.getInstance().start_password, true);
                }
            });
            WorkManager.getInstance(MainActivity.getInstance()).cancelAllWork();
            e.printStackTrace();
        }
        t2.interrupt();

        return Result.success();
    }
}
