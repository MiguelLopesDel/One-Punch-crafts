package com.onepunchcrafts.client.input;

/** Decides what releasing the selector key means for the radial wheel. */
public final class TechniqueWheelReleasePolicy {
    private TechniqueWheelReleasePolicy() {}

    public static Action resolve(boolean pointerMoved, boolean techniqueHovered) {
        if (!pointerMoved) return Action.KEEP_OPEN;
        return techniqueHovered ? Action.CONFIRM : Action.CANCEL;
    }

    public enum Action { CONFIRM, CANCEL, KEEP_OPEN }
}
