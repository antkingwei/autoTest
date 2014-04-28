package com.rolmex.android.autotest.utils;

import javax.mail.*;

public class CustomizedAuthenticator extends Authenticator{
    
    String userName = null;
    String password = null;
    public CustomizedAuthenticator(){
        
    }
    public CustomizedAuthenticator(String userName,String password){
        this.userName = userName;
        this.password = password;
    }
    protected PasswordAuthentication getPasswordAuthentication(){
        return new PasswordAuthentication(userName,password);
    }

}
