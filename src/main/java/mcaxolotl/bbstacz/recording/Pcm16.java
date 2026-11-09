package mcaxolotl.bbstacz.recording;

/** Deterministic PCM baking: resolved native pitch, sub-tick start padding and interruption trimming. */
public final class Pcm16 {
    private Pcm16() {}
    public static byte[] bake(byte[] source, int channels, int rate, float pitch, double leadingSeconds, double audibleSeconds) {
        if (channels < 1 || channels > 2 || rate <= 0 || source.length % (channels * 2) != 0
            || !Float.isFinite(pitch) || pitch <= 0 || !Double.isFinite(leadingSeconds) || leadingSeconds < 0
            || !Double.isFinite(audibleSeconds) || audibleSeconds < 0) throw new IllegalArgumentException("Invalid PCM bake parameters");
        int frames = source.length / (channels * 2);
        long leading = Math.round(leadingSeconds * rate);
        long audible = Math.min((long)Math.ceil(frames / (double)pitch), (long)Math.floor(audibleSeconds * rate));
        if (audible == 0) return new byte[0];
        if (leading > Integer.MAX_VALUE / (channels * 2) || audible > Integer.MAX_VALUE / (channels * 2) - leading) throw new IllegalArgumentException("PCM bake is too large");
        byte[] out = new byte[(int)((leading + audible) * channels * 2)];
        for (int frame = 0; frame < audible; frame++) {
            double position = frame * (double)pitch;
            int a = Math.min(frames - 1, (int)position), b = Math.min(frames - 1, a + 1);
            double fraction = position - a;
            for (int channel = 0; channel < channels; channel++) {
                int left = sample(source, (a * channels + channel) * 2), right = sample(source, (b * channels + channel) * 2);
                int value = (int)Math.round(left + (right - left) * fraction);
                int offset = (int)((leading + frame) * channels + channel) * 2;
                out[offset] = (byte)value; out[offset + 1] = (byte)(value >> 8);
            }
        }
        return out;
    }
    private static int sample(byte[] data, int offset) { return (short)((data[offset] & 255) | data[offset + 1] << 8); }
}
