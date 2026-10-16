package com.brittech.minibuddy;
import android.app.*;
import android.content.*;
public final class Notifications {
    public static Notification build(Context c,String text,Intent stop){
        NotificationManager manager=c.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("buddy","MiniBuddy ativo",NotificationManager.IMPORTANCE_LOW));
        PendingIntent panel=PendingIntent.getActivity(c,1,new Intent(c,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent end=PendingIntent.getService(c,stop.getComponent().getClassName().hashCode(),stop,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(c,"buddy").setSmallIcon(com.brittech.minibuddy.R.drawable.ic_robot)
            .setContentTitle("Thigas MiniBuddy").setContentText(text).setContentIntent(panel).setOngoing(true)
            .addAction(new Notification.Action.Builder(null,"Parar",end).build()).build();
    }
}
