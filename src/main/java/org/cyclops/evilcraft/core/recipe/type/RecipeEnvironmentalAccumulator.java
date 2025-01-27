package org.cyclops.evilcraft.core.recipe.type;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.cyclops.cyclopscore.recipe.ItemStackFromIngredient;
import org.cyclops.evilcraft.RegistryEntries;
import org.cyclops.evilcraft.block.BlockEnvironmentalAccumulatorConfig;
import org.cyclops.evilcraft.core.weather.WeatherType;

import java.util.List;
import java.util.Optional;

/**
 * Environmental Accumulator recipe
 * @author rubensworks
 */
public class RecipeEnvironmentalAccumulator implements Recipe<RecipeEnvironmentalAccumulator.Inventory> {

    private final Ingredient inputIngredient;
    private final WeatherType inputWeather;
    private final Either<ItemStack, ItemStackFromIngredient> outputItem;
    private final WeatherType outputWeather;
    private final Optional<Integer> duration;
    private final Optional<Integer> cooldownTime;
    private final Optional<Float> processingSpeed;

    public RecipeEnvironmentalAccumulator(Ingredient inputIngredient, WeatherType inputWeather,
                                          Either<ItemStack, ItemStackFromIngredient> outputItem, WeatherType outputWeather,
                                          Optional<Integer> duration, Optional<Integer> cooldownTime, Optional<Float> processingSpeed) {
        this.inputIngredient = inputIngredient;
        this.inputWeather = inputWeather;
        this.outputItem = outputItem;
        this.outputWeather = outputWeather;
        this.duration = duration;
        this.cooldownTime = cooldownTime;
        this.processingSpeed = processingSpeed;
    }

    public Ingredient getInputIngredient() {
        return inputIngredient;
    }

    public WeatherType getInputWeather() {
        return inputWeather;
    }

    public Either<ItemStack, ItemStackFromIngredient> getOutputItem() {
        return outputItem;
    }

    public ItemStack getOutputItemFirst() {
        return getOutputItem().map(l -> l, ItemStackFromIngredient::getFirstItemStack);
    }

    public WeatherType getOutputWeather() {
        return outputWeather;
    }

    public Optional<Integer> getDurationRaw() {
        return this.duration;
    }

    public Optional<Integer> getCooldownTimeRaw() {
        return this.cooldownTime;
    }

    public Optional<Float> getProcessingSpeedRaw() {
        return this.processingSpeed;
    }

    public int getDuration() {
        int duration = getDurationRaw().orElse(-1);
        // Note: we need to do this because defaultProcessItemTickCount is set AFTER the recipes are created
        if (duration < 0)
            return BlockEnvironmentalAccumulatorConfig.defaultProcessItemTickCount;

        return duration;
    }

    public int getCooldownTime() {
        int cooldownTime = getCooldownTimeRaw().orElse(-1);
        // Note: we need to do this because defaultProcessItemTickCount is set AFTER the recipes are created
        if (cooldownTime < 0)
            return BlockEnvironmentalAccumulatorConfig.defaultTickCooldown;

        return cooldownTime;
    }

    public float getProcessingSpeed() {
        float processingSpeed = getProcessingSpeedRaw().orElse(-1.0F);
        // Note: we need to do this because defaultProcessItemSpeed is set AFTER the recipes are created
        if (processingSpeed < 0)
            return (float) BlockEnvironmentalAccumulatorConfig.defaultProcessItemSpeed;

        return processingSpeed;
    }

    @Override
    public boolean matches(RecipeEnvironmentalAccumulator.Inventory inv, Level worldIn) {
        return inputIngredient.test(inv.getItem(0))
                && inputWeather.isActive(worldIn);
    }

    @Override
    public ItemStack assemble(RecipeEnvironmentalAccumulator.Inventory inv, HolderLookup.Provider registryAccess) {
        ItemStack inputStack = inv.getItem(0);
        ItemStack itemStack = getResultItem(registryAccess).copy();
        if (!inputStack.isEmpty()) {
            for (DataComponentType<?> dataComponentType : inputStack.getComponents().keySet()) {
                if (dataComponentType != RegistryEntries.COMPONENT_WEATHER_CONTAINER_TYPE.value()) {
                    itemStack.set((DataComponentType) dataComponentType, inputStack.get(dataComponentType));
                }
            }
        }
        return itemStack;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height <= 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registryAccess) {
        return this.getOutputItemFirst().copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RegistryEntries.RECIPESERIALIZER_ENVIRONMENTAL_ACCUMULATOR.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RegistryEntries.RECIPETYPE_ENVIRONMENTAL_ACCUMULATOR.get();
    }

    public static interface Inventory extends RecipeInput {
        public Level getWorld();
        public BlockPos getPos();
    }

    public static class InventoryDummy implements Inventory {
        private final List<ItemStack> itemStacks;

        public InventoryDummy(ItemStack... stacksIn) {
            this.itemStacks = Lists.newArrayList(stacksIn);
        }

        @Override
        public Level getWorld() {
            return null;
        }

        @Override
        public BlockPos getPos() {
            return null;
        }

        @Override
        public ItemStack getItem(int i) {
            return itemStacks.get(i);
        }

        @Override
        public int size() {
            return itemStacks.size();
        }
    }

}
