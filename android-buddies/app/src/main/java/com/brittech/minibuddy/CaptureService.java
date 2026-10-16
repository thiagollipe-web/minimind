package com.brittech.minibuddy;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.*;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.*;
import java.nio.ByteBuffer;

/** Explicit MediaProjection, no file recordings, microphone or internet. */
public final class CaptureService extends Service {
    private Session session;private HandlerThread thread;private Handler worker;
    private MediaProjection projection;private VirtualDisplay display;private ImageReader reader;
    private long lastFrame,saved;private boolean[] oldMask;private boolean visible=true;
    private int captureW,captureH,dpi;private boolean destroyed;
    private final MediaProjection.Callback callback=new MediaProjection.Callback(){
        @Override public void onStop(){
            session.message="Captura encerrada pelo Android. Autorize novamente para retomar.";
            if(session.mode.equals("tela"))session.mode("pausado");stopSelf();
        }
        @Override public void onCapturedContentResize(int width,int height){
            if(width>0&&height>0&&projection!=null&&display!=null){
                int w=240,h=Math.max(160,Math.min(640,(int)(240f*height/width)));
                if(captureW!=w||captureH!=h){
                    ImageReader old=reader;reader=newReader(w,h);display.resize(w,h,dpi);display.setSurface(reader.getSurface());
                    if(old!=null)old.close();captureW=w;captureH=h;oldMask=null;
                }
            }
        }
        @Override public void onCapturedContentVisibilityChanged(boolean value){visible=value;}
    };
    @Override public void onCreate(){super.onCreate();session=Session.get(this);thread=new HandlerThread("buddy-capture");thread.start();worker=new Handler(thread.getLooper());}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent==null||"STOP".equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        if(projection!=null){session.message="Uma captura já está ativa.";return START_NOT_STICKY;}
        try{
            Intent consent=intent.getParcelableExtra("consent");int code=intent.getIntExtra("result",0);
            if(consent==null||code!=Activity.RESULT_OK)throw new SecurityException("Autorização de captura ausente");
            Notification notice=Notifications.build(this,"Observando mudanças visuais • sem gravar imagens",new Intent(this,CaptureService.class).setAction("STOP"));
            if(Build.VERSION.SDK_INT>=29)startForeground(11,notice,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);else startForeground(11,notice);
            projection=getSystemService(MediaProjectionManager.class).getMediaProjection(code,consent);
            projection.registerCallback(callback,worker);
            session.mode("tela");session.message="Captura autorizada. Saia do painel para observar os outros apps.";
            worker.post(()->{
                if(destroyed||projection==null)return;
                try{
                    DisplayMetrics m=new DisplayMetrics();getSystemService(WindowManager.class).getDefaultDisplay().getRealMetrics(m);
                    captureW=240;captureH=Math.max(160,Math.min(640,(int)(240f*m.heightPixels/m.widthPixels)));dpi=m.densityDpi;
                    reader=newReader(captureW,captureH);
                    display=projection.createVirtualDisplay("ThigasVisual",captureW,captureH,dpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,worker);
                }catch(Exception e){session.message="Falha ao iniciar captura: "+e.getMessage();stopSelf();}
            });
        }catch(Exception e){session.message="Captura não iniciada: "+e.getMessage();stopSelf();}
        return START_NOT_STICKY;
    }
    private ImageReader newReader(int w,int h){
        ImageReader result=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,2);
        result.setOnImageAvailableListener(this::imageAvailable,worker);return result;
    }
    private void imageAvailable(ImageReader source){
        try(Image image=source.acquireLatestImage()){
            if(image==null)return;long now=SystemClock.elapsedRealtime();
            if(session.uiVisible||session.bubbleVisible||!visible||now-lastFrame<250||!session.mode.equals("tela"))return;
            lastFrame=now;
            Image.Plane plane=image.getPlanes()[0];ByteBuffer buffer=plane.getBuffer();
            int row=plane.getRowStride(),pixel=plane.getPixelStride(),iw=image.getWidth(),ih=image.getHeight();
            float[] input=new float[Brain.W*Brain.H];boolean[] mask=new boolean[input.length];
            Rect buddy=new Rect(session.overlayBounds);
            DisplayMetrics m=new DisplayMetrics();getSystemService(WindowManager.class).getDefaultDisplay().getRealMetrics(m);
            for(int y=0;y<Brain.H;y++)for(int x=0;x<Brain.W;x++){
                int i=y*Brain.W+x;
                int screenX=(int)((x+.5f)*m.widthPixels/Brain.W),screenY=(int)((y+.5f)*m.heightPixels/Brain.H);
                // Exclude whole grid cells that overlap the floating buddy.
                int cellL=x*m.widthPixels/Brain.W,cellR=(x+1)*m.widthPixels/Brain.W;
                int cellT=y*m.heightPixels/Brain.H,cellB=(y+1)*m.heightPixels/Brain.H;
                mask[i]=!buddy.isEmpty()&&Rect.intersects(buddy,new Rect(cellL-5,cellT-5,cellR+5,cellB+5));
                float sum=0;
                for(int sy=0;sy<4;sy++)for(int sx=0;sx<4;sx++){
                    int px=Math.min(iw-1,(int)((x+(sx+.5f)/4)*iw/Brain.W));
                    int py=Math.min(ih-1,(int)((y+(sy+.5f)/4)*ih/Brain.H));int offset=py*row+px*pixel;
                    int r=buffer.get(offset)&255,g=buffer.get(offset+1)&255,b=buffer.get(offset+2)&255;
                    sum+=(.299f*r+.587f*g+.114f*b)/255f;
                }
                input[i]=sum/16;
            }
            boolean[] combined=mask.clone();for(int i=0;i<input.length;i++){
                if(oldMask!=null)combined[i]|=oldMask[i];if(combined[i])input[i]=0;
            }
            oldMask=mask;session.maskPrevious(combined);session.process(input,false);
            if(now-saved>60000){session.save();saved=now;}
        }catch(IllegalStateException ignored){
            // An old ImageReader may deliver a queued callback after resize/close.
        }catch(Exception e){session.message="Erro na captura: "+e.getMessage();stopSelf();}
    }
    @Override public void onDestroy(){
        destroyed=true;if(session.mode.equals("tela"))session.mode("pausado");
        worker.post(()->{
            if(display!=null){display.release();display=null;}
            if(reader!=null){reader.close();reader=null;}
            if(projection!=null){projection.unregisterCallback(callback);projection.stop();projection=null;}
            session.save();thread.quitSafely();
        });super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
