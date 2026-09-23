package com.redcodersgroup.bubbleshooter.physics;

import android.graphics.PointF;
import com.redcodersgroup.bubbleshooter.board.BubbleGrid;
import com.redcodersgroup.bubbleshooter.board.GridPosition;
import com.redcodersgroup.bubbleshooter.bubble.Bubble;
import java.util.ArrayList;
import java.util.List;

public class TrajectoryCalculator {
    public static final float STEP_SIZE = 22f; // px between guide points
    public static final int MAX_STEPS = 120;
    public static final int MAX_BOUNCES = 5;

    public static class TrajectoryResult {
        public final List<PointF> points;
        public final boolean bounceLimitExceeded;

        public TrajectoryResult(List<PointF> points, boolean bounceLimitExceeded) {
            this.points = points;
            this.bounceLimitExceeded = bounceLimitExceeded;
        }
    }

    public static List<PointF> calculateTrajectory(
            float startX, float startY, float angleRad,
            float leftBound, float rightBound, float topBound,
            BubbleGrid grid, float bubbleRadius) {
        return calculateTrajectory(startX, startY, angleRad, leftBound, rightBound, topBound, grid, bubbleRadius, false, MAX_BOUNCES).points;
    }

    public static List<PointF> calculateTrajectory(
            float startX, float startY, float angleRad,
            float leftBound, float rightBound, float topBound,
            BubbleGrid grid, float bubbleRadius, boolean isPiercing) {
        return calculateTrajectory(startX, startY, angleRad, leftBound, rightBound, topBound, grid, bubbleRadius, isPiercing, isPiercing ? 1 : MAX_BOUNCES).points;
    }

    public static TrajectoryResult calculateTrajectory(
            float startX, float startY, float angleRad,
            float leftBound, float rightBound, float topBound,
            BubbleGrid grid, float bubbleRadius, boolean isPiercing, int maxBounces) {

        List<PointF> points = new ArrayList<>();
        float minX = leftBound + bubbleRadius;
        float maxX = rightBound - bubbleRadius;
        float minY = topBound + bubbleRadius;

        float rx = startX;
        float ry = startY;
        float dx = (float) Math.cos(angleRad);
        float dy = (float) Math.sin(angleRad);

        // Normalize direction
        float len = (float) Math.hypot(dx, dy);
        if (len == 0) return new TrajectoryResult(points, false);
        dx /= len;
        dy /= len;

        int bounces = 0;
        boolean bounceLimitExceeded = false;
        float colRadius = bubbleRadius * 2f;
        float collisionDistSq = colRadius * colRadius;

        for (int i = 0; i < MAX_STEPS; i++) {
            float prevRx = rx;
            float prevRy = ry;
            rx += dx * STEP_SIZE;
            ry += dy * STEP_SIZE;

            // 1. Wall bounce checks
            if (rx <= minX && dx < 0) {
                float tWall = (dx != 0) ? (minX - prevRx) / dx : 0f;
                float wallY = prevRy + dy * tWall;
                rx = minX;
                ry = wallY;
                if (maxBounces > 0 && bounces >= maxBounces) {
                    bounceLimitExceeded = true;
                    points.add(new PointF(rx, ry));
                    break;
                }
                dx = -dx;
                bounces++;
            } else if (rx >= maxX && dx > 0) {
                float tWall = (dx != 0) ? (maxX - prevRx) / dx : 0f;
                float wallY = prevRy + dy * tWall;
                rx = maxX;
                ry = wallY;
                if (maxBounces > 0 && bounces >= maxBounces) {
                    bounceLimitExceeded = true;
                    points.add(new PointF(rx, ry));
                    break;
                }
                dx = -dx;
                bounces++;
            }

            // 2. Ceiling hit
            if (ry <= minY && dy < 0) {
                float tCeil = (dy != 0) ? (minY - prevRy) / dy : 0f;
                float ceilX = prevRx + dx * tCeil;
                points.add(new PointF(ceilX, minY));
                break;
            }

            // 3. Collision with existing grid bubbles (ignored if piercing)
            boolean collided = false;
            float bestHitT = Float.MAX_VALUE;
            if (!isPiercing && grid != null) {
                for (Bubble b : grid.getAllBubbles()) {
                    if (b != null && !b.isPopping() && !b.isFalling()) {
                        float vx = prevRx - b.getX();
                        float vy = prevRy - b.getY();
                        float c = vx * vx + vy * vy - collisionDistSq;
                        if (c <= 0) {
                            bestHitT = 0f;
                            collided = true;
                            break;
                        }
                        float bDot = vx * dx + vy * dy;
                        if (bDot < 0) {
                            float disc = bDot * bDot - c;
                            if (disc >= 0) {
                                float t = -bDot - (float) Math.sqrt(disc);
                                if (t >= 0 && t <= STEP_SIZE && t < bestHitT) {
                                    bestHitT = t;
                                    collided = true;
                                }
                            }
                        }
                    }
                }
            }

            if (collided) {
                float hitX = prevRx + dx * bestHitT;
                float hitY = prevRy + dy * bestHitT;
                points.add(new PointF(hitX, hitY));
                break;
            }

            points.add(new PointF(rx, ry));
        }

        return new TrajectoryResult(points, bounceLimitExceeded);
    }
}
