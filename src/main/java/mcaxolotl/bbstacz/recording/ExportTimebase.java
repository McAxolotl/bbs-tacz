package mcaxolotl.bbstacz.recording;

/** Maps a paused native clock onto video time without rewinding epochs/cooldowns at export boundaries. */
public final class ExportTimebase {
    private boolean exporting;
    private long offset, last, anchor, videoAnchor;
    public synchronized long sample(long nativeNanos, boolean recording, long videoNanos) {
        if (recording) {
            if (!exporting) {
                anchor=Math.max(last,nativeNanos+offset);
                videoAnchor=videoNanos; exporting=true;
            }
            last=Math.max(last,anchor+Math.max(0,videoNanos-videoAnchor));
        } else {
            if (exporting) { offset=last-nativeNanos; exporting=false; }
            last=Math.max(last,nativeNanos+offset);
        }
        return last;
    }
    public synchronized void reset() { exporting=false; offset=last=anchor=videoAnchor=0; }
}
