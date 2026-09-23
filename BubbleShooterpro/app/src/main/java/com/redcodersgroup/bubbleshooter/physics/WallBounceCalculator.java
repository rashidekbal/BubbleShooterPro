package com.redcodersgroup.bubbleshooter.physics;

import com.redcodersgroup.bubbleshooter.bubble.BubbleProjectile;

public class WallBounceCalculator {

    /**
     * Default wall bounce check (unlimited bounces for standard bubbles).
     */
    public static boolean checkAndHandleWallBounce(BubbleProjectile projectile, float leftBound, float rightBound) {
        return checkAndHandleWallBounce(projectile, leftBound, rightBound, -1);
    }

    /**
     * Checks if the projectile has collided with the left or right wall,
     * clamps its position within boundaries, reverses horizontal velocity,
     * and tracks the bounce count.
     * If maxBounces > 0, enforces that the projectile cannot bounce more than maxBounces times.
     * If maxBounces <= 0, allows unlimited bounces.
     * Returns true if a bounce occurred.
     */
    public static boolean checkAndHandleWallBounce(BubbleProjectile projectile, float leftBound, float rightBound, int maxBounces) {
        if (projectile == null || !projectile.isActive()) return false;

        float r = projectile.getRadius();
        float minX = leftBound + r;
        float maxX = rightBound - r;

        boolean hitLeft = (projectile.getX() <= minX && projectile.getVx() < 0);
        boolean hitRight = (projectile.getX() >= maxX && projectile.getVx() > 0);

        if (hitLeft || hitRight) {
            if (maxBounces > 0 && projectile.getBounceCount() >= maxBounces) {
                return false;
            }

            if (hitLeft) {
                projectile.setX(minX);
            } else {
                projectile.setX(maxX);
            }
            projectile.setVx(-projectile.getVx());
            projectile.incrementBounceCount();
            return true;
        }

        return false;
    }
}
