# The Combat System

**LeagueMechanics** replace vanilla Minecraft combat with a League of Legends style system. Every attack is recalculated from the attacker's **statistics** and the target's **resistances**, then applied to the target as a single custom damage instance. Vanilla melee damage is discarded, vanilla fall damage is cancelled, and attack frequency is driven by **attack speed** instead of the vanilla cooldown.

This document covers the two systems that make that work:

- **Player statistics**: where every stat comes from and how it is totalled.
- **Damage calculation**: how a hit is turned into damage, including resistances, penetration, critical strikes, life steal and blocking.

## Contents

- [1 Overview](#1-overview)
- [2 Player statistics](#2-player-statistics)
  - [2.1 Defensive](#21-defensive)
  - [2.2 Offensive](#22-offensive)
  - [2.3 Utility](#23-utility)
  - [2.4 Item stats](#24-item-stats)
  - [2.5 Classes](#25-classes)
  - [2.6 League level](#26-league-level)
  - [2.7 Rune shards](#27-rune-shards)
  - [2.8 Temporary modifiers](#28-temporary-modifiers)
- [3 Damage](#3-damage)
  - [3.1 Damage types](#31-damage-types)
  - [3.2 Damage calculation](#32-damage-calculation)
  - [3.3 Resistances and penetration](#33-resistances-and-penetration)
  - [3.4 Calculation modes](#34-calculation-modes)
  - [3.5 Adaptive force](#35-adaptive-force)
  - [3.6 Level bonus](#36-level-bonus)
  - [3.7 Critical strike](#37-critical-strike)
  - [3.8 Life steal and healing](#38-life-steal-and-healing)
  - [3.9 Blocking](#39-blocking)
  - [3.10 Monster damage](#310-monster-damage)
  - [3.11 Player versus player](#311-player-versus-player)
  - [3.12 Attack speed and attack cooldown](#312-attack-speed-and-attack-cooldown)
  - [3.13 Fall damage](#313-fall-damage)
- [4 Examples](#4-examples)
- [5 Debug mode](#5-debug-mode)
- [6 Implementation notes](#6-implementation-notes)
- [7 Source map](#7-source-map)
- [8 See also](#8-see-also)

## 1 Overview

Each hit follows the same pipeline:

```
Attack event (melee or projectile)
  -> PvP checks (both players opted in, level gap, self-hit)
  -> Attack cooldown check (attack speed)
  -> DamageManager.DamageCalculation(...)
       stats of source  +  resistances of target  ->  base damage
  -> Critical strike roll          (players only)
  -> Life steal                    (players only)
  -> Block mitigation              (if the target is blocking and the hit did not crit)
  -> target.damage(final)
  -> Item passives and runes: onDealDamage / onTakeDamage / onCrit
```

Two values are used throughout:

- **Damage balancer** `7.0`: total **AD** and total **AP** are divided by this value before they are used as damage. A player with a displayed 70 AD deals 10 damage before resistances.
- **League level**: a stored per-player level that is separate from the Minecraft experience level. See [League level](#26-league-level).

## 2 Player statistics

A **statistic** is a numeric attribute of a player that is calculated from several sources and read on demand. All player stats live in `PlayerStats`, one cached instance per player UUID.

Stat sources, in the order they are summed:

1. **Class base** stats (see [Classes](#25-classes)). A player with no class selected has `0` for every class stat.
2. **Vanilla attributes** (attack damage, armor, max health, movement speed, attack speed).
3. **Item** stats from custom items in the inventory (see [Item stats](#24-item-stats)).
4. **Rune shards** (see [Rune shards](#27-rune-shards)).
5. **Temporary modifiers** applied by item passives and keystones (see [Temporary modifiers](#28-temporary-modifiers)).
6. **Potion effects** and **enchantments** (Weakness, Resistance, Sharpness, Protection).

### 2.1 Defensive

- **Health** [**HP**]: bonus maximum health added on top of the vanilla 20 as an `ADD_NUMBER` modifier on `MAX_HEALTH`.

  ```
  Bonus HP = class base HP + item HP + shard HP + league level × 4 + Grasp hearts × 2
  ```

- **Health regeneration** [**HR**]: `item HR + shard HR`. Reduced by 40% while **Grievous Wounds** is active.
- **Armor** [**AR**]: reduces physical damage taken (see [Resistances and penetration](#33-resistances-and-penetration)).

  ```
  AR = (class base AR + vanilla armor attribute + item AR + temporary AR)
       × (1 + 0.05 × total Protection levels on worn armor)
       × (1 + 0.2 × (Resistance amplifier + 1))        only while Resistance is active
  ```

- **Magic resistance** [**MR**]: reduces magic damage taken.

  ```
  MR = (class base MR + item MR + temporary MR)
       × (1 + 0.05 × total Protection levels on worn armor)
       × (1 + 0.2 × (Resistance amplifier + 1))        only while Resistance is active
  ```

- **Tenacity** [**TN**]: `item TN + shard TN`. Reduces the duration of **slows** by that percentage (capped at 100%). No other debuff type is affected.
- **Heal power** [**HM**]: `1.0 × (1 + item HM / 100)`. Multiplies all life steal healing.

AR and MR cannot go below `0`.

### 2.2 Offensive

- **Attack damage** [**AD**]: the physical damage of a basic attack.

  ```
  AD = max(0, base + item AD + Sharpness + temporary AD) × Weakness multiplier ÷ 7

  base       = class base AD + vanilla attack damage attribute + 1 (if the main hand is a weapon)
  Sharpness  = 0.5 + 3.5 × Sharpness level                      (0 without Sharpness)
  Weakness   = 1 − 0.2 × (amplifier + 1), minimum 0              (only while Weakness is active)
  ```

  Weapons are swords, axes, pickaxes, shovels, hoes, tridents and maces.

- **Ability power** [**AP**]: the magic damage scaling stat.

  ```
  AP = max(0, class base AP + item AP + Dark Seal AP + temporary AP) × Weakness multiplier ÷ 7
  ```

- **Adaptive force** [**AF**]: classifies the build as physical or magic. It is not summed with anything.

  | Condition | AF |
    | --- | --- |
  | item AD greater than item AP | `0.6` |
  | item AP greater than item AD | `0.8` |
  | equal | `0` |

  `AF ≤ 0.7` is treated as AD-leaning, `AF > 0.7` as AP-leaning.

- **Armor penetration** [**APEN**] and **magic penetration** [**MPEN**]: item stats with a flat and a percent variant each (`APEN`, `APEN_PERCENT`, `MPEN`, `MPEN_PERCENT`). See [Resistances and penetration](#33-resistances-and-penetration).
- **Critical strike chance** [**CC**]: `item CC` as a percentage. See [Critical strike](#37-critical-strike).
- **Critical strike damage** [**CSD**]: `item CSD` as a percentage bonus to the critical multiplier.
- **Life steal** [**LS**]: `item LS` as a percentage of damage dealt.
- **True damage** [**TD**]: flat damage that ignores resistances.

  ```
  TD = item TD + level-based TD + temporary TD
  ```

  Level-based TD uses the **Minecraft experience level** of the player:

  | Minecraft level | Level-based TD |
    | --- | --- |
  | below 50 | 2 |
  | 50 or more | 4 |
  | 100 or more | 8 |
  | 200 or more | 12 |
  | 300 or more | 15 |

- **Attack speed** [**AS**]: attacks per second. See [Attack speed and attack cooldown](#312-attack-speed-and-attack-cooldown).

  ```
  AS = AS ratio × ((item AS + weapon AS + AS ratio + shard AS) / 100) + base AS
  ```

### 2.3 Utility

- **Movement speed** [**MS**]: item, shard and temporary movement speed are applied to the vanilla `MOVEMENT_SPEED` attribute.

  ```
  Bonus MS = item MS + shard MS + temporary MS
  Attribute modifier = 0.1 × (Bonus MS / 100)                    (ADD_NUMBER)
  ```

  The displayed MS value is `(0.1 + attribute) × 100 + Speed effect (20 per level) + item MS + shard MS + temporary MS`.

- **Cooldown haste** [**CH**]: `item CH + shard CH`.

### 2.4 Item stats

Custom items carry their stats in item data and are identified by a persistent `league_item_id`. `ItemStatsManager` totals them with these rules:

- Only the **first 6** league items found in the inventory (in inventory order) are counted.
- Totals are cached per player for **100 ms**.

Tracked stat keys:

| Key | Stat | Key | Stat |
| --- | --- | --- | --- |
| `HP` | Health | `LS` | Life steal |
| `HR` | Health regeneration | `CC` | Critical strike chance |
| `AD` | Attack damage | `CSD` | Critical strike damage |
| `AP` | Ability power | `MS` | Movement speed |
| `TD` | True damage | `CH` | Cooldown haste |
| `AS` | Attack speed | `TN` | Tenacity |
| `AR` | Armor | `HM` | Heal power |
| `MR` | Magic resistance | `SR` | Item stat key, no combat formula |
| `APEN` / `APEN_PERCENT` | Flat / percent armor penetration | `MPEN` / `MPEN_PERCENT` | Flat / percent magic penetration |

### 2.5 Classes

A player picks one of six classes. The class provides the base stats at league level 1. Each class has an **attack speed ratio** that multiplies bonus attack speed.

| Class | AD | AP | HP | AR | MR | Base AS | AS ratio |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Fighter | 12 | 4 | 0 | 10 | 15 | 0.60 | 0.60 |
| Support | 10 | 10 | 4 | 5 | 5 | 0.20 | 0.20 |
| Assassin | 7 | 4 | 0 | 4 | 20 | 0.75 | 0.75 |
| Mage | 1 | 18 | 0 | 5 | 7 | 0.55 | 0.55 |
| Tank | 1 | 2 | 10 | 25 | 15 | 0.15 | 0.15 |
| Marksman | 4 | 2 | 0 | 5 | 8 | 0.85 | 0.85 |

The class is stored in the database (`RunePersistence`) and cached in memory.

### 2.6 League level

The **league level** is stored per player and loaded when the player's stats are first created. It is independent of the Minecraft experience level.

It affects:

- **Health**: `+4 HP` per level.
- **Level bonus** for adaptive and non-item magic damage (see [Level bonus](#36-level-bonus)).
- **PvP**: players more than **3** levels apart cannot damage each other.

### 2.7 Rune shards

Each player has three shard rows. Defaults are `Adaptive Force`, `Movement Speed`, `Health Regen`.

| Row | Option 1 | Option 2 | Option 3 |
| --- | --- | --- | --- |
| 1 | Adaptive Force | Attack Speed (+10) | Cooldown Haste (+18) |
| 2 | Adaptive Force | Movement Speed (+15) | Health Regen (+2) |
| 3 | Health (+20) | Tenacity (+30) | Health Regen (+2) |

**Adaptive Force** shards give `15.4` per shard while the build is AD-leaning (`AF ≤ 0.7`) and `19.0` per shard while it is AP-leaning. They count as AD or AP respectively.

### 2.8 Temporary modifiers

Item passives and keystones adjust stats through additive temporary modifiers on `PlayerStats`: `modifyAD`, `modifyAP`, `modifyAR`, `modifyMR`, `modifyCritDamage`, plus the temporary TD, MS and AS fields. Each caller is responsible for removing its own amount when the effect ends.

Examples in the codebase: Recurve Bow (AD), Hexdrinker (MR), Guardian (AR), Glacial Augment (reduces the AD and AP of the targeted player), Warden's Mail (negative crit damage on the wearer).

## 3 Damage

### 3.1 Damage types

- **Physical damage** is reduced by the target's **AR**.
- **Magic damage** is reduced by the target's **MR**.
- **True damage** is not reduced by anything.

### 3.2 Damage calculation

`DamageManager.DamageCalculation(source, target, currentStacks, runesAdaptive, runesTrueDamage, procDamage)` returns the damage before crits and blocking.

```
Final damage = (Base damage + Proc damage) × Stacks

Stacks = currentStacks   if per-stack scaling is enabled
       = 1               otherwise
```

`Base damage` depends on the calculation mode (see [Calculation modes](#34-calculation-modes)). For a normal basic attack:

```
Source AD = AD + Doran's on-hit AD + shard AD
Base damage = Source AD × 100 / (100 + Effective AR)
```

Where:

- `AD` is the player's total AD after the damage balancer.
- `Doran's on-hit AD` is `+10` for each Doran's Ring or Doran's Shield in the inventory.
- `shard AD` is the Adaptive Force shard value when the build is AD-leaning, otherwise it is added to AP.
- `Effective AR` comes from [Resistances and penetration](#33-resistances-and-penetration).

When projectile damage is enabled and the source is a player, a bonus magic hit is added to the base:

```
Projectile bonus = (AP × 0.6) × 100 / (100 + Effective MR)
```

### 3.3 Resistances and penetration

**Armor** and **magic resistance** mitigate damage by a percentage:

```
Damage dealt = Raw damage × 100 / (100 + Effective resistance)
Damage reduction = Resistance / (100 + Resistance)
```

Penetration lowers the resistance used in that calculation. The target's actual resistance is unchanged.

```
Effective resistance = max(0, Resistance − Flat penetration) × (1 − Percent penetration / 100)
```

| Damage | Resistance | Flat penetration | Percent penetration |
| --- | --- | --- | --- |
| Physical | Target AR | `APEN` | `APEN_PERCENT` |
| Magic | Target MR | `MPEN` | `MPEN_PERCENT` |

Resistance by target:

| Target | AR / MR source |
| --- | --- |
| Player | `PlayerStats.getPlayerAR` / `getPlayerMR` |
| Supported mob | `MobStats.getMobAR` / `getMobMR` |
| Anything else | `0` |

Monsters do not have penetration.

### 3.4 Calculation modes

`DamageManager` is configured per call with flags. When more than one damage flag is set, the first matching row below is used.

| Priority | Flag | Base damage |
| --- | --- | --- |
| 1 | `enableOnlyAP` | Magic proc. Base is the proc damage reduced by MR. Proc damage is multiplied by the [level bonus](#36-level-bonus) unless `enableItemProc` is also set. |
| 2 | `enableTrueDamage` | `TD + (Source AD + AP) × runesTrueDamage / 100`. Not reduced by resistances. |
| 3 | `enableAdaptiveDamage` | `runesAdaptive × level bonus` (see [Adaptive force](#35-adaptive-force)), reduced by AR or MR depending on the build. |
| 4 | none | Physical basic attack from AD. |

Modifier flags:

| Flag | Effect |
| --- | --- |
| `enableAdaptiveScaling` | Adds the player's AF to the adaptive damage. |
| `enablePerStackScaling` | Multiplies the result by `currentStacks`. |
| `enableItemProc` | Skips the level bonus for magic procs. |
| `enableProjectileDamage` | Adds the projectile bonus magic damage. |

Callers in the codebase:

| Source | Mode |
| --- | --- |
| Basic attacks (`DamageListener`) | none |
| Hextech Alternator | only AP, item proc |
| Electrocute | adaptive |
| Conqueror | adaptive, adaptive scaling, per stack |
| First Strike | true damage |

### 3.5 Adaptive force

Adaptive damage chooses its type from the build:

```
Damage type = Magic    if item AP > item AD
            = Physical otherwise

Adaptive damage = runesAdaptive × level bonus
Adaptive damage with scaling = adaptive + (adaptive + AF)
```

Adaptive Force rune shards and AF-based AD/AP follow the same AD-leaning (`AF ≤ 0.7`) and AP-leaning (`AF > 0.7`) split described in [Offensive](#22-offensive).

### 3.6 Level bonus

Adaptive damage and non-item magic procs are multiplied by a bonus that depends on the **league level**:

| League level | Multiplier |
| --- | ---: |
| 1 - 2 | 1.07 |
| 3 - 7 | 1.15 |
| 8 - 12 | 1.37 |
| 13 - 17 | 1.75 |
| 18 or more | 1.90 |

### 3.7 Critical strike

A **critical strike** multiplies the damage of a player's basic attack when it succeeds. It uses **critical strike chance** to decide and **critical strike damage** to size the hit. Critical strikes only occur on player attacks (`DamageListener.damageEvent`), only when damage is above `0`, and not on rune or passive procs.

**Chance.** `CC` is read from items and rounded to one decimal.

- At `0` or less, an attack never crits and the streak resets.
- At `100` or more, an attack always crits.
- Otherwise `CritManager` adds a small streak-based bonus:

  ```
  Effective CC = min(100, CC + 0.5 × (5 − min(streak, 5)))
  ```

  `streak` is the number of consecutive crits. It resets to `0` on a miss. The bonus is therefore `+2.5%` right after a miss and shrinks by `0.5%` for each consecutive crit down to `0%` at a streak of 5.

**Damage.**

```
Critical multiplier = 1.75 × (1 + CSD / 100) + temporary crit damage
Critical damage = Damage × Critical multiplier
```

The base critical multiplier is `1.75` (175% damage). Temporary crit damage comes from effects such as Warden's Mail, which applies a negative amount.

**Average damage.** Over many attacks, with `C = CC / 100` and `M` the critical multiplier:

```
Average damage multiplier = 1 + C × (M − 1)
```

For example, 50% CC with no CSD gives `1 + 0.5 × 0.75 = 1.375`.

Critical strikes play a heavy mace smash sound at the attacker and trigger `onCrit` on every equipped rune. A crit is never reduced by [blocking](#39-blocking).

### 3.8 Life steal and healing

```
Healing = Damage × (Effective LS / 100) × Effective HM
```

- `Damage` is the damage after the critical multiplier and before block mitigation.
- **Grievous Wounds** multiplies life steal, heal power and health regeneration by `0.6`.
- Healing uses `player.heal`.

### 3.9 Blocking

A target is **blocking** when it is holding a shield or a sword and has its hand raised. Blocking:

- plays a block sound and iron block particles,
- damages the held sword and the shield by `max(1, ceil(sqrt(damage)))` durability,
- multiplies the hit by `1 − mitigation / 100`.

| Item | Mitigation |
| --- | ---: |
| Shield (offhand takes priority) | 50% |
| Wooden sword | 10% |
| Stone sword | 20% |
| Iron or golden sword | 28% |
| Diamond or netherite sword | 40% |

Critical strikes ignore block mitigation. Monster hits on blocking players always apply it.

A **parry** is a separate mechanic: right-clicking with a sword opens a short window that is closed after **4 ticks** if nothing hits and is released early when the player stops blocking (a miss starts a cooldown). Hits are checked against a **700 ms** limit. Any melee or projectile hit inside the window is cancelled. A parried melee hit also knocks the attacker back, applies Slowness and Weakness to them for 30 ticks, and grants the parrying player Strength for 50 ticks.

### 3.10 Monster damage

Monsters use the same pipeline through `DamageListener.entityDamageEvent`. Their stats come from `MobStats`, which reads the `mob-stats` section of `config.yml` and falls back to built-in defaults.

```yaml
mob-stats:
  zombie:
    ad: 37.0
    ap: 12.0
    ar: 150.0
    mr: 70.0
    type: UNDEAD
```

Monster damage is physical:

```
Monster AD = base AD × (1 − 0.2 × Weakness level) × (1 + 0.2 × Strength level)
Damage = Monster AD × 100 / (100 + target AR)
```

Monster stats are not divided by the damage balancer. Monsters have no crits, penetration or life steal. Each hit also damages the target's armor pieces by 1 durability.

### 3.11 Player versus player

A player hit is cancelled if any of these is true:

- the attacker and target are the same player,
- either player has not enabled PvP (both must have it on),
- the league levels differ by more than **3**.

Every valid hit is recorded in `CombatStateManager` (last attacker, hit times) and in `KillSourceTracker` for kill credit.

### 3.12 Attack speed and attack cooldown

Attack speed controls how often a player may attack. Melee swings, bow draws and projectile launches are cancelled while on cooldown.

```
Attack cooldown (ticks) = 20 / AS
```

When an attack is made, the cooldown is applied to every weapon in the hotbar and offhand, and an attack bar is shown as a title.

### 3.13 Fall damage

Vanilla fall damage is cancelled. Landing from a fall is handled by `DamageListener`:

```
Fall damage = (blocks fallen − 4) × 1.0              for falls of 4 or more blocks
```

Every full **10 blocks** fallen (tier `t`) also applies a slow:

```
Slow strength = min(100, 20 × 1.2^(t − 1))
Slow duration (ticks) = round(60 × 1.1^(t − 1))
```

Fall tracking is skipped in water, while climbing, gliding or flying, and in creative or spectator mode. Dashing can grant a one-time fall immunity.

## 4 Examples

### Physical basic attack

A Fighter holding an iron sword with 51 item AD attacks a Zombie (AR 150).

```
Base AD  = 12 (class) + 6 (iron sword attribute) + 1 (weapon bonus) = 19
Total AD = 19 + 51 = 70
AD       = 70 ÷ 7 = 10

Damage = 10 × 100 / (100 + 150) = 4.0
```

With 30 flat armor penetration and 20% armor penetration:

```
Effective AR = max(0, 150 − 30) × (1 − 0.20) = 96
Damage       = 10 × 100 / (100 + 96) = 5.10
```

If that hit critically strikes with no CSD:

```
Critical damage = 5.10 × 1.75 = 8.93
```

### Adaptive keystone damage

An AP-leaning player at league level 8 triggers a keystone with `runesAdaptive = 30` (illustrative value) against a target with 70 MR and no magic penetration.

```
Adaptive damage = 30 × 1.37 = 41.1
Damage          = 41.1 × 100 / (100 + 70) = 24.18
```

### Armor

A Tank with 40 item AR and 4 total Protection levels:

```
AR = (25 + 40) × (1 + 0.05 × 4) = 78
Damage reduction = 78 / (100 + 78) = 43.8%
```

While Resistance II is active the result is multiplied by `1 + 0.2 × 2 = 1.4`, giving `109.2` AR.

### Health

A Mage at league level 10 with 100 item HP and the Health shard:

```
Bonus HP = 0 (class) + 100 (items) + 20 (shard) + 10 × 4 (level) = 160
Max health = 20 + 160 = 180
```

### Attack speed

A Marksman with 40 item AS, a sword (attribute attack speed 1.6) and the Attack Speed shard:

```
Bonus multiplier = (40 + 1.6 + 0.85 + 10) / 100 = 0.5245
AS = 0.85 × 0.5245 + 0.85 = 1.30

Attack cooldown = 20 / 1.30 = 15.4 ticks
```

## 5 Debug mode

With `general-settings.debug: true` in `config.yml`, every player hit prints a debug block to the attacker:

```
Trigger Type: Melee | Projectile
[Source] Total AD = ... | Total AP = ...
[Source] Is Critical? = ... | Chance: ...% | Streak = ...
[Source] Physical Damage = ... | Magic Damage = ...
[Target] Magic Resist = ... | Armor = ...
[Target] Damage Received = ...
```

`Total AD` and `Total AP` are shown after multiplying back by the damage balancer.

## 6 Implementation notes

Behavior of the current code that differs from what the formulas above may suggest, or from the League of Legends wiki:

- **Penetration order.** Flat penetration is subtracted first and percent penetration is applied after. The wiki applies percent first.
- **Magic procs.** In the `enableOnlyAP` mode the proc damage is reduced by resistance to form the base, and the raw proc damage is then added again in the final sum.
- **Adaptive scaling.** `adaptive += adaptive + AF` evaluates to `2 × adaptive + AF`.
- **Projectile bonus.** `enableProjectileDamage()` is called after `DamageCalculation` inside `damageEvent`, so the 60% AP bonus is not part of player projectile hits. In `entityDamageEvent` it is enabled first but only applies to player sources.
- **Attack speed.** Lethal Tempo and Hail of Blades bonuses are read in `getPlayerAS` but not added to the result, and the temporary AS modifier is not read by it.
- **Health.** `getPlayerHP` (used by Aftershock) adds class, item, shard and level HP on top of the `MAX_HEALTH` attribute, which already contains the league modifier, and uses `8` per level instead of `4`.
- **Grievous Wounds on life steal.** Both the life steal percent and the heal power are multiplied by `0.6`, so life steal healing is multiplied by `0.36`.
- **Balancer order.** Doran's on-hit AD and Adaptive Force shard values are added after the division by the damage balancer.
- **Crit luck table.** `CritManager` precomputes a luck modifier table that the roll does not use. The streak counts successes, not failures.

## 7 Source map

| File | Responsibility |
| --- | --- |
| `entity/player/PlayerStats.java` | Stat totals, temporary modifiers, healing and Grievous Wounds helpers |
| `entity/player/PlayerClass.java`, `PlayerClassType.java` | Class base stats and class persistence |
| `entity/mob/MobStats.java` | Monster AD, AP, AR and MR from config |
| `manager/DamageManager.java` | Damage calculation, resistances, penetration, level bonus, crit multiplier |
| `manager/CritManager.java` | Critical strike roll and streaks |
| `manager/ItemStatsManager.java` | Item stat totals with the 6-item limit and 100 ms cache |
| `manager/StatScalingManager.java` | Generic AD/AP percentage scaling helper |
| `manager/DebuffManager.java` | Slow, Grievous Wounds and Inflame with tenacity |
| `manager/CombatStateManager.java` | Last hit and kill tracking, fall immunity |
| `listener/DamageListener.java` | Attack, projectile, parry, block, fall and attack cooldown handling |
| `listener/PlayerStatsListener.java` | Applies HP and MS attribute modifiers and syncs item stats |
| `rune/shards/ShardStats.java` | Rune shard stat values |
| `resources/config.yml` | Debug flag, icons, rune values, `mob-stats` |

## 8 See also

- [Champion statistic](https://wiki.leagueoflegends.com/en-us/Champion_statistic)
- [Critical strike](https://wiki.leagueoflegends.com/en-us/Critical_strike)
- [Damage](https://wiki.leagueoflegends.com/en-us/Damage)
