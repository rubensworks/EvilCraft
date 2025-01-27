package org.cyclops.evilcraft.core.broom;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.cyclopscore.helper.MinecraftHelpers;
import org.cyclops.evilcraft.EvilCraft;
import org.cyclops.evilcraft.Reference;
import org.cyclops.evilcraft.RegistryEntries;
import org.cyclops.evilcraft.api.broom.BroomModifier;
import org.cyclops.evilcraft.api.broom.IBroomModifierRegistry;
import org.cyclops.evilcraft.api.broom.IBroomPart;
import org.cyclops.evilcraft.item.ItemBroomConfig;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Default registry for broom modifiers.
 * @author rubensworks
 */
public class BroomModifierRegistry implements IBroomModifierRegistry {

    private final Map<ResourceLocation, BroomModifier> broomModifiers = Maps.newLinkedHashMap();
    private final Map<BroomModifier, IBroomPart> broomModifierParts = Maps.newHashMap();
    private final List<Pair<Supplier<ItemStack>, Map<BroomModifier, Float>>> broomItems = Lists.newArrayList();

    public BroomModifierRegistry() {
        EvilCraft._instance.getModEventBus().addListener(EventPriority.HIGHEST, this::beforeItemsRegistered);
        NeoForge.EVENT_BUS.addListener(this::onTooltipEvent);
    }

    @Override
    public BroomModifier registerModifier(BroomModifier modifier) {
        broomModifiers.put(modifier.getId(), modifier);
        BroomPartModifier broomPart = new BroomPartModifier(modifier);
        overrideDefaultModifierPart(modifier, broomPart);
        return modifier;
    }

    @Nullable
    @Override
    public BroomModifier getModifier(ResourceLocation id) {
        return broomModifiers.get(id);
    }

    @Override
    public void overrideDefaultModifierPart(BroomModifier modifier, @Nullable IBroomPart broomPart) {
        broomModifierParts.put(modifier, broomPart);
        if (broomPart != null) {
            BroomParts.REGISTRY.registerPart(broomPart);
        }
    }

    @Override
    public @Nullable IBroomPart getModifierPart(BroomModifier modifier) {
        return broomModifierParts.get(modifier);
    }

    @Override
    public void clearModifierItems() {
        broomItems.clear();
    }

    @Override
    public void registerModifiersItem(Map<BroomModifier, Float> modifiers, Supplier<ItemStack> item) {
        Objects.requireNonNull(item);
        broomItems.add(Pair.of(item, modifiers));
    }

    @Override
    public void registerModifiersItem(BroomModifier modifier, float modifierValue, Supplier<ItemStack> item) {
        Map<BroomModifier, Float> map = Maps.newHashMap();
        map.put(modifier, modifierValue);
        registerModifiersItem(map, item);
    }

    @Override
    public Map<BroomModifier, Float> getModifiersFromItem(ItemStack item) {
        for (Pair<Supplier<ItemStack>, Map<BroomModifier, Float>> entry : broomItems) {
            if (ItemStack.isSameItemSameComponents(item, entry.getKey().get())) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public Map<ItemStack, Float> getItemsFromModifier(BroomModifier modifier) {
        Map<ItemStack, Float> modifiers = Maps.newHashMap();
        for (Pair<Supplier<ItemStack>, Map<BroomModifier, Float>> entry : broomItems) {
            for (Map.Entry<BroomModifier, Float> itModifiers : entry.getValue().entrySet()) {
                if (itModifiers.getKey() == modifier) {
                    modifiers.put(entry.getKey().get(), itModifiers.getValue());
                }
            }
        }
        return modifiers;
    }

    @Override
    public Collection<BroomModifier> getModifiers() {
        return Collections.unmodifiableCollection(broomModifiers.values());
    }

    @Override
    public Map<BroomModifier, Float> getModifiers(ItemStack broomStack) {
        if(broomStack != null) {
            Map<BroomModifier, Float> modifiers = Maps.newHashMap();

            // Base values
            for (BroomModifier modifier : getModifiers()) {
                if (modifier.isBaseModifier()) {
                    modifiers.put(modifier, modifier.getDefaultValue());
                }
            }

            // Hardcoded values
            if(broomStack.has(RegistryEntries.COMPONENT_BROOM_MODIFIERS)) {
                modifiers.putAll(broomStack.get(RegistryEntries.COMPONENT_BROOM_MODIFIERS).getModifiers());
            }

            return modifiers;
        }
        return Collections.emptyMap();
    }

    @Override
    public void setModifiers(ItemStack broomStack, Map<BroomModifier, Float> modifiers) {
        // Write modifiers
        broomStack.set(RegistryEntries.COMPONENT_BROOM_MODIFIERS, new BroomModifiersContents(modifiers));

        // Write corresponding modifier parts
        Collection<IBroomPart> parts = BroomParts.REGISTRY.getBroomParts(broomStack);
        for (Map.Entry<BroomModifier, Float> entry : modifiers.entrySet()) {
            if (entry.getValue() > 0) {
                IBroomPart part = getModifierPart(entry.getKey());
                int tier = BroomModifier.getTier(entry.getKey(), entry.getValue());
                if (part != null) {
                    for (int i = 0; i < tier; i++) {
                        parts.add(part);
                    }
                }
            }
        }
        BroomParts.REGISTRY.setBroomParts(broomStack, parts);
    }

    public void onTooltipEvent(ItemTooltipEvent event) {
        if (ItemBroomConfig.broomModifierTooltips) {
            Map<BroomModifier, Float> modifiers = getModifiersFromItem(event.getItemStack());
            if (modifiers != null) {
                if (MinecraftHelpers.isShifted()) {
                    event.getToolTip().add(Component.translatable("broom.modifiers." + Reference.MOD_ID + ".types")
                            .withStyle(ChatFormatting.ITALIC));
                    for (Map.Entry<BroomModifier, Float> entry : modifiers.entrySet()) {
                        event.getToolTip().add(entry.getKey().getTooltipLine("  ", entry.getValue(), 0, false));
                    }
                } else {
                    event.getToolTip().add(Component.translatable("broom.modifiers." + Reference.MOD_ID + ".shiftinfo")
                            .withStyle(ChatFormatting.ITALIC));
                }
            }
        }
    }

    public void beforeItemsRegistered(RegisterEvent event) {
        // The block registry even is called before the items event
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            broomModifiers.clear();
            broomModifierParts.clear();
            broomItems.clear();
        }
    }
}
