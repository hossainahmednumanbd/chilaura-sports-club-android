package bd.chilaura.sportsclub;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class NoticeJobService extends JobService {
    public static final String CHANNEL = "club_notices";
    private static final String API = "https://ryjggubezqtainzpyrkt.supabase.co/rest/v1/club_content?section=eq.notices&select=id,payload,updated_at&order=updated_at.desc&limit=1";
    private static final String KEY = "sb_publishable_mgXfxchkGkTRXzzxbQk15w_hvc_rtWz";

    @Override public boolean onStartJob(JobParameters params) {
        new Thread(() -> {
            try {
                HttpURLConnection conn=(HttpURLConnection)new URL(API).openConnection();
                conn.setConnectTimeout(12000);conn.setReadTimeout(12000);
                conn.setRequestProperty("apikey",KEY);
                conn.setRequestProperty("Accept","application/json");
                if(conn.getResponseCode()!=200)throw new Exception("HTTP "+conn.getResponseCode());
                InputStream in=conn.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                byte[] buffer=new byte[4096];int n;
                while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);
                in.close();conn.disconnect();
                JSONArray arr=new JSONArray(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
                if(arr.length()>0){
                    JSONObject latest=arr.getJSONObject(0);
                    String id=latest.optString("id")+":"+latest.optString("updated_at");
                    SharedPreferences prefs=getSharedPreferences("club_notice_alerts",Context.MODE_PRIVATE);
                    String prev=prefs.getString("last_notice",null);
                    prefs.edit().putString("last_notice",id).apply();
                    if(prev!=null&&!prev.equals(id))sendAlert(latest.optJSONObject("payload"));
                }
            } catch(Exception ignored) {}
            finally {jobFinished(params,false);}
        }).start();
        return true;
    }
    private void sendAlert(JSONObject payload){
        NotificationManager manager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(CHANNEL,"ক্লাব নোটিশ ও সংবাদ",NotificationManager.IMPORTANCE_DEFAULT));
        Intent intent=new Intent(this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending=PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        String title=payload==null?"নতুন ক্লাব নোটিশ":payload.optString("title","নতুন ক্লাব নোটিশ");
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("চিলাউড়া স্পোর্টস ক্লাব").setContentText(title).setStyle(new Notification.BigTextStyle().bigText(title)).setAutoCancel(true).setContentIntent(pending);
        try{manager.notify(1107,b.build());}catch(SecurityException ignored){}
    }
    @Override public boolean onStopJob(JobParameters params){return true;}
}
