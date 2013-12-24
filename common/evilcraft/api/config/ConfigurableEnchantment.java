package evilcraft.api.config;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import evilcraft.Reference;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.util.StatCollector;

public class ConfigurableEnchantment extends Enchantment implements Configurable {

    protected ExtendedConfig eConfig = null;
    
    public static ElementType TYPE = ElementType.ENCHANTMENT;
    
    protected ConfigurableEnchantment(ExtendedConfig eConfig, int par1, int par2,
            EnumEnchantmentType par3EnumEnchantmentType) {
        super(par1, par2, par3EnumEnchantmentType);
        eConfig.ID = this.effectId; // Initialize this ID
        this.setConfig(eConfig);
        this.setName(this.getUniqueName());
        
    }
    
    // Set a configuration for this enchantment
    public void setConfig(ExtendedConfig eConfig) {
        this.eConfig = eConfig;
    }
    
    public String getUniqueName() {
        return "enchantments."+eConfig.NAMEDID;
    }
    
    public boolean isEntity() {
        return false;
    }
    
    public String getTranslatedName(int level) {
        String enchantmentName = eConfig.NAME;
        return enchantmentName + " " + StatCollector.translateToLocal("enchantment.level." + level);
    }

}
