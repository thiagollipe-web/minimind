import com.brittech.minibuddy.BuddyArt;
import java.util.*;
/** Check the public render stream: distinct geometry, bounded coordinates, balanced state and reactions. */
public final class BuddyArtTest {
    static final class Recorder implements BuddyArt.Painter {
        final StringBuilder data=new StringBuilder();int depth,shapes;
        void record(String kind,int color,float... points){shapes++;data.append(kind).append(color);for(float v:points){if(!Float.isFinite(v)||v< -8||v>168)throw new AssertionError("Invalid/out-of-bounds coordinate: "+v);data.append('|').append(v);}data.append(';');}
        public void ellipse(float x,float y,float rx,float ry,int c){record("ellipse",c,x,y,rx,ry);}
        public void rect(float x,float y,float w,float h,float r,int c){record("rect",c,x,y,w,h,r);}
        public void line(float x,float y,float xx,float yy,float w,int c){record("line",c,x,y,xx,yy,w);}
        public void polygon(int c,float... p){if(p.length<6||p.length%2!=0)throw new AssertionError("Polygon");record("polygon",c,p);}
        public void push(float x,float y){depth++;record("push",0,x,y);}
        public void pop(){if(--depth<0)throw new AssertionError("Transform underflow");data.append("pop;");}
        String signature(){if(depth!=0||shapes<15)throw new AssertionError("Incomplete drawing");return data.toString();}
    }
    static String draw(String id,double time,boolean happy,boolean rest,int level){Recorder r=new Recorder();BuddyArt.draw(r,id,time,happy,rest,0,level);return r.signature();}
    public static void main(String[] args){
        Set<String> shapesWithoutColor=new HashSet<>();
        for(String id:BuddyArt.IDS){
            String normal=draw(id,.7,false,false,1);
            // Ignore paint colors: all twelve must still produce different geometry streams.
            String geometry=normal.replaceAll("(ellipse|rect|line|polygon|push)-?[0-9]+","$1");
            if(!shapesWithoutColor.add(geometry))throw new AssertionError("Duplicate geometry: "+id);
            if(normal.equals(draw(id,1.1,false,false,1)))throw new AssertionError("No animation: "+id);
            if(normal.equals(draw(id,.7,true,false,1)))throw new AssertionError("No pet reaction: "+id);
            if(normal.equals(draw(id,.7,false,true,1)))throw new AssertionError("No resting expression: "+id);
            if(!draw(id,.7,false,true,1).equals(draw(id,1.1,false,true,1)))throw new AssertionError("Resting animation did not stop: "+id);
            if(normal.equals(draw(id,.7,false,false,3)))throw new AssertionError("Lost level accessories: "+id);
            for(double t:new double[]{0,.2,2.7,10,100})for(int level:new int[]{1,2,3})draw(id,t,true,false,level);
        }
        System.out.println("BuddyArtTest OK: 12 distinct geometries, animation, pet reactions, rest and level accessories.");
    }
}
