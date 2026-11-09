package mcaxolotl.bbstacz;

import mcaxolotl.bbstacz.recording.Pcm16;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Pcm16Test {
    private static byte[] pcm(int... values) { byte[] data=new byte[values.length*2]; for(int i=0;i<values.length;i++) { data[i*2]=(byte)values[i]; data[i*2+1]=(byte)(values[i]>>8); } return data; }
    @Test void unitPitchIsBitExact() { var input=pcm(-32768,-100,0,100,32767); assertArrayEquals(input,Pcm16.bake(input,1,10,1,0,1)); }
    @Test void resolvedPitchChangesPlaybackLengthAndSamples() { var input=pcm(0,100,200,300); assertArrayEquals(pcm(0,200),Pcm16.bake(input,1,10,2,0,1)); assertArrayEquals(pcm(0,50,100,150,200,250,300,300),Pcm16.bake(input,1,10,0.5f,0,1)); }
    @Test void subTickPaddingDoesNotQuantizeCueToANeighboringTick() { assertArrayEquals(pcm(0,0,100,200),Pcm16.bake(pcm(100,200),1,100,1,0.02,1)); }
    @Test void nativeStopTrimsOnlyTheAudibleTail() { assertArrayEquals(pcm(0,100,200),Pcm16.bake(pcm(100,200,300,400),1,10,1,0.1,0.2)); }
    @Test void stereoChannelsDoNotBleedTogether() { assertArrayEquals(pcm(0,1000,100,1100),Pcm16.bake(pcm(0,1000,200,1200),2,10,0.5f,0,0.2)); }
    @Test void zeroLengthCueHasNoPaddingOnlyFile() { assertArrayEquals(new byte[0],Pcm16.bake(pcm(100),1,10,1,0.1,0)); }
    @Test void hugePaddingCannotOverflowTheArrayLength() { assertThrows(IllegalArgumentException.class,()->Pcm16.bake(pcm(100),1,48000,1,Double.MAX_VALUE,1)); }
    @Test void corruptPcmAndNonfiniteTimingAreRejected() {
        assertThrows(IllegalArgumentException.class,()->Pcm16.bake(new byte[3],1,10,1,0,1));
        assertThrows(IllegalArgumentException.class,()->Pcm16.bake(new byte[0],0,10,1,0,1));
        assertThrows(IllegalArgumentException.class,()->Pcm16.bake(pcm(100),1,10,Float.NaN,0,1));
        assertThrows(IllegalArgumentException.class,()->Pcm16.bake(pcm(100),1,10,1,Double.POSITIVE_INFINITY,1));
    }
}
