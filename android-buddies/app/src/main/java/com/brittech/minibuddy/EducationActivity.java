package com.brittech.minibuddy;
import android.app.*;
import android.content.*;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.io.*;

/** Offline pilot: checked multiple choice, explicit teaching review and delayed strategy rewards. */
public final class EducationActivity extends Activity {
 private EducationDb db;private SubjectConfig config;private String profile,mode="adaptativo";private LinearLayout root,card;private TextView report;private int index;private boolean assisted,answered;private JSONObject current;private long reviewId=-1;private String context;private PedagogyPolicy.Choice choice;
 @Override public void onCreate(Bundle state){super.onCreate(state);db=new EducationDb(this);config=SubjectConfig.load(this);profile=db.profile(this);mode=getPreferences(0).getString("policy","adaptativo");render();}
 private void text(LinearLayout p,String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(Color.WHITE);v.setPadding(6,12,6,10);p.addView(v);}
 private void button(LinearLayout p,String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);p.addView(b);b.setOnClickListener(v->{try{r.run();}catch(Exception e){new AlertDialog.Builder(this).setMessage(e.getMessage()).setPositiveButton("OK",null).show();}});}
 private EditText input(LinearLayout p,String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(0xffbbbbbb);e.setMinLines(2);p.addView(e);return e;}
 private void render(){ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(24,32,24,36);root.setBackgroundColor(0xff121525);scroll.addView(root);setContentView(scroll);if(Build.VERSION.SDK_INT>=30){scroll.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets b=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());scroll.setPadding(b.left,b.top,b.right,b.bottom);return insets;});scroll.requestApplyInsets();}
  text(root,config.nickname()+" • "+config.name(),25);text(root,"Protótipo offline • 3 atividades + 3 revisões por matéria. Sem conversa generativa.",14);
  LinkedHashMap<String,String> profiles=db.profiles();text(root,"Perfil educativo: "+profiles.get(profile),16);
  button(root,"Trocar perfil educativo",()->{String[] ids=profiles.keySet().toArray(new String[0]);String[] names=profiles.values().toArray(new String[0]);new AlertDialog.Builder(this).setTitle("Perfis de estudo").setItems(names,(d,n)->{profile=ids[n];getSharedPreferences("education",0).edit().putString("profile",profile).apply();render();}).show();});
  button(root,"Novo perfil educativo",()->{EditText e=new EditText(this);new AlertDialog.Builder(this).setTitle("Nome do perfil").setView(e).setPositiveButton("Criar",(d,n)->{try{profile=db.addProfile(e.getText().toString());getSharedPreferences("education",0).edit().putString("profile",profile).apply();render();}catch(Exception ex){toast(ex.getMessage());}}).setNegativeButton("Cancelar",null).show();});
  text(root,"Perfis separam registros de estudo. Cuidados e rede visual pertencem ao bichinho deste app.",12);
  button(root,"Estratégia: "+mode,()->{String[] modes={"adaptativo","fixo","aleatorio"};new AlertDialog.Builder(this).setTitle("Comparação de estratégias").setItems(modes,(d,n)->{mode=modes[n];getPreferences(0).edit().putString("policy",mode).apply();render();}).show();});
  card=new LinearLayout(this);card.setOrientation(1);root.addView(card);newQuestion(false);
  button(root,"Nova atividade",()->newQuestion(false));button(root,"Revisão sem ajuda (após 30 minutos)",()->newQuestion(true));
  button(root,"Modo aprendiz: ensinar ao bichinho",this::teach);
  button(root,"Revisar, editar ou aprovar ensinamentos",this::reviewKnowledge);
  EditText query=input(root,"Buscar uma explicação aprovada");button(root,"Consultar memória revisada",()->new AlertDialog.Builder(this).setTitle("Registros locais do perfil").setMessage(db.search(profile,query.getText().toString())).setPositiveButton("Fechar",null).show());
  report=new TextView(this);report.setTextColor(Color.WHITE);root.addView(report);refresh();button(root,"Ver evidências e missões",()->{refresh();new AlertDialog.Builder(this).setTitle("Evidências registradas").setMessage(db.report(profile)).setPositiveButton("OK",null).show();});
  button(root,"Exportar estudo e memória JSON",()->startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"minibuddy_"+config.data.optString("id")+"_"+profile+".json"),201));
  button(root,"Voltar ao bichinho e laboratório SNN",this::finish);
 }
 private void refresh(){report.setText(db.report(profile));Session.get(this).care.state.bonusXp=db.claim(profile);}
 private void newQuestion(boolean review){card.removeAllViews();reviewId=-1;assisted=false;answered=false;JSONObject due=review?db.due(profile):null;if(review&&due==null){text(card,"Nenhuma revisão vencida. Faça uma atividade e volte após 30 minutos. O app não precisa ficar aberto.",16);return;}
  if(review){for(int i=0;i<config.questions.length();i++)if(config.questions.optJSONObject(i).optString("id").equals(due.optString("qid"))){index=i;break;}reviewId=due.optLong("id");}else index=new Random().nextInt(config.questions.length());
  current=config.questions.optJSONObject(index);context=db.context(profile,current.optString("skill"));choice=db.choose(profile,context,mode);
  text(card,(review?"Revisão sem ajuda • ":"Atividade • ")+current.optString("skill"),20);text(card,current.optString(review?"reviewQuestion":"question"),19);
  JSONArray options=current.optJSONArray(review?"reviewOptions":"options");ArrayList<Integer> order=new ArrayList<>();for(int i=0;i<options.length();i++)order.add(i);Collections.shuffle(order);
  for(int option:order)button(card,options.optString(option),()->answer(option));
  if(!review)button(card,"Pedir dica: "+PedagogyPolicy.ACTIONS[choice.action],()->{if(answered)return;assisted=true;String[] keys={"explanation","example","guide","prerequisite"};text(card,current.optString(keys[choice.action]),16);});
 }
 private void answer(int option){if(answered){toast("Resposta já registrada. Escolha outra atividade.");return;}answered=true;boolean review=reviewId>=0;boolean correct=option==current.optInt(review?"reviewAnswer":"answer");if(review)db.review(profile,reviewId,correct);else db.attempt(profile,current,context,choice,mode,correct,assisted);text(card,(correct?"Correto. ":"Vamos revisar. ")+current.optString("explanation")+(review?"\nRevisão registrada; a recompensa só é aplicada uma vez.":"\nFaça uma revisão após 30 minutos para avaliar a estratégia."),16);refresh();if(correct)Chirps.play(Session.get(this));}
 private void teach(){JSONObject q=config.questions.optJSONObject(new Random().nextInt(config.questions.length()));LinearLayout box=new LinearLayout(this);box.setOrientation(1);text(box,q.optString("teachQuestion"),18);EditText answer=input(box,"Explique e dê um exemplo");AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Você ensina; eu registro").setView(box).setPositiveButton("Salvar pendente",null).setNegativeButton("Cancelar",null).create();dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(w->{try{db.teach(profile,q.optString("teachQuestion"),answer.getText().toString());dialog.dismiss();toast("Salvo como pendente de revisão, sem assumir que está correto.");refresh();}catch(Exception e){answer.setError(e.getMessage());}}));dialog.show();}
 private void reviewKnowledge(){JSONArray rows=db.knowledge(profile);if(rows.length()==0){toast("Nenhum ensinamento neste perfil.");return;}String[] labels=new String[rows.length()];for(int i=0;i<rows.length();i++){JSONObject r=rows.optJSONObject(i);labels[i]="#"+r.optLong("id")+" ["+r.optString("status")+"] "+r.optString("question");}new AlertDialog.Builder(this).setTitle("Revisão humana local (sem autenticação)").setItems(labels,(d,n)->edit(rows.optJSONObject(n))).setNegativeButton("Fechar",null).show();}
 private void edit(JSONObject row){LinearLayout box=new LinearLayout(this);box.setOrientation(1);text(box,row.optString("question"),16);EditText e=input(box,"Explicação");e.setText(row.optString("text"));Spinner status=new Spinner(this);String[] states={"pendente","aprovado","retirado"};status.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,states));status.setSelection(Arrays.asList(states).indexOf(row.optString("status")));box.addView(status);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Revisar a exatidão antes de aprovar").setView(box).setPositiveButton("Salvar versão",null).setNegativeButton("Cancelar",null).create();dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(w->{try{db.revise(profile,row.optLong("id"),e.getText().toString(),states[status.getSelectedItemPosition()]);dialog.dismiss();refresh();}catch(Exception ex){e.setError(ex.getMessage());}}));dialog.show();}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==201&&result==RESULT_OK&&data!=null){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(db.export(profile).toString(2).getBytes("UTF-8"));toast("Arquivo exportado. Inclui os textos ensinados e tentativas deste perfil.");}catch(Exception e){toast("Falha ao exportar: "+e.getMessage());}}}
 @Override protected void onResume(){super.onResume();Session.get(this).uiVisible=true;}
 @Override protected void onPause(){Session.get(this).uiVisible=false;super.onPause();}
 @Override protected void onDestroy(){if(db!=null)db.close();super.onDestroy();}
}
