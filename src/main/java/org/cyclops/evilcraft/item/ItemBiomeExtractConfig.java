package org.cyclops.evilcraft.item;

import com.google.common.collect.Lists;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.cyclops.cyclopscore.config.ConfigurableProperty;
import org.cyclops.cyclopscore.config.extendedconfig.ItemConfig;
import org.cyclops.cyclopscore.helper.MinecraftHelpers;
import org.cyclops.evilcraft.EvilCraft;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Config for the {@link ItemBiomeExtract}.
 * @author rubensworks
 *
 */
public class ItemBiomeExtractConfig extends ItemConfig {

    @ConfigurableProperty(category = "item", comment = "If creative versions for all variants should be added to the creative tab.", requiresMcRestart = true)
    public static boolean creativeTabVariants = true;

    @ConfigurableProperty(category = "item",
            comment = "A list of biome names for which no Biome Extracts may be created.")
    public static List<String> craftingBlacklist = Lists.newArrayList();

    /**
     * A list of biome ids for which no Biome Extracts may be used.
     */
    @ConfigurableProperty(category = "item",
            comment = "A list of biome names for which no Biome Extracts may be used.")
    public static List<String> usageBlacklist = Lists.newArrayList();

    public ItemBiomeExtractConfig() {
        super(
                EvilCraft._instance,
                "biome_extract",
                eConfig -> new ItemBiomeExtract(new Item.Properties()
                        )
        );
        if (MinecraftHelpers.isClientSide()) {
            EvilCraft._instance.getModEventBus().addListener(this::onRegisterColors);
        }
        EvilCraft._instance.getModEventBus().addListener(this::onCreativeModeTabBuildContents);
    }

    @OnlyIn(Dist.CLIENT)
    public void onRegisterColors(RegisterColorHandlersEvent.Item event) {
        event.register(new ItemBiomeExtract.ItemColor(), getInstance());
    }

    public static boolean isCraftingBlacklisted(Registry<Biome> biomeRegistry, Biome biome) {
        return craftingBlacklist.contains(biomeRegistry.getKey(biome).toString());
    }

    public static boolean isUsageBlacklisted(Registry<Biome> biomeRegistry, Biome biome) {
        return usageBlacklist.contains(biomeRegistry.getKey(biome).toString());
    }

    @Override
    protected Collection<ItemStack> getDefaultCreativeTabEntries() {
        // Register tab entries later, when the world is available
        return Collections.emptyList();
    }

    protected void onCreativeModeTabBuildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == getMod().getDefaultCreativeTab()) {
            List<ItemStack> list = Lists.newArrayList();
            list.add(new ItemStack(getInstance()));
            if (creativeTabVariants) {
                try {
                    Registry<Biome> registry = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD).registryAccess().registryOrThrow(Registries.BIOME);
                    for (Biome biome : ((ItemBiomeExtract) getInstance()).getBiomes()) {
                        list.add(((ItemBiomeExtract) getInstance()).createItemStack(registry::getKey, biome, 1));
                    }
                } catch (RuntimeException e) {
                    // Ignore errors
                }
            }
            event.acceptAll(list);
        }
    }
}
