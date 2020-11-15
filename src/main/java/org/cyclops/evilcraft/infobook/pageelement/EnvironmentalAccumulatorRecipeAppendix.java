package org.cyclops.evilcraft.infobook.pageelement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import org.cyclops.cyclopscore.infobook.AdvancedButtonEnum;
import org.cyclops.cyclopscore.infobook.IInfoBook;
import org.cyclops.cyclopscore.infobook.InfoSection;
import org.cyclops.cyclopscore.infobook.ScreenInfoBook;
import org.cyclops.cyclopscore.infobook.pageelement.RecipeAppendix;
import org.cyclops.evilcraft.Reference;
import org.cyclops.evilcraft.RegistryEntries;
import org.cyclops.evilcraft.core.helper.ItemHelpers;
import org.cyclops.evilcraft.core.recipe.type.RecipeEnvironmentalAccumulator;
import org.cyclops.evilcraft.core.weather.WeatherType;
import org.cyclops.evilcraft.tileentity.tickaction.sanguinaryenvironmentalaccumulator.AccumulateItemTickAction;

import java.util.HashMap;
import java.util.Map;

/**
 * Blood Infuser recipes.
 * @author rubensworks
 */
public class EnvironmentalAccumulatorRecipeAppendix extends RecipeAppendix<RecipeEnvironmentalAccumulator> {

    private static final ResourceLocation WEATHERS = new ResourceLocation(Reference.MOD_ID, Reference.TEXTURE_PATH_GUI + "weathers.png");
    private static final Map<WeatherType, Integer> X_ICON_OFFSETS = new HashMap<WeatherType, Integer>();
    static {
        X_ICON_OFFSETS.put(WeatherType.CLEAR, 0);
        X_ICON_OFFSETS.put(WeatherType.RAIN, 16);
        X_ICON_OFFSETS.put(WeatherType.LIGHTNING, 32);
    }
    private static final int SLOT_OFFSET_X = 16;
    private static final int SLOT_OFFSET_Y = 23;
    private static final int START_X_RESULT = 68;
    private static final int Y_START = 2;

    private static final AdvancedButtonEnum INPUT = AdvancedButtonEnum.create();
    private static final AdvancedButtonEnum RESULT = AdvancedButtonEnum.create();

    public EnvironmentalAccumulatorRecipeAppendix(IInfoBook infoBook, RecipeEnvironmentalAccumulator recipe) {
        super(infoBook, recipe);
    }

    @Override
    protected int getWidth() {
        return START_X_RESULT + 32;
    }

    @Override
    protected int getHeightInner() {
        return 42;
    }

    @Override
    protected String getUnlocalizedTitle() {
        return "block.evilcraft.environmental_accumulator";
    }

    @Override
    public void bakeElement(InfoSection infoSection) {
        renderItemHolders.put(INPUT, new ItemButton(getInfoBook()));
        renderItemHolders.put(RESULT, new ItemButton(getInfoBook()));
        super.bakeElement(infoSection);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawElementInner(ScreenInfoBook gui, int x, int y, int width, int height, int page, int mx, int my) {
        boolean sanguinary = (getTick(gui) % 2) == 1;
        int middle = (width - SLOT_SIZE) / 2;
        gui.drawArrowRight(x + middle - 3, y + SLOT_OFFSET_Y + 2);

        // Prepare items
        int tick = getTick(gui);
        ItemStack input = prepareItemStacks(recipe.getInputIngredient().getMatchingStacks(), tick);
        ItemStack result = prepareItemStack(recipe.getOutputItem(), tick);

        // Items
        renderItem(gui, x + SLOT_OFFSET_X, y + SLOT_OFFSET_Y, input, mx, my, INPUT);
        renderItem(gui, x + START_X_RESULT, y + SLOT_OFFSET_Y, result, mx, my, RESULT);

        renderItem(gui, x + middle, y + SLOT_OFFSET_Y, new ItemStack(sanguinary
                ? RegistryEntries.BLOCK_SANGUINARY_ENVIRONMENTAL_ACCUMULATOR
                : RegistryEntries.BLOCK_ENVIRONMENTAL_ACCUMULATOR), mx, my, false, null);

        // Draw weathers
        Integer inputX = X_ICON_OFFSETS.get(recipe.getInputWeather());
        if(inputX != null) {
            Minecraft.getInstance().getTextureManager().bindTexture(WEATHERS);
            gui.blit(x + SLOT_OFFSET_X, y + Y_START, inputX, 0, 16, 16);
            gui.drawOuterBorder(x + SLOT_OFFSET_X, y + Y_START, SLOT_SIZE, SLOT_SIZE, 1, 1, 1, 0.2f);
            Integer outputX = X_ICON_OFFSETS.get(recipe.getOutputWeather());
            Minecraft.getInstance().getTextureManager().bindTexture(WEATHERS);
            gui.blit(x + START_X_RESULT, y + Y_START, outputX, 0, 16, 16);
            gui.drawOuterBorder(x + START_X_RESULT, y + Y_START, SLOT_SIZE, SLOT_SIZE, 1, 1, 1, 0.2f);
        }
        if(sanguinary) {
            // Draw blood usage
            renderItem(gui, x + middle, y + 2, ItemHelpers.getBloodBucket(), mx, my, false, null);

            // Blood amount text
            FontRenderer fontRenderer = gui.getFontRenderer();
            boolean oldUnicode = fontRenderer.getBidiFlag();
            fontRenderer.setBidiFlag(true);
            fontRenderer.setBidiFlag(false);
            int amount = AccumulateItemTickAction.getUsage(recipe.getCooldownTime());
            FluidStack fluidStack = new FluidStack(RegistryEntries.FLUID_BLOOD, amount);
            String line = fluidStack.getAmount() + " mB";
            fontRenderer.drawSplitString(line, x + middle - 5, y + SLOT_SIZE, 200, 0);
            fontRenderer.setBidiFlag(oldUnicode);
        }
    }
}
