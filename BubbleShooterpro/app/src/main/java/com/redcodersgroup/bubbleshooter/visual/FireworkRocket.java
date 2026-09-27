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
    private boolean exploded = false;

    public FireworkRocket(float startX, float startY, float targetX, float targetY, float duration, BubbleColor color, float bubbleRadius) {
        this.startX = startX;
        this.startY = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.currentX = startX;
        this.currentY = startY;
        this.duration = Math.max(0.1f, duration);
        this.bubbleColor = (color != null && color != BubbleColor.NONE) ? color : BubbleColor.YELLOW;
        this.bubble = new Bubble(this.bubbleColor, BubbleType.NORMAL, null);
        this.bubble.setRadius(bubbleRadius * 0.9f);
        this.bubble.setScale(1.0f);
        this.bubble.setAlpha(1.0f);
        this.bubble.setX(startX);
        this.bubble.setY(startY);
    }

    public void update(float dt, ConfettiSystem confettiSystem) {
        if (exploded) return;
        elapsed += dt;
        float t = Math.min(1.0f, elapsed / duration);

        // Smooth ease-out quadratic arc upwards
        float easeT = 1.0f - (1.0f - t) * (1.0f - t);
        currentX = startX + (targetX - startX) * easeT;
        currentY = startY + (targetY - startY) * easeT;

        bubble.setX(currentX);
        bubble.setY(currentY);
        bubble.setScale(1.0f + 0.15f * (float) Math.sin(t * Math.PI));

        if (confettiSystem != null) {
            int pColor = bubbleColor.primaryColor != 0 ? bubbleColor.primaryColor : Color.parseColor("#FEF08A");
            confettiSystem.spawnSparkTrail(currentX, currentY, pColor);
        }

        if (elapsed >= duration) {
            exploded = true;
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        if (!exploded && bubble != null) {
            bubble.draw(canvas, paint);
        }
    }

    public boolean isExploded() {
        return exploded;
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
