package org.cyclops.evilcraft.core.recipe.type;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.ResourceLocation;
import org.cyclops.evilcraft.core.weather.WeatherType;

/**
 * Recipe serializer for environmental accumulator recipes
 * @author rubensworks
 */
public class RecipeSerializerEnvironmentalAccumulator extends RecipeSerializerEnvironmentalAccumulatorAbstract<RecipeEnvironmentalAccumulator> {
    @Override
    protected RecipeEnvironmentalAccumulator createRecipe(ResourceLocation id,
                             Ingredient inputIngredient, WeatherType inputWeather,
                             ItemStack outputItem, WeatherType outputWeather,
                             int duration, int cooldownTime, float processingSpeed) {
        return new RecipeEnvironmentalAccumulator(id, inputIngredient, inputWeather, outputItem, outputWeather, duration, cooldownTime, processingSpeed);
    }
}
