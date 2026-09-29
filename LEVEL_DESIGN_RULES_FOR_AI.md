# Bubble Shooter Pro — AI Level Design Rules

## 1. Purpose

These rules define the constraints and progression system for generating Bubble Shooter Pro levels.

The AI must generate levels that:

* Follow all row-placement constraints.
* Introduce mechanics gradually.
* Increase difficulty smoothly.
* Avoid unfair or sudden difficulty spikes.
* Make each world progressively harder than the previous world.
* Use boss levels as intentional difficulty peaks.
* Respect all one-level introduction exceptions.

---

# 2. Available Power-Ups

The available power-ups are:

1. **Fire**
2. **Rainbow**
3. **Lightning / Splash Lightning**
4. **Bomb**

The AI must introduce power-ups progressively rather than making all power-ups available immediately.

Power-up introduction order must follow the progression defined by the game design. Do not introduce multiple new power-ups too quickly unless explicitly specified.

---

# 3. Universal Row Rules

These rules apply to normal levels unless a specific one-level introduction exception is active.

## 3.1 Top Row

The top row must NEVER contain:

* Stone Bubble
* Power-up
* Clear Bubble

These restrictions are absolute for normal levels.

---

## 3.2 Bottom Row

The bottom row must normally NEVER contain:

* Power-up
* Stone Bubble
* Full-row Clear Bubble

The bottom row should remain free from these mechanics to prevent unfair difficulty.

### Important

There are specific one-level introduction exceptions defined later in this document.

Those exceptions override the normal bottom-row rules **only for the specified introduction level**.

After the introduction level, immediately return to the normal bottom-row rules.

---

## 3.3 Middle Rows

Middle rows have the following restrictions:

* Never create a full row of Stone Bubbles.
* Never create a full row of Power-ups.
* Each middle row may contain **at most 1 Power-up**.
* A Power-up is optional.
* Do NOT place a Power-up in every middle row.

Power-ups should be distributed naturally rather than appearing mechanically on every row.

---

# 4. Clear Bubble Rules

Clear Bubbles are a special difficulty mechanic.

## 4.1 Adjacent Clear Bubble Batches

Do NOT place Clear Bubble batches directly adjacent to each other.

Adjacent Clear Bubble batches can create excessive difficulty and should be avoided.

The AI should distribute Clear Bubble groups with sufficient separation.

## 4.2 Full-Row Clear Bubble

A complete bottom row of Clear Bubbles is prohibited during normal gameplay.

The only exception is the specific Splash Lightning introduction level described below.

## 4.3 Clear Bubble Availability

Clear Bubbles become part of the normal level-design pool after their introduction.

Once available, they may appear in later levels while still obeying all placement restrictions.

---

# 5. Stone Bubble Rules

Stone Bubbles are a difficulty mechanic.

Normal rules:

* No Stone Bubbles in the top row.
* No Stone Bubbles in the bottom row.
* Never create a full middle row of Stone Bubbles.

Stone Bubbles may be used in middle rows after they have been introduced.

They should be introduced gradually and their frequency/placement should increase carefully as world difficulty increases.

---

# 6. Bomb Introduction Exception

When the **Bomb** power-up is first introduced, there is a deliberate one-level exception.

### Bomb Introduction Level

On the exact level where Bomb is introduced:

* Stone Bubbles may be placed in the bottom row.
* This is intentional and exists specifically to demonstrate/integrate the new mechanic.

### Critical Restriction

This is a **ONE-TIME exception only**.

The AI must NEVER repeat this bottom-row Stone Bubble exception on later levels.

After the Bomb introduction level:

* Bottom row → no Stone Bubble.
* Normal bottom-row rules resume immediately.

---

# 7. Splash Lightning Introduction Exception

When **Splash Lightning** is first introduced, there is another deliberate one-level exception.

### Splash Lightning Introduction Level

On the exact level where Splash Lightning is introduced:

* The entire bottom row may be filled with Clear Bubbles.

This is intentional and exists specifically as part of the mechanic introduction.

### Critical Restriction

This is a **ONE-TIME exception only**.

The AI must NEVER generate a full bottom row of Clear Bubbles again after this introduction level.

After the introduction level:

* Bottom row → no full-row Clear Bubble.
* Normal bottom-row rules resume immediately.

---

# 8. Post-Introduction Availability

After the relevant mechanics have been introduced:

* Clear Bubbles may be used in subsequent levels.
* Stone Bubbles may be used in subsequent levels.
* Their usage should increase gradually with world difficulty.
* Normal row restrictions always remain active.
* Introduction exceptions must NOT become permanent rules.

The AI must distinguish between:

**"Mechanic has been introduced"**

and

**"Introduction exception is allowed."**

These are not the same thing.

A mechanic can remain available permanently while its special introduction exception is allowed only once.

---

# 9. Difficulty Progression

Difficulty must increase **gradually**.

The game should never feel like:

> Easy → Suddenly Very Hard

Instead, progression should feel like:

> Easy → Slightly Harder → Moderate → Challenging → Hard → Very Hard

The AI must prioritize player progression and fairness over simply maximizing difficulty.

---

# 10. World-Based Difficulty

Bubble Shooter Pro is structured around worlds.

Difficulty must progress at the **world level** as well as the individual level.

### Within a World

Early levels of a world should introduce or reinforce that world's mechanics.

Middle levels should gradually increase complexity.

Later levels should combine mechanics and provide greater challenge.

### Between Worlds

Each new world should generally be harder than the previous world.

However, the increase must be gradual.

Do not make the first level of a new world dramatically harder than the final levels of the previous world.

A new world can introduce a new mechanic, but the player should have time to understand and adapt to it.

---

# 11. Difficulty Should Come From Multiple Factors

The AI should not increase difficulty using only one variable.

Difficulty can gradually increase through combinations of:

* More complex bubble layouts.
* More constrained shooting paths.
* More Stone Bubble usage.
* More Clear Bubble usage.
* More strategic placement of obstacles.
* More complex combinations of mechanics.
* Increasingly demanding target arrangements.
* More careful power-up placement.
* Reduced opportunities for easy matches.
* More sophisticated bubble structures.

Do not increase every difficulty factor simultaneously.

Use controlled progression.

---

# 12. New Mechanic Introduction

When introducing a new mechanic:

1. Introduce it clearly.
2. Give the player a level where the mechanic can be understood.
3. Avoid combining it immediately with every other difficult mechanic.
4. Gradually increase its complexity in later levels.
5. Eventually combine it with previously introduced mechanics.

The first appearance of a mechanic should feel like a natural gameplay progression, not an unfair surprise.

---

# 13. Power-Up Distribution

Power-ups must be used strategically.

For middle rows:

* Maximum 1 power-up per row.
* Power-ups are optional.
* Do not place power-ups in every middle row.
* Never create a complete row of power-ups.

For the top row:

* No power-ups.

For the bottom row:

* No power-ups.

Power-ups should support interesting gameplay rather than simply increasing difficulty.

---

# 14. Boss Levels

Boss levels occur in a repeating pattern.

## Moderate Boss

Levels:

* 5
* 15
* 25
* 35
* 45
* 55
* etc.

These are **Moderate Boss Levels**.

They should provide a noticeable challenge increase while remaining reasonable for the current progression.

## Hard Boss

Levels:

* 10
* 20
* 30
* 40
* 50
* 60
* etc.

These are **Hard Boss Levels**.

They should be stronger difficulty peaks than the corresponding Moderate Boss levels.

### Boss Design Principle

Boss levels should feel like:

> "This is a challenge I have been building toward."

They should NOT feel like:

> "The game suddenly became unfair."

Boss difficulty must still respect the fundamental placement rules unless a specifically defined introduction exception applies.

---

# 15. Boss Progression

Boss difficulty should also increase over time.

For example:

* Early Moderate Boss → moderate challenge.
* Later Moderate Boss → more complex challenge.
* Early Hard Boss → hard but learnable.
* Later Hard Boss → increasingly complex combinations.

Do not make every boss equally difficult.

Boss difficulty should evolve with the world and overall game progression.

---

# 16. Fairness Rules

The AI must prioritize playable and understandable layouts.

Avoid:

* Sudden extreme difficulty spikes.
* Excessive obstacle stacking.
* Adjacent Clear Bubble batches.
* Full Stone Bubble rows.
* Full Power-up rows.
* Repeated bottom-row exceptions.
* Excessive power-up placement.
* Introducing several new mechanics at once without a learning level.

A level can be difficult without being unfair.

---

# 17. Exception Priority

When generating a level, use this priority:

### 1. Specific Introduction Exception

If this is the exact mechanic-introduction level, its explicitly defined exception may be used.

### 2. Universal Rules

Otherwise, all normal row restrictions apply.

### 3. Difficulty Progression

Within those constraints, adjust difficulty according to the world and level progression.

### 4. Boss Rules

If the level is a boss level, increase the challenge appropriately while maintaining fairness.

The AI must never assume that an introduction exception continues after its introduction level.

---

# 18. Level Generation Checklist

Before finalizing any generated level, the AI must verify:

### Row Validation

* [ ] Top row contains no Stone Bubble.
* [ ] Top row contains no Power-up.
* [ ] Top row contains no Clear Bubble.
* [ ] Bottom row contains no Power-up.
* [ ] Bottom row contains no Stone Bubble.
* [ ] Bottom row does not contain a full row of Clear Bubbles.
* [ ] Middle rows contain no full Stone Bubble row.
* [ ] Middle rows contain no full Power-up row.
* [ ] Each middle row contains at most 1 Power-up.

### Clear Bubble Validation

* [ ] No adjacent Clear Bubble batches.
* [ ] Full bottom-row Clear Bubbles only occur on the one specified Splash Lightning introduction level.

### Introduction Validation

* [ ] Bomb introduction bottom-row Stone exception occurs only once.
* [ ] Splash Lightning introduction bottom-row Clear exception occurs only once.
* [ ] Introduction exceptions are not repeated.

### Progression Validation

* [ ] Difficulty is appropriate for the level's world.
* [ ] Difficulty is appropriate relative to nearby levels.
* [ ] New mechanics are introduced gradually.
* [ ] Previously introduced mechanics are reused progressively.
* [ ] The level does not create an unexpected difficulty spike.

### Boss Validation

* [ ] Levels 5, 15, 25, 35, ... are Moderate Boss levels.
* [ ] Levels 10, 20, 30, 40, ... are Hard Boss levels.
* [ ] Boss difficulty is appropriate for the current world/progression.

---

# 19. Core AI Principle

The AI should optimize for:

**Gradual difficulty + Fairness + Variety + Learnability + Strategic layouts**

The objective is not to make every level harder than the previous level.

Instead, create a smooth difficulty curve where players naturally feel:

> "The game is getting harder, but I understand why."

New mechanics should be introduced, learned, reused, combined, and eventually mastered.

Difficulty should increase through **controlled complexity**, not arbitrary punishment.
