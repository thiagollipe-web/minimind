import com.brittech.minibuddy.Brain;
import com.brittech.minibuddy.TextTools;
import java.io.*;
import java.util.*;
public final class CoreTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static float[][] copy(float[][] values){float[][] out=new float[values.length][];for(int i=0;i<values.length;i++)out[i]=values[i].clone();return out;}
    static boolean equal(float[][] a,float[][] b){for(int i=0;i<a.length;i++)if(!Arrays.equals(a[i],b[i]))return false;return true;}
    public static void main(String[] args)throws Exception {
        Brain b=new Brain();float[][] initial=copy(b.weights);int total=0;
        for(int i=0;i<160;i++)total+=b.process(Brain.demo(i),true).spikes;
        check(total>0,"A rede precisa disparar para aprender");
        check(!equal(initial,b.weights),"STDP precisa alterar conexões");
        float[][] trained=copy(b.weights);
        for(int i=0;i<80;i++)b.process(Brain.demo(i),false);
        check(equal(trained,b.weights),"Congelamento deve preservar pesos");
        for(float[] row:b.weights)for(float f:row)check(Float.isFinite(f)&&f>=0&&f<=.15001f,"Peso fora dos limites");
        File file=File.createTempFile("buddy-test",".bin");
        b.save(file);Brain restored=Brain.load(file);check(equal(b.weights,restored.weights),"Persistência deve recuperar pesos exatos");check(b.frames==restored.frames,"Contador deve persistir");file.delete();
        File bad=File.createTempFile("buddy-bad",".bin");
        try(FileOutputStream out=new FileOutputStream(bad)){out.write(new byte[]{1,2,3,4});}
        boolean rejected=false;try{Brain.load(bad);}catch(IOException e){rejected=true;}check(rejected,"Arquivo inválido deve ser rejeitado");bad.delete();
        Brain quiet=new Brain();for(int i=0;i<10;i++)check(quiet.process(new float[Brain.W*Brain.H],true).spikes==0,"Tela estática deve ficar silenciosa");
        check(TextTools.tokens("Programação, memória e Python").contains("programacao"),"Busca deve normalizar acentos");
        check(TextTools.features("Aula de Python","movimento","tela").contains("texto:python"),"Texto deve compor contexto das ações");
        System.out.println("OK: "+total+" spikes; STDP, congelamento, limites, persistência, rejeição de arquivo inválido e normalização textual.");
    }
}
