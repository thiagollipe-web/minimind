package com.brittech.minibuddy;
import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.view.*;

/** Original pastel squircle buddy, inspired by soft expressive desk pets.
 * No Coucou code, images, trademark or recorded sound is bundled.
 */
public final class RobotView extends View {
    private final int buddyColor;private final String buddyName;
    public interface Gestures {void pet();void drag(float dx,float dy);void menu();void positionSaved();}
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Session session;
    private final Gestures gestures;
    private final InteractionLogic.Drag drag=new InteractionLogic.Drag();
    private boolean moved,held;
    private long lastPet=0;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable hold;
    public RobotView(Context c,Session s,Gestures g){super(c);SubjectConfig cfg=SubjectConfig.load(c);buddyColor=cfg.color();buddyName=cfg.nickname();session=s;gestures=g;hold=()->{held=true;gestures.menu();};setContentDescription("Thigas: toque para carinho, arraste pelo corpo inteiro; segure para abrir painel");setFocusable(true);}
    private void round(Canvas c,float l,float t,float r,float b,float radius,int color){paint.setColor(color);paint.setStyle(Paint.Style.FILL);c.drawRoundRect(l,t,r,b,radius,radius,paint);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);c.save();c.scale(getWidth()/160f,getHeight()/178f);
        double time=SystemClock.elapsedRealtime()/1000.;boolean happy=SystemClock.elapsedRealtime()<session.happyUntil;
        String mood=session.mood();boolean sleepy=mood.equals("Repouso")||mood.equals("Pausado");
        float bounce=(float)Math.sin(time*3)*(happy?3:1.3f);c.translate(0,bounce);
        paint.setColor(0x243b305d);c.drawOval(27,139,133,153,paint);
        // Small feet and soft side hands.
        round(c,43,127,66,147,10,0xffada8dc);round(c,94,127,117,147,10,0xffada8dc);
        round(c,12,77,33,105,10,0xffe8e5ff);round(c,127,77,148,105,10,0xffe8e5ff);
        paint.setShader(new LinearGradient(25,25,135,135,new int[]{0xfffaf9ff,0xffdfd9ff,buddyColor},null,Shader.TileMode.CLAMP));
        c.drawRoundRect(24,26,136,136,35,35,paint);paint.setShader(null);
        paint.setColor(0xfff7f3ff);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);c.drawRoundRect(25,27,135,135,34,34,paint);paint.setStyle(Paint.Style.FILL);
        round(c,32,38,128,121,28,0xfff8f6ff);
        float direction=session.result==null?0:session.result.direction*6;
        boolean blink=((int)(time*10)%51)<2;
        for(float x:new float[]{58,102}){
            if(happy){
                paint.setColor(0xff313251);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(5);paint.setStrokeCap(Paint.Cap.ROUND);
                c.drawArc(x-10,65,x+10,86,195,150,false,paint);paint.setStyle(Paint.Style.FILL);
            }else if(sleepy||blink){round(c,x-10,77,x+10,82,3,0xff343450);}
            else{
                round(c,x-10+direction,57,x+10+direction,89,9,0xff343450);
                round(c,x-6+direction,61,x-1+direction,69,3,0xffc6d5ff);
            }
        }
        paint.setColor(0x75ffb1cd);c.drawOval(39,89,55,98,paint);c.drawOval(105,89,121,98,paint);
        paint.setColor(0xff6c6587);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setStrokeCap(Paint.Cap.ROUND);
        c.drawArc(73,91,87,103,10,160,false,paint);paint.setStyle(Paint.Style.FILL);
        if(happy){
            float rise=(float)((time%1)*12);
            for(float x:new float[]{29,129}){
                Path heart=new Path();float y=20-rise;
                heart.moveTo(x,y+8);heart.cubicTo(x-18,y-2,x-4,y-13,x,y-5);heart.cubicTo(x+5,y-13,x+18,y-2,x,y+8);
                paint.setColor(0xfffa86b8);c.drawPath(heart,paint);
            }
        }
        if(session.care.state.level()>=2){
            paint.setColor(0xffedb95d);Path star=new Path();
            for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float radius=i%2==0?8:3.5f;
                float x=117+(float)Math.cos(a)*radius,y=114+(float)Math.sin(a)*radius;if(i==0)star.moveTo(x,y);else star.lineTo(x,y);}
            star.close();c.drawPath(star,paint);
        }
        if(session.care.state.level()>=3){
            paint.setColor(0xffe3bc68);Path crown=new Path();crown.moveTo(57,28);crown.lineTo(54,13);crown.lineTo(68,22);crown.lineTo(80,8);crown.lineTo(93,22);crown.lineTo(106,13);crown.lineTo(103,28);crown.close();c.drawPath(crown,paint);
        }
        paint.setColor(0xffc2bcdf);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));paint.setTextSize(12);
        c.drawText(happy?"♥":buddyName+" • Lv "+session.care.state.level(),78,167,paint);paint.setTextSize(20);c.drawText("⋯",143,165,paint);
        c.restore();
    }
    private void pet(){long now=SystemClock.elapsedRealtime();if(now-lastPet>700){lastPet=now;gestures.pet();performClick();}}
    @Override public boolean performClick(){super.performClick();return true;}
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
                handler.removeCallbacks(hold);if(!held&&!moved)pet();if(moved&&!held)gestures.positionSaved();return true;
            case MotionEvent.ACTION_CANCEL:handler.removeCallbacks(hold);if(moved&&!held)gestures.positionSaved();return true;
            case MotionEvent.ACTION_POINTER_DOWN:handler.removeCallbacks(hold);held=true;return true;
        }return super.onTouchEvent(e);
    }
    @Override protected void onDetachedFromWindow(){handler.removeCallbacksAndMessages(null);super.onDetachedFromWindow();}
}
