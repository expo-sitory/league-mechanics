<div align="center">

## The Combat System

</div>

### Player Statistics
When referring to player units a statistic measures the magnitude of an unmodified basic attribute or capability. For example: how durable the unit is; how fast it is able to move; how quickly it is able to perform attacks; etc. An effect (e.g. how much damage an attack deals) that increases in strength by a statistic is said to "scale with" or "scale off of" that statistic. Scaling type can either directly or indirectly influence the player's capabilities, and often slightly correlates to the player's country.

Units are typically given a group of non-zero, fundamental base stats by the server rules, while the remaining stats always have a base value of zero. A subset of those non-zero base stats can naturally improve, or grow, by progressing.

    A base statistic is a statistic's unmodified value.
    A growth statistic is an increase to the base amount that is gained explicitly by progressing.

Increases and reductions to the statistics that are gained from any other sources such as runes, items, buffs and debuffs, are called bonuses. The exception is attack speed growth, which base value is depends on classes (Fighter, Support, Assasin, Mage, Tank & Marksman) but uniquely counts toward the bonus amount.

#### Types

There are only **14** current types of player statistics divided into 3 different categories: **Defensive**, **Offensive** and **Utility**. In the list below, only the basic attribute of each statistic is presented. Inside the server, **weapons**, **items**, and **runes**, may freely scale off of any and/or multiple statistics (and sometimes other effects), whether in their damage, defense, or any other attribute. 

- ##### Defensive
  - <a id="health-bullet"></a>Health **[HP]**: A player dies when their health is reduced to zero. Some items and effects may scale off of your own, on your ally's, or on a target enemy's: current, bonus, missing, or maximum health.
  - Health Regeneration **[HR]**: The amount of health the player passively restores per 5 seconds. (Vanilla health regen from foods are disabled)
  - <a id="armor-bullet"></a>Armor **[AR]**: Reduces (mitigates) the amount of physical damage taken.
  - <a id="mr-bullet"></a>Magic Resistance **[MR]**: Reduces (mitigates) the amount of magic damage taken.
  - Tenacity **[TN]**: Reduces the duration of movement speed debuffs by percentage
- ##### Offensive
  - Attack speed **[AS]**: The number of attacks the player is allowed to perform per second.
  - <a id="ad-bullet"></a>Attack damage **[AD]**: One of the two main offensive statistics, along with ability power. Unmodified basic attacks deal exactly this amount of damage.
  - <a id="ap-bullet"></a>Ability power **[AP]**: One of the two main offensive statistics, along with attack damage.
  - Critical strike chance **[Crit%]**: Denotes the chance that a basic attack will critically strike. (Yes vanilla jump crit attack deals no extra damage)
  - Armor Penetration **[APEN]**: When applying physical damage to a target, ignores a part of their **armor** in the damage calculations. This can be either flat (lethality) or percentage-based.
  - Magic Penetration **[MPEN]**: When applying magic damage to a target, ignores a part of their **magic resistance** in the damage calculations. This can be either flat for percentage-based.
  - Life Steal **[LS]**: How much health a player restores, as a percentage of post-mitigation damage dealth by attacks.
- ##### Utility
  - Saturation Regeneration **[SR]**: The amount of food saturation the player passively restores per 5 seconds. (Might use this to serve as mana/energy in the future)
  - Movement Speed **[MS]**: How quickly a player moves, measured in game-distance units per second.

---

### Damage

    Not to be confused with attack damage, a player statistic, nor conflated with health costs, which are not damage.


Damage is the result of a direct combat interaction between two units: a source unit causes a deduction of a target unit's current health via a attack (or accompanying debuff). Damage takes place in engine time ("instantaneously") and the application itself is formally called a damage instance or damage event. 

#### Types

The following three damage types exist: **Physical**, **Magic**, and **True**. Each damage event can fall under only one of these three categories.

These three types exist to add variability to gameplay beyond simple deduction of health equal to the source's outgoing damage value, by potentially reducing, or mitigating it instead.

  - **Physical damage** is innately mitigated by  armor, a statistic that units possess.
  - **Magic damage** is innately mitigated by  magic resistance, a statistic that units possess.
  - **True damage** cannot be mitigated by any innate statistic.

#### Adaptive Force

 Adaptive force is a stat that grants the player either  attack damage or  ability power, depending on the current amount of bonus attack damage and ability power of the champion, as follows:

     Higher Bonus AD →  Attack damage
     Higher AP →  Ability power

1 point of Adaptive Force provides **0.6** bonus AD or **0.8** AP.

If the bonus attack damage and the ability power of the unit are equal, the stat granted will always fallback to **AD**.

#### Adaptive Damage

A similar effect can be found on:

Adaptive Damage: This effect deals either physical or magic damage depending on whether you have more  bonus **attack damage** or  **ability power**.

    Greater  bonus attack damage → Physical damage
    Greater  bonus ability power → Magic damage

The player total adaptive damage scales with their current level across 5 tiers:

- Level 1-2: **1.07x**
- Level 3-7: **1.15x**
- Level 8-12: **1.3x**
- Level 13-17: **1.5x**
- Level 18: **1.7x**

#### Damage Calculation

A damage event is resolved in four steps: **base damage → resistance → multiplier → stacks**. Each damage type uses a different base-damage formula, but all three go through the same resistance and multiplier stages.

##### Base Damage

The base damage value depends on the source's outgoing damage and the type of the attack:

- **Stats Damage** (default): The attack's raw damage is the sum of two components, the attacker's total **attack damage** (AD) and the attacker's total **ability power** (AP), each bonus value from the items divided by `17`. Physical damage uses the AD component; magic damage uses the AP component scaled by a `0.6` default ratio. Both are summed.
- **True Damage**: The base damage is the attacker's total **true damage** (TD) plus `(AD + AP) × runesTrueDamage / 100`.
- **Adaptive Damage**: The base damage is `runesAdaptive × levelBasedBonus(player)`, optionally multiplied further by the attacker's adaptive force if adaptive scaling is enabled. The damage type (physical or magic) is chosen by comparing the attacker's total AD vs AP, higher AP means magic damage, higher AD means physical damage.

##### Resistance

Each damage component is individually mitigated by the target's resistance:

```
damage =  BaseDamage + (vanillaDamage / 2) + ((ItemAD + ItemAP)/ 17)
effectiveResist = max(0, resist − flatPen) × (1 − percentPen / 100)
finalDamage = damage / (1 + effectiveResist / 100)
```

- **Physical damage** is mitigated by the target's **armor** (AR), using flat and percent armor penetration.
- **Magic damage** is mitigated by the target's **magic resistance** (MR), using flat and percent magic penetration.
- **True damage** bypasses this stage entirely, it is never mitigated.

##### Multipliers

After resistance is applied, the damage is multiplied by:

- **Critical strike**: If the attack critically strikes, the damage is multiplied by `1.75 + bonusCritDamage` (see Critical Strike below).
- **Stacks**: If the effect is per-stack, the damage is multiplied by the current stack count.

##### Stacks

The final damage is multiplied by the number of stacks the effect has accumulated (`currentStacks`), or `1` if the effect is not per-stack.

---

### Critical Strike

Critical strike chance (**Crit%**) is a statistic that determines the probability a basic attack will critically strike. Unlike vanilla Minecraft, a critical strike here deals **extra damage**, the attack's damage is multiplied by `1.75` plus any bonus critical damage from runes or passives.

#### Base Crit Damage

The base critical damage multiplier is **`1.75`**. Bonus critical damage from runes and passives is added directly:

```
critDamageMultiplier = 1.75 + bonusCritDamage
```

#### Crit Chance

The player's effective critical strike chance is the sum of their item **critical strike chance** statistic plus any bonus from runes. It is capped at `100%` (a guaranteed crit) and floored at `0%`.

#### Luck Modifier

The server uses a luck-based critical strike system to prevent long streaks of non-critical attacks. For a given crit chance `C` (as a fraction between 0 and 1):

```
luckModifier = max(0, min(1, C × (1 − C) × 2))
```

This modifier is used to compute the average expected critical damage over many attacks, factoring in both the crit chance and the crit damage multiplier.

#### Failure Streaks

Each player tracks a **failure streak**, the number of consecutive non-critical attacks. The streak is capped at `5`. On each attack:

- If the attack critically strikes, the streak resets to `0`.
- If the attack does not critically strike, the streak increments by `1`.

The streak is used to compute a penalty that temporarily reduces the player's effective crit chance after consecutive misses, making critical strikes feel more consistent.

#### Average Crit Multiplier

The average critical damage multiplier over many attacks, accounting for both crit chance and bonus crit damage:

```
averageCritMultiplier = 1.0 + critChance × (1.0 + bonusCritDamage)
```

where `critChance` is the player's crit chance as a fraction (e.g. `0.3` for 30%).

---

### Player Runes

Runes are enhancements that add new abilities or buffs to the player. Players can choose their loadout of runes inside the server spawn.

#### Rune paths

Runes are divided into five paths:

  - **Precision** - Improved attacks and sustained damage
  - **Domination** - Burst damage and target access
  - **Sorcery** - Empowered abilities and resource manipulation
  - **Resolve** - Durability and crowd control
  - **Inspiration** - Creative tools and rule bending
  

  `For League mfs: All runes currently consist of one path and a keystone only. More content along the way.`

#### Tree

<div align="center">
<table>
  <tr>
    <th colspan="3" align="center">PRECISION</th>
  </tr>
  <tr>
    <td>Keystones</td>
    <td>Description</td>
    <td>Cooldown</td>
  </tr>
  <tr align="justify">
    <td align="center">Press The Attack</td>
    <td>ᴘᴀꜱꜱɪᴠᴇ: Melee attacks against entities apply a stack for 3 seconds, refreshing on subsequent applications. Stacking up to 3 times, the third stack consumes all stacks to deal 3.5 - 5.9 (base on level) bonus adaptive damage</td>
    <td align="center">6s</td>
  </tr>
    <tr align="justify">
    <td align="center">Lethal Tempo</td>
    <td>ᴘᴀꜱꜱɪᴠᴇ: Melee attacks against entities grant a stack for 6 seconds, refreshing on subsequent attacks and stacking up to 6 times. While stacking, attacks deal 2.7 - 4.5 (based on level) bonus adaptive damage upon arrival. At maximum stacks, basic attacks empower to fire a bolt gaining 60% bonus attack speed, lasting 3 seconds with a 30 seconds cooldown after expiring</td>
    <td align="center">30s</td>
  </tr>
    <tr align="justify">
    <td align="center">Fleet Footwork</td>
    <td>ᴘᴀꜱꜱɪᴠᴇ: Moving and projectile attacks generate Charges, up to 100. At 100 Charges, your next projectile attack consumes all stacks and heals you for 25% (+ 10% from total AD & 5% for AP) of missing health plus gain +20% movement speed lasting for 5 seconds which refreshes on your next projectile attack.</td>
    <td align="center">N/A</td>
  </tr>
    </tr>
    <tr align="justify">
    <td align="center">Conqueror</td>
    <td>ᴘᴀꜱꜱɪᴠᴇ: Dealing damage to entities generates stacks of Conqueror, lasting 5 seconds and refreshing on subsequent damage. Stacking up to 12 times, each stack grants 1.7 bonus adaptive damage, stacking up to 20 bonus adaptive damage at maximum stacks.</td>
    <td align="center">N/A</td>
  </tr>
</table>

<table>
  <tr>
    <th colspan="3" align="center">DOMINATION</th>
  </tr>
  <tr>
    <td>Keystones</td>
    <td>Description</td>
    <td>Cooldown</td>
  </tr>
  <tr align="justify">
    <td align="center">Electrocute</td>
    <td>ᴘᴀꜱꜱɪᴠᴇ: Damaging melee or projectile attacks apply stacks against entities, up to one per cast instance per entity. Applying 3 stacks to a target within 3 second period causes them to be struck by lightning, dealing them 5.5 - 9.3 (based on level) (+ 5% from total AD and 10% for AP) adaptive damage.</td>
    <td align="center">25s</td>
  </tr>
    <tr align="justify">
    <td align="center">Dark Harvest</td>
    <td>ᴘᴀꜱꜱɪᴠᴇ:  Reap the souls of fallen enemies. Dealing either melee or projectile damage to entities below of their 50% of maximum health deals 0.4 - 1.0 (based on level) bonus adaptive damage (+ 0.4 per soul, up to 20 stacks) and after 0.75 seconds delay, reap 1 soul. This cannot occur again for 1 minute.</td>
    <td align="center">60s</td>
  </tr>
    <tr align="justify">
    <td align="center">Hail of Blades</td>
    <td>ᴘᴀꜱꜱɪᴠᴇ: Striking with a melee attack against entities triggers Hail of Blades, and if the windup completes you gain 4 stacks of the effect for 3 seconds, with the duration refreshes on each melee attacks until all stacks are consumed, expiring one by one after not attacking for 3 seconds. While Hail of blades are active, you gain 10% bonus attack speed and 7% true damage (+ 8% from total AD and 6% for AP.</td>
    <td align="center">60s</td>
  </tr>
</table>

<table>
  <tr>
    <th colspan="3" align="center">SORCERY</th>
  </tr>
  <tr>
    <td>Keystones</td>
    <td>Description</td>
    <td>Cooldown</td>
  </tr>
  <tr align="justify">
    <td align="center">Arcane Comet</td>
    <td>ᴘᴀꜱꜱɪᴠᴇ: Striking with a projectile hurls an Arcane Comet at your target's location that lands after 0.825 seconds, dealing 4.5 - 7.6 (based on level) adaptive damage (+ 5% from total AD and 15% for AP). The comet's impact is amplified 2x while falling, making distance and positioning crucial.</td>
    <td align="center">20s</td>
  </tr>
    <tr align="justify">
    <td align="center">Deathfire Torch</td>
    <td>ᴘᴀꜱꜱɪᴠᴇ:  Ignite your enemies with searing flames. Weapon attacks with Fire Aspect or Flame enchantments engulfs a target in fire for 5 seconds, ticking every 10 ticks for a total of 11 damage instances. Each tick deals magic damage from +5%  of total AD and 10% for AP, scaling massively with magical power. Multiple targets can burn simultaneously, letting you engulf entire groups in flames.</td>
    <td align="center">N/A</td>
  </tr>
    <tr align="justify">
    <td align="center">Storm Raider's Surge</td>
    <td>ᴘᴀꜱꜱɪᴠᴇ: Dealing damage to a entity equal to 25% of their maximum health within 3 seconds removes all active debuff effects and grants you 20% bonus movement speed.</td>
    <td align="center">20s</td>
  </tr>
</table>

<table>
  <tr>
    <th colspan="3" align="center">RESOLVE</th>
  </tr>
  <tr>
    <td>Keystones</td>
    <td>Description</td>
    <td>Cooldown</td>
  </tr>
  <tr align="justify">
    <td align="center">Grasp of the Undying</td>
    <td>ᴘᴀꜱꜱɪᴠᴇ: Entering combat generates stacks for 5 seconds, stacking up to 4 times. At maximum stacks, your next melee attack deals bonus damage multiplied by your current absorption hearts (20% per heart), heals you for 15% of missing health, and permanently grants 1 absorption heart.</td>
    <td align="center">60s</td>
  </tr>
    <tr align="justify">
    <td align="center">Aftershock</td>
    <td>ᴘᴀꜱꜱɪᴠᴇ:  Being down to 30% of your max health grants you a static 45 (+ 75% bonus armor) bonus armor and 45 (+ 75% bonus magic resistance) bonus magic resistance for 2.5 seconds. After the duration, you release a shockwave that deals 3.5 – 7 (based on level) (+ 8% of your bonus health) magic damage to entities within 5 blocks radius.</td>
    <td align="center">20s</td>
  </tr>
    <tr align="justify">
    <td align="center">Guardian</td>
    <td>ᴘᴀꜱꜱɪᴠᴇ: Being near with players (5 max) within 10 blocks radius without you getting damaged for 10 seconds grants all of you 80% absorption with a duration of 50 seconds</td>
    <td align="center">60s</td>
  </tr>
</table>

<table>
  <tr>
    <th colspan="3" align="center">INSPIRATION</th>
  </tr>
  <tr>
    <td>Keystones</td>
    <td>Description</td>
    <td>Cooldown</td>
  </tr>
  <tr align="justify">
    <td align="center">First Strike</td>
    <td>ᴘᴀꜱꜱɪᴠᴇ: Strike first, strike hard. Your first attack after 0.25 seconds of not attacking grants 10 XP and activates First Strike for 3 seconds. During this window, deal 7% true damage bonus on every attack while tracking total bonus damage dealt. When First Strike expires, gain bonus XP equal to your total tracked damage multiplied by (+ 20% from total AD and 15% for AP).</td>
    <td align="center">60s</td>
  </tr>
  <tr align="justify">
    <td align="center">Glacial Augment</td>
    <td>ᴘᴀꜱꜱɪᴠᴇ: Freeze your enemies in place. Striking with a projectile will cause powdered snow to encase and slow them by 20% (+ 7% from total AD and 6% for AP) and have their damage reduced by 15%</td>
    <td align="center">45s</td>
  </tr>
</table>

</div>




