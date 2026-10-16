package com.brittech.minibuddy;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Compact study balloon: lessons, checked exercises and apprentice replies next to the pet. */
public final class StudyBubble {
    private final Context c;
    private final Session session;
    private final WindowManager wm;
    private View view;
    private EducationDb db;
    private SubjectConfig config;
    private String profile,tab="exercicio",policy,context;
    private int grade,generation;
    private JSONObject lesson,question;
    private PedagogyPolicy.Choice choice;
    private boolean answered,assisted,strategyShown;
    private LinearLayout body;
    private ScrollView contentScroll;
    private WindowManager.LayoutParams params;

    public StudyBubble(Context c,Session s){this.c=c;session=s;wm=c.getSystemService(WindowManager.class);}
    public boolean visible(){return view!=null;}
    public void close(){
        if(view!=null){
            ((InputMethodManager)c.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(),0);
            try{wm.removeView(view);}catch(IllegalArgumentException ignored){}
            view=null;
        }
        if(db!=null){db.close();db=null;}
        session.bubbleVisible=false;
    }
    private int dp(int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    private void text(LinearLayout parent,String value,int size){TextView v=new TextView(c);v.setText(value);v.setTextColor(Color.WHITE);v.setTextSize(size);v.setPadding(dp(2),dp(5),dp(2),dp(5));parent.addView(v);}
    private Button button(LinearLayout parent,String title,Runnable action){
        Button b=new Button(c);b.setText(title);b.setAllCaps(false);parent.addView(b);
        b.setOnClickListener(v->{if(db==null)return;try{action.run();}catch(Exception e){Toast.makeText(c,"Não foi possível concluir: "+e.getMessage(),Toast.LENGTH_LONG).show();}});return b;
    }
    private void chooseLesson(){
        question=LessonCatalog.nextQuestion(c,config,profile,grade);
        lesson=LessonCatalog.findLesson(config,question.optString("lesson_id"));
        if(lesson==null)throw new IllegalStateException("Exercício sem miniaula");
        generation++;answered=false;strategyShown=false;
        assisted=db.recentLesson(profile,lesson.optString("id"));context=db.context(profile,question.optString("skill"));
        policy=c.getSharedPreferences("education",0).getString("policy_"+profile,"adaptativo");choice=db.choose(profile,context,policy);
    }
    public void show(){
        if(view!=null){try{if(profile.equals(db.profile(c))){chooseLesson();tab="exercicio";render();return;}}catch(Exception e){session.message=e.getMessage();}close();}
        try{
            db=new EducationDb(c);config=SubjectConfig.load(c);profile=db.profile(c);grade=LessonCatalog.selectedGrade(c,config,profile);
            tab="exercicio";chooseLesson();
            LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(10),dp(8),dp(10),dp(6));
            GradientDrawable background=new GradientDrawable();background.setColor(0xff20263b);background.setCornerRadius(dp(18));background.setStroke(dp(2),config.color());box.setBackground(background);
            text(box,config.nickname()+" • "+config.name(),17);
            String name=db.profiles().get(profile);text(box,"Perfil: "+(name==null?profile:name),12);
            int[] grades=LessonCatalog.grades(config);String[] labels=new String[grades.length];int selected=0;
            for(int i=0;i<grades.length;i++){labels[i]=LessonCatalog.gradeLabel(grades[i]);if(grades[i]==grade)selected=i;}
            Spinner years=new Spinner(c);years.setContentDescription("Ano escolar das miniaulas e exercícios");years.setAdapter(new ArrayAdapter<>(c,android.R.layout.simple_spinner_dropdown_item,labels));years.setSelection(selected);box.addView(years);
            years.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                public void onNothingSelected(AdapterView<?> parent){}
                public void onItemSelected(AdapterView<?> parent,View v,int n,long id){if(db!=null&&grades[n]!=grade){grade=grades[n];LessonCatalog.selectGrade(c,profile,grade);chooseLesson();tab="exercicio";render();}}
            });
            LinearLayout tabs=new LinearLayout(c);
            String[] modes={"aula","exercicio","ensinar"},titles={"Miniaula","Exercício","Ensinar"};
            for(int i=0;i<modes.length;i++){final String mode=modes[i];Button b=button(tabs,titles[i],()->{tab=mode;render();});b.setTextSize(11);b.setPadding(0,0,0,0);b.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));}box.addView(tabs);
            ScrollView scroll=new ScrollView(c);contentScroll=scroll;scroll.setScrollbarFadingEnabled(false);scroll.setFillViewport(true);body=new LinearLayout(c);body.setOrientation(LinearLayout.VERTICAL);scroll.addView(body);box.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
            LinearLayout actions=new LinearLayout(c);Button care=button(actions,"♡ Carinho",()->{session.pet();Chirps.play(session);});care.setTextSize(11);care.setContentDescription("Fazer carinho no Buddy");Button panel=button(actions,"Painel",()->{close();c.startActivity(new Intent(c,EducationActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));});Button quit=button(actions,"Fechar",this::close);
            care.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));panel.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));quit.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));box.addView(actions);
            Rect area=usableArea();int width=Math.min(dp(320),area.width()-dp(12)),height=Math.min(dp(460),area.height()-dp(16));
            params=new WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);params.gravity=Gravity.TOP|Gravity.LEFT;params.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
            placeNearPet(area);render();wm.addView(box,params);view=box;session.bubbleVisible=true;
        }catch(Exception e){close();session.message="Balão indisponível: "+e.getMessage();}
    }
    private Rect usableArea(){
        if(Build.VERSION.SDK_INT>=30){WindowMetrics metrics=wm.getCurrentWindowMetrics();Rect area=new Rect(metrics.getBounds());Insets b=metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());area.left+=b.left;area.top+=b.top;area.right-=b.right;area.bottom-=b.bottom;return area;}
        android.util.DisplayMetrics m=c.getResources().getDisplayMetrics();return new Rect(dp(6),dp(28),m.widthPixels-dp(6),m.heightPixels-dp(28));
    }
    private void placeNearPet(Rect area){
        Rect pet=session.overlayBounds;int gap=dp(8),x=pet.centerX()-params.width/2,y;
        if(pet.right+gap+params.width<=area.right){x=pet.right+gap;y=pet.centerY()-params.height/2;}
        else if(pet.left-gap-params.width>=area.left){x=pet.left-gap-params.width;y=pet.centerY()-params.height/2;}
        else if(pet.top-gap-params.height>=area.top)y=pet.top-gap-params.height;
        else y=pet.bottom+gap;
        params.x=Math.max(area.left+dp(4),Math.min(x,area.right-params.width-dp(4)));
        params.y=Math.max(area.top+dp(4),Math.min(y,area.bottom-params.height-dp(4)));
    }
    private void render(){
        body.removeAllViews();contentScroll.post(()->contentScroll.scrollTo(0,0));if(!"exercicio".equals(tab))text(body,lesson.optString("title"),18);text(body,LessonCatalog.tag(lesson),12);
        if("aula".equals(tab)){
            db.openLesson(profile,lesson.optString("id"));assisted=true;
            text(body,LessonCatalog.lessonText(lesson),15);
            button(body,"Praticar este conteúdo",()->{tab="exercicio";render();});
        }else if("ensinar".equals(tab)){
            text(body,question.optString("teachQuestion"),15);EditText answer=new EditText(c);answer.setTextColor(Color.WHITE);answer.setHintTextColor(0xffcccccc);answer.setHint("Sua explicação e um exemplo");answer.setMinLines(3);answer.setMaxLines(5);body.addView(answer);
            button(body,"Salvar explicação pendente",()->{db.teach(profile,question.optString("teachQuestion"),answer.getText().toString());answer.setText("");text(body,"Registrado para revisão humana no painel.",14);Chirps.play(session);});
        }else{
            text(body,question.optString("question"),16);
            if(answered){text(body,"Resposta já registrada. Use Outro exercício para tentar novamente.",14);}
            else{
                JSONArray options=question.optJSONArray("options");ArrayList<Integer> order=new ArrayList<>();for(int i=0;i<options.length();i++)order.add(i);Collections.shuffle(order);
                final int displayedGeneration=generation;
                for(int option:order){Button b=button(body,options.optString(option),()->{if(displayedGeneration==generation)answer(option);});b.setTextSize(13);}
                button(body,"Dica: "+PedagogyPolicy.ACTIONS[choice.action],()->{if(answered)return;assisted=true;strategyShown=true;String[] keys={"explanation","example","guide","prerequisite"};text(body,question.optString(keys[choice.action]),14);});
            }
        }
        text(body,"Perguntas deste ano: "+LessonCatalog.questionsAtGrade(config,grade).length()+" • sorteio sem repetição na rodada",11);
        button(body,"Outro exercício deste ano",()->{chooseLesson();tab="exercicio";render();});
        button(body,"Ver referência BNCC",()->{text(body,"Referência curricular, não certificação do MEC. "+lesson.optString("alignment_note")+"\n"+lesson.optString("source")+"\nPágina "+lesson.optInt("source_pdf_page")+" do PDF.",12);});
    }
    private void answer(int option){
        if(answered)return;boolean correct=option==question.optInt("answer");
        boolean supported=assisted||db.recentLesson(profile,lesson.optString("id"));
        db.curriculumAttempt(profile,question,context,choice,policy,correct,supported,strategyShown,"balao");answered=true;
        body.removeAllViews();contentScroll.post(()->contentScroll.scrollTo(0,0));text(body,LessonCatalog.tag(question),12);text(body,correct?"Muito bem!":"Vamos compreender a resposta.",18);
        text(body,"Resposta: "+question.optJSONArray("options").optString(question.optInt("answer"))+"\n\n"+question.optString("explanation"),16);
        text(body,(supported?"Prática com apoio da miniaula ou dica.":"Tentativa sem apoio exibido pelo app.")+" A revisão posterior fica no painel de estudos.",12);
        session.care.state.bonusXp=db.claim(profile);if(correct)Chirps.play(session);
        button(body,"Rever a miniaula",()->{tab="aula";render();});button(body,"Outro exercício",()->{chooseLesson();tab="exercicio";render();});
    }
}
