package io.hamlook.aetheria.features.custommenu.animation;

public class AnimationController {
    private long startedAt;
    private CMMAnimation animation;

    public void start(CMMAnimation animation) {
        this.animation = animation;
        this.startedAt = System.currentTimeMillis();
    }

    public float value() {
        return animation == null ? 1f : animation.progress(System.currentTimeMillis() - startedAt);
    }
}
