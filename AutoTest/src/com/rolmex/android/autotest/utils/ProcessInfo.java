package com.rolmex.android.autotest.utils;

import com.rolmex.android.autotest.entity.Programe;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ProcessInfo {
    private static final String LOG_TAG = "AutoTest-"+ProcessInfo.class.getSimpleName();
    private static final String PACKAGE_NAME = "com.rolmex.android.autotest";
    
    public List<Programe> getRunningProgress(Context context){
        Log.i(LOG_TAG, "get running processes");
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> run = am.getRunningAppProcesses();
        PackageManager pm = context.getPackageManager();
        List<Programe> progressList = new ArrayList<Programe>();
        boolean launchTag;
        for(ApplicationInfo appinfo:getPackagesInfo(context)){
           launchTag = false;
           Programe programe = new Programe();
           if(((appinfo.flags & ApplicationInfo.FLAG_SYSTEM)>0)
                   || ((appinfo.processName!=null)&&(appinfo.processName.equals(PACKAGE_NAME)))){
               continue;
           }
           for(RunningAppProcessInfo runningProcess:run){
               if((runningProcess.processName !=null)
                       && runningProcess.processName.equals(appinfo.processName)){
                   launchTag = true;
                   programe.setPid(runningProcess.pid);
                   programe.setUid(runningProcess.uid);
                   break;
               }
           }
           programe.setPackageName(appinfo.processName);
           programe.setProcessName(appinfo.loadLabel(pm).toString());
           if(launchTag){
               programe.setIcon(appinfo.loadIcon(pm));
           }
           progressList.add(programe);
           
        }
        return progressList;
      
    }
    private List<ApplicationInfo> getPackagesInfo(Context context){
        PackageManager pm = context.getApplicationContext().getPackageManager();
        List<ApplicationInfo> appList = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        return appList;
    }
    

}
