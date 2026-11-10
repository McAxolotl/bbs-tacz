package mcaxolotl.bbstacz.smoke;

/** Reopen the real complete six-motion FP take and export every motion from a fresh renderer session. */
public final class RecordedFirstPersonF6Review extends RecordedThirdPersonF6Review {
    @Override protected boolean firstPerson() { return true; }
    @Override protected String sourceScenario() { return "MovingGunWorkflowReview"; }
}
