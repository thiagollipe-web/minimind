package com.brittech.minibuddy;
import java.text.Normalizer;
import java.util.*;
public final class TextTools {
    private static final Set<String> STOP=new HashSet<>(Arrays.asList(("a o os as um uma uns umas de da do das dos e em no na nos nas para por com que eu voce ele ela isso este esta meu minha seu sua sou ser ao ou se mas como sobre foi tem nao sim quero qual quando onde").split(" ")));
    public static List<String> tokens(String text){
        String clean=Normalizer.normalize(text.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}+","");
        ArrayList<String> out=new ArrayList<>();
        for(String t:clean.split("[^a-z0-9_+#]+"))if(t.length()>1&&!STOP.contains(t)&&out.size()<20000)out.add(t);
        return out;
    }
    public static List<String> features(String text,String visual,String mode){
        List<String> out=new ArrayList<>(Arrays.asList("global","modo:"+mode,"visual:"+visual));
        LinkedHashSet<String> words=new LinkedHashSet<>(tokens(text));int count=0;
        for(String w:words){if(count++>=12)break;out.add("texto:"+w);out.add("contexto:"+visual+":"+w);}
        return out;
    }
}
