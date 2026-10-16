package com.brittech.minibuddy;
import android.content.*;
/** Signature-protected handoff: no study data or personal information is broadcast. */
public final class FamilyCoordinator extends BroadcastReceiver {
 public static final String PERMISSION="com.brittech.minibuddies.permission.COORDINATE";
 private static final String[] SUBJECTS={"matematica","ingles","portugues","espanhol","ciencias","historia","geografia","sociologia","filosofia","fisica","quimica","biologia"};
 public static void activate(Context c){for(String subject:SUBJECTS){String target="com.brittech.minibuddies."+subject;if(!target.equals(c.getPackageName()))c.sendBroadcast(new Intent("com.brittech.minibuddies.ACTIVATE").setPackage(target),PERMISSION);}}
 @Override public void onReceive(Context c,Intent i){if("com.brittech.minibuddies.ACTIVATE".equals(i.getAction())){c.stopService(new Intent(c,OverlayService.class));c.stopService(new Intent(c,CaptureService.class));}}
}
