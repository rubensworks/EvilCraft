package org.cyclops.evilcraft.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.cyclops.cyclopscore.config.ConfigurableProperty;
import org.cyclops.cyclopscore.config.extendedconfig.ItemConfig;
import org.cyclops.evilcraft.EvilCraft;

/**
 * Config for the {@link ItemExaltedCrafter}.
 * @author rubensworks
 *
 */
public class ItemExaltedCrafterConfig extends ItemConfig {

    @ConfigurableProperty(category = "item", comment = "If shift clicking on an item should first try to go into the crafting grid.", isCommandable = true)
    public static boolean shiftCraftingGrid = false;

    public ItemExaltedCrafterConfig(boolean wooden, boolean empowered) {
        super(
                EvilCraft._instance,
                "exalted_crafter" + (wooden ? "_wooden" : "") + (empowered ? "_empowered" : ""),
                eConfig -> new ItemExaltedCrafter(new Item.Properties()
                        .component(DataComponents.RARITY, empowered ? Rarity.UNCOMMON : Rarity.COMMON)
                        .stacksTo(1), wooden, empowered)
        );
        EvilCraft._instance.getModEventBus().addListener(this::registerCapability);
    }

    protected void registerCapability(RegisterCapabilitiesEvent event) {
        event.registerItem(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ITEM, (stack, context) -> new ItemExaltedCrafter.ItemHandler(stack), getInstance());
    }

}
