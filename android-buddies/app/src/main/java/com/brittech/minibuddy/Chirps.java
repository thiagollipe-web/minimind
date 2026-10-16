package com.brittech.minibuddy;
import android.media.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Chirps {
    private static final AtomicBoolean playing=new AtomicBoolean(false);
    public static short[] samples(){return samples("carinho");}
    public static short[] samples(String reaction){
        int rate=22050;short[] pcm=new short[(int)(.38*rate)];double phase=0;
        for(int i=0;i<pcm.length;i++){
            double t=i/(double)rate,local=t%.12,ratio=local/.12;
            double base=reaction.equals("Repouso")?330:reaction.equals("Curioso")?740:reaction.equals("Padrão recorrente")?880:550;
            double freq=base+200*(int)(t/.12)+180*ratio;phase+=2*Math.PI*freq/rate;
            double env=Math.pow(Math.sin(Math.PI*ratio),2);
            pcm[i]=(short)(32767*.25*env*Math.sin(phase));
        }return pcm;
    }
    public static void play(Session session){play(session,"carinho");}
    public static void play(Session session,String reaction){
        if(session.prefs.getBoolean("muted",false)||!playing.compareAndSet(false,true))return;
        new Thread(()->{
            AudioTrack track=null;
            try{
                short[] pcm=samples(reaction);
                track=new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(new AudioFormat.Builder().setSampleRate(22050).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                    .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(pcm.length*2).build();
                if(track.getState()!=AudioTrack.STATE_INITIALIZED)throw new IllegalStateException("Áudio indisponível");
                track.setVolume(Math.max(0f,Math.min(1f,session.prefs.getFloat("volume",.8f))));
                int written=track.write(pcm,0,pcm.length);
                if(written!=pcm.length)throw new IllegalStateException("Não foi possível carregar o som");
                track.play();Thread.sleep(480);
            }catch(Exception e){session.message="Áudio indisponível: "+e.getMessage();}
            finally{if(track!=null){try{track.stop();}catch(Exception ignored){}track.release();}playing.set(false);}
        },"buddy-audio").start();
    }
}
