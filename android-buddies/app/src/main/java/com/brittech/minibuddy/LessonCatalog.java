package com.brittech.minibuddy;
import android.content.*;
import org.json.*;
import java.util.*;
/** One catalog and per-profile grade selection for both the floating balloon and study panel. */
public final class LessonCatalog {
    public static JSONArray lessons(SubjectConfig c){JSONArray a=c.data.optJSONArray("lessons");return a==null?new JSONArray():a;}
    public static int[] grades(SubjectConfig c){TreeSet<Integer> out=new TreeSet<>();JSONArray a=lessons(c);for(int i=0;i<a.length();i++)out.add(a.optJSONObject(i).optInt("grade"));int[] result=new int[out.size()];int n=0;for(int grade:out)result[n++]=grade;return result;}
    public static String gradeLabel(int grade){return grade>9?(grade-10)+"ª série EM (sugerida)":grade+"º ano EF";}
    public static int selectedGrade(Context c,SubjectConfig config,String profile){int[] allowed=grades(config);if(allowed.length==0)throw new IllegalStateException("Catálogo sem miniaulas");int selected=c.getSharedPreferences("education",0).getInt("grade_"+profile,allowed[0]);for(int g:allowed)if(g==selected)return selected;return allowed[0];}
    public static void selectGrade(Context c,String profile,int grade){c.getSharedPreferences("education",0).edit().putInt("grade_"+profile,grade).apply();}
    public static JSONArray atGrade(SubjectConfig c,int grade){JSONArray out=new JSONArray(),all=lessons(c);for(int i=0;i<all.length();i++){JSONObject l=all.optJSONObject(i);if(l.optInt("grade")==grade)out.put(l);}return out;}
    public static JSONObject findLesson(SubjectConfig c,String id){JSONArray all=lessons(c);for(int i=0;i<all.length();i++)if(all.optJSONObject(i).optString("id").equals(id))return all.optJSONObject(i);return null;}
    public static JSONObject findQuestion(SubjectConfig c,String id){for(int i=0;i<c.questions.length();i++)if(c.questions.optJSONObject(i).optString("id").equals(id))return c.questions.optJSONObject(i);return null;}
    public static JSONObject forLesson(SubjectConfig c,String id){for(int i=0;i<c.questions.length();i++){JSONObject q=c.questions.optJSONObject(i);if(q.optString("lesson_id").equals(id))return q;}throw new IllegalStateException("Miniaula sem exercício");}
    public static JSONObject randomQuestion(SubjectConfig c,int grade){JSONArray ls=atGrade(c,grade);if(ls.length()==0)throw new IllegalStateException("Sem conteúdo neste ano");return forLesson(c,ls.optJSONObject(new Random().nextInt(ls.length())).optString("id"));}
    public static String tag(JSONObject q){if(!q.has("bncc"))return "Catálogo inicial livre (registro anterior)";return q.optString("bncc")+" • "+gradeLabel(q.optInt("grade"))+("complementar".equals(q.optString("alignment"))?"\nEspanhol complementar • vínculo por área":"");}
    public static String lessonText(JSONObject lesson){return lesson.optString("objective")+"\n\n"+lesson.optString("body")+"\n\nExemplo: "+lesson.optString("example")+"\n\n"+lesson.optString("alignment_note")+"\nFonte: BNCC/MEC, página "+lesson.optInt("source_pdf_page")+" do PDF.";}
}
