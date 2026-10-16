import com.brittech.minibuddy.QuestionDeck;
import java.util.*;
public final class QuestionDeckTest {
    static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
    public static void main(String[] args){
        List<String> ids=Arrays.asList("q1","q2","q3","q4");
        QuestionDeck deck=new QuestionDeck(ids,null,new Random(7));String previous="";
        for(int round=0;round<100;round++){
            Set<String> shown=new HashSet<>();
            for(int i=0;i<ids.size();i++){
                String next=deck.next();check(!next.equals(previous),"Immediate repeat");check(shown.add(next),"Repeated before round exhausted");previous=next;
                // Reopen the balloon/process after every draw. The persisted remainder must survive.
                deck=new QuestionDeck(ids,deck.save(),new Random(20+round));
            }
            check(shown.equals(new HashSet<>(ids)),"Question omitted");
        }
        QuestionDeck live=new QuestionDeck(ids,null,new Random(3));live.next();
        QuestionDeck reopened=new QuestionDeck(ids,live.save(),new Random(99));
        for(int i=0;i<3;i++)check(live.next().equals(reopened.next()),"Reopen lost the order");
        QuestionDeck changed=new QuestionDeck(Arrays.asList("q1","q2","q3","q4","q5"),live.save(),new Random(2));
        Set<String> all=new HashSet<>();for(int i=0;i<5;i++)all.add(changed.next());check(all.size()==5,"Catalog upgrade omitted a question");
        QuestionDeck corrupt=new QuestionDeck(ids,"1\nq1\nq1,q2,q3,q4\nq2,q2",new Random(4));all.clear();for(int i=0;i<4;i++)all.add(corrupt.next());check(all.size()==4,"Corrupt state not recovered");
        QuestionDeck one=new QuestionDeck(Collections.singletonList("one"),null,new Random(1));for(int i=0;i<20;i++)check(one.next().equals("one"),"Single item must terminate");
        boolean rejected=false;try{new QuestionDeck(Collections.emptyList(),null,new Random());}catch(IllegalArgumentException e){rejected=true;}check(rejected,"Reject empty pool");
        for(List<String> invalid:Arrays.asList(Arrays.asList("same","same"),Arrays.asList("a\nb\nc"),Arrays.asList("a,b"))){boolean bad=false;try{new QuestionDeck(invalid,null,new Random());}catch(IllegalArgumentException e){bad=true;}check(bad,"Reject unserializable or duplicate IDs");}
        System.out.println("QuestionDeckTest OK: 100 rounds, persistence, boundary repeats, catalog changes and corruption recovery.");
    }
}
