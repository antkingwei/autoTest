package com.rolmex.android.autotest.adapter;

import com.rolmex.android.autotest.R;
import com.rolmex.android.autotest.activity.MainActivity;
import com.rolmex.android.autotest.entity.Programe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.List;

public class ListAdapter extends BaseAdapter{
    List<Programe> programe;
    private Context context;
    private MainActivity activity;
    int tempPosition =-1;
    private LayoutInflater layoutInflater;
    class ViewHolder{
        TextView txtAppName;
        ImageView imgViAppIcon;
        RadioButton rdoBtnApp;
    }
    public ListAdapter(MainActivity activity,Context context,List<Programe> programe){
        this.context = context;
        this.activity = activity;
        this.programe = programe;
        layoutInflater = LayoutInflater.from(context);
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
        ViewHolder holder;
        final int i= position;
        if(convertView == null){
            holder = new ViewHolder();
            convertView = layoutInflater.inflate(R.layout.list_item, null);
            holder.imgViAppIcon = (ImageView)convertView.findViewById(R.id.image);
            holder.txtAppName = (TextView)convertView.findViewById(R.id.text);
            holder.rdoBtnApp = (RadioButton)convertView.findViewById(R.id.rb);     
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder)convertView.getTag();
        }
        holder.rdoBtnApp.setId(position);
        holder.rdoBtnApp.setOnCheckedChangeListener(new OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
                if(isChecked){
                    MainActivity.isRadioChecked = true;
                    if(tempPosition !=-1){
                        RadioButton tempButton = (RadioButton) activity.findViewById(tempPosition);
                        if((tempButton!=null) && (tempPosition !=i)){
                            tempButton.setChecked(false);
                        }
                    }
                    tempPosition = buttonView.getId();
                    MainActivity.packageName = programe.get(tempPosition).getPackageName();
                    MainActivity.processName = programe.get(tempPosition).getPackageName();
                }
            }
            
        });
        if(tempPosition == position){
            if(!holder.rdoBtnApp.isChecked()){
                holder.rdoBtnApp.setChecked(true);
            }
        }
       Programe pr = (Programe) programe.get(position);
       holder.imgViAppIcon.setImageDrawable(pr.getIcon());
       holder.txtAppName.setText(pr.getProcessName());
        return convertView  ;
    }

}
