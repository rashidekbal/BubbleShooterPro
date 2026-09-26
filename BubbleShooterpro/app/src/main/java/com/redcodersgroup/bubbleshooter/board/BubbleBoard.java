package com.redcodersgroup.bubbleshooter.board;

import com.redcodersgroup.bubbleshooter.bubble.Bubble;
import com.redcodersgroup.bubbleshooter.bubble.BubbleColor;
import com.redcodersgroup.bubbleshooter.bubble.BubbleType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class BubbleBoard {
    private final BubbleGrid grid;

    public BubbleBoard(BubbleGrid grid) {
        this.grid = grid;
    }

    public BubbleGrid getGrid() {
        return grid;
    }

    private List<GridPosition> getNeighbors(GridPosition pos) {
        return NeighborCalculator.getNeighbors(pos, grid.getRowParity());
    }

    /**
     * Previews what existing grid bubbles would be popped if a bubble of (color, type) landed at snapPos.
     * Does NOT mutate the grid permanently.
     */
    public List<GridPosition> previewMatches(GridPosition snapPos, BubbleColor color, BubbleType type) {
        if (snapPos == null || color == null) return new ArrayList<>();

        // If cell is already occupied, cannot snap here
        if (grid.getBubble(snapPos) != null) return new ArrayList<>();

        // Temporarily place simulated bubble
        Bubble tempBubble = new Bubble(color, type != null ? type : BubbleType.NORMAL, snapPos);
        grid.setBubble(snapPos, tempBubble);

        List<GridPosition> matches;
        try {
            matches = findMatches(snapPos);
        } finally {
            grid.removeBubble(snapPos);
        }

        // Return only existing board bubbles that would pop (excluding snapPos itself)
        List<GridPosition> existingPopped = new ArrayList<>();
        for (GridPosition pos : matches) {
            if (!pos.equals(snapPos)) {
                existingPopped.add(pos);
            }
        }
        return existingPopped;
    }

    /**
     * Finds matching connected bubbles of the same color starting at startPos.
     * Special handling for Fireball (incinerate blast), Lightning (row vaporize), Bomb (radius burst, direct hits, chain explosions),
     * Rainbow (wildcard), and Stone (unbreakable by normal/rainbow, crushed only by powers).
     */
    public List<GridPosition> findMatches(GridPosition startPos) {
        List<GridPosition> matchedPositions = new ArrayList<>();
        if (startPos == null) return matchedPositions;

        Bubble startBubble = grid.getBubble(startPos);
        if (startBubble == null) return matchedPositions;

        // 1. Check if startBubble is Fireball OR if projectile landed touching any Fireball on the grid
        Set<GridPosition> detonatingFireballs = new HashSet<>();
        if (startBubble.getType() == BubbleType.FIREBALL || startBubble.getColor() == BubbleColor.FIREBALL) {
            detonatingFireballs.add(startPos);
        }

        for (GridPosition n : getNeighbors(startPos)) {
            Bubble nb = grid.getBubble(n);
            if (nb != null && !nb.isPopping() && !nb.isFalling()) {
                if (nb.getType() == BubbleType.FIREBALL || nb.getColor() == BubbleColor.FIREBALL) {
                    detonatingFireballs.add(n);
                }
            }
        }

        if (!detonatingFireballs.isEmpty()) {
            Set<GridPosition> fireballArea = getFireballExplosionPositions(detonatingFireballs, startPos);
            matchedPositions.addAll(fireballArea);
            return matchedPositions;
        }

        // 2. Check if startBubble is Lightning OR if projectile landed touching any Lightning on the grid
        Set<GridPosition> detonatingLightning = new HashSet<>();
        if (startBubble.getType() == BubbleType.LIGHTNING || startBubble.getColor() == BubbleColor.LIGHTNING) {
            detonatingLightning.add(startPos);
        }

        for (GridPosition n : getNeighbors(startPos)) {
            Bubble nb = grid.getBubble(n);
            if (nb != null && !nb.isPopping() && !nb.isFalling()) {
                if (nb.getType() == BubbleType.LIGHTNING || nb.getColor() == BubbleColor.LIGHTNING) {
                    detonatingLightning.add(n);
                }
            }
        }

        if (!detonatingLightning.isEmpty()) {
            Set<GridPosition> lightningArea = getLightningExplosionPositions(detonatingLightning, startPos);
            matchedPositions.addAll(lightningArea);
            return matchedPositions;
        }

        // 3. Check if startBubble is a Bomb OR if projectile landed touching any Bomb on the grid
        Set<GridPosition> detonatingBombs = new HashSet<>();
        if (startBubble.getType() == BubbleType.BOMB || startBubble.getColor() == BubbleColor.BOMB) {
            detonatingBombs.add(startPos);
        }

        for (GridPosition n : getNeighbors(startPos)) {
            Bubble nb = grid.getBubble(n);
            if (nb != null && !nb.isPopping() && !nb.isFalling()) {
                if (nb.getType() == BubbleType.BOMB || nb.getColor() == BubbleColor.BOMB) {
                    detonatingBombs.add(n);
                }
            }
        }

        // If any bomb was struck or fired, trigger blast explosion!
        if (!detonatingBombs.isEmpty()) {
            Set<GridPosition> blastArea = getBombExplosionPositions(detonatingBombs);
            blastArea.add(startPos);
            matchedPositions.addAll(blastArea);
            return matchedPositions;
        }

        // 4. Stone Bubbles cannot be popped by normal shots or rainbow (but adjacent transparent burst)
        Set<GridPosition> directTransparent = new HashSet<>();
        for (GridPosition n : getNeighbors(startPos)) {
            Bubble nb = grid.getBubble(n);
            if (nb != null && !nb.isPopping() && !nb.isFalling()) {
                if (nb.getType() == BubbleType.TRANSPARENT || nb.getColor() == BubbleColor.TRANSPARENT) {
                    directTransparent.add(n);
                }
            }
        }
        if (startBubble.getType() == BubbleType.TRANSPARENT || startBubble.getColor() == BubbleColor.TRANSPARENT) {
            directTransparent.add(startPos);
        }
        Set<GridPosition> transparentToBurst = getTransparentBurstPositions(directTransparent);

        if (startBubble.getType() == BubbleType.STONE || startBubble.getColor() == BubbleColor.STONE) {
            return new ArrayList<>(transparentToBurst);
        }

        // 5. Rainbow Bubble: matches any valid neighbor color it touches (cannot match stone or transparent)
        BubbleColor targetColor = startBubble.getColor();
        if (startBubble.getType() == BubbleType.RAINBOW || targetColor == BubbleColor.RAINBOW) {
            // Find most prevalent neighbor color (excluding specials, stone, transparent)
            for (GridPosition n : getNeighbors(startPos)) {
                Bubble nb = grid.getBubble(n);
                if (nb != null && nb.getColor() != BubbleColor.RAINBOW && nb.getColor() != BubbleColor.NONE
                        && nb.getColor() != BubbleColor.BOMB && nb.getColor() != BubbleColor.LIGHTNING
                        && nb.getColor() != BubbleColor.FIREBALL && nb.getColor() != BubbleColor.STONE
                        && nb.getColor() != BubbleColor.TRANSPARENT
                        && nb.getType() != BubbleType.STONE && nb.getType() != BubbleType.TRANSPARENT) {
                    targetColor = nb.getColor();
                    break;
                }
            }
        }

        // 6. Normal BFS match 3 or more (Stone and Transparent handled separately)
        Set<GridPosition> visited = new HashSet<>();
        Queue<GridPosition> queue = new ArrayDeque<>();

        if (startBubble.getType() != BubbleType.TRANSPARENT && startBubble.getColor() != BubbleColor.TRANSPARENT) {
            visited.add(startPos);
            queue.add(startPos);
        }

        while (!queue.isEmpty()) {
            GridPosition current = queue.poll();
            matchedPositions.add(current);

            for (GridPosition neighbor : getNeighbors(current)) {
                if (visited.contains(neighbor)) continue;

                Bubble nb = grid.getBubble(neighbor);
                if (nb != null && !nb.isPopping() && !nb.isFalling()) {
                    // Stone and Transparent cannot be matched by normal colors
                    if (nb.getType() == BubbleType.STONE || nb.getColor() == BubbleColor.STONE
                            || nb.getType() == BubbleType.TRANSPARENT || nb.getColor() == BubbleColor.TRANSPARENT) {
                        continue;
                    }

                    boolean isMatch = (nb.getColor() == targetColor) ||
                            (nb.getType() == BubbleType.RAINBOW) ||
                            (nb.getColor() == BubbleColor.RAINBOW);

                    if (isMatch) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }

        // A valid match requires 3 or more bubbles
        if (matchedPositions.size() >= 3) {
            // Check if any matched bubble is adjacent to a Bomb, Lightning, Fireball, or Transparent bubble
            Set<GridPosition> adjacentBombs = new HashSet<>();
            Set<GridPosition> adjacentLightning = new HashSet<>();
            Set<GridPosition> adjacentFireballs = new HashSet<>();
            Set<GridPosition> adjacentTransparent = new HashSet<>(directTransparent);

            for (GridPosition mp : matchedPositions) {
                for (GridPosition n : getNeighbors(mp)) {
                    Bubble nb = grid.getBubble(n);
                    if (nb != null && !nb.isPopping() && !nb.isFalling()) {
                        if (nb.getType() == BubbleType.BOMB || nb.getColor() == BubbleColor.BOMB) {
                            adjacentBombs.add(n);
                        } else if (nb.getType() == BubbleType.LIGHTNING || nb.getColor() == BubbleColor.LIGHTNING) {
                            adjacentLightning.add(n);
                        } else if (nb.getType() == BubbleType.FIREBALL || nb.getColor() == BubbleColor.FIREBALL) {
                            adjacentFireballs.add(n);
                        } else if (nb.getType() == BubbleType.TRANSPARENT || nb.getColor() == BubbleColor.TRANSPARENT) {
                            adjacentTransparent.add(n);
                        }
                    }
                }
            }

            Set<GridPosition> total = new HashSet<>(matchedPositions);
            total.addAll(getTransparentBurstPositions(adjacentTransparent));

            if (!adjacentBombs.isEmpty()) {
                total.addAll(getBombExplosionPositions(adjacentBombs));
            }
            if (!adjacentLightning.isEmpty()) {
                total.addAll(getLightningExplosionPositions(adjacentLightning, null));
            }
            if (!adjacentFireballs.isEmpty()) {
                total.addAll(getFireballExplosionPositions(adjacentFireballs, null));
            }
            return new ArrayList<>(total);
        }

        matchedPositions.clear();
        if (!transparentToBurst.isEmpty()) {
            matchedPositions.addAll(transparentToBurst);
        }
        return matchedPositions;
    }

    /**
     * Recursively/iteratively finds all contiguous Transparent bubbles connected to the seed positions.
     */
    public Set<GridPosition> getTransparentBurstPositions(Set<GridPosition> initialTransparent) {
        Set<GridPosition> burstPositions = new HashSet<>();
        if (initialTransparent == null || initialTransparent.isEmpty()) return burstPositions;

        Queue<GridPosition> queue = new ArrayDeque<>(initialTransparent);
        Set<GridPosition> visited = new HashSet<>(initialTransparent);

        while (!queue.isEmpty()) {
            GridPosition pos = queue.poll();
            burstPositions.add(pos);

            for (GridPosition neighbor : getNeighbors(pos)) {
                if (visited.contains(neighbor)) continue;
                Bubble nb = grid.getBubble(neighbor);
                if (nb != null && !nb.isPopping() && !nb.isFalling()) {
                    if (nb.getType() == BubbleType.TRANSPARENT || nb.getColor() == BubbleColor.TRANSPARENT) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        return burstPositions;
    }

    /**
     * Calculates all bubbles destroyed by Fireball (incinerates everything in a 2-ring radius, including Stone bubbles).
     */
    public Set<GridPosition> getFireballExplosionPositions(Set<GridPosition> initialFireballs, GridPosition startPos) {
        Set<GridPosition> incineratedPositions = new HashSet<>();
        Queue<GridPosition> fireballQueue = new ArrayDeque<>(initialFireballs);
        Set<GridPosition> processedFireballs = new HashSet<>(initialFireballs);

        Set<GridPosition> bombsToDetonate = new HashSet<>();
        Set<GridPosition> lightningToTrigger = new HashSet<>();

        while (!fireballQueue.isEmpty()) {
            GridPosition fPos = fireballQueue.poll();
            incineratedPositions.add(fPos);

            // Radius: 2 neighbor rings around the fireball impact
            Set<GridPosition> localRadius = new HashSet<>();
            localRadius.add(fPos);
            for (GridPosition n1 : getNeighbors(fPos)) {
                localRadius.add(n1);
                for (GridPosition n2 : getNeighbors(n1)) {
                    localRadius.add(n2);
                }
            }

            for (GridPosition pos : localRadius) {
                Bubble b = grid.getBubble(pos);
                if (b != null && !b.isPopping() && !b.isFalling()) {
                    incineratedPositions.add(pos);
                    if (b.getType() == BubbleType.BOMB || b.getColor() == BubbleColor.BOMB) {
                        bombsToDetonate.add(pos);
                    } else if (b.getType() == BubbleType.LIGHTNING || b.getColor() == BubbleColor.LIGHTNING) {
                        lightningToTrigger.add(pos);
                    } else if ((b.getType() == BubbleType.FIREBALL || b.getColor() == BubbleColor.FIREBALL) && !processedFireballs.contains(pos)) {
                        processedFireballs.add(pos);
                        fireballQueue.add(pos);
                    }
                }
            }
        }

        if (!bombsToDetonate.isEmpty()) {
            incineratedPositions.addAll(getBombExplosionPositions(bombsToDetonate));
        }
        if (!lightningToTrigger.isEmpty()) {
            incineratedPositions.addAll(getLightningExplosionPositions(lightningToTrigger, null));
        }

        if (startPos != null) {
            incineratedPositions.add(startPos);
        }

        return incineratedPositions;
    }

    /**
     * Calculates all bubbles destroyed by lightning (entire target rows vaporized, plus chain explosions).
     * Mechanic:
     * - When hit on the line bottom: the row above that was hit vanishes.
     * - When in between two bubbles in a line: that hit line vanishes.
     */
    public Set<GridPosition> getLightningExplosionPositions(Set<GridPosition> initialLightning, GridPosition startPos) {
        Set<GridPosition> clearedPositions = new HashSet<>();
        Set<Integer> targetRows = new HashSet<>();
        Queue<GridPosition> lightningQueue = new ArrayDeque<>(initialLightning);
        Set<GridPosition> processedLightning = new HashSet<>(initialLightning);

        // Determine target row based on where projectile struck
        if (startPos != null) {
            Bubble startB = grid.getBubble(startPos);
            if (startB != null && (startB.getType() == BubbleType.LIGHTNING || startB.getColor() == BubbleColor.LIGHTNING)) {
                boolean hasSameRowNeighbor = false;
                boolean hasAboveNeighbor = false;

                for (GridPosition n : getNeighbors(startPos)) {
                    if (n.equals(startPos)) continue;
                    Bubble nb = grid.getBubble(n);
                    if (nb != null && !nb.isPopping() && !nb.isFalling()) {
                        if (n.getRow() == startPos.getRow()) {
                            hasSameRowNeighbor = true;
                        } else if (n.getRow() < startPos.getRow()) {
                            hasAboveNeighbor = true;
                            targetRows.add(n.getRow());
                        }
                    }
                }

                // If wedged between bubbles in same row, or ceiling hit
                if (hasSameRowNeighbor || (!hasAboveNeighbor && targetRows.isEmpty())) {
                    targetRows.add(startPos.getRow());
                }
            }
        }

        while (!lightningQueue.isEmpty()) {
            GridPosition lPos = lightningQueue.poll();
            clearedPositions.add(lPos);
            targetRows.add(lPos.getRow());
        }

        // Clear all bubbles in all target rows and trigger any nested bombs/lightning
        Set<GridPosition> bombsToDetonate = new HashSet<>();
        for (int r : targetRows) {
            if (r < 0 || r >= BubbleGrid.MAX_ROWS) continue;
            int cols = grid.getCols(r);
            for (int c = 0; c < cols; c++) {
                GridPosition pos = new GridPosition(r, c);
                Bubble b = grid.getBubble(pos);
                if (b != null && !b.isPopping() && !b.isFalling()) {
                    clearedPositions.add(pos);
                    if (b.getType() == BubbleType.BOMB || b.getColor() == BubbleColor.BOMB) {
                        bombsToDetonate.add(pos);
                    } else if ((b.getType() == BubbleType.LIGHTNING || b.getColor() == BubbleColor.LIGHTNING) && !processedLightning.contains(pos)) {
                        processedLightning.add(pos);
                        targetRows.add(pos.getRow());
                    }
                }
            }
        }

        if (!bombsToDetonate.isEmpty()) {
            clearedPositions.addAll(getBombExplosionPositions(bombsToDetonate));
        }

        if (startPos != null) {
            clearedPositions.add(startPos);
        }

        return clearedPositions;
    }

    /**
     * Recursively/iteratively calculates all bubbles destroyed by detonating bombs (including chain reactions)
     */
    public Set<GridPosition> getBombExplosionPositions(Set<GridPosition> initialBombs) {
        Set<GridPosition> explodedPositions = new HashSet<>();
        Queue<GridPosition> bombQueue = new ArrayDeque<>(initialBombs);
        Set<GridPosition> processedBombs = new HashSet<>(initialBombs);
        Set<GridPosition> fireballsToTrigger = new HashSet<>();
        Set<GridPosition> lightningToTrigger = new HashSet<>();

        while (!bombQueue.isEmpty()) {
            GridPosition bombPos = bombQueue.poll();
            explodedPositions.add(bombPos);

            // Radius: 2 neighbor rings around the bomb
            Set<GridPosition> localRadius = new HashSet<>();
            localRadius.add(bombPos);
            for (GridPosition n1 : getNeighbors(bombPos)) {
                localRadius.add(n1);
                for (GridPosition n2 : getNeighbors(n1)) {
                    localRadius.add(n2);
                }
            }

            for (GridPosition pos : localRadius) {
                Bubble b = grid.getBubble(pos);
                if (b != null && !b.isPopping() && !b.isFalling()) {
                    explodedPositions.add(pos);
                    // If another special is caught in blast, chain detonate it!
                    if ((b.getType() == BubbleType.BOMB || b.getColor() == BubbleColor.BOMB) && !processedBombs.contains(pos)) {
                        processedBombs.add(pos);
                        bombQueue.add(pos);
                    } else if (b.getType() == BubbleType.FIREBALL || b.getColor() == BubbleColor.FIREBALL) {
                        fireballsToTrigger.add(pos);
                    } else if (b.getType() == BubbleType.LIGHTNING || b.getColor() == BubbleColor.LIGHTNING) {
                        lightningToTrigger.add(pos);
                    }
                }
            }
        }

        if (!fireballsToTrigger.isEmpty()) {
            explodedPositions.addAll(getFireballExplosionPositions(fireballsToTrigger, null));
        }
        if (!lightningToTrigger.isEmpty()) {
            explodedPositions.addAll(getLightningExplosionPositions(lightningToTrigger, null));
        }

        return explodedPositions;
    }

    /**
     * Traverses from the ceiling (row 0) using BFS. Any bubbles not connected to the ceiling
     * are floating/unsupported and returned for drop animation and bonus scoring.
     */
    public List<Bubble> findFloatingBubbles() {
        Set<GridPosition> connectedToCeiling = new HashSet<>();
        Queue<GridPosition> queue = new ArrayDeque<>();

        // 1. Seed queue with all non-null bubbles touching the ceiling (row 0)
        int topCols = grid.getCols(0);
        for (int c = 0; c < topCols; c++) {
            GridPosition pos = new GridPosition(0, c);
            Bubble b = grid.getBubble(pos);
            if (b != null && !b.isPopping() && !b.isFalling()) {
                connectedToCeiling.add(pos);
                queue.add(pos);
            }
        }

        // 2. BFS to find all reachable bubbles
        while (!queue.isEmpty()) {
            GridPosition current = queue.poll();
            for (GridPosition neighbor : getNeighbors(current)) {
                if (connectedToCeiling.contains(neighbor)) continue;

                Bubble nb = grid.getBubble(neighbor);
                if (nb != null && !nb.isPopping() && !nb.isFalling()) {
                    connectedToCeiling.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        // 3. Identify and collect unvisited bubbles
        List<Bubble> floatingBubbles = new ArrayList<>();
        for (int r = 0; r < BubbleGrid.MAX_ROWS; r++) {
            int cols = grid.getCols(r);
            for (int c = 0; c < cols; c++) {
                GridPosition pos = new GridPosition(r, c);
                Bubble b = grid.getBubble(pos);
                if (b != null && !b.isPopping() && !b.isFalling()) {
                    if (!connectedToCeiling.contains(pos)) {
                        floatingBubbles.add(b);
                        grid.removeBubble(pos);
                    }
                }
            }
        }

        return floatingBubbles;
    }

    /**
     * Finds the closest empty grid cell neighboring an existing bubble or touching the ceiling.
     */
    public GridPosition findNearestSnapPosition(float projX, float projY) {
        float minDistanceSq = Float.MAX_VALUE;
        GridPosition bestPos = null;

        // Consider all valid empty positions
        for (int r = 0; r < BubbleGrid.MAX_ROWS; r++) {
            int cols = grid.getCols(r);
            for (int c = 0; c < cols; c++) {
                if (grid.isEmpty(r, c)) {
                    GridPosition candidate = new GridPosition(r, c);
                    // Candidate is valid if it touches the ceiling (row 0) OR touches at least one occupied neighbor
                    boolean hasNeighborOrCeiling = (r == 0);
                    if (!hasNeighborOrCeiling) {
                        for (GridPosition nb : getNeighbors(candidate)) {
                            Bubble b = grid.getBubble(nb);
                            if (b != null && !b.isPopping() && !b.isFalling()) {
                                hasNeighborOrCeiling = true;
                                break;
                            }
                        }
                    }

                    if (hasNeighborOrCeiling) {
                        float cx = grid.getCenterX(r, c);
                        float cy = grid.getCenterY(r);
                        float distSq = (projX - cx) * (projX - cx) + (projY - cy) * (projY - cy);
                        if (distSq < minDistanceSq) {
                            minDistanceSq = distSq;
                            bestPos = candidate;
                        }
                    }
                }
            }
        }

        return bestPos;
    }
}
