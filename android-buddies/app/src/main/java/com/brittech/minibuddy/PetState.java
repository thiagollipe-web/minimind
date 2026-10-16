package com.brittech.minibuddy;

/** Game rules only. XP, needs and mission completion are not neural intelligence. */
public final class PetState {
    public static final String[] TITLES={"Primeiros carinhos","Uma nova lembrança","Ensinar preferências","Treinar padrões visuais","Testar com pesos congelados"};
    public static final int[] TARGETS={3,1,5,1,1},REWARDS={20,25,35,60,60};
    public volatile float energy=85,happiness=70;
    public volatile int bond=0,xp=0,bonusXp=0;
    public volatile boolean resting=false,trained=false,tested=false;
    public long baseNotes,baseVotes,basePets,lastFeed;
    private long notes,votes,pets;
    public final boolean[] claimed=new boolean[5];
    public PetState(long n,long v,long p){baseNotes=n;baseVotes=v;basePets=p;observe(n,v,p);}
    public synchronized void observe(long n,long v,long p){notes=n;votes=v;pets=p;}
    public synchronized int progress(int index){
        long raw=index==0?pets-basePets:index==1?notes-baseNotes:index==2?votes-baseVotes:index==3?(trained?1:0):(tested?1:0);
        return (int)Math.max(0,Math.min(TARGETS[index],raw));
    }
    public synchronized boolean claim(int index){
        if(index<0||index>=5||claimed[index]||progress(index)<TARGETS[index])return false;
        claimed[index]=true;xp+=REWARDS[index];happiness=Math.min(100,happiness+5);return true;
    }
    public synchronized boolean isClaimed(int index){return claimed[index];}
    public synchronized int level(){return 1+(xp+bonusXp)/75;}
    public synchronized void pet(){happiness=Math.min(100,happiness+4);bond=Math.min(100,bond+1);}
    public synchronized boolean feed(long now){
        if(lastFeed>0&&now>=lastFeed&&now-lastFeed<120000)return false;
        lastFeed=now;energy=Math.min(100,energy+10);happiness=Math.min(100,happiness+2);return true;
    }
    /** Only active runtime elapsed is supplied. Sleeping/closed Android time is ignored. */
    public synchronized void advance(float seconds,boolean observing){
        seconds=Math.max(0,Math.min(30,seconds));
        if(resting)energy=Math.min(100,energy+seconds/20f);
        else if(observing){energy=Math.max(20,energy-seconds/300f);happiness=Math.max(30,happiness-seconds/900f);}
    }
    public synchronized String accessory(){return level()>=3?"Coroa de explorador":level()>=2?"Estrela de aprendiz":"Visual inicial";}
}
