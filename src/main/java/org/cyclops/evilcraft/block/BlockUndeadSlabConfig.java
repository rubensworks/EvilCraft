package org.cyclops.evilcraft.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import org.cyclops.cyclopscore.config.extendedconfig.BlockConfig;
import org.cyclops.cyclopscore.helper.BlockHelpers;
import org.cyclops.evilcraft.EvilCraft;

/**
 * Config for the Undead Slab.
 * @author rubensworks
 *
 */
public class BlockUndeadSlabConfig extends BlockConfig {

    public BlockUndeadSlabConfig() {
        super(
                EvilCraft._instance,
            "undead_slab",
                eConfig -> new SlabBlock(BlockBehaviour.Properties.of(Material.WOOD, MaterialColor.TERRACOTTA_ORANGE)
                        .strength(2.0F, 3.0F)
                        .sound(SoundType.WOOD)),
                getDefaultItemConstructor(EvilCraft._instance)
        );
    }
    
    @Override
    public void onForgeRegistered() {
        super.onForgeRegistered();
        BlockHelpers.setFireInfo(getInstance(), 5, 20);
    }
    
}
