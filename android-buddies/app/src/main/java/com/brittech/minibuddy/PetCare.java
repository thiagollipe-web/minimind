package com.brittech.minibuddy;
import android.content.SharedPreferences;
import android.os.SystemClock;
import org.json.*;

/** Persists game state in preferences without changing legacy SQLite or brain files. */
public final class PetCare {
    public final PetState state;
    private final SharedPreferences prefs;
    private final MemoryDb memory;
    private long lastTick=SystemClock.elapsedRealtime(),lastCounters=0,lastSaved=0;
    public PetCare(SharedPreferences prefs,MemoryDb memory){
        this.prefs=prefs;this.memory=memory;
        long notes=memory.count("notes"),votes=memory.count("feedback"),pets=memory.count("interactions");
        state=new PetState(notes,votes,pets);
        String saved=prefs.getString("pet_state_v1",null);
        if(saved!=null)try{
            JSONObject j=new JSONObject(saved);
            state.energy=(float)Math.max(20,Math.min(100,j.optDouble("energy",85)));
            state.happiness=(float)Math.max(30,Math.min(100,j.optDouble("happiness",70)));
            state.bond=Math.max(0,Math.min(100,j.optInt("bond",0)));state.xp=Math.max(0,Math.min(200,j.optInt("xp",0)));
            state.resting=j.optBoolean("resting",false);state.trained=j.optBoolean("trained",false);state.tested=j.optBoolean("tested",false);
            state.baseNotes=j.optLong("baseNotes",notes);state.baseVotes=j.optLong("baseVotes",votes);state.basePets=j.optLong("basePets",pets);
            state.lastFeed=j.optLong("lastFeed",0);
            JSONArray claimed=j.optJSONArray("claimed");if(claimed!=null)for(int i=0;i<5;i++)state.claimed[i]=claimed.optBoolean(i,false);
        }catch(JSONException ignored){}
        refresh();save();
    }
    public synchronized void tick(boolean observing){
        long now=SystemClock.elapsedRealtime();if(now-lastTick<=30000)state.advance((now-lastTick)/1000f,observing);lastTick=now;
        if(now-lastCounters>5000){refresh();lastCounters=now;}
        if(now-lastSaved>15000){save();lastSaved=now;}
    }
    public synchronized void refresh(){state.observe(memory.count("notes"),memory.count("feedback"),memory.count("interactions"));}
    public synchronized void pet(){state.pet();refresh();save();}
    public synchronized boolean feed(){boolean result=state.feed(System.currentTimeMillis());if(result)save();return result;}
    public synchronized void rest(boolean value){state.resting=value;lastTick=SystemClock.elapsedRealtime();save();}
    public synchronized boolean claim(int index){refresh();boolean changed=state.claim(index);if(changed)save();return changed;}
    public synchronized void trained(){state.trained=true;state.tested=false;save();}
    public synchronized void tested(){state.tested=true;save();}
    public synchronized void save(){
        try{
            JSONObject j=new JSONObject();j.put("energy",state.energy);j.put("happiness",state.happiness);j.put("bond",state.bond);j.put("xp",state.xp);
            j.put("resting",state.resting);j.put("trained",state.trained);j.put("tested",state.tested);
            j.put("baseNotes",state.baseNotes);j.put("baseVotes",state.baseVotes);j.put("basePets",state.basePets);j.put("lastFeed",state.lastFeed);
            JSONArray claimed=new JSONArray();for(boolean b:state.claimed)claimed.put(b);j.put("claimed",claimed);
            prefs.edit().putString("pet_state_v1",j.toString()).apply();
        }catch(JSONException e){throw new IllegalStateException(e);}
    }
    public synchronized String summary(){return "Nível "+state.level()+" • "+state.xp+" XP • vínculo "+state.bond+"/100\nEnergia "+Math.round(state.energy)+"/100 • felicidade "+Math.round(state.happiness)+"/100"+(state.resting?" • descansando":"");}
}
