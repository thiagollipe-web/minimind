package com.brittech.minibuddy;

import java.io.*;
import java.util.*;

/** Pure Java LIF+trace STDP, suitable for headless tests and Android CPU.
 * 48 units, 24x16 ON/OFF input, 50 simulated ms per frame.
 * Prototype includes neural response AND spatial change descriptor to reduce
 * trivial matches from a single spiking neuron. These are familiarity heuristics.
 */
public final class Brain {
    public static final int W=24,H=16,N=48,IN=W*H*2,STEPS=50,MAX=128,DIM=N+48;
    public float[][] weights=new float[N][IN];
    private float[] v=new float[N],pre=new float[IN],post=new float[N],theta=new float[N];
    private int[] refractory=new int[N];
    private float[] previous;
    private float[] lastResponse=new float[N];
    public synchronized float[] response(){return lastResponse.clone();}
    private final ArrayList<float[]> prototypes=new ArrayList<>();
    private final ArrayList<Integer> repeats=new ArrayList<>();
    public long frames;
    private final Random random=new Random(42);
    public static final class Result {
        public final int spikes,patterns;
        public final float activity,similarity,direction,meanWeight;
        public final boolean familiar;
        public final long frames;
        public Result(int s,int p,float a,float sim,float dir,float mean,boolean f,long count){
            spikes=s;patterns=p;activity=a;similarity=sim;direction=dir;meanWeight=mean;familiar=f;frames=count;
        }
    }
    public Brain(){
        Arrays.fill(theta,1f);
        for(int n=0;n<N;n++){
            float sum=0;
            for(int i=0;i<IN;i++){weights[n][i]=.01f+random.nextFloat()*.02f;sum+=weights[n][i];}
            for(int i=0;i<IN;i++)weights[n][i]*=15f/sum;
        }
    }
    public synchronized void clearDynamics(){
        previous=null;Arrays.fill(v,0);Arrays.fill(pre,0);Arrays.fill(post,0);Arrays.fill(refractory,0);
    }
    private static float clip(float f){return Math.max(0,Math.min(.15f,f));}
    public synchronized Result process(float[] image, boolean learn){
        if(image.length!=W*H)throw new IllegalArgumentException("Dimensão inválida");
        for(float f:image)if(!Float.isFinite(f)||f<0||f>1)throw new IllegalArgumentException("Pixel inválido");
        frames++;Arrays.fill(lastResponse,0);
        if(previous==null){previous=image.clone();return result(0,0,0,0,false);}
        float activity=0,direction=0,total=0;
        int[] times=new int[IN];Arrays.fill(times,-1);
        float[] descriptor=new float[DIM];
        for(int i=0;i<image.length;i++){
            float delta=image[i]-previous[i],m=Math.abs(delta);activity+=m;total+=m;
            direction+=m*(2f*(i%W)/(W-1)-1);
            if(m>.06f){
                int ix=delta>0?i:i+W*H;
                times[ix]=(int)Math.floor((1-Math.min(1,m))*(STEPS-1));
                int cell=(i/W/4)*6+(i%W/4);
                descriptor[N+cell+(delta<0?24:0)]+=m;
            }
        }
        previous=image.clone();activity/=image.length;direction/=Math.max(1e-8f,total);
        int spikes=0;
        for(int t=0;t<STEPS;t++){
            for(int i=0;i<IN;i++)pre[i]*=.9512294f;
            for(int n=0;n<N;n++){
                post[n]*=.9512294f;v[n]*=.9512294f;
                if(learn)theta[n]+=(1-theta[n])*.001f;
                refractory[n]=Math.max(0,refractory[n]-1);
            }
            for(int i=0;i<IN;i++)if(times[i]==t){
                for(int n=0;n<N;n++){
                    v[n]+=weights[n][i]*3;
                    if(learn)weights[n][i]=clip(weights[n][i]-.00084f*post[n]);
                }
            }
            int winner=-1;float surplus=-Float.MAX_VALUE;
            for(int n=0;n<N;n++){
                if(refractory[n]>0)v[n]=0;
                if(v[n]>=theta[n]&&v[n]-theta[n]>surplus){winner=n;surplus=v[n]-theta[n];}
            }
            if(winner>=0){
                if(learn){
                    for(int i=0;i<IN;i++)weights[winner][i]=clip(weights[winner][i]+.0008f*pre[i]);
                    theta[winner]=Math.min(2.5f,theta[winner]+.035f);
                }
                descriptor[winner]++;spikes++;post[winner]++;
                for(int n=0;n<N;n++)v[n]*=.25f;
                v[winner]=0;refractory[winner]=3;
            }
            for(int i=0;i<IN;i++)if(times[i]==t)pre[i]++;
        }
        if(learn)for(int n=0;n<N;n++){
            float sum=0;for(float w:weights[n])sum+=w;
            float scale=.99f+.01f*15f/Math.max(sum,1e-6f);
            for(int i=0;i<IN;i++)weights[n][i]=clip(weights[n][i]*scale);
        }
        System.arraycopy(descriptor,0,lastResponse,0,N);
        normalize(descriptor,0,N);normalize(descriptor,N,DIM);
        normalize(descriptor,0,DIM);
        float similarity=0;int best=-1;boolean familiar=false;
        if(spikes>0&&activity>.001f){
            for(int p=0;p<prototypes.size();p++){
                float dot=dot(descriptor,prototypes.get(p));
                if(dot>similarity){similarity=dot;best=p;}
            }
            familiar=similarity>=.88f&&best>=0&&repeats.get(best)>=3;
            if(learn){
                if(best>=0&&similarity>=.88f){
                    float[] proto=prototypes.get(best);
                    for(int i=0;i<DIM;i++)proto[i]=.95f*proto[i]+.05f*descriptor[i];
                    normalize(proto,0,DIM);repeats.set(best,Math.min(1000000,repeats.get(best)+1));
                }else if(prototypes.size()<MAX){
                    prototypes.add(descriptor.clone());repeats.add(1);
                }else{
                    int weakest=0;for(int i=1;i<MAX;i++)if(repeats.get(i)<repeats.get(weakest))weakest=i;
                    prototypes.set(weakest,descriptor.clone());repeats.set(weakest,1);
                }
            }
        }
        return result(spikes,activity,similarity,direction,familiar);
    }
    private Result result(int spikes,float activity,float sim,float dir,boolean familiar){
        float sum=0;for(float[] row:weights)for(float w:row)sum+=w;
        return new Result(spikes,prototypes.size(),activity,sim,dir,sum/(N*IN),familiar,frames);
    }
    private static void normalize(float[] d,int start,int end){
        float sum=0;for(int i=start;i<end;i++)sum+=d[i]*d[i];
        if(sum>0){float norm=(float)Math.sqrt(sum);for(int i=start;i<end;i++)d[i]/=norm;}
    }
    private static float dot(float[] a,float[] b){float sum=0;for(int i=0;i<a.length;i++)sum+=a[i]*b[i];return sum;}
    public synchronized void maskPrevious(boolean[] mask){if(previous!=null)for(int i=0;i<mask.length;i++)if(mask[i])previous[i]=0;}
    public synchronized void save(File file)throws IOException{
        File tmp=new File(file.getPath()+".tmp");
        try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)))){
            out.writeInt(0x54484231);out.writeLong(frames);
            for(float[] row:weights)for(float w:row)out.writeFloat(w);
            for(float f:theta)out.writeFloat(f);
            out.writeInt(prototypes.size());
            for(int p=0;p<prototypes.size();p++){
                out.writeInt(repeats.get(p));for(float f:prototypes.get(p))out.writeFloat(f);
            }
        }
        java.nio.file.Files.move(tmp.toPath(),file.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
    public static Brain load(File file)throws IOException{
        Brain b=new Brain();
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(file)))){
            if(in.readInt()!=0x54484231)throw new IOException("Memória visual incompatível");
            b.frames=in.readLong();if(b.frames<0)throw new IOException("Contador inválido");
            for(int n=0;n<N;n++)for(int i=0;i<IN;i++){
                float f=in.readFloat();if(!Float.isFinite(f)||f<0||f>.15001f)throw new IOException("Peso inválido");b.weights[n][i]=f;
            }
            for(int n=0;n<N;n++){
                float f=in.readFloat();if(!Float.isFinite(f)||f<1||f>2.5f)throw new IOException("Limiar inválido");b.theta[n]=f;
            }
            int count=in.readInt();if(count<0||count>MAX)throw new IOException("Protótipos inválidos");
            for(int p=0;p<count;p++){
                int repeats=in.readInt();if(repeats<1)throw new IOException("Repetição inválida");
                float[] proto=new float[DIM];for(int i=0;i<DIM;i++){
                    proto[i]=in.readFloat();if(!Float.isFinite(proto[i])||proto[i]<0||proto[i]>1.001f)throw new IOException("Descritor inválido");
                }
                b.prototypes.add(proto);b.repeats.add(repeats);
            }
        }
        return b;
    }
    public static float[] demo(int i){
        float[] image=new float[W*H];int x=(i/2)%(W-5);
        for(int y=4;y<12;y++)for(int j=x;j<x+5;j++)image[y*W+j]=.95f;
        return image;
    }
}
