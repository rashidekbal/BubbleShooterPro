package com.redcodersgroup.bubbleshooter.visual;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ConfettiSystem {
    private final List<Particle> particles = new ArrayList<>();
    private static final int[] CONFETTI_COLORS = new int[]{
            Color.parseColor("#FF3366"),
            Color.parseColor("#F1C40F"),
            Color.parseColor("#2ECC71"),
            Color.parseColor("#3498DB"),
            Color.parseColor("#9B59B6"),
            Color.parseColor("#E67E22"),
            Color.parseColor("#00E5FF")
    };

    public void spawnPopParticles(float x, float y, int bubbleColor, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            float speed = (float) (200 + Math.random() * 500);
            float vx = (float) (Math.cos(angle) * speed);
            float vy = (float) (Math.sin(angle) * speed);
            float size = (float) (8 + Math.random() * 12);
            float life = (float) (0.4 + Math.random() * 0.4);

            Particle.ParticleShape shape = (Math.random() < 0.35) ?
                    Particle.ParticleShape.STAR : Particle.ParticleShape.CIRCLE;

            particles.add(new Particle(x, y, vx, vy, size, bubbleColor, life, shape));
        }
    }

    public void spawnFireworkBurst(float x, float y, int mainColor, int count) {
        // 1. Radial primary sparkling sphere with stars and glows
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            float speed = (float) (220 + Math.random() * 580);
            float vx = (float) (Math.cos(angle) * speed);
            float vy = (float) (Math.sin(angle) * speed);
            float size = (float) (9 + Math.random() * 14);
            float life = (float) (0.55 + Math.random() * 0.55);

            int pColor = (Math.random() < 0.65) ? mainColor :
                    ((Math.random() < 0.5) ? Color.parseColor("#FEF08A") : Color.WHITE);
            Particle.ParticleShape shape = (Math.random() < 0.5) ?
                    Particle.ParticleShape.STAR : Particle.ParticleShape.CIRCLE;

            particles.add(new Particle(x, y, vx, vy, size, pColor, life, shape));
        }

        // 2. High-speed golden glitter spark ring
        for (int i = 0; i < 16; i++) {
            double angle = (i / 16.0) * Math.PI * 2 + (Math.random() * 0.2);
            float speed = (float) (420 + Math.random() * 240);
            float vx = (float) (Math.cos(angle) * speed);
            float vy = (float) (Math.sin(angle) * speed);
            float size = (float) (8 + Math.random() * 8);
            float life = (float) (0.65 + Math.random() * 0.4);

            particles.add(new Particle(x, y, vx, vy, size, Color.parseColor("#FDE047"), life, Particle.ParticleShape.STAR));
        }
    }

    public void spawnSparkTrail(float x, float y, int color) {
        for (int i = 0; i < 2; i++) {
            float vx = (float) ((Math.random() - 0.5) * 80);
            float vy = (float) (80 + Math.random() * 140);
            float size = (float) (5 + Math.random() * 7);
            float life = (float) (0.22 + Math.random() * 0.2);
            int sparkColor = (Math.random() < 0.5) ? color : Color.parseColor("#FEF08A");
            particles.add(new Particle(x, y, vx, vy, size, sparkColor, life, Particle.ParticleShape.STAR));
        }
    }

    public void spawnCelebrationBurst(float width, float height, int count) {
        for (int i = 0; i < count; i++) {
            float x = (float) (width * 0.2 + Math.random() * width * 0.6);
            float y = (float) (height * 0.3 + Math.random() * height * 0.3);
            double angle = Math.random() * Math.PI * 2;
            float speed = (float) (300 + Math.random() * 700);
            float vx = (float) (Math.cos(angle) * speed);
            float vy = (float) (Math.sin(angle) * speed - 200);
            float size = (float) (10 + Math.random() * 16);
            float life = (float) (1.2 + Math.random() * 1.0);
            int color = CONFETTI_COLORS[(int) (Math.random() * CONFETTI_COLORS.length)];

            Particle.ParticleShape shape = (Math.random() < 0.5) ?
                    Particle.ParticleShape.CONFETTI : Particle.ParticleShape.STAR;

            particles.add(new Particle(x, y, vx, vy, size, color, life, shape));
        }
    }

    public void update(float dt) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update(dt);
            if (!p.isAlive()) {
                it.remove();
            }
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        for (Particle p : particles) {
            p.draw(canvas, paint);
        }
    }

    public void clear() {
        particles.clear();
    }
}
