package com.rolmex.android.autotest.entity;

import android.app.Application;
import android.view.WindowManager;

public class MyApplication extends Application{
    
    private WindowManager.LayoutParams wmParams =  new WindowManager.LayoutParams();
    public WindowManager.LayoutParams getMywmParams(){
        return wmParams;
    }

}
