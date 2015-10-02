package com.hacktivators.dsd;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ListViewAdpter extends BaseAdapter {
    private LayoutInflater li;
    private ArrayList<ListViewItem> items;
    public class ViewHolder{
        TextView title,date,size,path;
    }
    public ListViewAdpter(Context context,ArrayList<ListViewItem> items){
        li=LayoutInflater.from(context);
        this.items=items;
    }
    @Override
    public int getCount() {
        return items.size();
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public ListViewItem getItem(int position) {
        return items.get(position);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder=null;
        if(convertView==null){
            holder=new ViewHolder();
            convertView=li.inflate(R.layout.row,null);
            holder.title=(TextView)convertView.findViewById(R.id.tvTitle);
            holder.date=(TextView)convertView.findViewById(R.id.tvDate);
            holder.size=(TextView)convertView.findViewById(R.id.tvSize);
            holder.path=(TextView)convertView.findViewById(R.id.tvPath);
            convertView.setTag(holder);
        }
        else holder=(ViewHolder) convertView.getTag();
        holder.title.setText(items.get(position).getTitle());
        holder.date.setText(items.get(position).getDate());
        holder.size.setText(items.get(position).getSize());
        holder.path.setText(items.get(position).getPath());
        return convertView;
    }
}
