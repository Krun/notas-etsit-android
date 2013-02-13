package com.puchinike.etsitnotas;


import com.puchinike.etsitnotas.model.Asignatura;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class NotasListViewAdapter extends ArrayAdapter<Asignatura> {
	 
    Context context;
 
    public NotasListViewAdapter(Context context, int resourceId,
            Asignatura[] items) {
        super(context, resourceId, items);
        this.context = context;
    }
 
    /*private view holder class*/
    private class ViewHolder {
        TextView txtName;
        TextView txtInfo;
    }
 
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        Asignatura asignatura = getItem(position);
 
        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.tablerow, null);
            holder = new ViewHolder();
            holder.txtInfo = (TextView) convertView.findViewById(R.id.info);
            holder.txtName = (TextView) convertView.findViewById(R.id.name);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();
 
        holder.txtInfo.setText(asignatura.getInfo());
        holder.txtName.setText(asignatura.getName());
 
        return convertView;
    }
}