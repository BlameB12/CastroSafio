package com.castrosafio.bossbalance;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod(CastroSafioBossBalance.MOD_ID)
public final class CastroSafioBossBalance {
    public static final String MOD_ID = "castrosafio_boss_balance";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation BOSS_HEALTH_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "boss_health");
    private static final ResourceLocation BOSS_DAMAGE_MOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "boss_damage");

    private static final Map<UUID, Tier> TIERS = new ConcurrentHashMap<>();
    private static final Map<UUID, HealDebuff> HEAL_DEBUFFS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SPAWN_HEALTH_GUARDS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LEGITIMATE_DEATH_WINDOWS = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> LAST_SAFE_HEALTH = new ConcurrentHashMap<>();

    private static final Set<String> EXPLICIT_BOSSES = Set.of(
            "minecraft:ender_dragon",
            "minecraft:wither",

            "cataclysm:ender_golem",
            "cataclysm:netherite_monstrosity",
            "cataclysm:ignis",
            "cataclysm:ender_guardian",
            "cataclysm:the_leviathan",
            "cataclysm:ancient_remnant",
            "cataclysm:the_harbinger",
            "cataclysm:maledictus",
            "cataclysm:scylla",

            "mowziesmobs:frostmaw",
            "mowziesmobs:ferrous_wroughtnaut",
            "mowziesmobs:umvuthi",

            "aquamirae:captain_cornelia",
            "aquamirae:ghost_of_captain_cornelia",

            "fdbosses:chesed",
            "fdbosses:malkuth",
            "fdbosses:geburah",
            "fdbosses:netzach",

            "bosses_of_mass_destruction:gauntlet",
            "bosses_of_mass_destruction:lich",
            "bosses_of_mass_destruction:obsidilith",
            "bosses_of_mass_destruction:void_blossom",

            "twilightforest:naga",
            "twilightforest:lich",
            "twilightforest:minoshroom",
            "twilightforest:hydra",
            "twilightforest:knight_phantom",
            "twilightforest:ur_ghast",
            "twilightforest:alpha_yeti",
            "twilightforest:snow_queen",

            "aether:slider",
            "aether:valkyrie_queen",
            "aether:sun_spirit",

            "block_factorys_bosses:yeti",
            "legendary_monsters:frostbitten_golem",
            "legendary_monsters:endersent",
            "legendary_monsters:shulker_mimic",

            "netherman:azazel",
            "netherman:azazel_human"
    );

    private static final Set<String> EXPLICIT_MINIBOSSES = Set.of(
            "mowziesmobs:sculptor"
    );

    private static final Set<String> BOSS_MOD_NAMESPACES = Set.of(
            "cataclysm",
            "mowziesmobs",
            "legendary_monsters",
            "block_factorys_bosses",
            "aquamirae",
            "fdbosses",
            "bosses_of_mass_destruction",
            "twilightforest",
            "aether",
            "graveyard",
            "alexscaves",
            "the_bumblezone",
            "irons_spellbooks",
            "macabre",
            "netherman"
    );

    private static final String[] HARD_CC_KEYWORDS = {
            "frozen", "freeze", "freezing", "stun", "paraly", "paralys", "parálisis",
            "immobil", "root", "rooted", "stasis", "petrif", "timestop", "time_stop",
            "incapac", "bind"
    };

    private static final String[] EXECUTE_KEYWORDS = {
            "execute", "execution", "instant_kill", "instant_death", "death_mark"
    };

    public CastroSafioBossBalance(IEventBus modBus, ModContainer container) {
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("CastroSafio Boss Balance loaded: endgame boss protection enabled.");
    }

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity living) || living.level().isClientSide() || living instanceof Player) {
            return;
        }

        String id = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString();
        boolean explicitEncounter = EXPLICIT_BOSSES.contains(id) || EXPLICIT_MINIBOSSES.contains(id);

        if (!explicitEncounter && isApothicSpecial(living)) {
            TIERS.remove(living.getUUID());
            SPAWN_HEALTH_GUARDS.remove(living.getUUID());
            LEGITIMATE_DEATH_WINDOWS.remove(living.getUUID());
            LAST_SAFE_HEALTH.remove(living.getUUID());
            removeOurModifiers(living);
            return;
        }

        Tier tier = getTier(living);
        if (tier == Tier.NONE) {
            return;
        }

        applyBalanceModifiers(living, tier);
        maintainInitialFullHealth(living);

        if (living.getHealth() > 0.0F) {
            LAST_SAFE_HEALTH.put(living.getUUID(), living.getHealth());
        }

        Long deathWindow = LEGITIMATE_DEATH_WINDOWS.get(living.getUUID());
        if (deathWindow != null && living.level().getGameTime() > deathWindow) {
            LEGITIMATE_DEATH_WINDOWS.remove(living.getUUID());
        }

        if (living.getTicksFrozen() > 0) {
            living.setTicksFrozen(0);
        }
    }

    @SubscribeEvent
    public void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity living = event.getEntity();
        Tier tier = getTier(living);
        if (tier == Tier.NONE || event.getEffectInstance() == null) {
            return;
        }

        String effectId = event.getEffectInstance().getEffect().unwrapKey()
                .map(key -> key.location().toString().toLowerCase())
                .orElse("");

        if (containsAny(effectId, HARD_CC_KEYWORDS)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        Tier tier = getTier(target);
        if (tier == Tier.NONE) {
            return;
        }

        String damageId = event.getSource().getMsgId().toLowerCase();
        if (containsAny(damageId, EXECUTE_KEYWORDS)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onFinalDamage(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        float currentDamage = event.getNewDamage();
        if (currentDamage <= 0.0001F) {
            return;
        }

        Tier targetTier = getTier(target);
        if (targetTier != Tier.NONE) {
            SPAWN_HEALTH_GUARDS.remove(target.getUUID());

            float maxHealth = target.getMaxHealth();
            float cap = maxHealth * (targetTier == Tier.BOSS ? 0.04F : 0.07F);
            float mitigated = currentDamage * (targetTier == Tier.BOSS ? 0.60F : 0.78F);
            float finalDamage = Math.min(mitigated, cap);
            event.setNewDamage(finalDamage);

            float healthAfterHit = target.getHealth() - finalDamage;
            long now = target.level().getGameTime();
            if (healthAfterHit <= 0.0001F) {
                // Only a real, processed lethal hit is allowed to kill a protected encounter.
                LEGITIMATE_DEATH_WINDOWS.put(target.getUUID(), now + 2L);
            } else {
                LEGITIMATE_DEATH_WINDOWS.remove(target.getUUID());
                LAST_SAFE_HEALTH.put(target.getUUID(), Math.max(1.0F, healthAfterHit));
            }
            return;
        }

        if (!(target instanceof Player player)) {
            return;
        }

        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) {
            return;
        }

        Tier attackerTier = getTier(attacker);
        if (attackerTier == Tier.NONE) {
            return;
        }

        float maxHealth = player.getMaxHealth();
        float original = event.getOriginalDamage();
        float floorPct;
        float capPct;
        float postArmorBoost;

        if (attackerTier == Tier.BOSS) {
            // Slightly stronger than V3: more pressure, still no one-shot.
            floorPct = original >= 30F ? 0.21F : original >= 15F ? 0.14F : original >= 4F ? 0.085F : 0.035F;
            capPct = 0.32F;
            postArmorBoost = 1.22F;
        } else {
            floorPct = original >= 30F ? 0.145F : original >= 15F ? 0.095F : original >= 4F ? 0.055F : 0.025F;
            capPct = 0.24F;
            postArmorBoost = 1.12F;
        }

        if (attacker.getMaxHealth() > 0F && attacker.getHealth() / attacker.getMaxHealth() <= 0.30F) {
            floorPct *= attackerTier == Tier.BOSS ? 1.20F : 1.10F;
            postArmorBoost *= attackerTier == Tier.BOSS ? 1.10F : 1.05F;
        }

        float desired = Math.max(currentDamage * postArmorBoost, maxHealth * floorPct);
        desired = Mth.clamp(desired, 0F, maxHealth * capPct);
        event.setNewDamage(desired);

        long now = player.level().getGameTime();
        if (attackerTier == Tier.BOSS) {
            HEAL_DEBUFFS.put(player.getUUID(), new HealDebuff(now + 120L, 0.60F));
        } else {
            HEAL_DEBUFFS.put(player.getUUID(), new HealDebuff(now + 80L, 0.75F));
        }
    }

    @SubscribeEvent
    public void onProtectedDeath(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();
        Tier tier = getTierForDeath(target);
        if (tier == Tier.NONE) {
            return;
        }

        String damageId = event.getSource().getMsgId().toLowerCase();
        // Keep OP/admin control intact. /kill should always work.
        if (damageId.equals("generickill") || damageId.equals("generic_kill")) {
            clearDeathTracking(target);
            return;
        }

        long now = target.level().getGameTime();
        Long allowedUntil = LEGITIMATE_DEATH_WINDOWS.get(target.getUUID());
        if (allowedUntil != null && now <= allowedUntil) {
            clearDeathTracking(target);
            return;
        }

        // No legitimate lethal damage was recorded. This is the important fallback for
        // execute effects implemented as direct kill()/setHealth(0), including "below 25% = die".
        event.setCanceled(true);
        float restore = LAST_SAFE_HEALTH.getOrDefault(
                target.getUUID(),
                Math.max(1.0F, target.getMaxHealth() * 0.05F)
        );
        target.setHealth(Mth.clamp(restore, 1.0F, target.getMaxHealth()));
        LEGITIMATE_DEATH_WINDOWS.remove(target.getUUID());
    }

    @SubscribeEvent
    public void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        HealDebuff debuff = HEAL_DEBUFFS.get(player.getUUID());
        if (debuff == null) {
            return;
        }

        if (player.level().getGameTime() > debuff.expiresAt()) {
            HEAL_DEBUFFS.remove(player.getUUID());
            return;
        }

        event.setAmount(event.getAmount() * debuff.factor());
    }

    private static Tier getTier(LivingEntity living) {
        if (living instanceof Player || !living.isAlive()) {
            return Tier.NONE;
        }

        String id = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString();

        if (EXPLICIT_BOSSES.contains(id)) {
            TIERS.put(living.getUUID(), Tier.BOSS);
            return Tier.BOSS;
        }
        if (EXPLICIT_MINIBOSSES.contains(id)) {
            TIERS.put(living.getUUID(), Tier.MINIBOSS);
            return Tier.MINIBOSS;
        }

        if (isApothicSpecial(living)) {
            TIERS.remove(living.getUUID());
            return Tier.NONE;
        }

        Tier cached = TIERS.get(living.getUUID());
        if (cached != null) {
            return cached;
        }

        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        if (!BOSS_MOD_NAMESPACES.contains(key.getNamespace())) {
            return Tier.NONE;
        }

        float hp = living.getMaxHealth();
        Tier tier;
        if (hp >= 180F) {
            tier = Tier.BOSS;
        } else if (hp >= 70F) {
            tier = Tier.MINIBOSS;
        } else {
            return Tier.NONE;
        }

        TIERS.put(living.getUUID(), tier);
        return tier;
    }

    private static Tier getTierForDeath(LivingEntity living) {
        if (living instanceof Player) {
            return Tier.NONE;
        }

        String id = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString();
        if (EXPLICIT_BOSSES.contains(id)) {
            return Tier.BOSS;
        }
        if (EXPLICIT_MINIBOSSES.contains(id)) {
            return Tier.MINIBOSS;
        }

        Tier cached = TIERS.get(living.getUUID());
        return cached == null ? Tier.NONE : cached;
    }

    private static boolean isApothicSpecial(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();

        if (data.contains("apoth.boss") || data.contains("apoth.miniboss")) {
            return true;
        }

        for (String tag : entity.getTags()) {
            String lower = tag.toLowerCase();
            if (lower.equals("apoth.boss") || lower.startsWith("apoth.boss.")
                    || lower.equals("apoth.miniboss") || lower.startsWith("apoth.miniboss.")) {
                return true;
            }
        }
        return false;
    }

    private static void applyBalanceModifiers(LivingEntity living, Tier tier) {
        AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getModifier(BOSS_HEALTH_MOD) == null) {
            float oldMax = living.getMaxHealth();
            boolean wasFull = living.getHealth() >= oldMax - 0.5F;
            double multiplier = healthMultiplier(oldMax, tier);

            health.addTransientModifier(new AttributeModifier(
                    BOSS_HEALTH_MOD,
                    multiplier - 1.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));

            if (wasFull) {
                living.setHealth(living.getMaxHealth());
                LAST_SAFE_HEALTH.put(living.getUUID(), living.getMaxHealth());
                SPAWN_HEALTH_GUARDS.put(living.getUUID(), living.level().getGameTime() + 100L);
            }
        }

        AttributeInstance damage = living.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null && damage.getModifier(BOSS_DAMAGE_MOD) == null) {
            damage.addTransientModifier(new AttributeModifier(
                    BOSS_DAMAGE_MOD,
                    tier == Tier.BOSS ? 0.35D : 0.15D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));
        }
    }

    private static void maintainInitialFullHealth(LivingEntity living) {
        Long expiresAt = SPAWN_HEALTH_GUARDS.get(living.getUUID());
        if (expiresAt == null) {
            return;
        }

        if (!living.isAlive() || living.level().getGameTime() > expiresAt) {
            SPAWN_HEALTH_GUARDS.remove(living.getUUID());
            return;
        }

        if (living.getHealth() < living.getMaxHealth() - 0.5F) {
            living.setHealth(living.getMaxHealth());
            LAST_SAFE_HEALTH.put(living.getUUID(), living.getMaxHealth());
        }
    }

    private static void removeOurModifiers(LivingEntity living) {
        SPAWN_HEALTH_GUARDS.remove(living.getUUID());
        LEGITIMATE_DEATH_WINDOWS.remove(living.getUUID());
        LAST_SAFE_HEALTH.remove(living.getUUID());

        AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getModifier(BOSS_HEALTH_MOD) != null) {
            health.removeModifier(BOSS_HEALTH_MOD);
            if (living.getHealth() > living.getMaxHealth()) {
                living.setHealth(living.getMaxHealth());
            }
        }

        AttributeInstance damage = living.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null && damage.getModifier(BOSS_DAMAGE_MOD) != null) {
            damage.removeModifier(BOSS_DAMAGE_MOD);
        }
    }

    private static void clearDeathTracking(LivingEntity living) {
        LEGITIMATE_DEATH_WINDOWS.remove(living.getUUID());
        LAST_SAFE_HEALTH.remove(living.getUUID());
        SPAWN_HEALTH_GUARDS.remove(living.getUUID());
    }

    private static double healthMultiplier(float oldMax, Tier tier) {
        if (tier == Tier.MINIBOSS) {
            if (oldMax <= 100F) return 4.0D;
            if (oldMax <= 250F) return 3.2D;
            return 2.5D;
        }

        if (oldMax <= 150F) return 10.0D;
        if (oldMax <= 350F) return 8.0D;
        if (oldMax <= 800F) return 6.0D;
        return 4.5D;
    }

    private static boolean containsAny(String text, String[] needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private enum Tier {
        NONE,
        MINIBOSS,
        BOSS
    }

    private record HealDebuff(long expiresAt, float factor) {
    }
}
