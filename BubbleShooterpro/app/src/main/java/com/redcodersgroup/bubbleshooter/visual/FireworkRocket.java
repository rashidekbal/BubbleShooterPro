package com.redcodersgroup.bubbleshooter.visual;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.redcodersgroup.bubbleshooter.bubble.Bubble;
import com.redcodersgroup.bubbleshooter.bubble.BubbleColor;
import com.redcodersgroup.bubbleshooter.bubble.BubbleType;

public class FireworkRocket {
    private final float startX, startY;
    private final float targetX, targetY;
    private float currentX, currentY;
    private final float duration;
    private float elapsed = 0f;
    private final Bubble bubble;
    private final BubbleColor bubbleColor;
    private final float bubbleRadius;
    private boolean detonated = false;
    private boolean burstEffectsSpawned = false;
    private float burstProgress = 0f;
    private static final float BURST_DURATION = 0.28f;

    public FireworkRocket(float startX, float startY, float targetX, float targetY, float duration, BubbleColor color, float bubbleRadius) {
        this.startX = startX;
        this.startY = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.currentX = startX;
        this.currentY = startY;
        this.duration = Math.max(0.12f, duration);
        this.bubbleRadius = bubbleRadius;
        this.bubbleColor = (color != null && color != BubbleColor.NONE) ? color : BubbleColor.YELLOW;
        this.bubble = new Bubble(this.bubbleColor, BubbleType.NORMAL, null);
        this.bubble.setRadius(bubbleRadius * 0.95f);
        this.bubble.setScale(1.0f);
        this.bubble.setAlpha(1.0f);
        this.bubble.setX(startX);
        this.bubble.setY(startY);
    }

    public void update(float dt, ConfettiSystem confettiSystem) {
        if (!detonated) {
            elapsed += dt;
            float t = Math.min(1.0f, elapsed / duration);

            // Smooth ease-out quadratic arc upwards
            float easeT = 1.0f - (1.0f - t) * (1.0f - t);
            currentX = startX + (targetX - startX) * easeT;
            currentY = startY + (targetY - startY) * easeT;

            bubble.setX(currentX);
            bubble.setY(currentY);
            bubble.setScale(1.0f + 0.18f * (float) Math.sin(t * Math.PI));

            if (confettiSystem != null) {
                int pColor = bubbleColor.primaryColor != 0 ? bubbleColor.primaryColor : Color.parseColor("#FEF08A");
                confettiSystem.spawnSparkTrail(currentX, currentY, pColor);
            }

            if (elapsed >= duration) {
                detonated = true;
                bubble.startPop();
            }
        } else {
            // Burst phase: update bubble pop & shockwave
            burstProgress += dt / BURST_DURATION;
            bubble.update(dt);
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        if (!detonated) {
            bubble.draw(canvas, paint);
        } else if (burstProgress < 1.0f) {
            // 1. Draw popping bubble with expanding scale & alpha fade
            bubble.draw(canvas, paint);

            // 2. Draw expanding pyrotechnic shockwave ring
            float ringRadius = bubbleRadius * (0.8f + burstProgress * 2.4f);
            int alpha = (int) (220 * (1.0f - burstProgress));
            int pColor = bubbleColor.primaryColor != 0 ? bubbleColor.primaryColor : Color.parseColor("#FEF08A");

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2.5f, bubbleRadius * 0.16f * (1.0f - burstProgress * 0.6f)));
            paint.setColor(pColor);
            paint.setAlpha(alpha);
            canvas.drawCircle(currentX, currentY, ringRadius, paint);

            // 3. Central brilliant white flash
            if (burstProgress < 0.45f) {
                float flashT = burstProgress / 0.45f;
                int flashAlpha = (int) (255 * (1.0f - flashT));
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.WHITE);
                paint.setAlpha(flashAlpha);
                canvas.drawCircle(currentX, currentY, bubbleRadius * 0.85f * (1.0f - flashT * 0.4f), paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }
    }

    public boolean shouldSpawnBurstEffects() {
        if (detonated && !burstEffectsSpawned) {
            burstEffectsSpawned = true;
            return true;
        }
        return false;
    }

    public boolean isFinished() {
        return detonated && burstProgress >= 1.0f;
    }

    public float getX() {
        return currentX;
    }

    public float getY() {
        return currentY;
    }

    public BubbleColor getBubbleColor() {
        return bubbleColor;
    }
}
