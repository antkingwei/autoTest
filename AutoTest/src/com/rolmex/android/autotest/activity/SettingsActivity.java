package com.rolmex.android.autotest.activity;

import com.rolmex.android.autotest.R;
import com.rolmex.android.autotest.utils.EncryptData;
import com.rolmex.android.autotest.utils.UIHelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsActivity extends Activity{
    
    private static final String LOG_TAG = "AutoTest-"
            +SettingsActivity.class.getSimpleName();
    
    private CheckBox chkFloat;
    private EditText edtTime;
    private EditText edtRecipients;
    private EditText edtSender;
    private EditText edtPassword;
    private EditText edtSmtp;
    private String time,sender;
    private String prePassword,curPassword;
    private String settingTempFile;
    private String recipients,smtp;
    private String[] receivers;
    EncryptData des;
    
    public void onCreate(Bundle savedInstanceState){
        Log.i(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        
        des = new EncryptData("autotest");
        Intent intent = this.getIntent();
        settingTempFile = intent.getStringExtra("settingTempFile");
        
        chkFloat = (CheckBox) this.findViewById(R.id.floating);
        edtTime = (EditText) this.findViewById(R.id.time);
        edtSender = (EditText) this.findViewById(R.id.sender);
        edtPassword = (EditText) this.findViewById(R.id.password);
        edtRecipients = (EditText) this.findViewById(R.id.recipients);
        edtSmtp = (EditText) this.findViewById(R.id.smtp);
        
        Button btnSave = (Button) this.findViewById(R.id.save);
        boolean floatingTag = true;
        try{
           Properties properties = new Properties();
           properties.load(new FileInputStream(settingTempFile));
           String interval = properties.getProperty("interval").trim();
           String isfloat = properties.getProperty("isfloat").trim();
           sender = properties.getProperty("sender").trim();
           prePassword = properties.getProperty("password").trim();
           recipients = properties.getProperty("recipients").trim();
           time = "".equals(interval) ? "5" : interval;
           floatingTag = "false".equals(isfloat) ? false :true;
           smtp = properties.getProperty("smtp");
        }catch(FileNotFoundException e){
            Log.e(LOG_TAG, "FileNotFoundException: "+ e.getMessage());
            e.printStackTrace();
        }
        catch(IOException e){
            Log.e(LOG_TAG, "IOException: "+e.getMessage());
            e.printStackTrace();
        }
        edtTime.setText(time);
        chkFloat.setChecked(floatingTag);
        edtRecipients.setText(recipients);  
        edtSender.setText(sender);
        edtPassword.setText(prePassword);
        edtSmtp.setText(smtp);
        btnSave.setOnClickListener(buttonListener);
    }
    private View.OnClickListener buttonListener = new View.OnClickListener() {
        
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            time = edtTime.getText().toString().trim();
            sender = edtSender.getText().toString().trim();
            if(!"".equals(sender) && !checkMailFormat(sender)){
                UIHelper.ShowMessage(getApplicationContext(), "发件人邮箱格式不正确");
                return;
            }
            recipients = edtRecipients.getText().toString().trim();
            receivers = recipients.split("\\s+");
            for(int i=0;i<receivers.length;i++){
                if(!"".endsWith(receivers[i])
                        && !checkMailFormat(receivers[i])){
                    UIHelper.ShowMessage(getApplicationContext(), "收件人邮箱"+receivers[i]+"格式不正确");
                    return;
                }
            }
            curPassword = edtPassword.getText().toString().trim();
            smtp = edtSmtp.getText().toString().trim();
            if(checkMailConfig(sender,recipients,smtp,curPassword) ==-1){
                UIHelper.ShowMessage(getApplicationContext(), "邮箱配置不完整，请完善所有信息");
                return;
            }
            if(!isNumeric(time)){
                UIHelper.ShowMessage(getApplicationContext(), "输入数据无效，请重新输入");
                edtTime.setText("");
            }else if("".equals("time") || Long.parseLong(time)==0){
                UIHelper.ShowMessage(getApplicationContext(), "输入数据为空，请重新输入");
                edtTime.setText("");
            }else if(Integer.parseInt(time)>600){
              UIHelper.ShowMessage(getApplicationContext(), "数据超过最大值600，请重新输入");
              
            }else {
                try{
                    Properties properties = new Properties();
                    properties.setProperty("interval", time);
                    properties.setProperty("isfloat", chkFloat.isChecked() ? "true" : "false");
                    properties.setProperty("sender", sender);
                    Log.d(LOG_TAG, "sender="+sender);
                    try{
                        //FIXME 注释
                        properties.setProperty("password", curPassword.equals(prePassword) ? curPassword :
                            ("".equals(curPassword) ? "" : des.encrypt(curPassword)));
                        Log.d(LOG_TAG, "curPassword="+curPassword);
                        Log.d(LOG_TAG, "encrtpt"+ des.encrypt(curPassword));
                    }catch(Exception e){
                        properties.setProperty("password", "");
                    }
                    properties.setProperty("recipients", recipients);
                    properties.setProperty("smtp", smtp);
                    FileOutputStream fos = new FileOutputStream(settingTempFile);
                    properties.store(fos, "Setting Data");
                    fos.close();
                    UIHelper.ShowMessage(getApplicationContext(), "保存成功");
                    Intent intent = new Intent();
                    setResult(Activity.RESULT_FIRST_USER,intent);
                    SettingsActivity.this.finish();
                }catch(FileNotFoundException e){
                    e.printStackTrace();
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
    };
    @Override
    public void finish(){
        super.finish();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
    private int checkMailConfig(String sender,String recipients,String smtp,String curPassword){
       if(!"".equals(curPassword) && !"".equals(sender)
               && !"".equals(recipients) && !"".equals(smtp)){
           return 1;
       }else if("".equals(curPassword) && "".equals(sender)
               && "".equals(recipients) && "".equals(smtp)){
           return 0;
       }else
        return -1;
    }
    /**
     * 检查邮件格式正确性
     * @param mail
     * @return
     */
    private boolean checkMailFormat(String mail){
        String strPattern = "^[a-zA-Z][\\w\\.-]*[a-zA-Z0-9]@[a-zA-Z0-9][\\w\\.-]*"
                + "[a-zA-Z0-9]\\.[a-zA-Z][a-zA-Z\\.]*[a-zA-Z]$";
        Pattern p = Pattern.compile(strPattern);
        Matcher m = p.matcher(mail);
        return m.matches();
    }
    /**
     * is input a number
     * 
     * @param inputStr
     *        intput string
     * @return
     *         true is numeric
     */
    private boolean isNumeric(String inputStr){
        for(int i= inputStr.length();--i >=0;){
            if(!Character.isDigit(inputStr.charAt(i))){
                return false;
            }
        }
        return true;
    }

}
