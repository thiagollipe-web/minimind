package com.brittech.minibuddy;
import java.util.*;

/** Persistable shuffle bag: show each ID once per round, avoiding a boundary repeat. */
public final class QuestionDeck {
    private final ArrayList<String> ids=new ArrayList<>(),remaining=new ArrayList<>();
    private final Random random;
    private final String catalog;
    private String last="";
    public QuestionDeck(List<String> candidates,String saved,Random random){
        if(candidates==null||candidates.isEmpty()||random==null)throw new IllegalArgumentException("Catálogo vazio");
        this.random=random;
        for(String id:candidates){if(id==null||id.isEmpty()||(id.indexOf(',')>=0||id.indexOf('\r')>=0||id.indexOf('\n')>=0))throw new IllegalArgumentException("ID inválido");if(ids.contains(id))throw new IllegalArgumentException("ID duplicado");ids.add(id);}
        Collections.sort(ids);catalog=String.join(",",ids);
        if(saved==null)return;
        String[] parts=saved.split("\n",-1);
        if(parts.length!=4||!parts[0].equals("1"))return;
        if(ids.contains(parts[1]))last=parts[1];
        if(!parts[2].equals(catalog))return;
        if(parts[3].isEmpty())return;
        HashSet<String> seen=new HashSet<>();
        for(String id:parts[3].split(",",-1)){
            if(!ids.contains(id)||!seen.add(id)){remaining.clear();return;}
            remaining.add(id);
        }
        if(remaining.contains(last))remaining.clear();
    }
    public String next(){
        if(remaining.isEmpty()){
            remaining.addAll(ids);Collections.shuffle(remaining,random);
            if(remaining.size()>1&&remaining.get(0).equals(last))Collections.swap(remaining,0,1+random.nextInt(remaining.size()-1));
        }
        last=remaining.remove(0);return last;
    }
    public String save(){return "1\n"+last+"\n"+catalog+"\n"+String.join(",",remaining);}
}
