import com.brittech.minibuddy.*;
import java.io.*;
import java.util.*;
public final class MissionsTest {
    static void check(boolean b,String text){if(!b)throw new AssertionError(text);}
    public static void main(String[] args)throws Exception{
        PetState pet=new PetState(33,8,9);
        check(pet.progress(0)==0&&pet.progress(1)==0,"Memórias antigas não completam missões novas");
        check(!pet.claim(0),"Não premiar tarefa incompleta");
        pet.observe(34,13,12);check(pet.progress(0)==3&&pet.progress(1)==1&&pet.progress(2)==5,"Contar ações novas");
        check(pet.claim(0)&&!pet.claim(0)&&pet.xp==20,"Recompensa apenas uma vez");
        pet.claim(1);pet.claim(2);check(pet.level()==2,"Progressão por XP");
        check(pet.feed(1000000)&&!pet.feed(1000001),"Alimentação com intervalo");
        pet.energy=40;pet.resting=true;pet.advance(20,false);check(pet.energy==41,"Descanso recupera energia");
        pet.advance(-5000,true);check(pet.energy==41,"Relógio não deve reduzir estado");
        pet.resting=false;for(int i=0;i<20000;i++)pet.advance(30,true);
        check(pet.energy>=20&&pet.happiness>=30,"Sem punição extrema ou morte por ausência");
        MissionLab lab=new MissionLab();lab.train(null);
        check(lab.trainingSpikes>0&&lab.changedWeights>0&&lab.meanWeightChange>0,"Treino altera conexões e gera spikes");
        int accuracy=lab.evaluate();check(accuracy>=0&&accuracy<=MissionLab.TESTS,"Resultado de avaliação válido");
        File temp=File.createTempFile("mission-lab",".bin");lab.save(temp);
        MissionLab loaded=new MissionLab(Brain.load(temp));check(loaded.evaluate()==accuracy,"Teste deve ser reproduzível após recuperar a rede");temp.delete();
        pet.trained=true;pet.tested=true;check(pet.claim(3)&&pet.claim(4),"Premiar execução dos experimentos");
        check(pet.xp==200&&pet.level()==3,"XP total coerente");
        System.out.printf(java.util.Locale.ROOT,"OK: cuidados, missões, recompensas únicas e persistência do laboratório. Antes=%d/%d; depois=%d/%d; conexões alteradas=%d; variação média=%.8f; spikes=%d.%n",lab.baselineCorrect,MissionLab.TESTS,accuracy,MissionLab.TESTS,lab.changedWeights,lab.meanWeightChange,lab.trainingSpikes);
    }
}
