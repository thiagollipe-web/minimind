package com.brittech.minibuddy;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.media.projection.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private int accent; private LinearLayout root;
    private Session session;private LinearLayout content;private TextView stats,output,message;
    private EditText text,query;private RobotView preview;private Button learning;private Spinner action;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private boolean pendingProjection;
    private static final int CAPTURE=101,IMPORT=102,EXPORT=103;
    interface Task {String run()throws Exception;}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);session=Session.get(this);
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
        accent=SubjectConfig.load(this).color();
        getWindow().setStatusBarColor(0xff101827);getWindow().setNavigationBarColor(0xff101827);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(20),dp(20),dp(28));content.setBackgroundColor(0xff101827);root=content;
        scroll.addView(content);setContentView(scroll);
        if(Build.VERSION.SDK_INT>=30){
            scroll.setOnApplyWindowInsetsListener((view,insets)->{
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                scroll.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;
            });scroll.requestApplyInsets();
        }
        label("MINIBUDDIES  /  SEU CANTINHO",12);
        section("Olá! Vamos aprender juntos?",false);
        title(SubjectConfig.load(this).nickname(),32);
        label("Seu companheiro de "+SubjectConfig.load(this).name()+" está esperando por você.",16);
        preview=new RobotView(this,session,new RobotView.Gestures(){
            public void pet(){session.pet();Chirps.play(session);}
            public void drag(float dx,float dy){}
            public void menu(){if(Settings.canDrawOverlays(MainActivity.this)){startForegroundService(new Intent(MainActivity.this,OverlayService.class).setAction("STUDY"));moveTaskToBack(true);}else startActivity(new Intent(MainActivity.this,EducationActivity.class));}
            public void positionSaved(){}
        });
        LinearLayout.LayoutParams previewParams=new LinearLayout.LayoutParams(dp(140),dp(156));previewParams.gravity=Gravity.CENTER_HORIZONTAL;content.addView(preview,previewParams);
        label("Toque em mim para estudar • segure para fazer carinho",13);
        button("Vamos estudar  →",()->startActivity(new Intent(this,EducationActivity.class)));
        button("Meu bichinho e minhas missões",()->startActivity(new Intent(this,MissionsActivity.class)));
        label("No seu ritmo. Sem internet. Com companhia.",12);
        section("✦  Leve seu Buddy com você",false);
        label("Ative o mascote para abrir exercícios sobre outros aplicativos.",14);
        button("Permitir mascote na tela",()->{
            try{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));}catch(Exception e){toast("Abra as configurações de permissões do MiniBuddy: "+e.getMessage());}
        });
        button("Mostrar meu Buddy",()->startOverlay(false));
        button("Miniaulas e exercícios BNCC no balão",()->{if(!Settings.canDrawOverlays(this)){toast("Primeiro permita aparecer sobre outros apps.");return;}startForegroundService(new Intent(this,OverlayService.class).setAction("STUDY"));moveTaskToBack(true);});
        section("↔  Aparência e posição",true);
        label("Escolha onde seu companheiro vai ficar.",14);
        button("Colocar no lado esquerdo",()->position("LEFT"));
        button("Colocar no lado direito",()->position("RIGHT"));
        button("Colocar no centro",()->position("CENTER"));
        Switch mute=new Switch(this);mute.setText("Silenciar sons");mute.setTextColor(Color.WHITE);mute.setChecked(session.prefs.getBoolean("muted",false));mute.setOnCheckedChangeListener((v,checked)->session.prefs.edit().putBoolean("muted",checked).apply());content.addView(mute);
        label("Volume dos sons do robô",14);
        SeekBar volume=new SeekBar(this);volume.setMax(100);volume.setProgress(Math.round(session.prefs.getFloat("volume",.8f)*100));
        volume.setContentDescription("Volume dos sons do robô");
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int value,boolean user){if(user)session.prefs.edit().putFloat("volume",value/100f).apply();}
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){}
        });content.addView(volume);
        label("Sons usam o volume de mídia. O botão lateral do celular ajusta esse volume neste painel.",12);
        button("Testar som",()->{
            android.media.AudioManager audio=getSystemService(android.media.AudioManager.class);
            if(session.prefs.getBoolean("muted",false)){toast("Desative Silenciar sons primeiro.");return;}
            if(session.prefs.getFloat("volume",.8f)<=0){toast("Aumente o controle de volume do robô.");return;}
            if(audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)==0){toast("O volume de mídia do celular está em zero. Aumente pelo botão lateral.");return;}
            Chirps.play(session);
        });
        label("Tamanho do mascote flutuante",14);
        Spinner sizes=new Spinner(this);sizes.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"80 dp — bem pequeno","100 dp — pequeno","130 dp — maior"}));
        int size=session.prefs.getInt("size",100);sizes.setSelection(size==80?0:size==130?2:1);
        sizes.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> p){}
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){session.prefs.edit().putInt("size",new int[]{80,100,130}[position]).apply();}
        });content.addView(sizes);label("O tamanho é aplicado ao fechar e mostrar novamente o mascote.",12);
        section("✎  Convites para aprender",true);
        Switch questions=new Switch(this);questions.setText("Abrir balões de estudo enquanto o mascote está ativo (silêncio 22h–7h)");questions.setTextColor(Color.WHITE);questions.setChecked(session.prefs.getBoolean("study_questions",false));questions.setOnCheckedChangeListener((v,on)->session.prefs.edit().putBoolean("study_questions",on).putLong("last_study_question",System.currentTimeMillis()).apply());content.addView(questions);
        Spinner intervals=new Spinner(this);intervals.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"A cada 1 hora","A cada 3 horas","A cada 6 horas"}));int hours=session.prefs.getInt("study_hours",3);intervals.setSelection(hours==1?0:hours==6?2:1);intervals.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int n,long id){session.prefs.edit().putInt("study_hours",new int[]{1,3,6}[n]).apply();}});content.addView(intervals);
        label("Toque no mascote ou em ⋯ para abrir uma pergunta com alternativas no balão. Segure para carinho. Balões automáticos dependem do serviço ativo; não há garantia de execução 24h pelo Android.",12);
        section("✧  Aprendizado e reações",true);
        button("Observar tela — pedir autorização",this::capture);
        button("Pausar captura / demonstração",()->{stopService(new Intent(this,CaptureService.class));session.mode("pausado");session.message="Observação pausada; carinho e memória continuam disponíveis.";});
        button("Demonstração visual",()->startOverlay(true));
        learning=button("Aprendizagem",()->session.setLearning(!session.learning));

        button("A reação foi útil (+1)",()->toast(session.rate(1)?"Preferência atualizada":"Ative observação e aprendizagem; aguarde uma ação ainda não avaliada."));
        button("A reação foi inadequada (−1)",()->toast(session.rate(-1)?"Preferência atualizada":"Sem ação disponível ou aprendizagem congelada."));
        action=new Spinner(this);action.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,MemoryDb.ACTIONS));content.addView(action);
        button("Ensinar reação preferida",()->{
            if(!session.learning||session.mode.equals("pausado")){toast("Ative a aprendizagem e uma observação primeiro.");return;}
            MemoryDb.Decision d=session.memory.suggest(TextTools.features(session.context,session.result!=null&&session.result.activity>.008f?"movimento":"estavel",session.mode),"Observando",false);
            d.action=MemoryDb.ACTIONS[action.getSelectedItemPosition()];session.decision=d;session.rate(1);toast("Preferência registrada.");
        });
        section("▤  Nosso caderno de memórias",true);
        label("Digite ou cole um texto. Salvar também o usa como contexto das reações.",14);
        text=new EditText(this);text.setHint("Nome, projeto, conversa ou anotação…");text.setMinLines(3);text.setGravity(Gravity.TOP);text.setMaxLines(8);text.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);content.addView(text);
        button("Salvar texto e usar como contexto",()->{String value=text.getText().toString().trim();runIO(()->{long id=session.memory.saveNote(value,"Texto registrado no Android");session.setContext(value);return "Texto #"+id+" salvo e contexto ativado.";});});
        button("Limpar contexto ativo",()->{session.setContext("");toast("Contexto limpo. Seus registros continuam salvos.");});
        query=new EditText(this);query.setSingleLine(true);query.setHint("Pesquisar palavras na memória");query.setContentDescription("Pesquisar palavras na memória");content.addView(query);
        button("Pesquisar / ver registros recentes",()->{String q=query.getText().toString();runIO(()->session.memory.search(q),true);});
        output=label("Os registros encontrados aparecerão aqui.",14);output.setTextIsSelectable(true);
        button("Carregar conceitos iniciais da matéria",()->runIO(()->{
            try(InputStream stream=getAssets().open("pack_subject.json")){int n=session.memory.importNotes(new JSONObject(readText(stream)));return n+" registros novos importados; duplicados preservados uma única vez.";}
        }));
        button("Importar pack JSON do Windows / celular",()->{
            Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,IMPORT);
        });
        button("Exportar memória textual JSON",()->startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"MiniBuddy_Memoria_Textual.json"),EXPORT));
        section("♡  Como seu Buddy está?",true);
        stats=label("Carregando indicadores…",14);message=label(session.message,14);
        section("Configurações e ajuda",true);
        label("No mascote: toque para abrir perguntas com alternativas; segure para carinho e arraste pelo corpo para mover. O botão Painel fica dentro do balão. A captura pausa enquanto o painel ou balão está aberto.",14);
        label("STDP aprende regularidades visuais. Textos são pesquisados por palavras. Reações aprendem com avaliações. Não há LLM, OCR ou leitura automática de conversas.",14);
        button("Recolher painel",()->moveTaskToBack(true));
        button("Encerrar mascote e salvar",()->{
            stopService(new Intent(this,CaptureService.class));stopService(new Intent(this,OverlayService.class));session.mode("pausado");runIO(()->{session.save();return "Serviços encerrados e memória visual salva.";});
        });
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},9);
    }
    private void position(String side){
        if(!Settings.canDrawOverlays(this)){toast("Permita aparecer sobre outros apps primeiro.");return;}
        startForegroundService(new Intent(this,OverlayService.class).setAction("MOVE_"+side));
        toast("Posição atualizada. Recolha o painel para conferir.");
    }
    private void startOverlay(boolean demo){
        if(!Settings.canDrawOverlays(this)){toast("Primeiro permita aparecer sobre outros apps.");return;}
        startForegroundService(new Intent(this,OverlayService.class).setAction(demo?"DEMO":"SHOW"));
        if(demo)toast("Demonstração iniciada. Pode recolher o painel.");else toast("Mascote ativado. Toque para perguntas; segure para carinho.");
    }
    private void capture(){
        if(pendingProjection){toast("Aguarde a autorização em andamento.");return;}
        if(session.mode.equals("tela")){toast("A captura já está ativa. Recolha o painel para observar outros apps.");return;}
        MediaProjectionManager manager=getSystemService(MediaProjectionManager.class);
        Intent consent=Build.VERSION.SDK_INT>=34?manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay()):manager.createScreenCaptureIntent();
        pendingProjection=true;startActivityForResult(consent,CAPTURE);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==CAPTURE){
            pendingProjection=false;
            if(result==RESULT_OK&&data!=null){
                try{startForegroundService(new Intent(this,CaptureService.class).putExtra("result",result).putExtra("consent",data));toast("Captura autorizada. Recolha o painel para aprender com outros apps.");}
                catch(Exception e){toast("Não foi possível iniciar captura: "+e.getMessage());}
            }else toast("Captura não autorizada. O mascote continua utilizável.");
        }else if(result==RESULT_OK&&data!=null&&data.getData()!=null){
            final Uri uri=data.getData();
            if(request==IMPORT)runIO(()->{try(InputStream in=getContentResolver().openInputStream(uri)){
                return session.memory.importNotes(new JSONObject(readText(in)))+" registros novos importados.";}});
            if(request==EXPORT)runIO(()->{String json=session.memory.exportNotes().toString(2);
                try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Destino indisponível");out.write(json.getBytes(StandardCharsets.UTF_8));}
                return "Memória textual exportada. Pesos visuais e preferências não fazem parte deste JSON.";});
        }
    }
    private static String readText(InputStream in)throws IOException{
        if(in==null)throw new IOException("Arquivo indisponível");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;
        while((n=in.read(b))!=-1){if(out.size()+n>5*1024*1024)throw new IOException("Limite de 5 MiB por arquivo");out.write(b,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);
    }
    private void runIO(Task task){runIO(task,false);}
    private void runIO(Task task,boolean isSearch){
        io.submit(()->{String result;try{result=task.run();}catch(Exception e){result="Erro: "+e.getMessage();}final String response=result;
            runOnUiThread(()->{if(isDestroyed())return;if(isSearch)output.setText(response);else{session.message=response;toast(response);}});
        });
    }
    private android.graphics.drawable.GradientDrawable surface(int color,int radius){
        android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private void section(String name,boolean collapsible){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(18),dp(12),dp(18),dp(18));
        card.setBackground(surface(0xff1b283c,24));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,dp(14),0,0);root.addView(card,cp);
        TextView heading=new TextView(this);heading.setText(name+(collapsible?"   ＋":""));heading.setTextSize(18);heading.setTextColor(0xfff4f6fc);heading.setTypeface(null,android.graphics.Typeface.BOLD);heading.setPadding(0,dp(10),0,dp(10));card.addView(heading);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);card.addView(body);content=body;
        if(collapsible){body.setVisibility(View.GONE);heading.setFocusable(true);heading.setContentDescription(name+", expandir seção");heading.setOnClickListener(v->{boolean open=body.getVisibility()==View.VISIBLE;body.setVisibility(open?View.GONE:View.VISIBLE);heading.setText(name+(open?"   ＋":"   −"));heading.setContentDescription(name+(open?", expandir seção":", recolher seção"));});}
    }
    private void title(String text,int size){TextView view=label(text,size);view.setTypeface(null,android.graphics.Typeface.BOLD);view.setTextColor(accent);view.setPadding(0,dp(8),0,dp(8));}
    private TextView label(String text,int size){TextView view=new TextView(this);view.setText(text);view.setTextSize(size);view.setTextColor(0xffcdd8e9);view.setLineSpacing(dp(3),1);view.setPadding(0,dp(6),0,dp(8));content.addView(view);return view;}
    private Button button(String text,Runnable action){Button view=new Button(this);view.setText(text);view.setTextSize(15);view.setTypeface(null,android.graphics.Typeface.BOLD);view.setAllCaps(false);view.setTextColor(0xfff5f8ff);view.setMinHeight(dp(52));view.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);view.setPadding(dp(16),dp(12),dp(16),dp(12));
        android.graphics.drawable.GradientDrawable normal=surface(0xff2a3c55,16);normal.setStroke(dp(1),0xff425673);
        view.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(accent),normal,null));
        view.setOnClickListener(v->action.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(6),0,dp(4));content.addView(view,lp);
        if(text.equals("Vamos estudar  →")||text.equals("Mostrar meu Buddy")){view.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent));view.setTextColor(0xff101827);}
        return view;
    }
    private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
    private final Runnable refresh=new Runnable(){public void run(){
        Brain.Result r=session.result;
        String display=session.care.summary()+"\n\nModo: "+session.mode+" • visual: "+session.visualMood()+"\n";
        if(r!=null)display+=String.format(Locale.getDefault(),"%d capturas • %d spikes/janela\n%d/128 protótipos • semelhança %.0f%%\n",r.frames,r.spikes,r.patterns,r.similarity*100);
        display+="Memória: "+session.memory.count("notes")+" textos • "+session.memory.count("feedback")+" avaliações\n"+session.memory.count("interactions")+" carinhos";
        if(session.decision!=null)display+="\nAção para avaliar: "+session.decision.action;
        stats.setText(display);message.setText(session.message);learning.setText(session.learning?"Aprendizagem: ligada":"Aprendizagem: congelada");preview.invalidate();ui.postDelayed(this,1000);
    }};
    @Override protected void onResume(){super.onResume();session.uiVisible=true;ui.removeCallbacks(refresh);ui.post(refresh);}
    @Override protected void onPause(){session.uiVisible=false;ui.removeCallbacks(refresh);super.onPause();}
    @Override protected void onDestroy(){ui.removeCallbacksAndMessages(null);io.shutdown();super.onDestroy();}
}
