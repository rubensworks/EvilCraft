package org.cyclops.evilcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.cyclops.cyclopscore.config.extendedconfig.BlockConfig;
import org.cyclops.evilcraft.EvilCraft;
import org.cyclops.evilcraft.RegistryEntries;

/**
 * Config for the Stripped Undead Wood.
 * @author rubensworks
 *
 */
public class BlockUndeadWoodStrippedConfig extends BlockConfig {

    public BlockUndeadWoodStrippedConfig() {
        super(
                EvilCraft._instance,
            "undead_wood_stripped",
                eConfig -> new RotatedPillarBlock(Block.Properties.of()
                        .mapColor(MapColor.TERRACOTTA_ORANGE)
                        .strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD)) {
                    @Override
                    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                        return 5;
                    }

                    @Override
                    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                        return 20;
                    }
                },
                getDefaultItemConstructor(EvilCraft._instance)
        );
        NeoForge.EVENT_BUS.addListener(this::toolActionEvent);
    }

    public void toolActionEvent(BlockEvent.BlockToolModificationEvent event) {
        if (event.getItemAbility() == ItemAbilities.AXE_STRIP && event.getState().getBlock() == RegistryEntries.BLOCK_UNDEAD_WOOD.get()) {
            BlockState blockStateNew = RegistryEntries.BLOCK_UNDEAD_WOOD_STRIPPED.get().defaultBlockState();
            for (Property property : event.getState().getProperties()) {
                if(blockStateNew.hasProperty(property))
                    blockStateNew = blockStateNew.setValue(property, event.getState().getValue(property));
            }
            event.setFinalState(blockStateNew);
        }
    }

}
