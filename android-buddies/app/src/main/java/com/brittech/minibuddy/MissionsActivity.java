package com.brittech.minibuddy;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.Locale;
import java.util.concurrent.*;

public final class MissionsActivity extends Activity {
    private Session session;private LinearLayout column;private TextView needs,report,status;
    private final TextView[] progress=new TextView[5];private final Button[] claim=new Button[5];
    private Button train,test;
    private PatternView pattern;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ExecutorService work=Executors.newSingleThreadExecutor();
    private volatile boolean busy,cancelled;
    private int dp(float f){return Math.round(f*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);session=Session.get(this);setTitle("Missões e cuidados");
        ScrollView scroll=new ScrollView(this);column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);column.setPadding(dp(18),dp(18),dp(18),dp(30));column.setBackgroundColor(0xff121525);scroll.addView(column);setContentView(scroll);
        if(Build.VERSION.SDK_INT>=30){scroll.setOnApplyWindowInsetsListener((v,insets)->{Insets b=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());scroll.setPadding(b.left,b.top,b.right,b.bottom);return insets;});scroll.requestApplyInsets();}
        heading("Meu bichinho virtual");needs=label("");
        button("Alimentar",()->Toast.makeText(this,session.care.feed()?"Energia +10; felicidade +2.":"Já foi alimentado. Aguarde dois minutos entre refeições.",Toast.LENGTH_SHORT).show());
        button("Descansar",()->{stopService(new Intent(this,CaptureService.class));session.mode("pausado");session.care.rest(true);Chirps.play(session,"Repouso");session.message="Descansando. Energia se recupera enquanto o aplicativo está ativo.";});
        button("Acordar",()->{session.care.rest(false);session.message="Acordado. Reative a observação no painel quando quiser.";});
        label("Ele não morre e não perde progresso por ausência. Energia e felicidade são regras do jogo; XP não mede inteligência.");
        heading("Cinco missões offline");
        String[] descriptions={"Receber 3 novos carinhos. Toque sem arrastar no mascote.","Salvar ou importar pelo menos 1 novo texto na memória.","Registrar 5 novas avaliações de reações (+1 ou −1). Carinho pode avaliar uma reação disponível.","Executar um treino de padrões artificiais com STDP e medir a alteração dos pesos.","Executar o teste separado com pesos congelados e comparar o resultado antes/depois."};
        for(int i=0;i<5;i++){
            final int index=i;heading((i+1)+" · "+PetState.TITLES[i]);label(descriptions[i]);progress[i]=label("");
            claim[i]=button("Receber "+PetState.REWARDS[i]+" XP",()->{
                if(session.care.claim(index)){session.happyUntil=SystemClock.elapsedRealtime()+3000;Chirps.play(session,"Padrão recorrente");Toast.makeText(this,"Recompensa recebida!",Toast.LENGTH_SHORT).show();}
                refreshNow();
            });
        }
        button("Abrir painel para textos e avaliações",()->{startActivity(new Intent(this,MainActivity.class));finish();});
        heading("Laboratório de plasticidade");
        label("Este experimento usa uma rede separada da tela e da demonstração. Mostra três padrões artificiais; rótulos servem somente à avaliação, não à regra STDP. O botão Treinar controla este experimento independentemente da aprendizagem da tela. Treinar novamente substitui apenas a rede deste laboratório.");
        pattern=new PatternView();column.addView(pattern,new LinearLayout.LayoutParams(-1,dp(160)));
        status=label("Pronto para treinar.");
        train=button("Missão 4 · Treinar padrões",this::trainLab);
        test=button("Missão 5 · Testar com pesos congelados",this::testLab);
        report=label(session.prefs.getString("lab_report","Ainda não há experimento concluído."));report.setTextIsSelectable(true);
        heading("O que aprendi");
        label("Textos: registros pesquisáveis. Preferências: avaliações de ações. Plasticidade: alteração dos pesos. Acerto no laboratório: resultado da tarefa curta, não compreensão de linguagem ou inteligência geral.");
        button("Voltar ao painel",this::finish);
    }
    private File labFile(){return new File(getFilesDir(),"visual_missoes.bin");}
    private void finishWork(String message){runOnUiThread(()->{busy=false;if(!isDestroyed()){status.setText(message);refreshNow();}});}
    private void trainLab(){
        if(busy)return;
        if(session.care.state.resting){status.setText("Acorde o bichinho antes de treinar.");return;}
        busy=true;cancelled=false;refreshNow();status.setText("Medindo resposta inicial com pesos fixos…");
        work.submit(()->{
            try{
                MissionLab lab=new MissionLab();
                lab.train((pixels,done,total)->{
                    if(cancelled||session.care.state.resting||Thread.currentThread().isInterrupted())return false;
                    if(done%2==0)runOnUiThread(()->{if(!isDestroyed()){pattern.pixels=pixels;pattern.invalidate();status.setText("Treino STDP: "+done+" / "+total+" quadros");}});
                    try{Thread.sleep(35);}catch(InterruptedException e){Thread.currentThread().interrupt();return false;}
                    return true;
                });
                if(cancelled)throw new InterruptedException("Treino cancelado");
                if(lab.trainingSpikes==0||lab.changedWeights==0)throw new IllegalStateException("Treino sem atividade ou alteração mensurável. Nenhuma missão foi concluída.");
                lab.save(labFile());
                String summary=String.format(Locale.getDefault(),"Treino concluído\nAntes: %d/%d sequências corretas\n%d conexões alteradas\nVariação média absoluta: %.8f\n%d spikes durante o treino\nPróximo passo: testar com pesos congelados.\nA variação inclui STDP e homeostase.",lab.baselineCorrect,MissionLab.TESTS,lab.changedWeights,lab.meanWeightChange,lab.trainingSpikes);
                session.prefs.edit().putInt("lab_before",lab.baselineCorrect).putInt("lab_changed",lab.changedWeights).putFloat("lab_delta",(float)lab.meanWeightChange).putString("lab_report",summary).apply();
                session.care.trained();runOnUiThread(()->{if(!isDestroyed())report.setText(summary);});finishWork("Missão 4 pronta para receber XP.");
            }catch(InterruptedException e){finishWork("Treino interrompido. A rede anterior do laboratório foi preservada.");}
            catch(Exception e){finishWork("Falha no laboratório: "+e.getMessage());}
        });
    }
    private void testLab(){
        if(busy)return;
        if(!session.care.state.trained||!labFile().isFile()){status.setText("Conclua o treino da missão 4 primeiro.");return;}
        busy=true;cancelled=false;refreshNow();status.setText("Testando sequências reservadas, sem alterar pesos…");
        work.submit(()->{
            try{
                MissionLab lab=new MissionLab(Brain.load(labFile()));int score=lab.evaluate();
                if(cancelled)throw new InterruptedException("Teste cancelado");
                int before=session.prefs.getInt("lab_before",0),difference=score-before;
                String conclusion=difference>0?"Melhora observada nesta tarefa curta.":difference==0?"Não houve ganho de pontuação neste teste.":"A pontuação diminuiu; treino não garantiu melhora.";
                String summary=String.format(Locale.getDefault(),"Teste com pesos congelados\nAntes: %d/%d • depois: %d/%d\nDiferença: %+d acertos\nPesos preservados durante o teste: SIM\nConexões alteradas no treino: %d\n\n%s\nReferência: 3 classes; 12 sequências de teste.\nAs referências do avaliador têm rótulos; a STDP não recebe rótulos.\nXP recompensa completar o experimento, não garante reconhecimento ou inteligência.",before,MissionLab.TESTS,score,MissionLab.TESTS,difference,session.prefs.getInt("lab_changed",0),conclusion);
                session.prefs.edit().putString("lab_report",summary).apply();session.care.tested();
                runOnUiThread(()->{if(!isDestroyed())report.setText(summary);});finishWork("Missão 5 pronta para receber XP.");
            }catch(InterruptedException e){finishWork("Teste interrompido.");}catch(Exception e){finishWork("Teste não concluído: "+e.getMessage());}
        });
    }
    private final Runnable refresh=new Runnable(){public void run(){refreshNow();ui.postDelayed(this,1000);}};
    private void refreshNow(){
        session.care.tick(!session.mode.equals("pausado"));
        needs.setText(session.care.summary()+"\n"+session.care.state.accessory()+"\nMemória: "+session.memory.count("notes")+" textos; "+session.memory.count("feedback")+" avaliações; "+session.memory.count("interactions")+" carinhos.");
        for(int i=0;i<5;i++){
            boolean done=session.care.state.isClaimed(i);progress[i].setText(done?"Concluída • recompensa já recebida":session.care.state.progress(i)+" / "+PetState.TARGETS[i]);
            claim[i].setEnabled(!done&&session.care.state.progress(i)>=PetState.TARGETS[i]);
        }
        train.setEnabled(!busy);test.setEnabled(!busy&&session.care.state.trained);
    }
    private void heading(String text){TextView label=label(text);label.setTextSize(21);label.setTypeface(null,Typeface.BOLD);label.setPadding(0,dp(18),0,dp(5));}
    private TextView label(String text){TextView v=new TextView(this);v.setText(text);v.setTextSize(15);v.setTextColor(0xffeeeaff);v.setPadding(0,dp(6),0,dp(6));column.addView(v);return v;}
    private Button button(String text,Runnable action){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setOnClickListener(v->action.run());column.addView(b);return b;}
    @Override protected void onResume(){super.onResume();session.uiVisible=true;ui.post(refresh);}
    @Override protected void onPause(){session.uiVisible=false;ui.removeCallbacks(refresh);session.care.save();super.onPause();}
    @Override protected void onDestroy(){cancelled=true;ui.removeCallbacksAndMessages(null);work.shutdownNow();super.onDestroy();}
    private final class PatternView extends View {
        private final Paint paint=new Paint();volatile float[] pixels=new float[Brain.W*Brain.H];
        PatternView(){super(MissionsActivity.this);setContentDescription("Padrões visuais artificiais usados no laboratório");}
        @Override protected void onDraw(Canvas c){
            c.drawColor(0xff080c16);float[] img=pixels;
            for(int y=0;y<Brain.H;y++)for(int x=0;x<Brain.W;x++){
                int light=Math.round(255*img[y*Brain.W+x]);paint.setColor(Color.rgb(light,light,light));
                c.drawRect(x*getWidth()/(float)Brain.W,y*getHeight()/(float)Brain.H,(x+1)*getWidth()/(float)Brain.W,(y+1)*getHeight()/(float)Brain.H,paint);
            }
        }
    }
}
