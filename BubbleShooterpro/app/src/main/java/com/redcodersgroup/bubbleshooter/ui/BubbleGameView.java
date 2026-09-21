package com.redcodersgroup.bubbleshooter.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import com.redcodersgroup.bubbleshooter.bubble.Bubble;
import com.redcodersgroup.bubbleshooter.game.GameEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BubbleGameView extends View {

    public enum BiomeTheme {
        MEADOWS("Bubble Meadows",
                new int[]{0xFF0E381E, 0xFF14532D, 0xFF166534},
                0xFF0F4724, 0xFF4ADE80, 0xFF22C55E, "pollen"),
        CRYSTALS("Crystal Caverns",
                new int[]{0xFF1A0933, 0xFF2E1065, 0xFF4C1D95},
                0xFF2A0F54, 0xFFC084FC, 0xFFA855F7, "crystal"),
        COSMOS("Celestial Cosmos",
                new int[]{0xFF0B0F2B, 0xFF1E1B4B, 0xFF312E81},
                0xFF171738, 0xFF818CF8, 0xFF6366F1, "stars"),
        VOLCANO("Volcanic Forge",
                new int[]{0xFF2B0A0A, 0xFF450A0A, 0xFF7F1D1D},
                0xFF3B0B0B, 0xFFFB923C, 0xFFEF4444, "embers"),
        CYBER("Neon Cyberland",
                new int[]{0xFF04202C, 0xFF0F172A, 0xFF2E1065},
                0xFF083344, 0xFF22D3EE, 0xFF06B6D4, "cyber"),
        ATLANTIS("Sunken Atlantis",
                new int[]{0xFF022B42, 0xFF075985, 0xFF0369A1},
                0xFF034164, 0xFF38BDF8, 0xFF0EA5E9, "bubbles"),
        JUNGLE("Enchanted Jungle",
                new int[]{0xFF022C1A, 0xFF064E3B, 0xFF14532D},
                0xFF064227, 0xFFA7F3D0, 0xFF10B981, "fireflies"),
        GLACIER("Frozen Glacier",
                new int[]{0xFF082F49, 0xFF0C4A6E, 0xFF0284C7},
                0xFF0A3B5C, 0xFFBAE6FD, 0xFF38BDF8, "snow"),
        DESERT("Desert Mirage",
                new int[]{0xFF3B1A04, 0xFF582B08, 0xFF78350F},
                0xFF4A2207, 0xFFFDE047, 0xFFF59E0B, "sand"),
        THUNDER("Thunder Peak",
                new int[]{0xFF1E1035, 0xFF3B0764, 0xFF4C1D95},
                0xFF2B1047, 0xFFFACC15, 0xFFA855F7, "lightning"),
        INFINITY("Infinity Realm",
                new int[]{0xFF180A2E, 0xFF2E1065, 0xFF831843},
                0xFF250E42, 0xFFF472B6, 0xFFEC4899, "astral");

        public final String title;
        public final int[] gradientColors;
        public final int ceilingColor;
        public final int railColor;
        public final int particleColor;
        public final String particleType;

        BiomeTheme(String title, int[] gradientColors, int ceilingColor, int railColor, int particleColor, String particleType) {
            this.title = title;
            this.gradientColors = gradientColors;
            this.ceilingColor = ceilingColor;
            this.railColor = railColor;
            this.particleColor = particleColor;
            this.particleType = particleType;
        }

        public static BiomeTheme forLevel(int level) {
            if (level <= 30) return MEADOWS;
            if (level <= 60) return CRYSTALS;
            if (level <= 90) return COSMOS;
            if (level <= 120) return VOLCANO;
            if (level <= 150) return CYBER;
            if (level <= 180) return ATLANTIS;
            if (level <= 210) return JUNGLE;
            if (level <= 240) return GLACIER;
            if (level <= 270) return DESERT;
            if (level <= 300) return THUNDER;
            return INFINITY;
        }
    }

    private static class AmbientParticle {
        float x, y;
        float vx, vy;
        float radius;
        float alpha;
        float alphaSpeed;
        float wobblePhase;
        float wobbleSpeed;
        int color;
    }

    private GameEngine gameEngine;
    private Paint paint;
    private Paint bgPaint;
    private Paint ceilingPaint;
    private Paint railPaint;
    private Paint vignettePaint;
    private Paint particlePaint;
    private Bitmap backgroundBitmap;
    private Rect bgSrcRect;
    private Rect bgDstRect;
    private Paint bgBitmapPaint;
    private long lastTimeNanos = 0;
    private LinearGradient backgroundGradient;
    private BiomeTheme currentBiome = BiomeTheme.MEADOWS;
    private int currentLevel = 1;
    private int viewWidth = 0;
    private int viewHeight = 0;
    private boolean isViewPaused = false;
    private final List<AmbientParticle> particles = new ArrayList<>();
    private final Random random = new Random();
    // Async background loading — prevents UI-thread bitmap decode blocking shot bursts
    private final ExecutorService bgLoadExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int lastLoadedEndlessWorld = -1; // tracks world number so we skip redundant loads

    public BubbleGameView(Context context) {
        super(context);
        init();
    }

    public BubbleGameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BubbleGameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        this.paint.setDither(true);

        this.bgBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        this.bgBitmapPaint.setDither(true);

        this.bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        this.bgPaint.setStyle(Paint.Style.FILL);

        this.ceilingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.ceilingPaint.setStyle(Paint.Style.FILL);

        this.railPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.railPaint.setStyle(Paint.Style.STROKE);

        this.vignettePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.vignettePaint.setStyle(Paint.Style.FILL);

        this.particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Bubble.initResources(getContext());
    }

    public void setBiomeLevel(int levelNumber) {
        this.currentLevel = levelNumber;
        this.currentBiome = BiomeTheme.forLevel(levelNumber);
        loadWorldBackground(levelNumber);
        updateBackgroundGradient();
        initParticles();
        invalidate();
    }

    /**
     * Sets the aesthetic biome and authentic campaign world background for Endless Mode.
     * Fixed to World 23 (Pinecone Peak / bg_game_world_23) so the background does not change during endless gameplay.
     */
    public void setEndlessBiome(int wave) {
        int fixedWorldNumber = 23;
        if (fixedWorldNumber == lastLoadedEndlessWorld && backgroundBitmap != null) return;
        lastLoadedEndlessWorld = fixedWorldNumber;

        int simulatedLevel = (fixedWorldNumber - 1) * 10 + 1; // Level 221
        this.currentLevel = simulatedLevel;
        this.currentBiome = BiomeTheme.forLevel(simulatedLevel);
        loadWorldBackground(simulatedLevel); // load immediately so frame 1 draws bg23 directly
        updateBackgroundGradient();
        initParticles();
        invalidate();
    }

    /** Synchronous load used by campaign mode (called before game starts, no race condition). */
    private void loadWorldBackground(int levelNumber) {
        int resId = resolveBackgroundResId(levelNumber);
        if (resId != 0) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                backgroundBitmap = BitmapFactory.decodeResource(getResources(), resId, options);
                updateBgRects();
            } catch (Throwable ignored) {
                backgroundBitmap = null;
            }
        } else {
            backgroundBitmap = null;
        }
    }

    /** Async load used by Endless Mode — decodes off the UI thread, applies on main thread. */
    private void loadWorldBackgroundAsync(int levelNumber) {
        final int resId = resolveBackgroundResId(levelNumber);
        if (resId == 0) {
            backgroundBitmap = null;
            return;
        }
        // Keep a local reference to resources/context before going off-thread
        final android.content.res.Resources res = getResources();
        bgLoadExecutor.execute(() -> {
            Bitmap loaded = null;
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                loaded = BitmapFactory.decodeResource(res, resId, options);
            } catch (Throwable ignored) {}
            final Bitmap result = loaded;
            mainHandler.post(() -> {
                backgroundBitmap = result;
                updateBgRects();
                invalidate();
            });
        });
    }

    private int resolveBackgroundResId(int levelNumber) {
        int resId = 0;
        try {
            com.redcodersgroup.bubbleshooter.data.WorldConfigManager.WorldModel world =
                    com.redcodersgroup.bubbleshooter.data.WorldConfigManager.getInstance(getContext()).getWorldForLevel(levelNumber);
            if (world != null && world.gameBackground != null) {
                resId = getResources().getIdentifier(world.gameBackground, "drawable", getContext().getPackageName());
            }
        } catch (Exception ignored) {}
        if (resId == 0) {
            int worldNumber = ((levelNumber - 1) / 10) + 1;
            resId = getResources().getIdentifier("bg_game_world_" + worldNumber, "drawable", getContext().getPackageName());
        }
        return resId;
    }

    private void updateBgRects() {
        if (backgroundBitmap != null && viewWidth > 0 && viewHeight > 0) {
            int bw = backgroundBitmap.getWidth();
            int bh = backgroundBitmap.getHeight();
            float scale = Math.max((float) viewWidth / bw, (float) viewHeight / bh);
            int scaledW = (int) (viewWidth / scale);
            int scaledH = (int) (viewHeight / scale);
            int left = Math.max(0, (bw - scaledW) / 2);
            int top = Math.max(0, (bh - scaledH) / 2);
            bgSrcRect = new Rect(left, top, Math.min(bw, left + scaledW), Math.min(bh, top + scaledH));
            bgDstRect = new Rect(0, 0, viewWidth, viewHeight);
        }
    }

    public BiomeTheme getCurrentBiome() {
        return currentBiome;
    }

    public void pause() {
        this.isViewPaused = true;
        this.lastTimeNanos = 0;
    }

    public void resume() {
        this.isViewPaused = false;
        this.lastTimeNanos = 0;
        postInvalidateOnAnimation();
    }

    public void setGameEngine(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
        if (viewWidth > 0 && viewHeight > 0) {
            this.gameEngine.setViewBounds(viewWidth, viewHeight);
        }
        invalidate();
    }

    public GameEngine getGameEngine() {
        return gameEngine;
    }

    private void updateBackgroundGradient() {
        if (viewWidth > 0 && viewHeight > 0 && currentBiome != null) {
            this.backgroundGradient = new LinearGradient(
                    0, 0, 0, viewHeight,
                    currentBiome.gradientColors,
                    new float[]{0.0f, 0.55f, 1.0f},
                    Shader.TileMode.CLAMP
            );
        }
    }

    private void initParticles() {
        particles.clear();
        if (viewWidth <= 0 || viewHeight <= 0) return;

        int particleCount = 22;
        for (int i = 0; i < particleCount; i++) {
            AmbientParticle p = new AmbientParticle();
            resetParticle(p, true);
            particles.add(p);
        }
    }

    private void resetParticle(AmbientParticle p, boolean randomY) {
        p.x = random.nextFloat() * (viewWidth > 0 ? viewWidth : 1080);
        p.y = randomY ? (random.nextFloat() * (viewHeight > 0 ? viewHeight : 1920)) : (viewHeight + 10f);
        p.alpha = 0.2f + random.nextFloat() * 0.6f;
        p.alphaSpeed = 0.5f + random.nextFloat() * 1.5f;
        p.wobblePhase = random.nextFloat() * 6.28f;
        p.wobbleSpeed = 1.0f + random.nextFloat() * 2.0f;
        p.color = currentBiome.particleColor;

        if ("embers".equals(currentBiome.particleType)) {
            p.radius = 2.5f + random.nextFloat() * 4.5f;
            p.vx = (random.nextFloat() - 0.5f) * 20f;
            p.vy = -35f - random.nextFloat() * 55f; // Fast rising embers
        } else if ("bubbles".equals(currentBiome.particleType)) {
            p.radius = 4.0f + random.nextFloat() * 8.0f;
            p.vx = (random.nextFloat() - 0.5f) * 15f;
            p.vy = -20f - random.nextFloat() * 35f; // Gentle rising aquatic bubbles
        } else if ("snow".equals(currentBiome.particleType)) {
            p.radius = 2.5f + random.nextFloat() * 4.0f;
            p.vx = (random.nextFloat() - 0.5f) * 25f;
            p.vy = 25f + random.nextFloat() * 40f; // Falling snowflakes
            if (!randomY) p.y = -10f;
        } else if ("fireflies".equals(currentBiome.particleType) || "pollen".equals(currentBiome.particleType)) {
            p.radius = 3.0f + random.nextFloat() * 5.0f;
            p.vx = (random.nextFloat() - 0.5f) * 15f;
            p.vy = -10f - random.nextFloat() * 20f;
        } else if ("cyber".equals(currentBiome.particleType) || "lightning".equals(currentBiome.particleType)) {
            p.radius = 2.0f + random.nextFloat() * 4.0f;
            p.vx = (random.nextFloat() - 0.5f) * 40f;
            p.vy = -20f - random.nextFloat() * 40f;
        } else {
            // Stars / Crystal glints / Astral stardust
            p.radius = 2.5f + random.nextFloat() * 5.0f;
            p.vx = (random.nextFloat() - 0.5f) * 10f;
            p.vy = -8f - random.nextFloat() * 15f;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.viewWidth = w;
        this.viewHeight = h;

        updateBgRects();
        updateBackgroundGradient();
        initParticles();

        if (gameEngine != null) {
            gameEngine.setViewBounds(w, h);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameEngine == null) return super.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                gameEngine.onTouchDown(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                gameEngine.onTouchMove(x, y);
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                gameEngine.onTouchUp(x, y);
                return true;
            case MotionEvent.ACTION_CANCEL:
                gameEngine.onTouchUp(x, y);
                return true;
        }

        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long now = System.nanoTime();
        if (lastTimeNanos == 0) {
            lastTimeNanos = now;
        }
        float dt = (now - lastTimeNanos) / 1_000_000_000.0f;
        lastTimeNanos = now;

        // Clamp delta time to avoid physics explosion if paused/backgrounded
        if (dt > 0.05f) dt = 0.05f;

        // 1. Draw Custom World Illustration Background if available, else Gradient
        if (backgroundBitmap != null && bgDstRect != null && bgSrcRect != null) {
            canvas.drawBitmap(backgroundBitmap, bgSrcRect, bgDstRect, bgBitmapPaint);
        } else if (backgroundGradient != null) {
            bgPaint.setShader(backgroundGradient);
            canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);
        } else {
            canvas.drawColor(currentBiome != null ? currentBiome.gradientColors[0] : 0xFF0E381E);
        }

        // 2. Draw Ambient Background Atmosphere Particles
        if (!isViewPaused) {
            updateAndDrawParticles(canvas, dt);
        } else {
            drawParticles(canvas);
        }

        // 3. Draw Biome Ceiling & Gold Accent Rail (Cleanly positioned under top HUD)
        float topY = (gameEngine != null) ? gameEngine.getBoardTop() : (92f * getResources().getDisplayMetrics().density);
        if (backgroundBitmap == null) {
            ceilingPaint.setColor(currentBiome != null ? currentBiome.ceilingColor : 0xFF0F4724);
            canvas.drawRect(0, 0, getWidth(), topY, ceilingPaint);
        } else {
            // Subtle top HUD vignette scrim for crystal clear readability over vibrant world artwork
            vignettePaint.setColor(Color.argb(95, 0, 0, 0));
            canvas.drawRect(0, 0, getWidth(), topY, vignettePaint);
        }

        // Ceiling accent rail
        railPaint.setColor(currentBiome != null ? currentBiome.railColor : 0xFF4ADE80);
        railPaint.setStrokeWidth(6f);
        canvas.drawLine(0, topY, getWidth(), topY, railPaint);

        // 3.5 Tablet & Wide Screen Boundaries (Elegant side rails and vignette framing)
        if (gameEngine != null && gameEngine.getBoardLeft() > 0) {
            float bLeft = gameEngine.getBoardLeft();
            float bRight = gameEngine.getBoardRight();

            // Side pillar subtle vignette shade
            vignettePaint.setColor(Color.argb(75, 0, 0, 0));
            canvas.drawRect(0, topY, bLeft, getHeight(), vignettePaint);
            canvas.drawRect(bRight, topY, getWidth(), getHeight(), vignettePaint);

            // Left and Right boundary accent rails
            railPaint.setColor(currentBiome != null ? currentBiome.railColor : 0xFF4ADE80);
            railPaint.setStrokeWidth(4.5f);
            canvas.drawLine(bLeft, topY, bLeft, getHeight(), railPaint);
            canvas.drawLine(bRight, topY, bRight, getHeight(), railPaint);
        }

        // 4. Update and Draw Game Engine
        if (gameEngine != null) {
            if (!isViewPaused) {
                gameEngine.update(dt);
            }
            paint.reset();
            paint.setAntiAlias(true);
            paint.setDither(true);

            // Clip drawing strictly to the playable area below the ceiling
            canvas.save();
            canvas.clipRect(0, topY, getWidth(), getHeight());
            gameEngine.draw(canvas, paint);
            canvas.restore();

            // Re-draw accent rails on top of clipped game area so top/side boundaries are crisp
            railPaint.setColor(currentBiome.railColor);
            railPaint.setStrokeWidth(6f);
            canvas.drawLine(0, topY, getWidth(), topY, railPaint);

            if (gameEngine.getBoardLeft() > 0) {
                float bLeft = gameEngine.getBoardLeft();
                float bRight = gameEngine.getBoardRight();
                railPaint.setStrokeWidth(4.5f);
                canvas.drawLine(bLeft, topY, bLeft, getHeight(), railPaint);
                canvas.drawLine(bRight, topY, bRight, getHeight(), railPaint);
            }
        }

        // 5. Continuously request redraw for 60 FPS smooth animations
        if (!isViewPaused) {
            postInvalidateOnAnimation();
        }
    }

    private void updateAndDrawParticles(Canvas canvas, float dt) {
        for (AmbientParticle p : particles) {
            p.wobblePhase += p.wobbleSpeed * dt;
            float wobbleX = (float) Math.sin(p.wobblePhase) * 12f * dt;
            p.x += (p.vx * dt) + wobbleX;
            p.y += p.vy * dt;

            // Pulse alpha
            p.alpha += (float) Math.sin(p.wobblePhase) * 0.15f * dt;
            if (p.alpha < 0.15f) p.alpha = 0.15f;
            if (p.alpha > 0.85f) p.alpha = 0.85f;

            // Out of bounds reset
            if (p.y < -20 || p.y > viewHeight + 30 || p.x < -20 || p.x > viewWidth + 20) {
                resetParticle(p, false);
            }

            drawSingleParticle(canvas, p);
        }
    }

    private void drawParticles(Canvas canvas) {
        for (AmbientParticle p : particles) {
            drawSingleParticle(canvas, p);
        }
    }

    private void drawSingleParticle(Canvas canvas, AmbientParticle p) {
        particlePaint.setColor(p.color);
        particlePaint.setAlpha((int) (p.alpha * 255));

        if ("bubbles".equals(currentBiome.particleType)) {
            particlePaint.setStyle(Paint.Style.STROKE);
            particlePaint.setStrokeWidth(1.8f);
            canvas.drawCircle(p.x, p.y, p.radius, particlePaint);

            // Bubble shine dot
            particlePaint.setStyle(Paint.Style.FILL);
            particlePaint.setColor(Color.WHITE);
            particlePaint.setAlpha((int) (p.alpha * 180));
            canvas.drawCircle(p.x - p.radius * 0.35f, p.y - p.radius * 0.35f, p.radius * 0.3f, particlePaint);
        } else {
            particlePaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(p.x, p.y, p.radius, particlePaint);
        }
    }
}
