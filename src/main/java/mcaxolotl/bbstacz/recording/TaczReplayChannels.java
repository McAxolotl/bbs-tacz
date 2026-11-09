package mcaxolotl.bbstacz.recording;

import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

public interface TaczReplayChannels {
    String STATE = "bbs_tacz:state";
    String SOUNDS = "bbs_tacz:sounds";
    String COMMANDS = "bbs_tacz:commands";
    KeyframeChannel<FrameSnapshot> bbstacz$state();
    KeyframeChannel<String> bbstacz$commands();
    KeyframeChannel<String> bbstacz$sounds();
}
