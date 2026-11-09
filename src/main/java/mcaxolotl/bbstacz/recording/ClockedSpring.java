package mcaxolotl.bbstacz.recording;

/** TACZ's original 0.05 integration at a 6ms cadence, driven by its animation clock instead of worker sleep. */
public final class ClockedSpring {
    private static final long STEP=6_000_000L;
    private final float k1,k2,k3;
    private float py,pyd,px,target;
    private long integrated;
    public ClockedSpring(float frequency,float damping,float response,float initial,long now) {
        k1=(float)(damping/(Math.PI*frequency));
        k2=(float)(1/((2*Math.PI*frequency)*(2*Math.PI*frequency)));
        k3=(float)(response*damping/(2*Math.PI*frequency));
        py=px=target=initial; integrated=now;
    }
    public float update(float value,long now,boolean stopped) {
        float result=get(now,stopped); target=Float.isFinite(value)?value:0; return result;
    }
    public float get(long now,boolean stopped) {
        if (now<integrated || stopped) integrated=now;
        long steps=Math.max(0,(now-integrated)/STEP);
        // A world unload/long idle is a settled spring, not millions of catch-up iterations.
        if (steps>10_000) { py=px=target; pyd=0; integrated=now; }
        else for (long i=0;i<steps;i++) {
            sanitize();
            float xd=(target-px)/0.05f, y=py+0.05f*pyd;
            pyd+=0.05f*(px+k3*xd-py-k1*pyd)/k2;
            px=target; py=y; integrated+=STEP;
        }
        sanitize(); float result=py+0.05f*pyd;
        return Float.isFinite(result)?result:0;
    }
    private void sanitize() { if (!Float.isFinite(py)) py=0; if (!Float.isFinite(pyd)) pyd=0; }
}
