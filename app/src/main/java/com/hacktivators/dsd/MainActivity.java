package com.hacktivators.dsd;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
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
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

public class MainActivity extends Activity {
    Context context=this;
    ListView downloadList;
    ArrayList<ListViewItem> dList;
    ListViewAdpter dAdapter;
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
    Intent i;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        downloadList=(ListView)findViewById(R.id.lvDownloadList);
        li=LayoutInflater.from(this);
        dList=new ArrayList<ListViewItem>();
        dAdapter=new ListViewAdpter(this,dList);
        downloadList.setAdapter(dAdapter);
        downloadList.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ListViewItem item = (ListViewItem) parent.getItemAtPosition(position);
                String path = item.getPath(),
                        title = item.getTitle();
                File file = new File(path, title);
                Long size = file.length();
                Toast.makeText(context, size.toString(), Toast.LENGTH_SHORT).show();
                Uri uri = Uri.fromFile(file);
                Intent i = new Intent();
                i.setAction(Intent.ACTION_VIEW);
                i.setDataAndType(uri, "*/*");
                startActivity(i);
                return true;
            }
        });
        progressB=(ProgressBar)findViewById(R.id.progressBar);
        progressB.setIndeterminate(false);
        tvCurrentDownload=(TextView)findViewById(R.id.tvCurrentDownload);
        tvCurrentDownload.setText(noDownloads);
        noDownloads=getString(R.string.noDownloads);
        downloadFolder=getString(R.string.downloadFolderName);
        createDirIfNotExits(downloadFolder);
        progressB.setIndeterminate(true);
        sysProperties=System.getProperties();
        fab=(FloatingActionButton)findViewById(R.id.btnFab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDialog(null);
            }
        });
        tvCurrentDownload.setText(noDownloads);
        if((i=getIntent())!=null && Intent.ACTION_VIEW.equals(i.getAction())){
            String url=i.getData().toString();
            openDialog(url);
        }
        if(!isMyServiceRunning(ClipBoardWatcher.class)) {
            i = new Intent(this, ClipBoardWatcher.class);
            startService(i);
        }
        loadList();
    }
    private void loadList(){
        pref=getSharedPreferences("Download List",MODE_PRIVATE);
        ListViewItem item;
        String title,date,size,path;
        for(int i=0;i<pref.getInt("List_Size",0);i++){
            title=pref.getString(i+"_Title",null);
            date=pref.getString(i+"_Date",null);
            size=pref.getString(i+"_Size",null);
            path=pref.getString(i+"_Path",null);
            item=new ListViewItem(title,date,size,path);
            dList.add(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveList();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        loadList();
    }

    private void saveList(){
        pref=context.getSharedPreferences("Download List",MODE_PRIVATE);
        editor=pref.edit();
        editor.putInt("List_Size",dList.size());
        for(int i=0;i<dList.size();i++){
            editor.putString(i+"_Title",dList.get(i).getTitle());
            editor.putString(i+"_Path",dList.get(i).getPath());
            editor.putString(i+"_Size",dList.get(i).getSize());
            editor.putString(i+"_Date",dList.get(i).getDate());
        }
        editor.commit();
    }
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    public void openDialog(final String link){
        dialogBox=li.inflate(R.layout.dialog, null);
        final EditText url = (EditText) dialogBox.findViewById(R.id.et_url);
        final EditText saveTo=(EditText)dialogBox.findViewById(R.id.et_saveTo);
        url.setText(link);
        saveTo.setText(Environment.getExternalStorageDirectory() + "/" + downloadFolder);
        new AlertDialog.Builder(this)
                .setView(dialogBox)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(getString(R.string.downloadDialogTitle))
                .setMessage(getString(R.string.downloadDialogMessage))
                .setPositiveButton("Download",new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String locX;
                        urlE = url.getText().toString();
                        locX=saveTo.getText().toString();
                        Toast.makeText(context,locX,Toast.LENGTH_SHORT).show();
                        title=getFileName(urlE, 0);
                        DownloadService ds=new DownloadService(MainActivity.this,title,locX);
                        ds.execute(urlE);
                        title=checkFileName(title,locX);
                        Toast.makeText(context,title,Toast.LENGTH_SHORT).show();
                        addToList(new ListViewItem(title,
                                        getDateTime(),
                                        getFileSize(urlE),locX));
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
    private String getDateTime(){
        String datetime=Calendar.getInstance().getTime().toString();
        datetime=datetime.substring(4, 20);
        return datetime;
    }
    private String getFileName(String url,int fullOrWExtOrWOExt){
        String name;
        int s,e;
        s=url.lastIndexOf('/');
        e=url.length();
        if(fullOrWExtOrWOExt==1) e=url.lastIndexOf('.');
        else if(fullOrWExtOrWOExt==2) s=url.lastIndexOf('.');
        name = url.substring(s+1,e);
        return name;
    }
    private String getFileSize(String url){
        String fileSize="None";

        return fileSize;
    }
    private void addToList(ListViewItem info){
        dList.add(info);
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
    private String checkFileName(String name,String loc){
        File file;
        int i=0;
        String nameE=name,nameWOExt=getFileName(name,1),ext=getFileName(name,2);;
        boolean isExits=false;
        do{
            file=new File(loc,nameE);
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
        String name,loc;
        public DownloadService(Context context,String name,String loc){
            this.context=context;
            this.name=name;
            this.loc=loc;
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
                name=checkFileName(name,loc);
                out = new FileOutputStream(loc + "/" + name);
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
