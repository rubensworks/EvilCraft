package org.cyclops.evilcraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraftforge.common.ToolType;
import org.cyclops.cyclopscore.config.extendedconfig.BlockConfig;
import org.cyclops.evilcraft.EvilCraft;

/**
 * Config for the {@link BlockDark}.
 * @author rubensworks
 *
 */
public class BlockDarkConfig extends BlockConfig {

    public BlockDarkConfig() {
        super(
                EvilCraft._instance,
            "dark",
                eConfig -> new BlockDark(Block.Properties.create(Material.ROCK)
                        .hardnessAndResistance(5.0F)
                        .sound(SoundType.METAL)
                        .harvestTool(ToolType.PICKAXE)
                        .harvestLevel(2)),
                getDefaultItemConstructor(EvilCraft._instance)
        );
    }
    
}