package com.rolmex.android.autotest.utils;


import com.rolmex.android.autotest.service.AutoService;

import android.content.Context;
import android.net.TrafficStats;
import android.os.Build;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class CpuInfo {
    private static final String LOG_TAG = "AutoTest-"
            +CpuInfo.class.getSimpleName();
    private Context context;
    private long processCpu;
    private long idleCpu;
    private long totalCpu;
    private boolean isInitialStatics = true;
    private SimpleDateFormat formatterFile;
    private MemoryInfo mi;
    private long totalMemorySize;
    private long initialTraffic;
    private long lastestTraffic;
    private long traffic;
    private TrafficInfo trafficInfo;
    private ArrayList<String> cpuUsedRatio;
    private long totalCpu2;
    private long idleCpu2;
    private long processCpu2;
    private String processCpuRatio ="";
    private String totalCpuRatio = "";
    private int pid;
    
    public CpuInfo(Context context,int pid,String uid){
        this.pid = pid;
        this.context = context;
        trafficInfo = new TrafficInfo(uid);
        formatterFile = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        mi = new MemoryInfo();
        totalMemorySize = mi.getTotalMemory();
        cpuUsedRatio = new ArrayList<String>();
        
    }
    
    public void readCpuStat(){
        String processPid = Integer.toString(pid);
        String cpuStatPath = "/proc/"+processPid+"/stat";
        try{
            RandomAccessFile processCpuInfo = new RandomAccessFile(cpuStatPath,"r");
            String line = "";
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.setLength(0);
            while((line= processCpuInfo.readLine())!=null){
                stringBuffer .append(line+"\n");
            }
            String[] tok = stringBuffer.toString().split(" ");
            processCpu = Long.parseLong(tok[13])+Long.parseLong(tok[14]);
            processCpuInfo.close();
        }catch(FileNotFoundException e){
            Log.e(LOG_TAG, "FileNotFoundException: "+e.getMessage());
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }
        try{
            RandomAccessFile cpuInfo = new RandomAccessFile("/proc/stat","r");
            String[] toks = cpuInfo.readLine().split("\\s+");
            idleCpu = Long.parseLong(toks[4]);
            totalCpu = Long.parseLong(toks[1])+Long.parseLong(toks[2])
                    +Long.parseLong(toks[3])+Long.parseLong(toks[4])
                    +Long.parseLong(toks[6])+Long.parseLong(toks[5])
                    +Long.parseLong(toks[7]);
            cpuInfo.close();
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }
        
        catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public String getCpuName(){
        try{
            RandomAccessFile  cpuStat = new RandomAccessFile("/proc/cpuinfo","r");
            String[] cpu = cpuStat.readLine().split(":");//cpu信息的前一段是含有processor字符
            cpuStat.close();
            return cpu[1];
        }catch(IOException e){
            Log.e(LOG_TAG, "IOException: "+e.getMessage());
        }
        return "";
    }
    public ArrayList<String> getCpuRatioInfo(){
        DecimalFormat fomart = new DecimalFormat();
        fomart.setMaximumFractionDigits(2);
        fomart.setMinimumFractionDigits(2);
        readCpuStat();
        cpuUsedRatio.clear();
        try{
            String mDateTime2;
            Calendar cal = Calendar.getInstance();
            if((Build.MODEL.equals("sdk"))
                    ||(Build.MODEL.equals("google_sdk"))){
                mDateTime2 = formatterFile.format(cal.getTime().getTime()+8*60*60*1000);
                
            }else{
                mDateTime2 = formatterFile.format(cal.getTime().getTime());
                if(isInitialStatics){
                    initialTraffic = trafficInfo.getTrafficInfo();
                    isInitialStatics= false;
                }else{
                    lastestTraffic = trafficInfo.getTrafficInfo();
                    if(initialTraffic==-1)
                        traffic = -1;
                    else
                        traffic = (lastestTraffic - initialTraffic+1023)/1024;
                    processCpuRatio = fomart.format(100*((double)(processCpu-processCpu2)/((double)(totalCpu - totalCpu2))));
                    totalCpuRatio = fomart.format(100*((double)((totalCpu-idleCpu) -(totalCpu2- idleCpu2))/(double)(totalCpu- totalCpu2)));
                    long pidMemory = mi.getPidMemorySize(pid, context);
                    String pMemory = fomart.format((double)pidMemory/1024);
                    long freeMemory = mi.getFreeMemorySize(context);
                    String fMemory = fomart.format((double)freeMemory/1024);
                    String percent = "统计出错";
                    if(totalMemorySize!=0){
                        percent = fomart.format(((double)pidMemory/(double)totalMemorySize) *100);
                    }
                    if(traffic ==-1){
                        AutoService.bw.write(mDateTime2+","+pMemory+","+percent+"fMemory"+","+processCpuRatio
                                +","+totalCpuRatio+","+"本程序或本设备不支持流量统计"+"\r\n");
                    }else{
                        AutoService.bw.write(mDateTime2 + "," + pMemory + ","
                                + percent + "," + fMemory + "," + processCpuRatio
                                + "," + totalCpuRatio + "," + traffic + "\r\n");
                    }
                }
                totalCpu2 = totalCpu;
                processCpu2 = processCpu;
                idleCpu2 = idleCpu;
                cpuUsedRatio.add(processCpuRatio);
                cpuUsedRatio.add(totalCpuRatio);
                cpuUsedRatio.add(String.valueOf(traffic));
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        return cpuUsedRatio;
        
    }
    

}
