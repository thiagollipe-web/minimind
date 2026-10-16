package com.brittech.minibuddy;
import java.util.Random;
/** Epsilon-greedy contextual bandit. The caller supplies only delayed review rewards. */
public final class PedagogyPolicy {
 public static final String[] ACTIONS={"Explicação curta","Exemplo","Pergunta orientadora","Pré-requisito"};
 public static final class Choice {public final int action;public final double probability;Choice(int a,double p){action=a;probability=p;}}
 public static Choice choose(int[] n,double[] total,String mode,Random random){
  if(n.length!=4||total.length!=4)throw new IllegalArgumentException("4 estratégias necessárias");
  if("fixo".equals(mode))return new Choice(0,1);
  if("aleatorio".equals(mode))return new Choice(random.nextInt(4),.25);
  int best=0;double max=-Double.MAX_VALUE;
  for(int i=0;i<4;i++){if(n[i]<0||!Double.isFinite(total[i]))throw new IllegalArgumentException();double mean=n[i]==0?0:total[i]/n[i];if(mean>max){max=mean;best=i;}}
  int selected=random.nextDouble()<.12?random.nextInt(4):best;
  return new Choice(selected,.12/4+(selected==best?.88:0));
 }
}
