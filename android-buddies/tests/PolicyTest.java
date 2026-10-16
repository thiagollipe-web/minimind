import com.brittech.minibuddy.PedagogyPolicy;
import java.util.Random;
public class PolicyTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){int[] counts={10,10,10,10};double[] rewards={-10,10,0,5};Random r=new Random(42);int best=0;int[] seen=new int[4];for(int i=0;i<20000;i++){PedagogyPolicy.Choice c=PedagogyPolicy.choose(counts,rewards,"adaptativo",r);seen[c.action]++;check(Math.abs(c.probability-(c.action==1?.91:.03))<1e-9,"Propensão correta");if(c.action==1)best++;}check(best>17500&&best<18800,"Explorar sem perder estratégia eficaz");for(int n:seen)check(n>400,"Todas as estratégias exploradas");check(PedagogyPolicy.choose(counts,rewards,"fixo",r).action==0,"Controle fixo");check(PedagogyPolicy.choose(counts,rewards,"aleatorio",r).probability==.25,"Controle aleatório");System.out.println("PolicyTest OK: "+best+"/20000 escolhas da estratégia com melhor recompensa");}
}
