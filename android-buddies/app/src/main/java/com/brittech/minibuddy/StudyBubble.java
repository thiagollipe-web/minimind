package com.brittech.minibuddy;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.util.Random;
/** Small focusable overlay: reply to the apprentice without opening the full app. */
public final class StudyBubble {
 private final Context c;private final Session session;private final WindowManager wm;private View view;private EducationDb db;
 public StudyBubble(Context c,Session s){this.c=c;session=s;wm=c.getSystemService(WindowManager.class);}
 public boolean visible(){return view!=null;}
 public void close(){if(view!=null){((InputMethodManager)c.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(),0);try{wm.removeView(view);}catch(IllegalArgumentException ignored){}view=null;}if(db!=null){db.close();db=null;}session.bubbleVisible=false;}
 public void show(){if(view!=null)return;db=new EducationDb(c);SubjectConfig config=SubjectConfig.load(c);JSONObject q=config.questions.optJSONObject(new Random().nextInt(config.questions.length()));String profile=db.profile(c);
  LinearLayout box=new LinearLayout(c);box.setOrientation(1);box.setPadding(dp(14),dp(10),dp(14),dp(10));box.setBackgroundColor(0xff20263b);
  TextView title=new TextView(c);title.setText(config.nickname()+" quer aprender:\n"+q.optString("teachQuestion"));title.setTextColor(Color.WHITE);title.setTextSize(16);box.addView(title);
  EditText answer=new EditText(c);answer.setTextColor(Color.WHITE);answer.setHintTextColor(0xffcccccc);answer.setHint("Sua explicação e um exemplo");answer.setMinLines(2);answer.setMaxLines(4);box.addView(answer);
  Button save=new Button(c);save.setText("Salvar para revisão");box.addView(save);save.setOnClickListener(v->{try{db.teach(profile,q.optString("teachQuestion"),answer.getText().toString());Toast.makeText(c,"Explicação pendente de revisão no painel.",Toast.LENGTH_LONG).show();Chirps.play(session);close();}catch(Exception e){answer.setError(e.getMessage());}});
  LinearLayout actions=new LinearLayout(c);Button panel=new Button(c);panel.setText("Estudar");Button quit=new Button(c);quit.setText("Fechar");actions.addView(panel,new LinearLayout.LayoutParams(0,-2,1));actions.addView(quit,new LinearLayout.LayoutParams(0,-2,1));box.addView(actions);panel.setOnClickListener(v->{close();c.startActivity(new Intent(c,EducationActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));});quit.setOnClickListener(v->close());
  int screen=c.getResources().getDisplayMetrics().widthPixels;WindowManager.LayoutParams p=new WindowManager.LayoutParams(Math.min(dp(310),screen-dp(20)),-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;p.y=dp(70);p.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
  try{wm.addView(box,p);view=box;session.bubbleVisible=true;}catch(Exception e){close();session.message="Balão indisponível: "+e.getMessage();}
 }
 private int dp(int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
}
