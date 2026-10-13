package com.castrosafio.menu;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Escanea TODO el RecipeManager cargado por el servidor. Esto incluye mesa de
 * crafteo, herreria, hornos, cortapiedras y RecipeType personalizados de mods.
 */
public final class UniversalCraftCommands {
    private UniversalCraftCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("castrosafio")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("crafteo")
                                .then(Commands.literal("autodetectar")
                                        .then(Commands.argument("item", StringArgumentType.greedyString())
                                                .executes(ctx -> detect(ctx.getSource(), StringArgumentType.getString(ctx, "item")))))
                                .then(Commands.literal("detectar")
                                        .then(Commands.argument("item", StringArgumentType.greedyString())
                                                .executes(ctx -> detect(ctx.getSource(), StringArgumentType.getString(ctx, "item"))))))
        );
    }

    private static int detect(CommandSourceStack source, String rawItemId) {
        String itemId = rawItemId == null ? "" : rawItemId.trim();
        ResourceLocation wanted = ResourceLocation.tryParse(itemId);
        if (wanted == null || !BuiltInRegistries.ITEM.containsKey(wanted)) {
            source.sendFailure(Component.literal("[CastroSafio] Ese objeto no existe: " + itemId));
            return 0;
        }

        Collection<RecipeHolder<?>> allRecipes = source.getServer().getRecipeManager().getRecipes();
        RecipeHolder<?> chosen = null;
        ItemStack chosenResult = ItemStack.EMPTY;
        int bestScore = Integer.MIN_VALUE;

        for (RecipeHolder<?> holder : allRecipes) {
            if (holder == null || holder.value() == null) continue;
            Recipe<?> recipe = holder.value();

            ItemStack result;
            try {
                result = recipe.getResultItem(source.getServer().registryAccess());
            } catch (Throwable ignored) {
                continue;
            }

            if (result == null || result.isEmpty()) continue;
            ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
            if (!wanted.equals(resultId)) continue;

            int score = score(recipe);
            if (chosen == null || score > bestScore) {
                chosen = holder;
                chosenResult = result.copy();
                bestScore = score;
            }
        }

        if (chosen == null) {
            source.sendFailure(Component.literal(
                    "[CastroSafio] No encontre ninguna receta registrada para " + itemId
                            + ". Se revisaron " + allRecipes.size()
                            + " recetas de Minecraft, mods y datapacks."
            ));
            return 0;
        }

        CraftDefinition craft = findCraftSlot(itemId);
        if (craft == null) {
            source.sendFailure(Component.literal("[CastroSafio] Ya estan ocupados los "
                    + CraftConfigManager.MAX_CRAFTS + " espacios de crafteos."));
            return 0;
        }

        Recipe<?> recipe = chosen.value();
        craft.ingredients = new ArrayList<>();
        for (int i = 0; i < 9; i++) craft.ingredients.add("");

        List<Ingredient> ingredients;
        try {
            ingredients = recipe.getIngredients();
        } catch (Throwable ignored) {
            ingredients = List.of();
        }

        if (recipe instanceof ShapedRecipe shaped) {
            int recipeWidth = Math.min(3, shaped.getWidth());
            int recipeHeight = Math.min(3, shaped.getHeight());
            for (int row = 0; row < recipeHeight; row++) {
                for (int col = 0; col < recipeWidth; col++) {
                    int ingredientIndex = row * shaped.getWidth() + col;
                    if (ingredientIndex >= ingredients.size()) continue;
                    craft.ingredients.set(row * 3 + col, representativeItem(ingredients.get(ingredientIndex)));
                }
            }
        } else {
            int slot = 0;
            for (Ingredient ingredient : ingredients) {
                String representative = representativeItem(ingredient);
                if (representative.isBlank()) continue;
                if (slot >= 9) break;
                craft.ingredients.set(slot++, representative);
            }
        }

        ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
        String typeText = typeId == null ? recipe.getType().toString() : typeId.toString();
        ResourceLocation recipeId = chosen.id();

        craft.result = wanted.toString();
        craft.resultAmount = Math.max(1, chosenResult.getCount());
        craft.name = chosenResult.getHoverName().getString();
        craft.recipeType = typeText;
        craft.recipeId = recipeId == null ? "" : recipeId.toString();
        craft.active = true;

        CraftConfigManager.save();
        MobConfigManager.syncAll();

        source.sendSuccess(() -> Component.literal(
                "[CastroSafio] Publicado: " + craft.name
                        + " | tipo: " + typeText
                        + " | receta: " + recipeId
        ), true);
        return 1;
    }

    private static int score(Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe) return 300;
        if (recipe instanceof CraftingRecipe) return 250;
        try {
            int ingredients = recipe.getIngredients().size();
            if (ingredients > 0) return 150 + Math.min(ingredients, 50);
        } catch (Throwable ignored) {
        }
        return 100;
    }

    private static CraftDefinition findCraftSlot(String resultId) {
        CraftDefinition firstInactive = null;
        for (CraftDefinition craft : CraftConfigManager.all()) {
            if (resultId.equals(craft.result)) return craft;
            if (!craft.active && firstInactive == null) firstInactive = craft;
        }
        return firstInactive;
    }

    private static String representativeItem(Ingredient ingredient) {
        if (ingredient == null) return "";
        try {
            for (ItemStack stack : ingredient.getItems()) {
                if (stack == null || stack.isEmpty()) continue;
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (id != null) return id.toString();
            }
        } catch (Throwable ignored) {
        }
        return "";
    }
}
