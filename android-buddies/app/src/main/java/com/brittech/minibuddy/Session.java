package com.brittech.minibuddy;
import android.content.*;
import android.graphics.Rect;
import android.os.SystemClock;
import java.io.*;

/** Shared state; all brain operations run off the UI thread. */
public final class Session {
    private static Session singleton;
    public final MemoryDb memory;
    public final SharedPreferences prefs;
    public final PetCare care;
    public volatile Brain.Result result;
    public volatile String mode="pausado",message="Pronto. A captura começa desligada.",context="";
    public volatile boolean learning=true,uiVisible=false,bubbleVisible=false;
    public volatile Rect overlayBounds=new Rect();
    public volatile MemoryDb.Decision decision;
    public volatile long happyUntil=0;
    private final File dir;
    private Brain screen,demo;
    private long nextDecision=0,lastMovement=0;
    private Session(Context c){
        memory=new MemoryDb(c);prefs=c.getSharedPreferences("buddy",0);dir=c.getFilesDir();
        learning=prefs.getBoolean("learning",true);
        screen=load("visual_tela.bin");demo=load("visual_demo.bin");
        care=new PetCare(prefs,memory);
        try(EducationDb education=new EducationDb(c)){care.state.bonusXp=education.claim(education.profile(c));}
    }
    public static synchronized Session get(Context c){if(singleton==null)singleton=new Session(c.getApplicationContext());return singleton;}
    private Brain load(String filename){try{return Brain.load(new File(dir,filename));}catch(FileNotFoundException e){return new Brain();}catch(IOException e){message="Memória visual não carregada: "+e.getMessage();return new Brain();}}
    public synchronized void mode(String newMode){
        mode=newMode;if(!newMode.equals("pausado"))care.rest(false);decision=null;nextDecision=0;screen.clearDynamics();demo.clearDynamics();result=null;
    }
    public synchronized void process(float[] image,boolean isDemo){
        if(care.state.resting||!(isDemo?mode.equals("demo"):mode.equals("tela")))return;
        care.tick(true);
        long now=SystemClock.elapsedRealtime();
        result=(isDemo?demo:screen).process(image,learning);
        if(result.activity>.008f)lastMovement=now;
        if(now>=nextDecision){
            String visual=visualMood();
            String feature=result.activity>.008f?"movimento":"estavel";
            if(result.familiar)feature+="-recorrente";
            decision=memory.suggest(TextTools.features(context,feature,mode),visual,learning);nextDecision=now+8000;
        }
    }
    public synchronized void maskPrevious(boolean[] mask){screen.maskPrevious(mask);}
    public String visualMood(){
        Brain.Result r=result;if(r==null||mode.equals("pausado"))return "Pausado";
        if(SystemClock.elapsedRealtime()-lastMovement>4000)return "Repouso";
        if(r.familiar)return "Padrão recorrente";return r.spikes>0?"Curioso":"Observando";
    }
    public String mood(){if(care.state.resting)return "Repouso";MemoryDb.Decision d=decision;return d!=null?d.action:visualMood();}
    public synchronized void setLearning(boolean value){
        learning=value;prefs.edit().putBoolean("learning",value).apply();screen.clearDynamics();demo.clearDynamics();decision=null;nextDecision=0;
    }
    public synchronized boolean rate(int reward){
        if(!learning||mode.equals("pausado"))return false;boolean changed=memory.reward(decision,reward);
        if(changed){care.refresh();decision=null;nextDecision=0;message="Preferência atualizada com feedback "+reward;}
        return changed;
    }
    public synchronized void pet(){
        happyUntil=SystemClock.elapsedRealtime()+3000;memory.pet();rate(1);care.pet();message="Carinho recebido!";
    }
    public synchronized void setContext(String text){context=text;decision=null;nextDecision=0;}
    public synchronized void save(){care.save();try{screen.save(new File(dir,"visual_tela.bin"));demo.save(new File(dir,"visual_demo.bin"));}catch(IOException e){message="Falha ao salvar rede: "+e.getMessage();}}
}
