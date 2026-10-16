package com.brittech.minibuddy;
import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.view.*;

/** Subject-specific original characters, drawn with shared vector geometry. */
public final class RobotView extends View {
    private final String buddyId,buddyName;
    public interface Gestures {void pet();void drag(float dx,float dy);void menu();void positionSaved();}
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Session session;
    private final Gestures gestures;
    private final InteractionLogic.Drag drag=new InteractionLogic.Drag();
    private boolean moved,held;
    private long lastPet=0;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable hold;
    public RobotView(Context c,Session s,Gestures g){super(c);SubjectConfig cfg=SubjectConfig.load(c);buddyId=cfg.data.optString("id","matematica");buddyName=cfg.nickname();session=s;gestures=g;hold=()->{held=true;performLongClick();};setContentDescription(buddyName+": toque para perguntas; segure para carinho; arraste para mover");setFocusable(true);setLongClickable(true);}
    private final CanvasPainter art=new CanvasPainter();
    private final class CanvasPainter implements BuddyArt.Painter {
        Canvas canvas;
        private void fill(int color){paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setShader(null);}
        public void ellipse(float x,float y,float rx,float ry,int color){fill(color);canvas.drawOval(x-rx,y-ry,x+rx,y+ry,paint);}
        public void rect(float x,float y,float w,float h,float r,int color){fill(color);canvas.drawRoundRect(x,y,x+w,y+h,r,r,paint);}
        public void line(float x,float y,float xx,float yy,float width,int color){fill(color);paint.setStrokeWidth(width);paint.setStrokeCap(Paint.Cap.ROUND);canvas.drawLine(x,y,xx,yy,paint);}
        public void polygon(int color,float... xy){fill(color);Path path=new Path();path.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);path.close();canvas.drawPath(path,paint);}
        public void push(float x,float y){canvas.save();canvas.translate(x,y);}
        public void pop(){canvas.restore();}
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);c.save();c.scale(getWidth()/160f,getHeight()/178f);art.canvas=c;
        double time=SystemClock.elapsedRealtime()/1000.;boolean happy=SystemClock.elapsedRealtime()<session.happyUntil;
        boolean resting=session.care.state.resting;
        float direction=session.result==null?0:session.result.direction*3;
        BuddyArt.draw(art,buddyId,time,happy,resting,direction,session.care.state.level());
        paint.setColor(0xffd8d5ed);paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));paint.setTextSize(12);
        c.drawText(buddyName+" • Lv "+session.care.state.level(),78,167,paint);paint.setTextSize(20);c.drawText("⋯",143,165,paint);
        c.restore();
    }
    private void pet(){long now=SystemClock.elapsedRealtime();if(now-lastPet>700){lastPet=now;gestures.pet();}}
    @Override public boolean performClick(){super.performClick();gestures.menu();return true;}
    @Override public boolean performLongClick(){pet();return true;}
    @Override public boolean onTouchEvent(android.view.MotionEvent e){
        float y=e.getY()*178/getHeight();
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                drag.down(e.getRawX(),e.getRawY(),ViewConfiguration.get(getContext()).getScaledTouchSlop());moved=false;held=false;
                if(e.getX()*160/getWidth()>126&&y>147){held=true;gestures.menu();}
                else handler.postDelayed(hold,650);
                return true;
            case MotionEvent.ACTION_MOVE:
                float[] delta=drag.move(e.getRawX(),e.getRawY());
                moved=drag.moved;
                if(moved){handler.removeCallbacks(hold);if(!held)gestures.drag(delta[0],delta[1]);}
                return true;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(hold);if(!held&&!moved)performClick();if(moved&&!held)gestures.positionSaved();return true;
            case MotionEvent.ACTION_CANCEL:handler.removeCallbacks(hold);if(moved&&!held)gestures.positionSaved();return true;
            case MotionEvent.ACTION_POINTER_DOWN:handler.removeCallbacks(hold);held=true;return true;
        }return super.onTouchEvent(e);
    }
    @Override protected void onDetachedFromWindow(){handler.removeCallbacksAndMessages(null);super.onDetachedFromWindow();}
}
