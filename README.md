# League of Legends Combat Mechanics

Every hit is recalculated from the attacker's stats and the target's resistances, then applied as one custom damage instance. Attack frequency is driven by **attack speed**. HP is counted in half-hearts, so **2 HP = 1 heart**.

### Preface

- **Damage calculation:** your stats against the target's resistances.
- **Critical strike roll:** players only.
- **Life steal:** players only.
- **Damage applied,** then item and rune effects trigger.
- **Melee:** Deals 100% physical damage.
- **Projectile:** Deals 100% physical damage & 80% magic damage.

**Dynamic Damage Balancer Divisor: 3.0**  
Total damage is divided by this value. Updated: October 4, 2026

---

## Stats

Sources: class base, weapons, items, rune shards, potions and enchantments.

### Defensive Stats

- **Health [HP]:** A player dies when their health is reduced to zero. Bonus health is added on top of the vanilla 20, plus 4 per league level.
- **Health regeneration [HP5]:** The amount of health a player passively restores per 5 seconds.
- **Armor [AR]:** Reduces (mitigates) the amount of physical damage taken, thus increasing your effective health against physical damage.
- **Magic resistance [MR]:** Reduces (mitigates) the amount of magic damage taken, thus increasing your effective health against magic damage.
- **Tenacity:** Reduces the duration of crowd control effects by a percentage (Slowed, Grounded, Stunned, Nearsighted & Silenced).

### Offensive Stats

- **Attack speed [AS]:** The number of basic attacks you are allowed to perform per second.
- **Attack damage [AD]:** One of the two main offensive statistics, along with ability power. Deals physical damage on basic attacks.
- **Ability power [AP]:** One of the two main offensive statistics, along with attack damage. Deals magic damage.
- **Critical strike chance [Crit%]:** Denotes the chance that a basic attack will critically strike.
- **Critical strike damage:** Increases the damage dealt when a basic attack critically strikes.
- **Armor penetration:** When applying physical damage to an enemy, ignores a part of their armor in the damage calculations. Can be either flat or percentage-based. The target's armor is unchanged.
- **Magic penetration:** When applying magic damage to an enemy, ignores a part of their magic resistance in the damage calculations. Can be either flat or percentage-based. The target's magic resistance is unchanged.
- **Life steal:** How much health you restore, as a percentage of the damage dealt by your basic attacks.

### Utility Stats

- **Cooldown haste [CH]:** Reduces the proc cooldown of runes / items.
- **Movement speed [MS]:** How quickly you move across the map.
- **Saturation regen:** The amount of hunger bars a player passively restores per 5 seconds.

### Vanilla Content Stats Modifiers

- **Sharpness Enchantment:** grants 10% of AD for every enchantment tier.
- **Protection Enchantment:** grants 10% of AR and 15% MR for every enchantment tier of all equipped armor.
- **Strength Effect:** increases AD by 20% for every attribute level.
- **Weakness Effect:** reduces AD and AP by 20% for every attribute level.
- **Swiftness Effect:** increases MS by 20% for every attribute level.
- **Slowness Effect:** reduces MS by 20% for every attribute level.
- **Weapons Attribute:** Vanilla Attack Damage and Attack Speed from weapons.

---

## Classes

| Class | AD | AP | HP | AR | MR | AS |
|-------|----|----|----|----|----|----|
| Fighter | 12 | 4 | 32 | 10 | 15 | 0.60 |
| Support | 9 | 7 | 24 | 5 | 5 | 0.20 |
| Assassin | 7 | 4 | 28 | 4 | 20 | 0.75 |
| Mage | 1 | 18 | 28 | 5 | 7 | 0.55 |
| Tank | 1 | 2 | 42 | 45 | 35 | 0.15 |
| Marksman | 4 | 2 | 24 | 5 | 8 | 0.85 |

---

## 3 Kinds of Damage

- **Physical:** is innately mitigated by armor, a statistic that units possess.
- **Magic:** is innately mitigated by magic resistance, a statistic that units possess.
- **True:** cannot be mitigated by any innate statistic.

True damage is affected by the League level of the player, which increases its flat value:

| League level | 1-2 | 3-7 | 8-12 | 13-17 | 18 |
|---|---|---|---|---|---|
| True Damage Growth | 0 | 1.7 | 2.2 | 2.7 | 3.50 |

---

## Adaptive Damage

Adaptive Damage is a type of damage that is triggered by Runes.

- Higher Bonus AD → Adaptive damage procs as physical damage.
- Higher Bonus AP → Adaptive damage procs as magic damage.

Adaptive damage is affected by the League level of the player, which increases the damage multiplier:

| League level | 1-2 | 3-7 | 8-12 | 13-17 | 18 |
|---|---|---|---|---|---|
| Adaptive Damage Growth | 1.07 | 1.15 | 1.37 | 1.75 | 1.90 |

### Example

A player is using a rune that triggers Adaptive Damage once it activates:

- If the player has more bonus AP than AD, the proc damage will be Magic damage.
- If the player has more bonus AD than AP, the proc damage will be Physical damage.

---

## Calculation Formulas

### Damage Calculation

**Given:**
```
baseAD = Source's class AD + Vanilla contents AD
baseAP = Source's class AP
baseAR = Target's class AR + Vanilla contents AR
baseMR = Target's class MR + Vanilla contents MR
rawAD = Source's base AD + Source's bonus AD
rawAP = Source's base AP + Source's bonus AP
rawAR = Target's base AR + Target's bonus AR
rawMR = Target's base MR + Target's bonus MR
effectiveAR = (rawAR − flatPen) * (1 − percentPen / 100)
```

**Equation:**
```
physicalDamage = rawAD * (100 / (100 + effectiveAR))
magicDamage = (rawAP * 0.8) * (100 / (100 + effectiveMR))

totalPhysicalDamage = (trueDamage + physicalDamage) / balancerDivisor
totalMagicDamage = (trueDamage + magicDamage) / balancerDivisor
```

### Attack Speed Calculation

**Given:**
```
baseAS = Source's class base AS + Vanilla contents AS
rawAS = Source's bonus AS
```

**Equation:**
```
effectiveAS = (baseAS + rawAS) / 100
totalAS = baseAS * effectiveAS + baseAS
attackSpeed (ticks) = 20 / totalAS
```

### Life Steal Calculation

**Given:**
```
damage = Damage dealt post-mitigation
rawLS = Source's item LS
rawHM = Source's item HM (Healing Multiplier Default = 1)
effectiveLS = 0.4 while Grievous Wounds is active on the Source, otherwise 1
```

**Equation:**
```
effectiveLS = rawLS * effectiveLS
effectiveHM = (1 + rawHM / 100) * effectiveLS
healthRestored = damage * (effectiveLS / 100) * effectiveHM
```

### Critical Strike

A critical strike is a damage event that deals 175% of its normal value by default, denoted as critical strike damage. It occurs semi-randomly on a player's attacks, based on their critical strike chance. Critical strike chance comes from items, and critical strike damage can be increased with bonus critical strike damage.

#### Critical Strike Algorithm

Critical strikes are not rolled with complete RNG. A small streak-based bonus is added to your critical strike chance to soften long dry spells right after a miss and to keep long critical strike chains in check. It follows these rules:

1. Each player tracks a **critical streak**: the number of critical strikes in a row. The streak resets to 0 on any non-critical hit.
2. The streak only counts up to 5. Past that, it behaves as if it were 5.
3. Every roll adds a **bonus chance** of `(5 - streak) * 0.5` percentage points. After a miss the bonus is +2.5%, it drops by 0.5% for each consecutive critical strike, and it is 0% at a streak of 5 or more.
4. The chance used for the roll is `min(100, critical strike chance + bonus chance)`.
5. A critical strike chance of 0% or lower never critically strikes, and 100% or higher always does. In both cases the bonus is skipped.

The streak is tracked per player, not per target. Over a large number of attacks, critical strike chance is essentially a damage multiplier:

```
Average damage multiplier = 1 + (Critical chance * (Critical multiplier - 1))
Critical multiplier = 1.75 * (1 + Bonus critical damage / 100)
```