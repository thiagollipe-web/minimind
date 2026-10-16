import com.brittech.minibuddy.InteractionLogic;
public final class InteractionTest {
    static void check(boolean b,String text){if(!b)throw new AssertionError(text);}
    public static void main(String[] args){
        InteractionLogic.Drag gesture=new InteractionLogic.Drag();
        gesture.down(20,45,8);float[] d=gesture.move(22,46);
        check(!gesture.moved&&d[0]==0,"Small finger jitter must remain a tap");
        d=gesture.move(80,45);
        check(gesture.moved&&d[0]==60,"Dragging from the face must produce motion");
        d=gesture.move(110,60);check(d[0]==30&&d[1]==15,"Drag deltas must track absolute finger position");
        check(InteractionLogic.clamp(999,6,300)==300,"Right edge must remain reachable and bounded");
        check(InteractionLogic.clamp(-999,6,300)==6,"Left edge must remain reachable and bounded");
        InteractionLogic.SoundGate sounds=new InteractionLogic.SoundGate();
        check(!sounds.changed("Observando",0,false),"Initial state must not spam audio");
        check(sounds.changed("Curioso",100,false),"A reaction change must trigger audio");
        check(!sounds.changed("Repouso",200,false),"Rapid changes must not spam audio");
        check(!sounds.changed("Curioso",5000,true),"Muted reactions must remain silent");
        check(sounds.changed("Padrão recorrente",6000,false),"Unmuted reaction must sound");
        check(!sounds.changed("Padrão recorrente",12000,false),"Unchanged mood must not repeat audio");
        System.out.println("OK: face drag, tap jitter, deltas, both edges, reaction sound events, cooldown and mute.");
    }
}
