package com.example.larry.myposdemo.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.larry.myposdemo.R;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CustomAdapter extends ArrayAdapter<JsonElement> implements View.OnClickListener{

    private List<JsonElement> dataSet;
    Context mContext;
    private static final String INVOICE_AT = "invoiced_at";
    private static final String RENTAL_AT = "created_at";

    // View lookup cache
    private static class ViewHolder {
        View container;
        TextView uuid;
        TextView list_info;
    }

    public CustomAdapter(List<JsonElement> data, Context context) {
        super(context, R.layout.list_item, data);
        this.dataSet = data;
        this.mContext=context;

    }

    @Override
    public void onClick(View v) {

    }

    private int lastPosition = -1;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        JsonElement data = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_item, parent, false);
            viewHolder.uuid = (TextView) convertView.findViewById(R.id.uuid);
            viewHolder.list_info = (TextView) convertView.findViewById(R.id.list_info);
            viewHolder.container = (View) convertView.findViewById(R.id.item);

            result=convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result=convertView;
        }

        Animation animation = AnimationUtils.loadAnimation(mContext, (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top);
        result.startAnimation(animation);
        lastPosition = position;

        viewHolder.uuid.setText(convertMainInfo(data));
        viewHolder.list_info.setText(convertSubInfo(data));
        viewHolder.container.setOnClickListener(this);

        // Return the completed view to render on screen
        return convertView;
    }

    private String convertMainInfo(JsonElement obj) {
        JsonObject data = obj.getAsJsonObject();
        StringBuilder sb = new StringBuilder();
        JsonObject user = data.get("user").getAsJsonObject();
        sb.append(data.get("bid").getAsString())
                .append(" - ")
                .append(user.has("username") ? user.get("username").getAsString() : user.get("name").getAsString())
                .append("/").append(user.has("mobile") ? user.get("mobile").getAsString() : "");
        return sb.toString();
    }

    private String convertSubInfo(JsonElement obj){
        JsonObject data = obj.getAsJsonObject();
        StringBuilder sb = new StringBuilder("创建时间: ");
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm");
        long timeStamp = data.has(INVOICE_AT) ? Long.valueOf(data.get(INVOICE_AT).getAsString()) : Long.valueOf(data.get(RENTAL_AT).getAsString());
        String date_string = sdf.format(new Date(timeStamp));
        return sb.append(date_string).toString();
    }
}
