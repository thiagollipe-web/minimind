package com.brittech.minibuddy;
import java.io.*;
import java.util.*;

/** Separate synthetic lab. Labels are used by the evaluator only, never by STDP.
 * Accuracy is a tiny controlled task metric, not a measure of general intelligence.
 */
public final class MissionLab {
    public interface Observer {boolean frame(float[] pixels,int done,int total);}
    public final Brain brain;
    public int baselineCorrect,afterCorrect;
    public static final int TESTS=12;
    public double meanWeightChange;
    public int changedWeights,trainingSpikes;
    public MissionLab(){brain=new Brain();}
    public MissionLab(Brain brain){this.brain=brain;}
    public static float[] pattern(int type,int step,int phase){
        float[] image=new float[Brain.W*Brain.H];
        int shift=(step+phase)%6;
        if(type<2){
            int y0=type==0?1:10,x0=2+shift;
            for(int y=y0;y<y0+5;y++)for(int x=x0;x<x0+6;x++)image[y*Brain.W+x]=.95f;
        }else{
            int x0=15+shift/2;
            for(int y=2;y<14;y++)for(int x=x0;x<x0+3;x++)image[y*Brain.W+x]=(step%2==0)?.95f:.25f;
        }return image;
    }
    private static float[][] copy(float[][] src){float[][] out=new float[src.length][];for(int i=0;i<src.length;i++)out[i]=src[i].clone();return out;}
    private float[] response(int type,int phase){
        brain.clearDynamics();float[] vector=new float[Brain.N];
        brain.process(new float[Brain.W*Brain.H],false);
        for(int step=0;step<12;step++){
            brain.process(pattern(type,step,phase),false);float[] r=brain.response();
            for(int i=0;i<r.length;i++)vector[i]+=r[i];
        }return vector;
    }
    private static void normalize(float[] v){float norm=0;for(float f:v)norm+=f*f;if(norm>0){norm=(float)Math.sqrt(norm);for(int i=0;i<v.length;i++)v[i]/=norm;}}
    public int evaluate(){
        float[][] before=copy(brain.weights);
        float[][] references=new float[3][Brain.N];
        for(int type=0;type<3;type++){
            for(int phase=0;phase<2;phase++){
                float[] response=response(type,phase);for(int i=0;i<Brain.N;i++)references[type][i]+=response[i];
            }normalize(references[type]);
        }
        int correct=0;
        // Different order and phases from the two labeled reference sequences.
        for(int phase=2;phase<6;phase++)for(int order=0;order<3;order++){
            int type=(order+phase)%3;float[] v=response(type,phase);normalize(v);
            float best=0;int predicted=-1;
            for(int candidate=0;candidate<3;candidate++){
                float dot=0;for(int i=0;i<Brain.N;i++)dot+=v[i]*references[candidate][i];
                if(dot>best){best=dot;predicted=candidate;}
            }
            if(predicted==type)correct++;
        }
        for(int n=0;n<Brain.N;n++)if(!Arrays.equals(before[n],brain.weights[n]))throw new IllegalStateException("O teste alterou pesos congelados");
        return correct;
    }
    public void train(Observer observer)throws InterruptedException{
        baselineCorrect=evaluate();float[][] initial=copy(brain.weights);
        int total=6*3*12,done=0;
        for(int repetition=0;repetition<6;repetition++)for(int order=0;order<3;order++){
            int type=(order+repetition)%3;brain.clearDynamics();brain.process(new float[Brain.W*Brain.H],false);
            for(int step=0;step<12;step++){
                float[] image=pattern(type,step,repetition%2);
                trainingSpikes+=brain.process(image,true).spikes;done++;
                if(observer!=null&&!observer.frame(image,done,total))throw new InterruptedException("Treino cancelado");
            }
        }
        double sum=0;changedWeights=0;
        for(int n=0;n<Brain.N;n++)for(int i=0;i<Brain.IN;i++){
            double difference=Math.abs(brain.weights[n][i]-initial[n][i]);sum+=difference;if(difference>1e-7)changedWeights++;
        }
        meanWeightChange=sum/(Brain.N*Brain.IN);brain.clearDynamics();
    }
    public void save(File file)throws IOException{brain.save(file);}
}
