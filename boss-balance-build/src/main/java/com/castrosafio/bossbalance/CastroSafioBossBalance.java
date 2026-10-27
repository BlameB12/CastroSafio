package com.castrosafio.bossbalance;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
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

    /**
     * Namespaces that predominantly contain boss/miniboss encounters in this pack.
     * Unknown entities from these mods are classified by their pre-balance max HP.
     * Ice and Fire is deliberately absent so normal dragons keep their original CC interactions.
     */
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
            "time_stop", "incapac", "bind"
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

        if (isApothicSpecial(living)) {
            TIERS.remove(living.getUUID());
            removeOurModifiers(living);
            return;
        }

        Tier tier = getTier(living);
        if (tier == Tier.NONE) {
            return;
        }

        applyBalanceModifiers(living, tier);

        // Prevent vanilla-style freezing from locking protected encounters down.
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

        // Protect against execute/instant-death damage types without making the boss immortal to normal damage.
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
            // Preserve shields, full blocks and genuine immunities.
            return;
        }

        Tier targetTier = getTier(target);
        if (targetTier != Tier.NONE) {
            float cap = target.getMaxHealth() * (targetTier == Tier.BOSS ? 0.075F : 0.11F);
            if (currentDamage > cap) {
                event.setNewDamage(cap);
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
            floorPct = original >= 30F ? 0.20F : original >= 15F ? 0.13F : original >= 4F ? 0.075F : 0.03F;
            capPct = 0.32F;
            postArmorBoost = 1.15F;
        } else {
            floorPct = original >= 30F ? 0.14F : original >= 15F ? 0.09F : original >= 4F ? 0.05F : 0.02F;
            capPct = 0.24F;
            postArmorBoost = 1.08F;
        }

        // Enrage: the last 30% is more threatening, but still respects the anti-one-shot cap.
        if (attacker.getMaxHealth() > 0F && attacker.getHealth() / attacker.getMaxHealth() <= 0.30F) {
            floorPct *= attackerTier == Tier.BOSS ? 1.20F : 1.10F;
            postArmorBoost *= attackerTier == Tier.BOSS ? 1.10F : 1.05F;
        }

        float desired = Math.max(currentDamage * postArmorBoost, maxHealth * floorPct);
        desired = Mth.clamp(desired, 0F, maxHealth * capPct);
        event.setNewDamage(desired);

        long now = player.level().getGameTime();
        if (attackerTier == Tier.BOSS) {
            HEAL_DEBUFFS.put(player.getUUID(), new HealDebuff(now + 120L, 0.60F)); // -40% for 6s
        } else {
            HEAL_DEBUFFS.put(player.getUUID(), new HealDebuff(now + 80L, 0.75F));  // -25% for 4s
        }
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

        // This check always wins, even if the entity was previously cached as a boss.
        if (isApothicSpecial(living)) {
            TIERS.remove(living.getUUID());
            return Tier.NONE;
        }

        Tier cached = TIERS.get(living.getUUID());
        if (cached != null) {
            return cached;
        }

        String id = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString();
        Tier tier;

        if (EXPLICIT_BOSSES.contains(id)) {
            tier = Tier.BOSS;
        } else if (EXPLICIT_MINIBOSSES.contains(id)) {
            tier = Tier.MINIBOSS;
        } else {
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
            if (!BOSS_MOD_NAMESPACES.contains(key.getNamespace())) {
                return Tier.NONE;
            }

            float hp = living.getMaxHealth();
            if (hp >= 180F) {
                tier = Tier.BOSS;
            } else if (hp >= 70F) {
                tier = Tier.MINIBOSS;
            } else {
                return Tier.NONE;
            }
        }

        TIERS.put(living.getUUID(), tier);
        return tier;
    }

    private static boolean isApothicSpecial(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        for (String key : data.getAllKeys()) {
            String lower = key.toLowerCase();
            if (lower.startsWith("apoth.boss") || lower.startsWith("apoth.miniboss") || lower.contains("apotheosis") || lower.contains("apothic")) {
                return true;
            }
        }

        for (String tag : entity.getTags()) {
            String lower = tag.toLowerCase();
            if (lower.contains("apoth.boss") || lower.contains("apoth.miniboss") || lower.contains("apotheosis") || lower.contains("apothic")) {
                return true;
            }
        }
        return false;
    }

    private static void applyBalanceModifiers(LivingEntity living, Tier tier) {
        AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getModifier(BOSS_HEALTH_MOD) == null) {
            float oldMax = living.getMaxHealth();
            double multiplier = healthMultiplier(oldMax, tier);
            health.addTransientModifier(new AttributeModifier(
                    BOSS_HEALTH_MOD,
                    multiplier - 1.0D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));

            // Fresh/full-health bosses remain full after scaling; already-damaged bosses preserve their damage state.
            if (living.getHealth() >= oldMax - 0.5F) {
                living.setHealth(living.getMaxHealth());
            }
        }

        AttributeInstance damage = living.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null && damage.getModifier(BOSS_DAMAGE_MOD) == null) {
            damage.addTransientModifier(new AttributeModifier(
                    BOSS_DAMAGE_MOD,
                    tier == Tier.BOSS ? 0.25D : 0.12D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));
        }
    }

    private static void removeOurModifiers(LivingEntity living) {
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

    private static double healthMultiplier(float oldMax, Tier tier) {
        if (tier == Tier.MINIBOSS) {
            if (oldMax <= 100F) return 3.0D;
            if (oldMax <= 250F) return 2.4D;
            return 2.0D;
        }

        if (oldMax <= 150F) return 5.0D;
        if (oldMax <= 350F) return 4.0D;
        if (oldMax <= 800F) return 3.2D;
        return 2.6D;
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
