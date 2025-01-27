package org.cyclops.evilcraft.entity.monster;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.cyclops.cyclopscore.config.extendedconfig.EntityConfig;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.cyclopscore.helper.MinecraftHelpers;
import org.cyclops.evilcraft.EvilCraft;
import org.cyclops.evilcraft.Reference;
import org.cyclops.evilcraft.client.render.entity.ModelWerewolf;
import org.cyclops.evilcraft.client.render.entity.RenderWerewolf;

/**
 * Config for the {@link EntityWerewolf}.
 * @author rubensworks
 *
 */
public class EntityWerewolfConfig extends EntityConfig<EntityWerewolf> {

    @OnlyIn(Dist.CLIENT)
    public static ModelLayerLocation MODEL;

    public EntityWerewolfConfig() {
        super(
                EvilCraft._instance,
                "werewolf",
                eConfig -> EntityType.Builder.<EntityWerewolf>of(EntityWerewolf::new, MobCategory.MONSTER)
                        .sized(0.6F, 2.9F),
                getDefaultSpawnEggItemConfigConstructor(EvilCraft._instance, "werewolf_spawn_egg", Helpers.RGBToInt(105, 67, 18), Helpers.RGBToInt(57, 25, 10))
        );
        EvilCraft._instance.getModEventBus().addListener(this::onEntityAttributeCreationEvent);
        if (MinecraftHelpers.isClientSide()) {
            ModelLoader.registerModel();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void onForgeRegistered() {
        super.onForgeRegistered();
        ClientHooks.registerLayerDefinition(MODEL, ModelWerewolf::createBodyLayer);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public EntityRenderer<? super EntityWerewolf> getRender(EntityRendererProvider.Context renderContext, ItemRenderer itemRenderer) {
        return new RenderWerewolf(renderContext, this, new ModelWerewolf(renderContext.bakeLayer(MODEL)), 0.5F);
    }

    public void onEntityAttributeCreationEvent(EntityAttributeCreationEvent event) {
        event.put(getInstance(), Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 35.0D)
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D)
                .build());
    }

    public static class ModelLoader {
        public static Object registerModel() {
            MODEL = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "werewolf"), "main");
            return null;
        }
    }

}
