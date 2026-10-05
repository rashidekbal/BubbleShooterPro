package com.redcodersgroup.bubbleshooter.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.CycleInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.redcodersgroup.bubbleshooter.ui.dialogs.ClaimGiftDialog;
import com.redcodersgroup.bubbleshooter.ui.dialogs.NoticeDialog;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.redcodersgroup.bubbleshooter.R;
import com.redcodersgroup.bubbleshooter.data.PreferencesManager;
import com.redcodersgroup.bubbleshooter.data.ProgressRepository;
import com.redcodersgroup.bubbleshooter.data.WorldConfigManager;
import com.redcodersgroup.bubbleshooter.data.WorldConfigManager.GiftConfig;
import com.redcodersgroup.bubbleshooter.data.WorldConfigManager.LevelCoord;
import com.redcodersgroup.bubbleshooter.data.WorldConfigManager.WorldModel;
import com.redcodersgroup.bubbleshooter.databinding.ItemSagaLevelRowBinding;
import com.redcodersgroup.bubbleshooter.databinding.ItemWorldMapPageBinding;
import com.redcodersgroup.bubbleshooter.audio.SoundManager;
import com.redcodersgroup.bubbleshooter.profile.AvatarManager;

public class WorldMapPagerAdapter extends RecyclerView.Adapter<WorldMapPagerAdapter.WorldViewHolder> {

    public interface OnWorldInteractionListener {
        void onLevelSelected(int levelNumber);
        void onWorldGiftClaimed(int worldNumber, int giftIndex, int bonusDiamonds);
    }

    private final Context context;
    private final ProgressRepository repository;
    private final PreferencesManager prefs;
    private final SoundManager soundManager;
    private final WorldConfigManager worldConfigManager;
    private final OnWorldInteractionListener listener;

    public WorldMapPagerAdapter(Context context, OnWorldInteractionListener listener) {
        this.context = context;
        this.listener = listener;
        this.repository = ProgressRepository.getInstance(context);
        this.prefs = repository.getPreferences();
        this.soundManager = SoundManager.getInstance(context);
        this.worldConfigManager = WorldConfigManager.getInstance(context);
    }

    @NonNull
    @Override
    public WorldViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWorldMapPageBinding binding = ItemWorldMapPageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new WorldViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull WorldViewHolder holder, int position) {
        // Reversed: page 0 = top locked world, last page = World 1 (bottom)
        // So scrolling DOWN from locked worlds reaches World 1
        int worldIndex = toWorldIndex(position);
        WorldModel world = worldConfigManager.getWorldByIndex(worldIndex);
        holder.bind(world, worldIndex);
    }

    public int getHighestUnlockedWorldIndex() {
        int highestUnlockedLevel = prefs.getHighestUnlockedLevel();
        return worldConfigManager.getWorldIndexForLevel(highestUnlockedLevel);
    }

    public int getVisibleWorldCount() {
        int highestWorld = getHighestUnlockedWorldIndex();
        int maxVisible = highestWorld + 1 + 3; // Unlocked worlds + max 3 locked worlds ahead
        return Math.min(maxVisible, worldConfigManager.getTotalWorlds());
    }

    @Override
    public int getItemCount() {
        return getVisibleWorldCount();
    }

    /** Convert ViewPager position to world index (reversed) */
    public int toWorldIndex(int pagerPosition) {
        int visibleCount = getVisibleWorldCount();
        int worldIndex = visibleCount - 1 - pagerPosition;
        if (worldIndex < 0) return 0;
        if (worldIndex >= worldConfigManager.getTotalWorlds()) {
            return worldConfigManager.getTotalWorlds() - 1;
        }
        return worldIndex;
    }

    /** Convert world index to ViewPager position (reversed) */
    public int toPagerPosition(int worldIndex) {
        int visibleCount = getVisibleWorldCount();
        int pos = visibleCount - 1 - worldIndex;
        if (pos < 0) return 0;
        if (pos >= visibleCount) return visibleCount - 1;
        return pos;
    }

    public class WorldViewHolder extends RecyclerView.ViewHolder {
        final ItemWorldMapPageBinding binding;

        public WorldViewHolder(@NonNull ItemWorldMapPageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(WorldModel world, int worldIndex) {
            // 1. Set World Map Background from config
            int mapRes = context.getResources().getIdentifier(world.mapBackground, "drawable", context.getPackageName());
            if (mapRes == 0) {
                mapRes = R.drawable.bg_map_world_1;
            }
            binding.ivWorldBackground.setImageResource(mapRes);

            int highestUnlockedWorld = getHighestUnlockedWorldIndex();
            boolean isLockedWorld = worldIndex > highestUnlockedWorld;

            if (isLockedWorld) {
                // 2a. Darkened locked world overlay with floating gold lock
                binding.layoutWorldLockedOverlay.setVisibility(View.VISIBLE);
                binding.layoutNodesOverlay.removeAllViews();

                binding.tvLockedWorldTitle.setText("WORLD " + world.worldNumber);
                binding.tvLockedWorldSubtitle.setText(world.subtitle);
                binding.tvLockedUnlockRequirement.setText("UNLOCK AT LEVEL " + world.startLevel);

                // Add subtle floating animation to the padlock
                binding.ivLockedBigPadlock.clearAnimation();
                android.animation.ObjectAnimator bobAnim = android.animation.ObjectAnimator.ofFloat(
                        binding.ivLockedBigPadlock, "translationY", 0f, -8f, 0f
                );
                bobAnim.setDuration(2400);
                bobAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                bobAnim.setRepeatMode(android.animation.ValueAnimator.RESTART);
                bobAnim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
                bobAnim.start();

                binding.layoutWorldLockedOverlay.setOnClickListener(v -> {
                    soundManager.playClick();
                    NoticeDialog.showWarning(
                            context,
                            "LOCKED REALM",
                            "WORLD " + world.worldNumber + " LOCKED",
                            world.subtitle.toUpperCase(),
                            "Reach Level " + world.startLevel + " to unlock and explore this realm!"
                    );
                });
            } else {
                // 2b. Unlocked world: hide locked overlay and populate playable level nodes
                binding.layoutWorldLockedOverlay.setVisibility(View.GONE);
                binding.layoutWorldContent.post(() -> populateNodes(world));
            }
        }

        private void populateNodes(WorldModel world) {
            int mapWidth = binding.layoutWorldContent.getWidth();
            int mapHeight = binding.layoutWorldContent.getHeight();
            if (mapWidth <= 0 || mapHeight <= 0) return;

            binding.layoutNodesOverlay.removeAllViews();
            int highestUnlocked = prefs.getHighestUnlockedLevel();
            float density = context.getResources().getDisplayMetrics().density;

            int totalLevels = world.levels.size();

            // Add Level Nodes with perspective depth scaling (larger at base, gradually smaller towards top)
            for (int i = 0; i < totalLevels; i++) {
                LevelCoord coord = world.levels.get(i);
                final int level = coord.level;

                // Strict vertical perspective depth scaling:
                // Bottom of screen (portal, Y ~ 0.71) is LARGEST (54dp)
                // Moving upwards on screen towards sky castle (Y ~ 0.18), nodes shrink gradually to 24dp
                float minY = 0.1789f;
                float maxY = 0.7122f;
                float normY = (coord.y - minY) / (maxY - minY);
                if (normY < 0f) normY = 0f;
                if (normY > 1f) normY = 1f;

                float minNodeDp = 24f;
                float maxNodeDp = 44f;
                float nodeDp = minNodeDp + normY * (maxNodeDp - minNodeDp);
                int nodeSize = Math.round(nodeDp * density);

                float minSp = 7.5f;
                float maxSp = 12.5f;
                float textSp = minSp + normY * (maxSp - minSp);
                if (level >= 100) {
                    textSp *= 0.80f;
                } else if (level >= 10) {
                    textSp *= 0.90f;
                }

                int posX = (int) (mapWidth * coord.x - nodeSize / 2f);
                int posY = (int) (mapHeight * coord.y - nodeSize / 2f);

                ItemSagaLevelRowBinding nodeBinding = ItemSagaLevelRowBinding.inflate(
                        LayoutInflater.from(context), binding.layoutNodesOverlay, false
                );

                // Dynamically resize inner orb & text
                ViewGroup.LayoutParams orbLp = nodeBinding.layoutNodeOrb.getLayoutParams();
                if (orbLp != null) {
                    orbLp.width = nodeSize;
                    orbLp.height = nodeSize;
                    nodeBinding.layoutNodeOrb.setLayoutParams(orbLp);
                }

                // Center text inside the 3D dome (offset by ~8% of node size to account for 3D bottom bevel)
                ViewGroup.LayoutParams textLp = nodeBinding.tvNodeLevelNumber.getLayoutParams();
                if (textLp instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) textLp;
                    flp.bottomMargin = Math.round(nodeSize * 0.08f);
                    nodeBinding.tvNodeLevelNumber.setLayoutParams(flp);
                }

                nodeBinding.tvNodeLevelNumber.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp);
                nodeBinding.tvNodeLevelNumber.setText(String.valueOf(level));

                // Scale avatar pin and milestone stars proportionally (smaller upward)
                int avatarWidth = Math.round((26f + normY * 22f) * density); // 26dp at top, 48dp at bottom
                int avatarHeight = Math.round(avatarWidth * 1.21f); // 38x46 pin aspect ratio
                ViewGroup.LayoutParams avatarLp = nodeBinding.layoutPlayerAvatarPin.getLayoutParams();
                if (avatarLp instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) avatarLp;
                    flp.width = avatarWidth;
                    flp.height = avatarHeight;
                    flp.topMargin = -Math.round(avatarHeight * 0.90f);
                    nodeBinding.layoutPlayerAvatarPin.setLayoutParams(flp);
                }

                int iconSize = Math.round(avatarWidth * 0.78f);
                ViewGroup.LayoutParams iconLp = nodeBinding.ivPlayerAvatarPin.getLayoutParams();
                if (iconLp instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams iflp = (FrameLayout.LayoutParams) iconLp;
                    iflp.width = iconSize;
                    iflp.height = iconSize;
                    iflp.topMargin = Math.round(avatarWidth * 0.05f);
                    nodeBinding.ivPlayerAvatarPin.setLayoutParams(iflp);
                }

                // Show player's current selected avatar inside the pin
                String currentAvatar = prefs.getPlayerAvatar();
                nodeBinding.ivPlayerAvatarPin.setImageResource(AvatarManager.getAvatarDrawable(currentAvatar));

                ViewGroup.LayoutParams starsLp = nodeBinding.layoutNodeStars.getLayoutParams();
                if (starsLp instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) starsLp;
                    flp.topMargin = -Math.round((7f + normY * 7f) * density);
                    nodeBinding.layoutNodeStars.setLayoutParams(flp);
                }

                int stars = prefs.getStarsForLevel(level);
                boolean isCompleted = (level < highestUnlocked) || (stars > 0);
                boolean isCurrent = (level == highestUnlocked);
                boolean isLocked = (level > highestUnlocked);
                boolean isBoss = (level % 5 == 0);

                if (isCurrent) {
                    // Yellow for current open, Red if current open is boss mode
                    int orbBg = isBoss ? R.drawable.btn_level_red : R.drawable.btn_level_yellow;
                    nodeBinding.layoutNodeOrb.setBackgroundResource(orbBg);
                    nodeBinding.tvNodeLevelNumber.setTextColor(Color.WHITE);
                    nodeBinding.layoutPlayerAvatarPin.setVisibility(View.VISIBLE);
                    nodeBinding.layoutNodeStars.setVisibility(View.GONE);

                    // Ensure current active node & avatar pin are drawn ON TOP of all other level buttons
                    float elev = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, context.getResources().getDisplayMetrics());
                    nodeBinding.getRoot().setElevation(elev);
                    nodeBinding.getRoot().setTranslationZ(elev);
                    nodeBinding.layoutPlayerAvatarPin.setElevation(elev + 8f);
                    nodeBinding.layoutPlayerAvatarPin.setTranslationZ(elev + 8f);
                } else if (isCompleted) {
                    // Green for finished level
                    nodeBinding.layoutNodeOrb.setBackgroundResource(R.drawable.btn_level_green);
                    nodeBinding.tvNodeLevelNumber.setTextColor(Color.WHITE);
                    nodeBinding.layoutPlayerAvatarPin.setVisibility(View.GONE);
                    nodeBinding.layoutNodeStars.setVisibility(View.VISIBLE);
                    nodeBinding.getRoot().setElevation(0f);
                    nodeBinding.getRoot().setTranslationZ(0f);

                    nodeBinding.ivNodeStar1.setImageResource(stars >= 1 ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
                    nodeBinding.ivNodeStar2.setImageResource(stars >= 2 ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
                    nodeBinding.ivNodeStar3.setImageResource(stars >= 3 ? R.drawable.ic_star_filled : R.drawable.ic_star_empty);
                } else {
                    // Red for boss mode, Gray for locked normal level
                    int orbBg = isBoss ? R.drawable.btn_level_red : R.drawable.btn_level_locked;
                    nodeBinding.layoutNodeOrb.setBackgroundResource(orbBg);
                    nodeBinding.tvNodeLevelNumber.setTextColor(Color.WHITE);
                    nodeBinding.layoutPlayerAvatarPin.setVisibility(View.GONE);
                    nodeBinding.layoutNodeStars.setVisibility(View.GONE);
                    nodeBinding.getRoot().setElevation(0f);
                    nodeBinding.getRoot().setTranslationZ(0f);
                }

                nodeBinding.getRoot().setOnClickListener(v -> {
                    if (!isLocked) {
                        if (listener != null) listener.onLevelSelected(level);
                    } else {
                        soundManager.playClick();
                        String title = isBoss ? "BOSS LEVEL LOCKED" : "LOCKED";
                        String subtitle = isBoss ? "BOSS STAGE NOT ACCESSIBLE" : "STAGE NOT ACCESSIBLE";
                        NoticeDialog.showWarning(
                                context,
                                title,
                                subtitle,
                                "LEVEL " + level + " LOCKED",
                                "Complete Level " + (level - 1) + " to unlock this stage."
                        );
                    }
                });

                FrameLayout.LayoutParams nodeLp = new FrameLayout.LayoutParams(nodeSize, nodeSize);
                nodeLp.leftMargin = posX;
                nodeLp.topMargin = posY;
                binding.layoutNodesOverlay.addView(nodeBinding.getRoot(), nodeLp);
            }

            // Ensure current active level node with the avatar pin is brought to the front of layoutNodesOverlay
            for (int n = 0; n < binding.layoutNodesOverlay.getChildCount(); n++) {
                View child = binding.layoutNodesOverlay.getChildAt(n);
                if (child.getElevation() > 0) {
                    child.bringToFront();
                    break;
                }
            }

            // Add Path Gift Chests with perspective scaling
            int totalGifts = world.gifts.size();
            for (int g = 0; g < totalGifts; g++) {
                GiftConfig gift = world.gifts.get(g);
                final int giftIndex = gift.giftIndex;
                final int requiredLevel = world.startLevel - 1 + gift.requiredLevelOffset;

                float tGift = totalGifts > 1 ? (float) g / (totalGifts - 1) : 0f;
                float giftDp = 38f - tGift * (38f - 24f); // 38dp (Gift 1) down to 24dp (Gift 2 near castle)
                int giftSize = Math.round(giftDp * density);

                int gX = (int) (mapWidth * gift.x - giftSize / 2f);
                int gY = (int) (mapHeight * gift.y - giftSize / 2f);

                ImageView ivGift = new ImageView(context);
                ivGift.setImageResource(R.drawable.ic_star_chest_gold);
                ivGift.setBackgroundResource(R.drawable.bg_chest_floating_badge);
                int pad = Math.max(2, (int) (3 * density));
                ivGift.setPadding(pad, pad, pad, pad);
                ivGift.setElevation(context.getResources().getDimension(R.dimen.dp_8));

                boolean isClaimed = prefs.hasClaimedWorldGift(world.worldNumber, giftIndex);
                boolean isUnlocked = highestUnlocked > requiredLevel;

                if (isClaimed) {
                    ivGift.setAlpha(0.55f);
                } else if (isUnlocked) {
                    ivGift.setAlpha(1.0f);
                    ivGift.animate().scaleX(1.15f).scaleY(1.15f).setDuration(600)
                            .setInterpolator(new CycleInterpolator(1))
                            .start();
                } else {
                    ivGift.setAlpha(0.85f);
                }

                ivGift.setOnClickListener(v -> {
                    soundManager.playClick();
                    boolean currentlyClaimed = prefs.hasClaimedWorldGift(world.worldNumber, giftIndex);
                    if (!isUnlocked) {
                        NoticeDialog.showWarning(
                                context,
                                "LOCKED",
                                "CHEST LOCKED",
                                "MYSTERY GIFT",
                                "Complete Level " + requiredLevel + " to open this mystery chest."
                        );
                    } else if (currentlyClaimed) {
                        NoticeDialog.showInfo(
                                context,
                                "NOTICE",
                                "ALREADY CLAIMED",
                                "REWARD COLLECTED",
                                "You have already collected this reward. Keep progressing!"
                        );
                    } else {
                        int bonusDiamonds = (gift != null && gift.rewardDiamonds > 0) ? gift.rewardDiamonds : 2;
                        ClaimGiftDialog claimDialog = new ClaimGiftDialog(
                                context,
                                world.worldNumber,
                                giftIndex,
                                bonusDiamonds,
                                (wNum, gIdx, diamondsEarned) -> {
                                    ivGift.setAlpha(0.55f);
                                    ivGift.animate().cancel();
                                    ivGift.setScaleX(1.0f);
                                    ivGift.setScaleY(1.0f);
                                    if (listener != null) {
                                        listener.onWorldGiftClaimed(wNum, gIdx, diamondsEarned);
                                    }
                                }
                        );
                        claimDialog.show();
                    }
                });

                FrameLayout.LayoutParams giftLp = new FrameLayout.LayoutParams(giftSize, giftSize);
                giftLp.leftMargin = gX;
                giftLp.topMargin = gY;
                binding.layoutNodesOverlay.addView(ivGift, giftLp);
            }
        }
    }
}
