package com.brittech.minibuddy;

/** Platform-independent gesture thresholds, placement and reaction sound gate. */
public final class InteractionLogic {
    public static final class Drag {
        private float x,y,lastX,lastY,slop;public boolean moved;
        public void down(float x,float y,float slop){this.x=lastX=x;this.y=lastY=y;this.slop=slop;moved=false;}
        public float[] move(float nx,float ny){
            if(!moved&&Math.hypot(nx-x,ny-y)<=slop)return new float[]{0,0};
            float dx=moved?nx-lastX:nx-x,dy=moved?ny-lastY:ny-y;
            moved=true;lastX=nx;lastY=ny;return new float[]{dx,dy};
        }
    }
    public static float clamp(float value,float min,float max){return Math.max(min,Math.min(Math.max(min,max),value));}
    public static final class SoundGate {
        private String previous;private long last=-10000;
        public boolean changed(String mood,long now,boolean muted){
            boolean change=previous!=null&&!previous.equals(mood);previous=mood;
            if(!change||muted||mood.equals("Pausado")||now-last<4000)return false;
            last=now;return true;
        }
    }
}
