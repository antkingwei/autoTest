package com.rolmex.android.autotest.utils;

import android.content.Context;
import android.widget.Toast;

public class UIHelper {
    
    public static void ShowMessage(Context context,String msg){
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

}
