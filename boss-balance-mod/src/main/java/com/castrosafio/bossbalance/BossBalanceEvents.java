package com.castrosafio.bossbalance;

import java.util.ArrayList;

import com.castrosafio.bossbalance.BossClassifier.Tier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class BossBalanceEvents {
    private static final ResourceLocation HEALTH_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
        CastroSafioBossBalance.MODID, "health_multiplier");

    private static final String PLAYER_COMBAT_UNTIL = "castrosafio_boss_balance.player_combat_until";
    private static final String LEGIT_LETHAL_UNTIL = "castrosafio_boss_balance.legit_lethal_until";
    private static final String PRESSURE_UNTIL = "castrosafio_boss_balance.pressure_until";
    private static final String PRESSURE_TIER = "castrosafio_boss_balance.pressure_tier";

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) return;

        Tier tier = BossClassifier.getTier(living);
        if (tier == Tier.NONE) return;

        ensureHealthBuff(living, tier);
        living.setTicksFrozen(0);

        if (living.tickCount % 10 == 0) {
            for (MobEffectInstance effect : new ArrayList<>(living.getActiveEffects())) {
                ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
                if (id != null && isHardControlEffect(id)) {
                    living.removeEffect(effect.getEffect());
                }
            }
        }
    }

    @SubscribeEvent
    public void onEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity target = event.getEntity();
        if (BossClassifier.getTier(target) == Tier.NONE) return;

        MobEffectInstance effect = event.getEffectInstance();
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
        if (id != null && isHardControlEffect(id)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public void onDamage(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();

        Tier targetTier = BossClassifier.getTier(target);
        if (targetTier != Tier.NONE && isPlayerCaused(source)) {
            protectBossFromBurst(event, target, targetTier);
            return;
        }

        if (target instanceof Player player) {
            Tier attackerTier = getProtectedAttackerTier(source);
            if (attackerTier != Tier.NONE) {
                makeBossHitMatter(event, player, attackerTier);
            }
        }
    }

    @SubscribeEvent
    public void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;

        long now = player.level().getGameTime();
        var data = player.getPersistentData();
        if (now >= data.getLong(PRESSURE_UNTIL)) return;

        int tier = data.getInt(PRESSURE_TIER);
        float factor = tier >= Tier.BOSS.ordinal() ? 0.60F : 0.75F;
        event.setAmount(event.getAmount() * factor);
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Tier tier = BossClassifier.getTier(entity);
        if (tier == Tier.NONE || entity.level().isClientSide()) return;

        long now = entity.level().getGameTime();
        var data = entity.getPersistentData();
        long combatUntil = data.getLong(PLAYER_COMBAT_UNTIL);
        long legitUntil = data.getLong(LEGIT_LETHAL_UNTIL);

        if (event.getSource().is(DamageTypes.GENERIC_KILL) && now >= combatUntil) return;

        if (now < combatUntil && now > legitUntil) {
            event.setCanceled(true);
            float rescueHealth = Math.max(1.0F, entity.getMaxHealth() * 0.05F);
            entity.setHealth(rescueHealth);
        }
    }

    private static void ensureHealthBuff(LivingEntity living, Tier tier) {
        AttributeInstance maxHealth = living.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null || maxHealth.hasModifier(HEALTH_MODIFIER_ID)) return;

        float oldMax = living.getMaxHealth();
        float ratio = oldMax > 0.0F ? living.getHealth() / oldMax : 1.0F;
        double multiplier = healthMultiplier(tier, oldMax);

        AttributeModifier modifier = new AttributeModifier(
            HEALTH_MODIFIER_ID,
            multiplier - 1.0D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        // Transient on purpose: removing the mod restores existing entities to their native attributes.
        maxHealth.addTransientModifier(modifier);
        living.setHealth(Math.max(1.0F, living.getMaxHealth() * ratio));
    }

    private static double healthMultiplier(Tier tier, float hp) {
        if (tier == Tier.BOSS) {
            if (hp < 500.0F) return 8.0D;
            if (hp < 1000.0F) return 5.0D;
            if (hp < 2000.0F) return 3.5D;
            return 2.5D;
        }

        if (hp < 200.0F) return 4.0D;
        if (hp < 500.0F) return 3.0D;
        if (hp < 1000.0F) return 2.25D;
        return 1.75D;
    }

    private static void protectBossFromBurst(LivingDamageEvent.Pre event, LivingEntity boss, Tier tier) {
        DamageSource source = event.getSource();
        long now = boss.level().getGameTime();

        if (source.is(DamageTypes.GENERIC_KILL) && source.getEntity() == null) return;

        boss.getPersistentData().putLong(PLAYER_COMBAT_UNTIL, now + 40L);

        float maxFraction = tier == Tier.BOSS ? 0.05F : 0.10F;
        float cap = boss.getMaxHealth() * maxFraction;
        float adjusted = Math.min(event.getNewDamage(), cap);
        event.setNewDamage(adjusted);

        if (adjusted > 0.0F && adjusted >= boss.getHealth()) {
            boss.getPersistentData().putLong(LEGIT_LETHAL_UNTIL, now + 2L);
        }
    }

    private static void makeBossHitMatter(LivingDamageEvent.Pre event, Player player, Tier tier) {
        float current = event.getNewDamage();
        if (current <= 0.0F) return;

        if (event.getContainer().getBlockedDamage() > 0.01F) return;

        float original = event.getOriginalDamage();
        float maxHealth = player.getMaxHealth();
        float capFraction = tier == Tier.BOSS ? 0.34F : 0.26F;
        float cap = maxHealth * capFraction;

        float adjusted = Math.min(current, cap);
        if (original >= 4.0F) {
            float baseFraction;
            if (tier == Tier.BOSS) {
                baseFraction = 0.055F + Math.min(original / 180.0F, 0.20F);
            } else {
                baseFraction = 0.035F + Math.min(original / 220.0F, 0.14F);
            }

            LivingEntity attacker = getProtectedAttacker(event.getSource());
            if (attacker != null && attacker.getHealth() <= attacker.getMaxHealth() * 0.30F) {
                baseFraction *= 1.15F;
            }

            float floor = Math.min(maxHealth * baseFraction, cap);
            adjusted = Math.max(adjusted, floor);
        }

        event.setNewDamage(Math.min(adjusted, cap));

        long now = player.level().getGameTime();
        var data = player.getPersistentData();
        long pressure = tier == Tier.BOSS ? 120L : 80L;
        data.putLong(PRESSURE_UNTIL, now + pressure);
        data.putInt(PRESSURE_TIER, tier.ordinal());
    }

    private static boolean isPlayerCaused(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof Player) return true;
        if (attacker instanceof TamableAnimal tame && tame.getOwner() instanceof Player) return true;

        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof Player) return true;
            if (owner instanceof TamableAnimal tame && tame.getOwner() instanceof Player) return true;
        }
        return false;
    }

    private static Tier getProtectedAttackerTier(DamageSource source) {
        LivingEntity attacker = getProtectedAttacker(source);
        return attacker == null ? Tier.NONE : BossClassifier.getTier(attacker);
    }

    private static LivingEntity getProtectedAttacker(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && BossClassifier.getTier(living) != Tier.NONE) return living;

        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner
            && BossClassifier.getTier(owner) != Tier.NONE) {
            return owner;
        }
        return null;
    }

    private static boolean isHardControlEffect(ResourceLocation id) {
        String full = id.toString().toLowerCase();
        String path = id.getPath().toLowerCase();

        if (full.equals("iceandfire:frozen")) return true;

        return path.contains("stun")
            || path.contains("frozen")
            || path.contains("freeze")
            || path.contains("paraly")
            || path.contains("petrif")
            || path.contains("immobil")
            || path.contains("rooted")
            || path.equals("sleep")
            || path.contains("stasis")
            || path.contains("time_stop")
            || path.contains("timestop")
            || path.contains("incapac")
            || path.contains("snare")
            || path.contains("entangle");
    }
}
