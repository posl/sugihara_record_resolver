/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.datagen.providers.recipes;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.DefaultResourceConditions;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import appeng.recipes.mattercannon.MatterCannonAmmoSerializer;

record MatterCannonAmmo(ResourceLocation id, TagKey<Item> tag, Item item, float weight) implements FinishedRecipe {

    public void serializeRecipeData(JsonObject json) {
        if (tag != null) {
            json.add("ammo", Ingredient.of(tag).toJson());
            ConditionJsonProvider.write(json, DefaultResourceConditions.itemTagsPopulated(tag));
        } else if (item != null) {
            json.add("ammo", Ingredient.of(item).toJson());
        }
        json.addProperty("weight", this.weight);
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public RecipeSerializer<?> getType() {
        return MatterCannonAmmoSerializer.INSTANCE;
    }

    @Nullable
    public JsonObject serializeAdvancement() {
        return null;
    }

    @Nullable
    public ResourceLocation getAdvancementId() {
        return null;
    }
}
