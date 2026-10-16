package com.brittech.minibuddy;
import android.content.Context;
import org.json.*;
import java.io.*;
public final class SubjectConfig {
 public final JSONObject data; public final JSONArray questions;
 private SubjectConfig(JSONObject d)throws JSONException{data=d;questions=d.getJSONArray("questions");}
 public String name(){return data.optString("name","MiniBuddy");}
 public String nickname(){return data.optString("nickname","Buddy");}
 public int color(){return android.graphics.Color.parseColor(data.optString("color","#D7BDE2"));}
 public static SubjectConfig load(Context c){try(InputStream in=c.getAssets().open("buddy_config.json");ByteArrayOutputStream b=new ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)b.write(buf,0,n);return new SubjectConfig(new JSONObject(new String(b.toByteArray(),"UTF-8")));}catch(Exception e){throw new IllegalStateException("Catálogo indisponível",e);}}
}
