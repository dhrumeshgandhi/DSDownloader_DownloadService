package com.hacktivators.dsd;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.webkit.URLUtil;
import android.widget.Toast;

import org.apache.http.client.utils.URIUtils;

public class ClipBoardWatcher extends Service {

    private ClipboardManager.OnPrimaryClipChangedListener
                listener=new ClipboardManager.OnPrimaryClipChangedListener() {
        @Override
        public void onPrimaryClipChanged() {
            performClipBoardCheck();
        }
    };
    @Override
    public void onCreate() {
        ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE))
                .addPrimaryClipChangedListener(listener);
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private void performClipBoardCheck(){
        ClipboardManager cbm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if(cbm.hasPrimaryClip()){
            ClipData cd= cbm.getPrimaryClip();
            if(cd.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                String data = cd.getItemAt(0).getText().toString();
                if(URLUtil.isValidUrl(data)) {
                    Uri url=Uri.parse(data);
                    Intent i = new Intent(this, MainActivity.class);
                    i.setAction(Intent.ACTION_VIEW);
                    i.setData(url);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
            }
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
