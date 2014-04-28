
package com.rolmex.android.autotest.activity;

import com.rolmex.android.autotest.R;

import com.rolmex.android.autotest.entity.Programe;
import com.rolmex.android.autotest.service.AutoService;
import com.rolmex.android.autotest.utils.ProcessInfo;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class MainActivity extends Activity {
    
    private static final String LOG_TAG = "AutoTest-"+MainActivity.class.getSimpleName();
    
    private static final int TIMEOUT = 20000;
    
    private List<Programe> processList;
    private ProcessInfo processInfo;
    private Intent monitorService;
    private ListView processListView;
    private Button btnTest;
    
    public static boolean isRadioChecked = false;
    private int pid,uid;
    public static String processName,packageName;
    private String settingTempFile;
    private boolean isServiceStop = false;
    private UpdateReceiver receiver;
    
   
    
   
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "MainActivity::onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNewFile();
        processInfo = new ProcessInfo();
        processListView = (ListView)this.findViewById(R.id.process_list);
        btnTest = (Button)this.findViewById(R.id.test_btn);
        btnTest.setOnClickListener(buttonListener);
        
    }
    protected void onStart(){
        Log.d(LOG_TAG, "onStart");
        receiver = new UpdateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.rolmex.android.autotest");
        this.registerReceiver(receiver, filter);
        super.onStart();
    }
    public void onResume(){
        super.onResume();
        Log.d(LOG_TAG, "onResume");
        if(AutoService.isStop){
            btnTest.setText("开始测试");
        }
//        List<Programe> list = processInfo.getRunningProgress(getApplicationContext());
//        Toast.makeText(getApplicationContext(), list.size()+"", Toast.LENGTH_LONG).show();
      processListView.setAdapter(new ListAdapter());
    }
    private View.OnClickListener buttonListener = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            if(v==btnTest){
                monitorService = new Intent();
                monitorService.setClass(MainActivity.this, AutoService.class);
                if("开始测试".equals(btnTest.getText().toString())){
                    if(isRadioChecked){
                        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
                        Log.d(LOG_TAG, packageName);
                        try{
                            startActivity(intent);
                        }catch(NullPointerException e){
                            Toast.makeText(getApplicationContext(), "改程序无法启动", Toast.LENGTH_LONG).show();
                            return;
                        }
                        waitForAppStart(packageName);
                        monitorService.putExtra("processName", processName);
                        monitorService.putExtra("pid", pid);
                        monitorService.putExtra("uid", uid);
                        monitorService.putExtra("packageName", packageName);
                        monitorService.putExtra("settingTempFile", settingTempFile);
                        startService(monitorService);
                        btnTest.setText("停止测试");
                    }else{
                        Toast.makeText(MainActivity.this, "请选择需要测试的应用程序", Toast.LENGTH_LONG).show();
                    }
                }else{
                    btnTest.setText("开始测试");
                    Toast.makeText(MainActivity.this, "测试结果文件：", Toast.LENGTH_LONG).show();
                    stopService(monitorService);
                }
            }
        }
        
    };
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
        menu.add(0,Menu.FIRST,0,"退出").setIcon(android.R.drawable.ic_menu_delete);
        menu.add(0,Menu.FIRST,1,"设置").setIcon(android.R.drawable.ic_menu_directions);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getOrder()){
            case 0:
                showDialog(0);
                break;
            case 1:
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SettingsActivity.class);
                intent.putExtra("settingTempFile", settingTempFile);
                startActivity(intent);
                break;
                
                default:
                    break;
        }
        return false;
    }
    public class UpdateReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            isServiceStop = intent.getExtras().getBoolean("isServiceStop");
            if(isServiceStop){
                btnTest.setText("开始测试");
            }
        }
        
    }
    private void createNewFile(){
        Log.i(LOG_TAG, "create new file to save setting data");
        settingTempFile = getBaseContext().getFilesDir().getPath()+"\\AutoTestSettings.properties";
        Log.i(LOG_TAG, "settingFile = "+settingTempFile);
        File settingFile = new File(settingTempFile);
        if(!settingFile.exists()){
            try{
                settingFile.createNewFile();
                Properties properties = new Properties();
                properties.setProperty("interval", "5");
                properties.setProperty("isfloat", "true");
                properties.setProperty("sender", "");
                properties.setProperty("password", "");
                properties.setProperty("recipients", "");
                properties.setProperty("smtp", "");
                FileOutputStream fos = new FileOutputStream(settingTempFile);
                properties.store(fos,"Setting Data");
                fos.close();
                
            }catch(IOException e){
                Log.d(LOG_TAG, "create new file exception:"+e.getMessage());
            }
        }
           
    }
    private void waitForAppStart(String packageName){
        Log.d(LOG_TAG, "wait for app start");
        boolean isProcessStarted = false;
        long startTime = System.currentTimeMillis();
        while(System.currentTimeMillis()<startTime+TIMEOUT){
            processList = processInfo.getRunningProgress(getBaseContext());
            for(Programe programe:processList){
                if((programe.getPackageName()!=null) && (programe.getPackageName().equals(packageName))){
                    pid = programe.getPid();
                    Log.d(LOG_TAG, "pid:"+pid);
                    uid = programe.getUid();
                    if(pid !=0){
                        isProcessStarted = true;
                        break;
                    }
                }
            }
            if(isProcessStarted){
                break;
            }
        }
    }
    public boolean onKeyDown(int keyCode,KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount()==0){
            showDialog(0);
        }
        return super.onKeyDown(keyCode, event);
    }
    protected Dialog onCreateDialog(int id){
        switch(id){
            case 0:
                return new AlertDialog.Builder(this)
                          .setTitle("确定退出程序？")
                          .setPositiveButton("确定", new OnClickListener() {
                            
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub
                                if(monitorService !=null){
                                    Log.d(LOG_TAG, "stop service");
                                    stopService(monitorService);
                                }
                                Log.d(LOG_TAG, "exit AutoTest");
                                
                                finish();
                                System.exit(0);
                            }
                        })
                        .setNegativeButton("取消", null).create();
                
            default:
                 return null;
        }
    }
    @Override
    public void finish(){
        super.finish();
        
    }
    @Override
    protected void onStop(){
        unregisterReceiver(receiver);
        super.onStop();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
    private class ListAdapter extends BaseAdapter{
        List<Programe> programe;
        int tempPosition =-1;
        
        class ViewHolder{
            TextView txtAppName;
            ImageView imgViAppIcon;
            RadioButton rdoBtnApp;
        }
       public ListAdapter(){
           programe = processInfo.getRunningProgress(getBaseContext());
       }
       
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return programe.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return programe.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            ViewHolder viewHolder;
            final int i = position;
            if(convertView ==null){
                viewHolder = new ViewHolder();
                convertView = MainActivity.this.getLayoutInflater().inflate(R.layout.list_item, null);
                viewHolder.imgViAppIcon = (ImageView)convertView.findViewById(R.id.image);
                viewHolder.txtAppName = (TextView)convertView.findViewById(R.id.rb);
                viewHolder.rdoBtnApp = (RadioButton)convertView.findViewById(R.id.rb);
                
                convertView.setTag(viewHolder);
            }else{
                viewHolder = (ViewHolder)convertView.getTag();
            }
            viewHolder.rdoBtnApp.setId(position);
            viewHolder.rdoBtnApp.setOnCheckedChangeListener(new OnCheckedChangeListener(){

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // TODO Auto-generated method stub
                    if(isChecked){
                        isRadioChecked = true;
                        if(tempPosition !=-1){
                            RadioButton tempButton = (RadioButton)findViewById(tempPosition);
                            if((tempButton !=null)&& (tempPosition !=i)){
                                tempButton.setChecked(false);
                            }
                        }
                        tempPosition = buttonView.getId();
                        packageName = programe.get(tempPosition).getPackageName();
                        processName = programe.get(tempPosition).getProcessName();
                    }
                }
                
            });
            if(tempPosition == position){
                if(!viewHolder.rdoBtnApp.isChecked())
                    viewHolder.rdoBtnApp.setChecked(true);
            }
            Programe pr = (Programe) programe.get(position);
            viewHolder.imgViAppIcon.setImageDrawable(pr.getIcon());
            viewHolder.txtAppName.setText(pr.getProcessName());
            return convertView;
        }
        
    }

}
