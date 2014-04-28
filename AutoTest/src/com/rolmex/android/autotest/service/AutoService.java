package com.rolmex.android.autotest.service;

import com.rolmex.android.autotest.R;
import com.rolmex.android.autotest.utils.CpuInfo;
import com.rolmex.android.autotest.utils.EncryptData;
import com.rolmex.android.autotest.utils.MailSender;
import com.rolmex.android.autotest.utils.MemoryInfo;
import com.rolmex.android.autotest.entity.MyApplication;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;

public class AutoService extends Service{
    
    private final static String LOG_TAG = "AutoTest-"+
    AutoService.class.getSimpleName();
    private WindowManager windowManager = null;
    private WindowManager.LayoutParams wmParams = null;
    private View viFloatingWindow;
    private float mTouchStartX;
    private float mTouchStartY;
    private float startX;
    private float startY;
    private float x;
    private float y;
    private TextView txtTotalMem;
    private TextView txtUnusedMem;
    private TextView txtTraffic;
    private ImageView imgViIcon;
    private TextView btnWifi;
    private int delaytime;
    private DecimalFormat fomart;
    private MemoryInfo memoryInfo;
    private WifiManager wifiManager;
    private Handler handler = new Handler();
    private CpuInfo cpuInfo;
    private String time;
    private boolean isFloating;
    private String processName,packageName,settingTempFile;
    private int pid,uid;
    private boolean isServiceStop = false;
    private String sender,password,recipients,smtp;
    private String[] receivers;
    private EncryptData des;
    
    public static BufferedWriter bw;
    public static FileOutputStream  out;
    public static OutputStreamWriter osw; 
    public static String resultFilePath;
    public static boolean isStop = false;
    
    public void onCreate(){
        Log.i(LOG_TAG, "onCreate");
        super.onCreate();
        isServiceStop = false;
        isStop = false;
        memoryInfo = new MemoryInfo();
        fomart = new DecimalFormat();
        fomart.setMaximumFractionDigits(2);
        fomart.setMinimumFractionDigits(0);
        des = new EncryptData("autotest");
        
    }
    public void onStart(Intent intent,int startId){
        Log.i(LOG_TAG, "onStart");
        super.onStart(intent, startId);
        pid = intent.getExtras().getInt("pid");
        uid = intent.getExtras().getInt("uid");
        processName = intent.getExtras().getString("processName");
        packageName = intent.getExtras().getString("packageName");
        Log.i(LOG_TAG, packageName);
        settingTempFile = intent.getExtras().getString("settingTempFile");
        Log.i(LOG_TAG, settingTempFile);
        cpuInfo = new CpuInfo(getBaseContext(),pid,Integer.toString(uid));
        readSettingInfo(intent);
        delaytime = Integer.parseInt(time)*1000;
        if(isFloating){
            viFloatingWindow = LayoutInflater.from(this).inflate(R.layout.floating, null);
            txtUnusedMem = (TextView)viFloatingWindow.findViewById(R.id.memunused);
            txtTotalMem = (TextView) viFloatingWindow.findViewById(R.id.memtotal);
            txtTraffic = (TextView) viFloatingWindow.findViewById(R.id.traffic);
            btnWifi = (TextView) viFloatingWindow.findViewById(R.id.wifi);
            Log.i(LOG_TAG, "OK");
            
            wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
            if(wifiManager.isWifiEnabled()){
                btnWifi.setText(R.string.closewifi);
            }else{
                btnWifi.setText(R.string.openwifi);
            }
            txtUnusedMem.setText("计算总，请稍后");
            Log.i(LOG_TAG, "OK1");

            imgViIcon = (ImageView)viFloatingWindow.findViewById(R.id.img2);
            Log.i(LOG_TAG, "OK2");
            imgViIcon.setVisibility(View.GONE);
            createFloatingWindow();
            
        }
        createResultCsv();
        handler.postDelayed(task,1000);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
    private void createFloatingWindow(){
        SharedPreferences shared = getSharedPreferences("float_flag",Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = shared.edit();
        editor.putInt("float", 1);
        editor.commit();
        windowManager = (WindowManager)getApplicationContext().getSystemService("window");
        wmParams = ((MyApplication) getApplication()).getMywmParams();
        wmParams.type = 2002;
        wmParams.flags |=8;
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        wmParams.x = 0;
        wmParams.y = 0;
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.format = 1;
        windowManager.addView(viFloatingWindow, wmParams);
        viFloatingWindow.setOnTouchListener(new OnTouchListener(){

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                x = event.getRawX();
                y = event.getRawY() -25;
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        startX = x;
                        startY = y;
                        mTouchStartX = event.getX();
                        mTouchStartY = event.getY();
                        Log.d("startP", "startX"+mTouchStartX+"====startY"+mTouchStartY);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        updateViewPosition();
                        break;
                    case MotionEvent.ACTION_UP:
                        updateViewPosition();
                        showImg();
                        mTouchStartX = mTouchStartY =0;
                }
                return true;
            }
            
        });
        btnWifi.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                try{
                    btnWifi = (Button)viFloatingWindow.findViewById(R.id.wifi);
                    String buttonText = (String)btnWifi.getText();
                    String wifiText = getResources().getString(R.string.openwifi);
                    if(buttonText.equals(wifiText)){
                        wifiManager.setWifiEnabled(true);
                        btnWifi.setText(R.string.closewifi);
                    }else{
                        wifiManager.setWifiEnabled(false);
                        btnWifi.setText(R.string.openwifi);
                    }
                }catch(Exception e){
                    Toast.makeText(viFloatingWindow.getContext(), "操作wifi失败", Toast.LENGTH_LONG).show();
                    Log.e(LOG_TAG, e.toString());
                }
            }
            
        });
    }
    /**
     * show the image
     */
    private void showImg(){
        if(Math.abs(x-startX)<1.5 && Math.abs(y - startY)<1.5 && !imgViIcon.isShown()){
            imgViIcon.setVisibility(View.VISIBLE);
        }else if(imgViIcon.isShown()){
            imgViIcon.setVisibility(View.GONE);
        }
    }
    private Runnable task = new Runnable(){
         public void run(){
             if(!isServiceStop){
                 dataRefresh();
                 handler.postDelayed(this, delaytime);
                 if(isFloating)
                     windowManager.updateViewLayout(viFloatingWindow, wmParams);
             }else{
                 Intent intent  = new Intent();
                 intent.putExtra("isServiceStop", true);
                 intent.setAction("com.rolmex.action.autoService");
                 sendBroadcast(intent);
                 stopSelf();
             }
         }
    };
    private void dataRefresh(){
        int pidMemory = memoryInfo.getPidMemorySize(pid,getBaseContext());
        long freeMemory = memoryInfo.getFreeMemorySize(getBaseContext());
        String freeMemoryKb = fomart.format((double) freeMemory/1024);
        String processMemory = fomart.format((double) pidMemory/1024);
        ArrayList<String> processInfo = cpuInfo.getCpuRatioInfo();
        if(isFloating){
            String processCpuRatio = "0";
            String totalCpuRatio = "0";
            String trafficSize = "0";
            int tempTraffic =0;
            double trafficMb =0;
            boolean isMb = false;
            if(!processInfo.isEmpty()){
                processCpuRatio = processInfo.get(0);
                totalCpuRatio = processInfo.get(1);
                trafficSize = processInfo.get(2);
                if("".equals(trafficSize) && !("-1".equals(trafficSize))){
                    tempTraffic = Integer.parseInt(trafficSize);
                    if(tempTraffic >1024){
                        isMb = true;
                        trafficMb = (double) tempTraffic/1024;
                    }
                }
            }
            if("0".equals(processMemory) && "0.00".equals(processCpuRatio)){
                closeOpenedStream();
                isServiceStop = true;
                return;
            }
            if(processCpuRatio != null && totalCpuRatio !=null){
                txtUnusedMem.setText("占用内存:"+processMemory+"MB"+",机器剩余:"
                 +freeMemoryKb+"MB");
                txtTotalMem.setText("占用CPU:"+processCpuRatio+"%"+
                  ",总体CPU:"+totalCpuRatio+"%");
                if("-1".equals(trafficSize)){
                    txtTraffic.setText("本程序或设备部支持流量统计");
                    
                }else if(isMb)
                    txtTraffic.setText("消耗流量："+fomart.format(trafficMb)+"MB");
                else
                    txtTraffic.setText("消耗流量："+trafficSize+"KB");
            }
        }
    }
    private void updateViewPosition(){
        wmParams.x = (int)(x-mTouchStartX);
        wmParams.y = (int)(y-mTouchStartY);
        windowManager.updateViewLayout(viFloatingWindow, wmParams);
    }
    public static void closeOpenedStream(){
        try{
            if(bw!=null)
                bw.close();
            if(osw!=null)
                osw.close();
            if(out !=null)
                out.close();
        }catch(Exception e){
            Log.d(LOG_TAG, e.getMessage());
        }
    }
    private void createResultCsv(){
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String mDateTime;
        if((Build.MODEL.equals("sdk"))||(Build.MODEL.equals("google_sdk")))
            mDateTime = formatter.format(cal.getTime().getTime()+8*60*60*1000);
        else 
            mDateTime = formatter.format(cal.getTime().getTime());
        if(android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
            resultFilePath = android.os.Environment.getExternalStorageDirectory()+File.separator
                    +"AutoService_TestResult_"+mDateTime+".csv";
        }
        else{
            resultFilePath = getBaseContext().getFilesDir().getPath()
                    +File.separator +"AutoService_TestResult_"+mDateTime+".csv";
        }
        try{
            File resultFile = new File(resultFilePath);
            resultFile.createNewFile();
            out = new FileOutputStream(resultFile);
            osw = new OutputStreamWriter(out,"GBK");
            bw = new BufferedWriter(osw);
            long totalMemorySize = memoryInfo.getTotalMemory();
            String totalMemory = fomart.format((double) totalMemorySize/1024);
            bw.write("指定应用的CPU内存监控情况\r\n"+"应用包名：，"+packageName+"\r\n"
                    +"应用名称："+processName+"\r\n"+"应用PID:,"+pid+"\r\n"+"机器内存大小（MB）:,"+totalMemory+"MB\r\n"
                    +"机器CPU型号：，"+cpuInfo.getCpuName()+"\r\n"
                    +"机器android系统版本：，"+memoryInfo.getSDKVersion()+"\r\n"
                    +"手机型号：，"+memoryInfo.getPhoneType()+"\r\n"+"UID:,"
                    +uid+"\r\n");
            bw.write("时间"+","+"应用占用内存PSS（MB）"+","+"应用占用内存比（%）"+","
                    +"机器生育内存（MB)"+","+"应用占用CPU率（%）"+","+"CPU总使用率（%）"
                    +","+"流量（KB）:"+"\r\n");
        }catch(IOException e){
            Log.e(LOG_TAG, e.getMessage());
        }
    }
    private void readSettingInfo(Intent intent){
        try{
            Properties properties = new Properties();
            properties.load(new FileInputStream(settingTempFile));
            String interval = properties.getProperty("interval").trim();
            isFloating = "true".equals(properties.getProperty("isfloat").trim())?true:false;
            sender = properties.getProperty("sender").trim();
            password = properties.getProperty("password").trim();
            time = "".equals(interval)?"5":interval;
            recipients = properties.getProperty("recipients");
            receivers = recipients.split("\\s+");
            smtp = properties.getProperty("smtp");
        }catch(IOException e){
            time = "5";
            isFloating = true;
            Log.e(LOG_TAG, e.getMessage());
        }
    }
    public void onDestroy(){
        Log.i(LOG_TAG,"onDestroy");
        if(windowManager !=null)
            windowManager.removeView(viFloatingWindow);
        handler.removeCallbacks(task);
        closeOpenedStream();
        isStop = true;
        boolean isSendSuccessfully = false;
//        try{
//            isSendSuccessfully = MailSender.sendTextMail(sender,des.decrypt(password),smtp,
//                    "AutoService performance Test Report","see attachment",resultFilePath,receivers);
//            
//        }catch(Exception e){
//            isSendSuccessfully = false;
//          
//        }
        if(isSendSuccessfully){
            Toast.makeText(this, "测试结果报表已经发送至邮箱:"+recipients,Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(this, "测试结果未成功发送至邮箱，结果存在："+AutoService.resultFilePath,Toast.LENGTH_LONG).show();
            Log.i(LOG_TAG, AutoService.resultFilePath);
        }
        super.onDestroy();
    }

}
