package com.redcodersgroup.bubbleshooter.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.redcodersgroup.bubbleshooter.audio.SoundManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Interactive Ambient Game Background for the Bubble Shooter Home Screen.
 * Features 3D floating tactile bubbles, twinkling arcade stars,
 * tumbling game confetti sprinkles, interactive cursor repulsion & tap-to-pop bursts.
 */
public class HomeBackgroundView extends View {

    private final Random random = new Random();
    private final float density;

    private boolean isRunning = false;
    private long lastFrameTimeNanos = 0;
    private float time = 0;

    // Touch / Cursor tracking
    private float cursorX = -9999f;
    private float cursorY = -9999f;
    private float targetCursorX = -9999f;
    private float targetCursorY = -9999f;
    private boolean isCursorActive = false;

    // Entities
    private final List<FloatingBubble> bubbles = new ArrayList<>();
    private final List<ArcadeStar> stars = new ArrayList<>();
    private final List<ConfettiSprinkle> sprinkles = new ArrayList<>();
    private final List<SparkBurstParticle> bursts = new ArrayList<>();
    private final List<ShockwaveRing> shockwaves = new ArrayList<>();

    // Reusable drawing objects to avoid GC allocations in onDraw
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint auraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sprinklePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spherePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shockwavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF tempRectF = new RectF();
    private final Path tempPath = new Path();

    private final DashPathEffect orbitDash1;
    private final DashPathEffect orbitDash2;

    // Game Bubble Color Palette
    private static final int[][] BUBBLE_PALETTES = new int[][]{
            // Red: primary, light, dark
            { Color.parseColor("#FF1744"), Color.parseColor("#FF5252"), Color.parseColor("#C62828") },
            // Green: primary, light, dark
            { Color.parseColor("#00C853"), Color.parseColor("#69F0AE"), Color.parseColor("#1B5E20") },
            // Blue: primary, light, dark
            { Color.parseColor("#0091EA"), Color.parseColor("#40C4FF"), Color.parseColor("#0D47A1") },
            // Yellow: primary, light, dark
            { Color.parseColor("#FFD600"), Color.parseColor("#FFFF00"), Color.parseColor("#FF6F00") },
            // Purple: primary, light, dark
            { Color.parseColor("#AA00FF"), Color.parseColor("#E040FB"), Color.parseColor("#4A148C") },
            // Orange: primary, light, dark
            { Color.parseColor("#FF6D00"), Color.parseColor("#FFAB40"), Color.parseColor("#BF360C") },
            // Cyan: primary, light, dark
            { Color.parseColor("#00E5FF"), Color.parseColor("#84FFFF"), Color.parseColor("#006064") }
    };

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!isRunning) return;

            if (lastFrameTimeNanos > 0) {
                float dt = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000.0f;
                if (dt > 0.05f) dt = 0.05f; // Cap delta time for stability
                time += dt;
                update(dt);
                invalidate();
            }
            lastFrameTimeNanos = frameTimeNanos;
            Choreographer.getInstance().postFrameCallback(this);
        }
    };

    public HomeBackgroundView(Context context) {
        this(context, null);
    }

    public HomeBackgroundView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeBackgroundView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;

        orbitDash1 = new DashPathEffect(new float[]{4 * density, 10 * density}, 0);
        orbitDash2 = new DashPathEffect(new float[]{2 * density, 14 * density}, 0);

        initPaints();
        setupEntities();
    }

    private void initPaints() {
        bgPaint.setStyle(Paint.Style.FILL);
        auraPaint.setStyle(Paint.Style.FILL);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(1.2f * density);

        starPaint.setStyle(Paint.Style.FILL);
        starCorePaint.setStyle(Paint.Style.FILL);
        starCorePaint.setColor(Color.WHITE);

        sprinklePaint.setStyle(Paint.Style.FILL);

        shadowPaint.setStyle(Paint.Style.FILL);
        bubblePaint.setStyle(Paint.Style.FILL);
        spherePaint.setStyle(Paint.Style.FILL);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1.5f * density);

        highlightPaint.setStyle(Paint.Style.FILL);
        highlightPaint.setColor(Color.WHITE);

        particlePaint.setStyle(Paint.Style.FILL);

        shockwavePaint.setStyle(Paint.Style.STROKE);
        shockwavePaint.setStrokeWidth(2.2f * density);
    }

    private void setupEntities() {
        bubbles.clear();
        // Depth 0: Background (smaller, faint), Depth 1: Midground, Depth 2: Foreground (prominent, punchy)
        bubbles.add(new FloatingBubble(0, 0.12f, 0.12f, 38, 1, -0.10f, 0)); // Red
        bubbles.add(new FloatingBubble(3, 0.88f, 0.14f, 44, 2, 0.12f, 1));  // Yellow
        bubbles.add(new FloatingBubble(1, 0.26f, 0.22f, 30, 0, 0.08f, 2));  // Green
        bubbles.add(new FloatingBubble(4, 0.08f, 0.35f, 48, 2, 0.18f, 3));  // Purple
        bubbles.add(new FloatingBubble(6, 0.92f, 0.38f, 42, 1, -0.15f, 4)); // Cyan
        bubbles.add(new FloatingBubble(2, 0.15f, 0.50f, 34, 0, -0.09f, 5)); // Blue
        bubbles.add(new FloatingBubble(5, 0.86f, 0.54f, 50, 2, 0.14f, 6));  // Orange
        bubbles.add(new FloatingBubble(1, 0.09f, 0.70f, 46, 1, -0.20f, 7)); // Green
        bubbles.add(new FloatingBubble(0, 0.90f, 0.72f, 52, 2, 0.16f, 8));  // Red
        bubbles.add(new FloatingBubble(4, 0.20f, 0.86f, 36, 0, 0.10f, 9));  // Purple
        bubbles.add(new FloatingBubble(3, 0.80f, 0.88f, 42, 1, -0.12f, 10)); // Yellow

        Collections.sort(bubbles, Comparator.comparingInt(b -> b.depth));

        // Twinkling Geometric Arcade Stars
        stars.clear();
        int starCount = 20;
        for (int i = 0; i < starCount; i++) {
            stars.add(new ArcadeStar(
                    0.04f + random.nextFloat() * 0.92f,
                    0.03f + random.nextFloat() * 0.94f,
                    5f + random.nextFloat() * 7f,
                    random.nextFloat() > 0.4f ? 4 : 8,
                    1.5f + random.nextFloat() * 2.5f,
                    random.nextFloat() * (float) (Math.PI * 2),
                    (random.nextFloat() - 0.5f) * 0.8f,
                    random.nextFloat() * (float) Math.PI,
                    0.30f + random.nextFloat() * 0.45f,
                    0.04f + random.nextFloat() * 0.08f,
                    random.nextFloat()
            ));
        }

        // Floating Confetti Sprinkles
        sprinkles.clear();
        int sprinkleCount = 16;
        for (int i = 0; i < sprinkleCount; i++) {
            boolean isBead = (i % 3 == 0);
            sprinkles.add(new ConfettiSprinkle(
                    0.03f + random.nextFloat() * 0.94f,
                    0.05f + random.nextFloat() * 0.90f,
                    !isBead,
                    isBead ? 5f : 4.5f,
                    isBead ? 5f : 11f,
                    random.nextFloat() * (float) (Math.PI * 2),
                    random.nextFloat() * (float) (Math.PI * 2),
                    0.6f + random.nextFloat() * 1.2f,
                    (random.nextFloat() - 0.5f) * 0.8f,
                    6f + random.nextFloat() * 10f,
                    i % BUBBLE_PALETTES.length,
                    0.35f + random.nextFloat() * 0.4f
            ));
        }
    }

    public void resumeAnimation() {
        if (!isRunning) {
            isRunning = true;
            lastFrameTimeNanos = 0;
            Choreographer.getInstance().postFrameCallback(frameCallback);
        }
    }

    public void pauseAnimation() {
        if (isRunning) {
            isRunning = false;
            Choreographer.getInstance().removeFrameCallback(frameCallback);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        resumeAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        pauseAnimation();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            update(0f);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                this.isCursorActive = true;
                this.targetCursorX = x;
                this.targetCursorY = y;
                handleTap(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                this.targetCursorX = x;
                this.targetCursorY = y;
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                this.isCursorActive = false;
                this.targetCursorX = -9999f;
                this.targetCursorY = -9999f;
                return true;
            case MotionEvent.ACTION_CANCEL:
                this.isCursorActive = false;
                this.targetCursorX = -9999f;
                this.targetCursorY = -9999f;
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /**
     * Hit test on floating bubbles. If a bubble is tapped, it bounces and spawns sparks.
     */
    public boolean handleTap(float tapX, float tapY) {
        FloatingBubble hitBubble = null;

        for (int i = bubbles.size() - 1; i >= 0; i--) {
            FloatingBubble b = bubbles.get(i);
            float radius = (b.sizePx * b.popScale) / 2.0f + 16 * density;
            float dx = tapX - b.x;
            float dy = tapY - b.y;
            if (dx * dx + dy * dy <= radius * radius) {
                hitBubble = b;
                break;
            }
        }

        if (hitBubble != null) {
            // Pop the bubble!
            hitBubble.popScale = 1.45f;
            hitBubble.popVelocity = 0.35f;
            hitBubble.wobble = (random.nextBoolean() ? 1f : -1f) * 0.30f;

            int[] pal = BUBBLE_PALETTES[hitBubble.colorIndex % BUBBLE_PALETTES.length];
            spawnSparkBurst(hitBubble.x, hitBubble.y, pal[0], 12);

            try {
                SoundManager.getInstance(getContext()).playPop(random.nextInt(3) + 1);
            } catch (Exception ignored) {}
            return true;
        } else {
            // Ambient tap shockwave & twinkles
            shockwaves.add(new ShockwaveRing(tapX, tapY, 10 * density, 80 * density, 0.5f, Color.parseColor("#38BDF8")));
            spawnSparkBurst(tapX, tapY, Color.parseColor("#FDE047"), 6);
            return false;
        }
    }

    private void spawnSparkBurst(float x, float y, int baseColor, int count) {
        int[] colors = new int[]{
                baseColor,
                Color.WHITE,
                Color.parseColor("#FFD600"),
                Color.parseColor("#38BDF8"),
                Color.parseColor("#F472B6"),
                Color.parseColor("#4ADE80")
        };

        for (int i = 0; i < count; i++) {
            float angle = (float) (Math.PI * 2 * i / count + (random.nextFloat() - 0.5f) * 0.5f);
            float speed = (2.0f + random.nextFloat() * 4.5f) * density;
            bursts.add(new SparkBurstParticle(
                    x, y,
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed - 1.2f * density,
                    colors[i % colors.length],
                    (3f + random.nextFloat() * 5f) * density,
                    i % 3 == 0 ? 0 : (i % 2 == 0 ? 1 : 2),
                    random.nextFloat() * (float) Math.PI,
                    (random.nextFloat() - 0.5f) * 8.0f,
                    26 + random.nextInt(14)
            ));
        }
    }

    private void update(float dt) {
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) return;

        // Smooth cursor interpolation
        if (isCursorActive) {
            cursorX += (targetCursorX - cursorX) * 0.18f;
            cursorY += (targetCursorY - cursorY) * 0.18f;
        } else {
            cursorX = -9999f;
            cursorY = -9999f;
        }

        float repulsionRadius = 110f * density;
        float repulsionForce = 180f * density;

        // Update Floating Bubbles
        for (int i = 0; i < bubbles.size(); i++) {
            FloatingBubble b = bubbles.get(i);
            b.sizePx = b.sizeDp * density;

            float targetBaseX = b.relX * width;
            float targetBaseY = b.relY * height;

            float oscX = (float) Math.sin(time * b.speedX + b.phaseX) * b.ampX * density;
            float oscY = (float) Math.cos(time * b.speedY + b.phaseY) * b.ampY * density;

            // Cursor magnetic repulsion
            if (isCursorActive) {
                float currentApproxX = targetBaseX + oscX + b.pushX;
                float currentApproxY = targetBaseY + oscY + b.pushY;
                float dx = currentApproxX - cursorX;
                float dy = currentApproxY - cursorY;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist < repulsionRadius && dist > 1f) {
                    float factor = (1f - dist / repulsionRadius);
                    b.pushX += (dx / dist) * repulsionForce * factor * dt;
                    b.pushY += (dy / dist) * repulsionForce * factor * dt;
                }
            }

            // Spring return for push offset
            b.pushX *= 0.91f;
            b.pushY *= 0.91f;

            b.x = targetBaseX + oscX + b.pushX;
            b.y = targetBaseY + oscY + b.pushY;

            // Pop spring physics
            if (b.popScale > 1.0f || Math.abs(b.popVelocity) > 0.01f) {
                float springForce = (1.0f - b.popScale) * 18.0f;
                b.popVelocity += springForce * dt;
                b.popVelocity *= 0.86f;
                b.popScale += b.popVelocity;

                if (Math.abs(b.popScale - 1.0f) < 0.01f && Math.abs(b.popVelocity) < 0.01f) {
                    b.popScale = 1.0f;
                    b.popVelocity = 0;
                }
            }

            b.wobble *= 0.92f;
        }

        // Update Sprinkles
        for (int i = 0; i < sprinkles.size(); i++) {
            ConfettiSprinkle s = sprinkles.get(i);
            s.angleX += s.rotSpeedX * dt;
            s.angleZ += s.rotSpeedZ * dt;

            s.relY -= (s.speedYDp / (height / density)) * dt;
            if (s.relY < -0.05f) {
                s.relY = 1.05f;
                s.relX = random.nextFloat();
            }
        }

        // Update Stars
        for (int i = 0; i < stars.size(); i++) {
            ArcadeStar st = stars.get(i);
            st.rot += st.rotSpeed * dt;
            st.relY -= (st.driftY / 100f) * dt;
            if (st.relY < -0.05f) {
                st.relY = 1.05f;
                st.relX = random.nextFloat();
            }
        }

        // Update Spark Bursts
        for (int i = bursts.size() - 1; i >= 0; i--) {
            SparkBurstParticle p = bursts.get(i);
            p.life++;
            p.x += p.vx;
            p.y += p.vy;
            p.vy += 0.12f * density;
            p.vx *= 0.96f;
            p.vy *= 0.96f;
            p.rot += p.rotSpeed * dt;
            p.alpha = Math.max(0f, 1.0f - (float) p.life / p.maxLife);

            if (p.life >= p.maxLife) {
                bursts.remove(i);
            }
        }

        // Update Shockwaves
        for (int i = shockwaves.size() - 1; i >= 0; i--) {
            ShockwaveRing sw = shockwaves.get(i);
            sw.radius += (sw.maxRadius - sw.radius) * 0.14f;
            sw.alpha *= 0.91f;
            if (sw.alpha < 0.02f || sw.radius >= sw.maxRadius - 2f * density) {
                shockwaves.remove(i);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) return;

        // 1. Deep Celestial Background
        bgPaint.setColor(Color.parseColor("#12162E"));
        canvas.drawRect(0, 0, width, height, bgPaint);

        // 2. Ambient Atmosphere & Radial Glow
        renderAmbientAtmosphere(canvas, width, height);

        // 3. Twinkling Arcade Stars
        renderStars(canvas, width, height);

        // 4. Floating Confetti Sprinkles
        renderSprinkles(canvas, width, height);

        // 5. Floating 3D Bubbles (depth-sorted)
        for (int i = 0; i < bubbles.size(); i++) {
            renderFloatingBubble(canvas, bubbles.get(i));
        }

        // 6. Click Shockwave Rings
        for (int i = 0; i < shockwaves.size(); i++) {
            ShockwaveRing sw = shockwaves.get(i);
            shockwavePaint.setColor(sw.color);
            shockwavePaint.setAlpha((int) (sw.alpha * 255));
            canvas.drawCircle(sw.x, sw.y, sw.radius, shockwavePaint);
        }

        // 7. Click Particle Spark Bursts
        for (int i = 0; i < bursts.size(); i++) {
            renderBurstParticle(canvas, bursts.get(i));
        }
    }

    private void renderAmbientAtmosphere(Canvas canvas, float width, float height) {
        float haloX = width * 0.5f;
        float haloY = height * 0.40f;
        float haloRadius = Math.max(20.0f, Math.min(width, 420 * density) * 0.9f);

        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 1.4f);
        float haloAlpha = 0.18f + pulse * 0.08f;

        int auraColor = Color.parseColor("#38BDF8");
        int centerAura = Color.argb((int) (haloAlpha * 255), Color.red(auraColor), Color.green(auraColor), Color.blue(auraColor));
        int midAura = Color.argb((int) (haloAlpha * 0.35f * 255), Color.red(auraColor), Color.green(auraColor), Color.blue(auraColor));
        int edgeAura = Color.TRANSPARENT;

        RadialGradient auraGrad = new RadialGradient(haloX, haloY, haloRadius,
                new int[]{centerAura, midAura, edgeAura},
                new float[]{0.0f, 0.55f, 1.0f},
                Shader.TileMode.CLAMP);
        auraPaint.setShader(auraGrad);
        canvas.drawCircle(haloX, haloY, haloRadius, auraPaint);

        // Orbital Celestial Rings
        int ringColor = Color.argb(22, 255, 255, 255);
        ringPaint.setColor(ringColor);

        // Orbit ring 1 (clockwise)
        canvas.save();
        canvas.translate(haloX, haloY);
        canvas.rotate(time * 3.2f);
        ringPaint.setPathEffect(orbitDash1);
        canvas.drawCircle(0, 0, 150 * density, ringPaint);
        canvas.restore();

        // Orbit ring 2 (counter-clockwise)
        canvas.save();
        canvas.translate(haloX, haloY);
        canvas.rotate(-time * 2.2f);
        ringPaint.setPathEffect(orbitDash2);
        canvas.drawCircle(0, 0, 230 * density, ringPaint);
        canvas.restore();
    }

    private void renderStars(Canvas canvas, float width, float height) {
        for (int i = 0; i < stars.size(); i++) {
            ArcadeStar st = stars.get(i);
            float x = st.relX * width;
            float y = st.relY * height;

            float twinkle = 0.5f + 0.5f * (float) Math.sin(time * st.twinkleSpeed + st.twinklePhase);
            float alpha = st.baseAlpha * (0.4f + 0.6f * twinkle);

            canvas.save();
            canvas.translate(x, y);
            canvas.rotate((float) Math.toDegrees(st.rot));

            int starColor = (st.hueOffset > 0.5f ? Color.WHITE : Color.parseColor("#FFD54F"));
            starPaint.setColor(starColor);
            starPaint.setAlpha((int) (alpha * 255));

            float outerRadius = st.sizeDp * density;
            float innerRadius = outerRadius * 0.32f;
            drawStarPath(tempPath, 0, 0, st.points, outerRadius, innerRadius);
            canvas.drawPath(tempPath, starPaint);

            // Glowing center core
            starCorePaint.setAlpha((int) (alpha * 255));
            canvas.drawCircle(0, 0, outerRadius * 0.18f, starCorePaint);

            canvas.restore();
        }
    }

    private void renderSprinkles(Canvas canvas, float width, float height) {
        int[] palette = new int[]{
                Color.parseColor("#FF1744"), Color.parseColor("#00C853"),
                Color.parseColor("#0091EA"), Color.parseColor("#FFD600"),
                Color.parseColor("#AA00FF"), Color.parseColor("#00E5FF")
        };

        for (int i = 0; i < sprinkles.size(); i++) {
            ConfettiSprinkle s = sprinkles.get(i);
            float x = s.relX * width;
            float y = s.relY * height;

            canvas.save();
            canvas.translate(x, y);
            canvas.rotate((float) Math.toDegrees(s.angleZ));
            canvas.scale(1.0f, (float) Math.cos(s.angleX));

            sprinklePaint.setColor(palette[s.colorIndex % palette.length]);
            sprinklePaint.setAlpha((int) (s.alpha * 255));

            float w = s.wDp * density;
            float h = s.hDp * density;

            if (s.isPill) {
                tempRectF.set(-w / 2f, -h / 2f, w / 2f, h / 2f);
                canvas.drawRoundRect(tempRectF, w / 2f, w / 2f, sprinklePaint);
            } else {
                canvas.drawCircle(0, 0, w / 2f, sprinklePaint);
            }

            canvas.restore();
        }
    }

    private void renderFloatingBubble(Canvas canvas, FloatingBubble b) {
        canvas.save();

        float scale = b.popScale;
        float baseSize = b.sizePx > 0 ? b.sizePx : (b.sizeDp * density);
        float radius = Math.max(4.0f, (baseSize * scale) / 2.0f);
        float baseAlpha = b.depth == 2 ? 0.90f : (b.depth == 1 ? 0.70f : 0.45f);

        canvas.translate(b.x, b.y);

        int[] pal = BUBBLE_PALETTES[b.colorIndex % BUBBLE_PALETTES.length];
        int primary = pal[0];
        int dark = pal[2];

        // 1. Soft Drop Shadow
        int shadowColor = Color.argb((int) (baseAlpha * 120), 0, 0, 0);
        shadowPaint.setColor(shadowColor);
        float shadowOffset = (3f + b.depth * 3f) * density * scale;
        canvas.drawCircle(0, shadowOffset, radius, shadowPaint);

        // 2. Base Bubble Circle
        bubblePaint.setColor(primary);
        bubblePaint.setAlpha((int) (baseAlpha * 255));
        canvas.drawCircle(0, 0, radius, bubblePaint);

        // 3. 3D Spherical Bevel Gradient
        float lightOffset = radius * 0.30f;
        float gradRadius = Math.max(5.0f, radius * 1.3f);
        RadialGradient sphereGrad = new RadialGradient(-lightOffset, -lightOffset, gradRadius,
                new int[]{
                        Color.argb((int) (baseAlpha * 140), 255, 255, 255),
                        Color.argb(0, 0, 0, 0),
                        Color.argb((int) (baseAlpha * 220), Color.red(dark), Color.green(dark), Color.blue(dark))
                },
                new float[]{0.0f, 0.45f, 1.0f},
                Shader.TileMode.CLAMP);
        spherePaint.setShader(sphereGrad);
        canvas.drawCircle(0, 0, radius, spherePaint);

        // 4. Outer Rim Stroke
        strokePaint.setColor(dark);
        strokePaint.setAlpha((int) (baseAlpha * 180));
        strokePaint.setStrokeWidth(radius * 0.08f);
        canvas.drawCircle(0, 0, radius, strokePaint);

        // 5. Specular Top-Left Crescent Highlight
        canvas.save();
        canvas.translate(-radius * 0.35f, -radius * 0.35f);
        canvas.rotate(-45f);
        highlightPaint.setAlpha((int) (baseAlpha * 200));
        tempRectF.set(-radius * 0.26f, -radius * 0.13f, radius * 0.26f, radius * 0.13f);
        canvas.drawOval(tempRectF, highlightPaint);
        canvas.restore();

        // 6. Pinpoint Sparkle
        highlightPaint.setAlpha((int) (baseAlpha * 230));
        canvas.drawCircle(-radius * 0.22f, -radius * 0.52f, radius * 0.07f, highlightPaint);

        canvas.restore();
    }

    private void renderBurstParticle(Canvas canvas, SparkBurstParticle p) {
        canvas.save();
        canvas.translate(p.x, p.y);
        canvas.rotate((float) Math.toDegrees(p.rot));

        particlePaint.setColor(p.color);
        particlePaint.setAlpha((int) (p.alpha * 255));

        if (p.type == 0) {
            drawStarPath(tempPath, 0, 0, 4, p.size, p.size * 0.30f);
            canvas.drawPath(tempPath, particlePaint);
        } else if (p.type == 1) {
            tempRectF.set(-p.size / 2f, -p.size, p.size / 2f, p.size);
            canvas.drawRoundRect(tempRectF, p.size / 2f, p.size / 2f, particlePaint);
        } else {
            canvas.drawCircle(0, 0, p.size / 2f, particlePaint);
        }

        canvas.restore();
    }

    private void drawStarPath(Path path, float cx, float cy, int spikes, float outerRadius, float innerRadius) {
        path.reset();
        double rot = (Math.PI / 2.0) * 3.0;
        double step = Math.PI / spikes;

        float x = cx + (float) (Math.cos(rot) * outerRadius);
        float y = cy + (float) (Math.sin(rot) * outerRadius);
        path.moveTo(x, y);

        for (int i = 0; i < spikes; i++) {
            x = cx + (float) (Math.cos(rot) * outerRadius);
            y = cy + (float) (Math.sin(rot) * outerRadius);
            path.lineTo(x, y);
            rot += step;

            x = cx + (float) (Math.cos(rot) * innerRadius);
            y = cy + (float) (Math.sin(rot) * innerRadius);
            path.lineTo(x, y);
            rot += step;
        }
        path.close();
    }

    // --- Entity Data Classes ---

    private static class FloatingBubble {
        final int colorIndex;
        final float relX, relY;
        final float sizeDp;
        final int depth;
        final float speedX, speedY;
        final float ampX, ampY;
        final float phaseX, phaseY;

        float x, y;
        float sizePx;
        float pushX = 0, pushY = 0;
        float popScale = 1.0f;
        float popVelocity = 0;
        float wobble = 0;

        FloatingBubble(int colorIndex, float relX, float relY, float sizeDp, int depth, float rotSpeed, int seed) {
            this.colorIndex = colorIndex;
            this.relX = relX;
            this.relY = relY;
            this.sizeDp = sizeDp;
            this.depth = depth;

            Random r = new Random(seed * 7919L);
            this.speedX = 0.6f + r.nextFloat() * 0.8f;
            this.speedY = 0.5f + r.nextFloat() * 0.7f;
            this.ampX = 6f + r.nextFloat() * 10f;
            this.ampY = 8f + r.nextFloat() * 12f;
            this.phaseX = r.nextFloat() * (float) (Math.PI * 2);
            this.phaseY = r.nextFloat() * (float) (Math.PI * 2);
        }
    }

    private static class ArcadeStar {
        float relX, relY;
        final float sizeDp;
        final int points;
        final float twinkleSpeed;
        final float twinklePhase;
        float rot;
        final float rotSpeed;
        final float baseAlpha;
        final float driftY;
        final float hueOffset;

        ArcadeStar(float relX, float relY, float sizeDp, int points, float twinkleSpeed, float twinklePhase,
                   float rotSpeed, float rot, float baseAlpha, float driftY, float hueOffset) {
            this.relX = relX;
            this.relY = relY;
            this.sizeDp = sizeDp;
            this.points = points;
            this.twinkleSpeed = twinkleSpeed;
            this.twinklePhase = twinklePhase;
            this.rotSpeed = rotSpeed;
            this.rot = rot;
            this.baseAlpha = baseAlpha;
            this.driftY = driftY;
            this.hueOffset = hueOffset;
        }
    }

    private static class ConfettiSprinkle {
        float relX, relY;
        final boolean isPill;
        final float wDp, hDp;
        float angleX, angleZ;
        final float rotSpeedX, rotSpeedZ;
        final float speedYDp;
        final int colorIndex;
        final float alpha;

        ConfettiSprinkle(float relX, float relY, boolean isPill, float wDp, float hDp,
                         float angleX, float angleZ, float rotSpeedX, float rotSpeedZ,
                         float speedYDp, int colorIndex, float alpha) {
            this.relX = relX;
            this.relY = relY;
            this.isPill = isPill;
            this.wDp = wDp;
            this.hDp = hDp;
            this.angleX = angleX;
            this.angleZ = angleZ;
            this.rotSpeedX = rotSpeedX;
            this.rotSpeedZ = rotSpeedZ;
            this.speedYDp = speedYDp;
            this.colorIndex = colorIndex;
            this.alpha = alpha;
        }
    }

    private static class SparkBurstParticle {
        float x, y;
        float vx, vy;
        final int color;
        final float size;
        final int type; // 0=star, 1=pill, 2=circle
        float rot;
        final float rotSpeed;
        int life = 0;
        final int maxLife;
        float alpha = 1.0f;

        SparkBurstParticle(float x, float y, float vx, float vy, int color, float size,
                           int type, float rot, float rotSpeed, int maxLife) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.size = size;
            this.type = type;
            this.rot = rot;
            this.rotSpeed = rotSpeed;
            this.maxLife = maxLife;
        }
    }

    private static class ShockwaveRing {
        final float x, y;
        float radius;
        final float maxRadius;
        float alpha;
        final int color;

        ShockwaveRing(float x, float y, float radius, float maxRadius, float alpha, int color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.maxRadius = maxRadius;
            this.alpha = alpha;
            this.color = color;
        }
    }
}
