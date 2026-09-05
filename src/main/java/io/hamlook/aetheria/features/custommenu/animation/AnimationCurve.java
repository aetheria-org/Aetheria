package io.hamlook.aetheria.features.custommenu.animation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Serializable normalized curve used by custom CMM animations. */
public class AnimationCurve {
    public List<Point> points = new ArrayList<>();

    public AnimationCurve() {
        points.add(new Point(0f, 0f));
        points.add(new Point(1f, 1f));
    }

    public float sample(float progress) {
        if (points.isEmpty()) return progress;
        points.sort(Comparator.comparingDouble(p -> p.x));
        if (progress <= points.get(0).x) return points.get(0).y;
        for (int i = 1; i < points.size(); i++) {
            Point b = points.get(i);
            Point a = points.get(i - 1);
            if (progress <= b.x) {
                float span = Math.max(0.0001f, b.x - a.x);
                float t = (progress - a.x) / span;
                return a.y + (b.y - a.y) * t;
            }
        }
        return points.get(points.size() - 1).y;
    }

    public static class Point {
        public float x;
        public float y;

        public Point() {}
        public Point(float x, float y) { this.x = x; this.y = y; }
    }
}
