package com.hacktivators.dsd;
public class ListViewItem {
    private String title,date,size,path;
    public ListViewItem(String title,String date,String size,String path){
        this.title=title;
        this.date=date;
        this.size=size;
        this.path=path;
    }
    public  String getTitle(){
        return title;
    }
    public  String getDate(){
        return date;
    }
    public  String getSize(){
        return size;
    }
    public String getPath() {
        return path;
    }
}
