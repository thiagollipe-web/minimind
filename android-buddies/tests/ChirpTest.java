import com.brittech.minibuddy.Chirps;
import java.util.Arrays;
public final class ChirpTest {
    public static void main(String[] args){
        short[] pet=Chirps.samples("carinho"),curious=Chirps.samples("Curioso"),rest=Chirps.samples("Repouso");
        if(pet.length<8000||Arrays.equals(pet,curious)||Arrays.equals(curious,rest))throw new AssertionError("Reactions need distinct finite audio samples");
        int peak=0;for(short value:pet)peak=Math.max(peak,Math.abs((int)value));
        if(peak<7000||peak>8192)throw new AssertionError("Unexpected audio amplitude: "+peak);
        System.out.println("OK: distinct reaction sounds, "+pet.length+" PCM samples, peak "+peak+" without clipping. Device playback untested.");
    }
}
