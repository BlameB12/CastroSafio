package com.castrosafio.bossbalance;

import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

public final class BossClassifier {
    private static final String TIER_KEY = "castrosafio_boss_balance.tier";

    private static final Set<String> BOSSES = Set.of(
        "minecraft:ender_dragon",
        "minecraft:wither",

        "cataclysm:ender_guardian",
        "cataclysm:netherite_monstrosity",
        "cataclysm:ignis",
        "cataclysm:the_harbinger",
        "cataclysm:the_leviathan",
        "cataclysm:ancient_remnant",
        "cataclysm:maledictus",
        "cataclysm:scylla",

        "mowziesmobs:frostmaw",
        "mowziesmobs:ferrous_wroughtnaut",
        "mowziesmobs:umvuthi",

        "aquamirae:captain_cornelia",

        "fdbosses:chesed",
        "fdbosses:malkuth",
        "fdbosses:geburah",
        "fdbosses:netzach",

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

        "netherman:azazel",
        "netherman:azazel_human",

        "bosses_of_mass_destruction:lich",
        "bosses_of_mass_destruction:obsidilith",
        "bosses_of_mass_destruction:gauntlet",
        "bosses_of_mass_destruction:void_blossom"
    );

    private static final Set<String> MINIBOSSES = Set.of(
        "minecraft:warden",
        "minecraft:elder_guardian",
        "minecraft:ravager",
        "legendary_monsters:frostbitten_golem",
        "legendary_monsters:shulker_mimic",
        "legendary_monsters:endersent",
        "block_factorys_bosses:yeti"
    );

    private static final Set<String> HEURISTIC_NAMESPACES = Set.of(
        "cataclysm",
        "mowziesmobs",
        "legendary_monsters",
        "block_factorys_bosses",
        "aquamirae",
        "bosses_of_mass_destruction",
        "fdbosses",
        "twilightforest",
        "aether",
        "netherman",
        "graveyard",
        "alexscaves",
        "the_bumblezone",
        "irons_spellbooks",
        "born_in_chaos_v1",
        "macabre"
    );

    private static final String[] HELPER_WORDS = {
        "projectile", "spawner", "crystal", "minion", "tentacle", "effect", "cloud",
        "slash", "ray", "missile", "fireball", "boulder", "chain", "floor", "platform",
        "eye_of_", "summon", "particle", "beam", "attack", "orb"
    };

    private BossClassifier() {}

    public static Tier getTier(LivingEntity entity) {
        if (isExcluded(entity)) return Tier.NONE;

        int saved = entity.getPersistentData().getInt(TIER_KEY);
        if (saved == Tier.MINIBOSS.id) return Tier.MINIBOSS;
        if (saved == Tier.BOSS.id) return Tier.BOSS;

        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String id = key.toString();

        Tier tier = classifyFresh(entity, key, id);
        if (tier != Tier.NONE) entity.getPersistentData().putInt(TIER_KEY, tier.id);
        return tier;
    }

    private static Tier classifyFresh(LivingEntity entity, ResourceLocation key, String id) {
        if (BOSSES.contains(id)) return Tier.BOSS;
        if (MINIBOSSES.contains(id)) return Tier.MINIBOSS;

        String namespace = key.getNamespace();
        String path = key.getPath();
        if (!HEURISTIC_NAMESPACES.contains(namespace) || isHelper(path)) return Tier.NONE;

        float hp = entity.getMaxHealth();
        if (hp >= 250.0F) return Tier.BOSS;
        if (hp >= 70.0F) return Tier.MINIBOSS;
        return Tier.NONE;
    }

    private static boolean isHelper(String path) {
        for (String word : HELPER_WORDS) {
            if (path.contains(word)) return true;
        }
        return false;
    }

    public static boolean isExcluded(LivingEntity entity) {
        if (entity instanceof TamableAnimal tamable && tamable.isTame()) return true;
        var data = entity.getPersistentData();
        return data.contains("apoth.boss") || data.contains("apoth.miniboss");
    }

    public enum Tier {
        NONE(0), MINIBOSS(1), BOSS(2);

        private final int id;
        Tier(int id) { this.id = id; }
    }
}
