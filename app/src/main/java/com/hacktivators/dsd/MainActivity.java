package com.hacktivators.dsd;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;

public class MainActivity extends Activity {
    Context context=this;
    ListView downloadList;
    ArrayList<String> dList;
    ArrayAdapter<String> dAdapter;
    String urlE,title,noDownloads,downloadFolder,
            proxyHost="10.10.0.23",
            proxyUser="3ce47",
            proxyPass="gandhi1996";
    int porxyPort=8089;
    View dialogBox;
    LayoutInflater li;
    Proxy proxy=null;
    Properties sysProperties;
    TextView tvCurrentDownload;
    ProgressBar progressB;
    Menu optMenu;
    FloatingActionButton fab;
    boolean isProxyUsed=false,isAuthReq=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        downloadList=(ListView)findViewById(R.id.lvDownloadList);
        li=LayoutInflater.from(this);
        dList=new ArrayList<String>();
        dAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dList);
        downloadList.setAdapter(dAdapter);
        progressB=(ProgressBar)findViewById(R.id.progressBar);
        progressB.setIndeterminate(false);
        tvCurrentDownload=(TextView)findViewById(R.id.tvCurrentDownload);
        tvCurrentDownload.setText(noDownloads);
        noDownloads=getString(R.string.noDownloads);
        downloadFolder=getString(R.string.downloadFolderName);
        createDirIfNotExits(downloadFolder);
        sysProperties=System.getProperties();
        fab=(FloatingActionButton)findViewById(R.id.btnFab);
        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                openDialog(v);
            }
        });
    }
    public void openDialog(View v){
        dialogBox=li.inflate(R.layout.dialog, null);
        new AlertDialog.Builder(this)
                .setView(dialogBox)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(getString(R.string.downloadDialogTitle))
                .setMessage(getString(R.string.downloadDialogMessage))
                .setPositiveButton("Download",new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String locX;
                        EditText url = (EditText) dialogBox.findViewById(R.id.et_url);
                        urlE = url.getText().toString();
                        Spinner loc=(Spinner) dialogBox.findViewById(R.id.storage);
                        locX=loc.getSelectedItem().toString();
                        title=getFileName(urlE,0);
                        DownloadService ds=new DownloadService(MainActivity.this,title);
                        ds.execute(urlE);
                        title=checkFileName(title);
                        Toast.makeText(context,title,Toast.LENGTH_SHORT).show();
                        addToList(title);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel",new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create()
                .show();
    }
    private String getFileName(String url,int fullOrWExtOrWOExt){
        String name;
        int s,e=0;
        s=url.lastIndexOf('/');
        e=url.length();
        if(fullOrWExtOrWOExt==1) e=url.lastIndexOf('.');
        else if(fullOrWExtOrWOExt==2) s=url.lastIndexOf('.');
        name = url.substring(s+1,e);
        return name;
    }
    private void addToList(String path){
        dList.add(path);
        dAdapter.notifyDataSetChanged();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        optMenu=menu;
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        else if(id == R.id.proxy){
            if(item.isChecked()){
                item.setChecked(false);
                isProxyUsed=false;
                optMenu.getItem(1).setEnabled(false);
            }
            else {
                item.setChecked(true);
                isProxyUsed=true;
                optMenu.getItem(1).setEnabled(true);
            }
        }
        else if(id == R.id.proxyAuth){
            if(item.isChecked()){
                item.setChecked(false);
                isAuthReq=true;
            }
            else {
                item.setChecked(true);
                isAuthReq=false;
            }
        }
        return super.onOptionsItemSelected(item);
    }
    private void createDirIfNotExits(String dirName){
        File file=new File(Environment.getExternalStorageDirectory(),dirName);
        if(!file.exists()){
            file.mkdirs();
        }
    }
    private String checkFileName(String name){
        File file;
        int i=0;
        String nameE=name,nameWOExt=getFileName(name,1),ext=getFileName(name,2);;
        boolean isExits=false;
        do{
            file=new File(Environment.getExternalStorageDirectory()+"/"+downloadFolder,nameE);
            if(file.exists()){
                isExits=true;
                i++;
                nameE=nameWOExt+"_"+i+"."+ext;
            }
            else isExits=false;
        }while (isExits);
        if(i>0) name=nameWOExt+"_"+i+"."+ext;
        return name;
    }
    private class DownloadService extends AsyncTask<String,Integer,String> {

        private Context context;
        String name;
        public DownloadService(Context context,String name){
            this.context=context;
            this.name=name;
        }
        @Override
        protected String doInBackground(String... urlList) {
            InputStream in=null;
            OutputStream out=null;
            HttpURLConnection cn=null;
            try{
                URL url=new URL(urlList[0]);
                if(isProxyUsed){
                    proxy=new Proxy(Proxy.Type.HTTP,new InetSocketAddress(proxyHost,porxyPort));
                    if(isAuthReq){
                        Authenticator.setDefault(new Authenticator() {
                            @Override
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return (new PasswordAuthentication(proxyUser,proxyPass.toCharArray()));
                            }
                        });
                    }
                    cn=(HttpURLConnection)url.openConnection(proxy);
                }
                else {
                    cn=(HttpURLConnection)url.openConnection();
                }
                cn.connect();
                if (cn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + cn.getResponseCode()
                            + " " + cn.getResponseMessage();
                }
                int fileSize=cn.getContentLength();
                in=cn.getInputStream();
                name=checkFileName(name);
                out=new FileOutputStream(Environment.getExternalStorageDirectory()+"/"+downloadFolder+"/"+name);
                byte data[]=new byte[4096];
                long total=0;
                int cnt;
                while ((cnt=in.read(data))!=-1){
                    if(isCancelled()){
                        in.close();
                        return null;
                    }
                    total+=cnt;
                    if(fileSize>0)
                        publishProgress((int)total*100/fileSize);
                    out.write(data,0,cnt);
                }
            }
            catch (Exception e){
                return e.toString();
            }
            finally {
                try{
                    if(out !=null) out.close();
                    if(in != null) in.close();
                }
                catch (Exception e){
                    Toast.makeText(context,e.getMessage(),Toast.LENGTH_SHORT).show();
                }
                if(cn != null) cn.disconnect();
            }
           return null;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            tvCurrentDownload.setText(title);
            progressB.setVisibility(View.VISIBLE);
        }
        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            progressB.setIndeterminate(false);
            progressB.setMax(100);
            progressB.setProgress(progress[0]);
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(result!=null)
                Toast.makeText(context,getString(R.string.downloadError)+result, Toast.LENGTH_LONG).show();
            else
                Toast.makeText(context,getString(R.string.downloadSuccessful), Toast.LENGTH_SHORT).show();
            progressB.setProgress(0);
            progressB.setVisibility(View.INVISIBLE);
            tvCurrentDownload.setText(noDownloads);
        }
    }
}