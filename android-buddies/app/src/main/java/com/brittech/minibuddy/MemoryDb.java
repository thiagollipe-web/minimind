package com.brittech.minibuddy;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public final class MemoryDb extends SQLiteOpenHelper {
    public static final String[] ACTIONS={"Observando","Curioso","Padrão recorrente","Repouso"};
    public static class Decision {
        public String id=UUID.randomUUID().toString(),action;
        public List<String> features;
        public boolean rated=false;
    }
    public MemoryDb(Context context){super(context,"memoria.db",null,1);setWriteAheadLoggingEnabled(true);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE notes(id INTEGER PRIMARY KEY,created TEXT NOT NULL,content TEXT NOT NULL,source TEXT NOT NULL,digest TEXT UNIQUE NOT NULL)");
        db.execSQL("CREATE TABLE terms(note_id INTEGER NOT NULL,token TEXT NOT NULL,PRIMARY KEY(note_id,token))");
        db.execSQL("CREATE INDEX idx_terms ON terms(token)");
        db.execSQL("CREATE TABLE feedback(id TEXT PRIMARY KEY,created INTEGER,features TEXT,action TEXT,reward INTEGER)");
        db.execSQL("CREATE TABLE action_stats(feature TEXT,action TEXT,n INTEGER,total REAL,PRIMARY KEY(feature,action))");
        db.execSQL("CREATE TABLE interactions(id INTEGER PRIMARY KEY,created INTEGER,kind TEXT)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV){throw new IllegalStateException("Versão desconhecida da memória");}
    private static String digest(String content){
        try{byte[] hash=MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder s=new StringBuilder();for(byte b:hash)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();
        }catch(Exception e){throw new IllegalStateException(e);}
    }
    public synchronized long saveNote(String text,String source){
        text=text.trim();if(text.isEmpty()||text.length()>100000)throw new IllegalArgumentException("Use de 1 a 100.000 caracteres");
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{long id=insertNote(db,text,source);db.setTransactionSuccessful();return id;}finally{db.endTransaction();}
    }
    private long insertNote(SQLiteDatabase db,String text,String source){
        String hash=digest(text);ContentValues values=new ContentValues();values.put("created",new Date().toString());
        values.put("content",text);values.put("source",source);values.put("digest",hash);
        db.insertWithOnConflict("notes",null,values,SQLiteDatabase.CONFLICT_IGNORE);
        long id;
        try(Cursor cursor=db.rawQuery("SELECT id FROM notes WHERE digest=?",new String[]{hash})){cursor.moveToFirst();id=cursor.getLong(0);}
        for(String word:new HashSet<>(TextTools.tokens(text))){
            ContentValues term=new ContentValues();term.put("note_id",id);term.put("token",word);
            db.insertWithOnConflict("terms",null,term,SQLiteDatabase.CONFLICT_IGNORE);
        }
        return id;
    }
    public synchronized String search(String query){
        List<String> words=new ArrayList<>(new LinkedHashSet<>(TextTools.tokens(query)));
        String sql="SELECT n.id,n.created,n.content FROM notes n ";String[] args=null;
        if(!query.trim().isEmpty()&&words.isEmpty())return "Nenhum termo pesquisável.";
        if(!words.isEmpty()){
            if(words.size()>40)words=words.subList(0,40);
            sql+="JOIN terms t ON t.note_id=n.id WHERE t.token IN ("+String.join(",",Collections.nCopies(words.size(),"?"))+") GROUP BY n.id ORDER BY COUNT(*) DESC,n.id DESC LIMIT 15";
            args=words.toArray(new String[0]);
        }else sql+="ORDER BY n.id DESC LIMIT 15";
        StringBuilder result=new StringBuilder();
        try(Cursor cursor=getReadableDatabase().rawQuery(sql,args)){
            while(cursor.moveToNext())result.append("#").append(cursor.getLong(0)).append(" • ").append(cursor.getString(1)).append("\n").append(cursor.getString(2)).append("\n\n");
        }
        return result.length()==0?"Nenhum registro encontrado.":result.toString();
    }
    public synchronized long count(String table){
        if(!Arrays.asList("notes","feedback","interactions").contains(table))throw new IllegalArgumentException();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM "+table,null)){c.moveToFirst();return c.getLong(0);}
    }
    public synchronized void pet(){ContentValues v=new ContentValues();v.put("created",System.currentTimeMillis());v.put("kind","carinho");getWritableDatabase().insertOrThrow("interactions",null,v);}
    public synchronized Decision suggest(List<String> features,String fallback,boolean explore){
        Decision d=new Decision();d.features=new ArrayList<>(features);d.action=fallback;
        float best=-Float.MAX_VALUE;
        for(String action:ACTIONS){
            float sum=0,divisor=0;
            for(String feature:features)try(Cursor c=getReadableDatabase().rawQuery("SELECT n,total FROM action_stats WHERE feature=? AND action=?",new String[]{feature,action})){
                if(c.moveToFirst()){float weight=feature.startsWith("contexto:")?2:1;sum+=weight*c.getFloat(1)/(c.getInt(0)+1);divisor+=weight;}
            }
            float score=sum/Math.max(1,divisor)+(action.equals(fallback)?.08f:0);
            if(score>best){best=score;d.action=action;}
        }
        Random random=new Random();if(explore&&random.nextFloat()<.1f)d.action=ACTIONS[random.nextInt(ACTIONS.length)];return d;
    }
    public synchronized boolean reward(Decision d,int reward){
        if(d==null||d.rated)return false;
        if((reward!=1&&reward!=-1)||!Arrays.asList(ACTIONS).contains(d.action))throw new IllegalArgumentException();
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            ContentValues values=new ContentValues();values.put("id",d.id);values.put("created",System.currentTimeMillis());values.put("features",new JSONArray(d.features).toString());values.put("action",d.action);values.put("reward",reward);
            if(db.insertWithOnConflict("feedback",null,values,SQLiteDatabase.CONFLICT_IGNORE)==-1)return false;
            for(String f:new LinkedHashSet<>(d.features)){
                db.execSQL("INSERT OR IGNORE INTO action_stats VALUES(?,?,0,0)",new Object[]{f,d.action});
                db.execSQL("UPDATE action_stats SET n=n+1,total=total+? WHERE feature=? AND action=?",new Object[]{reward,f,d.action});
            }
            db.setTransactionSuccessful();d.rated=true;return true;
        }finally{db.endTransaction();}
    }
    public synchronized int importNotes(JSONObject pack)throws JSONException{
        if(pack.optInt("version",-1)!=1)throw new IllegalArgumentException("Formato incompatível");
        JSONArray notes=pack.getJSONArray("notes");if(notes.length()>1000)throw new IllegalArgumentException("Limite de 1000 registros por pack");
        SQLiteDatabase db=getWritableDatabase();long before=count("notes");db.beginTransaction();
        try{
            for(int i=0;i<notes.length();i++){
                JSONObject n=notes.getJSONObject(i);String title=n.optString("title","").trim();String text=n.getString("text").trim();
                String content=(title.isEmpty()?text:title+"\n"+text).trim();
                if(content.isEmpty()||content.length()>100000)throw new IllegalArgumentException("Texto inválido no pack");
                insertNote(db,content,"Pack importado");
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        return (int)(count("notes")-before);
    }
    public synchronized JSONObject exportNotes()throws JSONException{
        JSONObject pack=new JSONObject();pack.put("version",1);pack.put("name","Memória textual MiniBuddy Android");JSONArray notes=new JSONArray();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT content FROM notes ORDER BY id",null)){
            while(c.moveToNext()){JSONObject n=new JSONObject();n.put("title","");n.put("text",c.getString(0));notes.put(n);}
        }
        pack.put("notes",notes);return pack;
    }
}
