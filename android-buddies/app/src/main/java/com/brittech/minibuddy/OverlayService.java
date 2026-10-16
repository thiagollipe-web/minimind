package com.brittech.minibuddy;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;

public final class OverlayService extends Service {
    private WindowManager wm;private WindowManager.LayoutParams params;private RobotView robot;private Session session;
    private final Handler main=new Handler(Looper.getMainLooper());
    private HandlerThread thread;private Handler work;private int demoIndex;private long saved;
    private float dragX,dragY;private StudyBubble bubble;private long lastQuestionCheck;
    private final InteractionLogic.SoundGate soundGate=new InteractionLogic.SoundGate();
    private final Runnable animate=new Runnable(){public void run(){
        if(robot!=null){if(!Settings.canDrawOverlays(OverlayService.this)){stopSelf();return;}robot.invalidate();updateBounds();
            if(SystemClock.elapsedRealtime()-lastQuestionCheck>30000){lastQuestionCheck=SystemClock.elapsedRealtime();maybeQuestion();}
            if(!session.mode.equals("pausado")&&soundGate.changed(session.mood(),SystemClock.elapsedRealtime(),
                session.prefs.getBoolean("muted",false)||session.uiVisible||SystemClock.elapsedRealtime()<session.happyUntil))Chirps.play(session,session.mood());
            main.postDelayed(this,100);}
    }};
    private final Runnable learn=new Runnable(){public void run(){
        session.care.tick(!session.mode.equals("pausado"));
        if("demo".equals(session.mode))session.process(Brain.demo(demoIndex++),true);
        if(SystemClock.elapsedRealtime()-saved>60000){session.save();saved=SystemClock.elapsedRealtime();}
        work.postDelayed(this,250);
    }};
    @Override public void onCreate(){super.onCreate();session=Session.get(this);bubble=new StudyBubble(this,session);thread=new HandlerThread("buddy-demo");thread.start();work=new Handler(thread.getLooper());}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        if("STOP".equals(intent.getAction())){stopService(new Intent(this,CaptureService.class));session.mode("pausado");stopSelf();return START_NOT_STICKY;}
        if(!Settings.canDrawOverlays(this)){session.message="Autorize aparecer sobre outros apps.";stopSelf();return START_NOT_STICKY;}
        try{
            Notification notice=Notifications.build(this,"Companheiro flutuante • toque para carinho",new Intent(this,OverlayService.class).setAction("STOP"));
            if(Build.VERSION.SDK_INT>=34)startForeground(10,notice,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(10,notice);
            if(robot==null){FamilyCoordinator.activate(this);showRobot();}
            if("STUDY".equals(intent.getAction()))bubble.show();
            if(intent.getAction()!=null&&intent.getAction().startsWith("MOVE_")){
                String side=intent.getAction().substring(5);
                Rect area=usableArea();float x=side.equals("RIGHT")?area.right-params.width:side.equals("CENTER")?(area.left+area.right-params.width)/2f:area.left;
                moveTo(x,params.y);session.prefs.edit().putString("dock",side).apply();savePosition();
                session.message="Mascote reposicionado: "+side.toLowerCase(java.util.Locale.ROOT);
            }
            if("DEMO".equals(intent.getAction())){
                // Stop projection first; onDestroy only pauses mode if still 'tela'.
                stopService(new Intent(this,CaptureService.class));session.mode("demo");demoIndex=0;
                session.message="Demonstração: bloco sintético, sem captura da tela.";
            }
        }catch(Exception e){session.message="Não foi possível exibir o mascote: "+e.getMessage();stopSelf();}
        return START_NOT_STICKY;
    }
    private void maybeQuestion(){
        if(!session.prefs.getBoolean("study_questions",false)||session.uiVisible||bubble.visible()||session.care.state.resting)return;
        android.os.PowerManager power=getSystemService(android.os.PowerManager.class);if(!power.isInteractive())return;
        int hour=java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);if(hour>=22||hour<7)return;
        long now=System.currentTimeMillis(),last=session.prefs.getLong("last_study_question",now);
        if(!session.prefs.contains("last_study_question")){session.prefs.edit().putLong("last_study_question",now).apply();return;}
        long interval=session.prefs.getInt("study_hours",3)*3600000L;
        if(now-last>=interval){session.prefs.edit().putLong("last_study_question",now).apply();bubble.show();}
    }
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void showRobot(){
        wm=getSystemService(WindowManager.class);int width=dp(session.prefs.getInt("size",100));
        params=new WindowManager.LayoutParams(width,width*178/160,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.LEFT;params.x=session.prefs.getInt("x",20);params.y=session.prefs.getInt("y",180);
        robot=new RobotView(this,session,new RobotView.Gestures(){
            public void pet(){session.pet();Chirps.play(session);}
            public void drag(float dx,float dy){
                session.prefs.edit().putString("dock","FREE").apply();
                moveTo(dragX+dx,dragY+dy);
            }
            public void positionSaved(){savePosition();}
            public void menu(){bubble.show();}
        });
        wm.addView(robot,params);restorePosition();main.post(animate);work.post(learn);Chirps.play(session);
    }
    private void updateBounds(){if(robot!=null){int[] loc=new int[2];robot.getLocationOnScreen(loc);session.overlayBounds=new Rect(loc[0],loc[1],loc[0]+robot.getWidth(),loc[1]+robot.getHeight());}}
    private Rect usableArea(){
        if(Build.VERSION.SDK_INT>=30){
            WindowMetrics metrics=wm.getMaximumWindowMetrics();Rect area=new Rect(metrics.getBounds());
            Insets bars=metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            area.left+=bars.left+dp(6);area.top+=bars.top+dp(4);area.right-=bars.right+dp(6);area.bottom-=bars.bottom+dp(4);return area;
        }
        android.util.DisplayMetrics m=new android.util.DisplayMetrics();wm.getDefaultDisplay().getRealMetrics(m);
        return new Rect(dp(6),dp(28),m.widthPixels-dp(6),m.heightPixels-dp(48));
    }
    private void moveTo(float x,float y){
        Rect area=usableArea();
        dragX=InteractionLogic.clamp(x,area.left,area.right-params.width);
        dragY=InteractionLogic.clamp(y,area.top,area.bottom-params.height);
        params.x=Math.round(dragX);params.y=Math.round(dragY);
        try{wm.updateViewLayout(robot,params);}catch(Exception e){session.message="Não foi possível mover o robô: "+e.getMessage();}
        updateBounds();
    }
    private void savePosition(){
        Rect area=usableArea();float range=Math.max(1,area.bottom-area.top-params.height);
        session.prefs.edit().putInt("x",params.x).putInt("y",params.y).putFloat("y_fraction",(params.y-area.top)/range).apply();
    }
    private void restorePosition(){
        Rect area=usableArea();String side=session.prefs.getString("dock","FREE");
        float x=side.equals("RIGHT")?area.right-params.width:side.equals("LEFT")?area.left:side.equals("CENTER")?(area.left+area.right-params.width)/2f:session.prefs.getInt("x",dp(8));
        float y=session.prefs.contains("y_fraction")?area.top+session.prefs.getFloat("y_fraction",.25f)*Math.max(1,area.height()-params.height):session.prefs.getInt("y",dp(120));
        moveTo(x,y);
    }
    @Override public void onConfigurationChanged(android.content.res.Configuration c){super.onConfigurationChanged(c);if(params!=null)restorePosition();}
    @Override public void onDestroy(){
        if(bubble!=null)bubble.close();main.removeCallbacksAndMessages(null);if(work!=null){work.removeCallbacksAndMessages(null);work.post(()->{session.save();thread.quitSafely();});}
        if(robot!=null){try{wm.removeView(robot);}catch(Exception ignored){}robot=null;}
        if(session!=null){session.overlayBounds=new Rect();if(session.mode.equals("demo"))session.mode("pausado");}
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
