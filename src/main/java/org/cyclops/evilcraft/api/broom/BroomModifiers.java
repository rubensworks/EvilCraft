package org.cyclops.evilcraft.api.broom;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.tags.ITag;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.evilcraft.EvilCraft;
import org.cyclops.evilcraft.ExtendedDamageSources;
import org.cyclops.evilcraft.Reference;
import org.cyclops.evilcraft.RegistryEntries;
import org.cyclops.evilcraft.core.broom.BroomParts;
import org.cyclops.evilcraft.core.broom.PotionEffectBroomCollision;
import org.cyclops.evilcraft.entity.item.EntityBroom;
import org.cyclops.evilcraft.item.ItemBroomConfig;
import org.cyclops.evilcraft.item.ItemMaceOfDistortion;

/**
 * A list of broom modifiers.
 * @author rubensworks
 */
public class BroomModifiers {

    public static final IBroomModifierRegistry REGISTRY = EvilCraft._instance.getRegistryManager().getRegistry(IBroomModifierRegistry.class);

    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(EventPriority.LOWEST, BroomModifiers::afterItemsRegistered);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(EventPriority.HIGHEST, BroomModifiers::afterAfterItemsRegistered);
        MinecraftForge.EVENT_BUS.addListener(BroomModifiers::onTagsUpdated);
        MinecraftForge.EVENT_BUS.addListener(BroomModifiers::onLivingHurt);
    }

    public static BroomModifier MODIFIER_COUNT;
    public static BroomModifier SPEED;
    public static BroomModifier ACCELERATION;
    public static BroomModifier MANEUVERABILITY;
    public static BroomModifier LEVITATION;

    public static BroomModifier DAMAGE;
    public static BroomModifier PARTICLES;
    public static BroomModifier FLAME;
    public static BroomModifier SMASH;
    public static BroomModifier BOUNCY;
    public static BroomModifier WITHERER;
    public static BroomModifier HUNGERER;
    public static BroomModifier KAMIKAZE;
    public static BroomModifier WITHERSHIELD;
    public static BroomModifier STURDYNESS;
    //public static BroomModifier LUCK;
    public static BroomModifier EFFICIENCY;
    public static BroomModifier SWIMMING;
    public static BroomModifier ICY;
    public static BroomModifier STICKY;

    public static void afterItemsRegistered(RegisterEvent event) {
        if (event.getRegistryKey().equals(ForgeRegistries.Keys.ITEMS)) {
            BroomParts.loadPre();
            loadPre();
        }
    }

    public static void afterAfterItemsRegistered(RegisterEvent event) {
        if (event.getRegistryKey().equals(ForgeRegistries.Keys.POTIONS)) {
            BroomParts.loadPost();
        }
    }

    public static void onTagsUpdated(TagsUpdatedEvent event) {
        loadPost();
    }

    protected static void loadPre() {
        // Base modifiers
        MODIFIER_COUNT = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "modifier_count"),
                BroomModifier.Type.ADDITIVE, 0F, 1F, 3, true,
                ChatFormatting.BOLD, Helpers.RGBToInt(0, 0, 0)));
        REGISTRY.overrideDefaultModifierPart(MODIFIER_COUNT, null);
        SPEED = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "speed"),
                BroomModifier.Type.ADDITIVE, 0F, 100F, 3, true,
                ChatFormatting.RED, Helpers.RGBToInt(230, 20, 20)));
        ACCELERATION = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "acceleration"),
                BroomModifier.Type.ADDITIVE, 0F, 100F, 3, true,
                ChatFormatting.DARK_GRAY, Helpers.RGBToInt(20, 20, 20)));
        MANEUVERABILITY = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "maneuverability"),
                BroomModifier.Type.ADDITIVE, 0F, 100F, 3, true,
                ChatFormatting.YELLOW, Helpers.RGBToInt(160, 160, 20)));
        LEVITATION = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "levitation"),
                BroomModifier.Type.ADDITIVE, 0F, 100F, 3, true,
                ChatFormatting.WHITE, Helpers.RGBToInt(230, 230, 230)));

        // Optional modifiers
        DAMAGE = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "damage"),
                BroomModifier.Type.ADDITIVE, 0F, 100F, 3, false,
                ChatFormatting.GRAY, Helpers.RGBToInt(100, 100, 100)));
        PARTICLES = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "particles"),
                BroomModifier.Type.ADDITIVE, 0F, 50F, 1, false,
                ChatFormatting.LIGHT_PURPLE, Helpers.RGBToInt(160, 20, 160)));
        FLAME = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "flame"),
                BroomModifier.Type.ADDITIVE, 0F, 4F, 3, false,
                ChatFormatting.GOLD, Helpers.RGBToInt(100, 100, 0)));
        SMASH = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "smash"),
                BroomModifier.Type.ADDITIVE, 0F, 10F, 2, false,
                ChatFormatting.AQUA, Helpers.RGBToInt(20, 60, 60)));
        BOUNCY = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "bouncy"),
                BroomModifier.Type.ADDITIVE, 0F, 10F, 3, false,
                ChatFormatting.GREEN, Helpers.RGBToInt(20, 200, 60)));
        WITHERER = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "witherer"),
                BroomModifier.Type.ADDITIVE, 0F, 10F, 3, false,
                ChatFormatting.DARK_GRAY, Helpers.RGBToInt(20, 20, 20)));
        HUNGERER = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "hungerer"),
                BroomModifier.Type.ADDITIVE, 0F, 10F, 3, false,
                ChatFormatting.DARK_GREEN, Helpers.RGBToInt(20, 120, 20)));
        KAMIKAZE = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "kamikaze"),
                BroomModifier.Type.ADDITIVE, 0F, 10F, 3, false,
                ChatFormatting.DARK_GREEN, Helpers.RGBToInt(20, 120, 20)));
        WITHERSHIELD = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "withershield"),
                BroomModifier.Type.ADDITIVE, 0F, 5F, 4, false,
                ChatFormatting.DARK_BLUE, Helpers.RGBToInt(20, 20, 120)));
        STURDYNESS = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "sturdyness"),
                BroomModifier.Type.ADDITIVE, 0F, 100F, 3, false,
                ChatFormatting.GRAY, Helpers.RGBToInt(100, 100, 100)));
        /*LUCK = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "luck"),
                BroomModifier.Type.ADDITIVE, 0F, 100F, 3, false,
                TextFormatting.BLUE, Helpers.RGBToInt(30, 20, 210)));*/
        EFFICIENCY = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "efficiency"),
                BroomModifier.Type.ADDITIVE, 0F, 10F, 3, false,
                ChatFormatting.DARK_RED, Helpers.RGBToInt(92, 29, 29)));
        SWIMMING = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "swimming"),
                BroomModifier.Type.ADDITIVE, 0F, 100F, 3, false,
                ChatFormatting.AQUA, Helpers.RGBToInt(150, 150, 235)));
        ICY = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "icy"),
                BroomModifier.Type.ADDITIVE, 0F, 10F, 3, false,
                ChatFormatting.WHITE, Helpers.RGBToInt(220, 220, 240)));
        STICKY = REGISTRY.registerModifier(new BroomModifier(
                new ResourceLocation(Reference.MOD_ID, "sticky"),
                BroomModifier.Type.ADDITIVE, 0F, 10F, 3, false,
                ChatFormatting.GOLD, Helpers.RGBToInt(78, 58, 12)));

        // Set modifier events
        DAMAGE.addCollisionListener(new BroomModifier.ICollisionListener() {
            @Override
            public void onCollide(EntityBroom broom, Entity entity, float modifierValue) {
                float damage = (modifierValue * (float) broom.getLastPlayerSpeed()) / 50F;
                if (damage > 0) {
                    LivingEntity passenger = broom.getControllingPassenger();
                    if (passenger != null) {
                        entity.hurt(ExtendedDamageSources.broomDamage(passenger), damage);
                    }
                }
            }
        });
        FLAME.addCollisionListener(new BroomModifier.ICollisionListener() {
            @Override
            public void onCollide(EntityBroom broom, Entity entity, float modifierValue) {
                if (modifierValue > 0) {
                    entity.setSecondsOnFire((int) modifierValue);
                }
            }
        });
        SMASH.addTickListener(new BroomModifier.ITickListener() {
            @Override
            public void onTick(EntityBroom broom, float modifierValue) {
                double pitch = ((broom.getXRot() + 90) * Math.PI) / 180;
                double yaw = ((broom.getYRot() + 90) * Math.PI) / 180;
                double x = Math.sin(pitch) * Math.cos(yaw);
                double z = Math.sin(pitch) * Math.sin(yaw);
                double y = Math.cos(pitch);

                double r = -0.1D;
                BlockPos blockpos = BlockPos.containing(
                        broom.getBoundingBox().minX + x + r,
                        broom.getBoundingBox().minY + y + r,
                        broom.getBoundingBox().minZ + z + r);
                BlockPos blockpos1 = BlockPos.containing(
                        broom.getBoundingBox().maxX + x - r,
                        broom.getBoundingBox().maxY + y - r + 1D,
                        broom.getBoundingBox().maxZ + z - r);
                Level world = broom.level();
                float maxHardness = modifierValue;
                float toughnessModifier = Math.min(1F, 0.5F + (broom.getModifier(BroomModifiers.STURDYNESS) / (BroomModifiers.STURDYNESS.getMaxTierValue() * 1.5F) / 2F));
                LivingEntity ridingEntity = broom.getControllingPassenger() instanceof LivingEntity ? (LivingEntity) broom.getControllingPassenger() : null;
                Player player = broom.getControllingPassenger() instanceof Player ? (Player) broom.getControllingPassenger() : null;

                if (world.hasChunksAt(blockpos, blockpos1)) {
                    for (int i = blockpos.getX(); i <= blockpos1.getX(); ++i) {
                        for (int j = blockpos.getY(); j <= blockpos1.getY(); ++j) {
                            for (int k = blockpos.getZ(); k <= blockpos1.getZ(); ++k) {
                                BlockPos pos = new BlockPos(i, j, k);
                                BlockState blockState = world.getBlockState(pos);
                                FluidState fluidState = world.getFluidState(pos);
                                Block block = blockState.getBlock();
                                if (!world.isEmptyBlock(pos) && broom.canConsume(ItemBroomConfig.bloodUsageBlockBreak, ridingEntity)) {
                                    float hardness = blockState.getDestroySpeed(world, pos);
                                    if (hardness > 0F && hardness <= maxHardness) {
                                        broom.consume(ItemBroomConfig.bloodUsageBlockBreak, ridingEntity);
                                        if (player == null) {
                                            // The mounted entity is no player, do regular block breaking
                                            world.destroyBlock(pos, true);
                                        } else {
                                            // The mounted entity is a player, apply fortune and drop xp
                                            // Inspired by TCon's block breaking code

                                            // Destroy the block
                                            if (!broom.level().isClientSide()) {
                                                ServerPlayer playerMp = (ServerPlayer) player;
                                                int expToDrop = ForgeHooks.onBlockBreakEvent(world, playerMp.gameMode.getGameModeForPlayer(), (ServerPlayer) player, pos);
                                                if (expToDrop >= 0) {
                                                    // Block breaking sequence
                                                    block.playerWillDestroy(world, pos, blockState, player);
                                                    if(block.onDestroyedByPlayer(blockState, world, pos, player, true, fluidState)) {
                                                        block.destroy(world, pos, blockState);
                                                        block.playerDestroy(world, player, pos, blockState, world.getBlockEntity(pos), ItemStack.EMPTY);
                                                        block.popExperience((ServerLevel) world, pos, expToDrop);
                                                    }

                                                    // Send block change packet to the client
                                                    playerMp.connection.send(new ClientboundBlockUpdatePacket(world, pos));
                                                }
                                            } else if (Minecraft.getInstance().hitResult.getType() == HitResult.Type.BLOCK) {
                                                // Play sound and client-side block breaking sequence
                                                world.globalLevelEvent(2001, pos, Block.getId(blockState));
                                                if(block.onDestroyedByPlayer(blockState, world, pos, player, true, fluidState)) {
                                                    block.destroy(world, pos, blockState);
                                                }

                                                // Tell the server we are done with breaking this block
                                                Minecraft.getInstance().getConnection().send(new ServerboundPlayerActionPacket(
                                                        ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos,
                                                        ((BlockHitResult) Minecraft.getInstance().hitResult).getDirection()));
                                            }
                                        }

                                        // Slow the broom down a bit
                                        broom.setLastPlayerSpeed(broom.getLastPlayerSpeed() * toughnessModifier);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
        BOUNCY.addCollisionListener(new BroomModifier.ICollisionListener() {
            @Override
            public void onCollide(EntityBroom broom, Entity entity, float modifierValue) {
                float power = (modifierValue * (float) broom.getLastPlayerSpeed()) / 20F;
                if (power > 0) {
                    double dx = entity.getX() - broom.getX();
                    double dy = entity.getY() + (double)entity.getEyeHeight() - broom.getY();
                    double dz = entity.getZ() - broom.getZ();
                    double d = (double) Mth.sqrt((float) (dx * dx + dy * dy + dz * dz));
                    if (d != 0.0D) {
                        dx /= d;
                        dy /= d;
                        dz /= d;
                        entity.setDeltaMovement(entity.getDeltaMovement()
                                .add(dx, dy, dz)
                                .multiply(power, power, power));
                        if (broom.level().isClientSide()) {
                            ItemMaceOfDistortion.showEntityDistored(broom.level(), null, entity, (int) (power / 10F));
                        }
                    }
                }
            }
        });
        WITHERER.addCollisionListener(new PotionEffectBroomCollision(MobEffects.WITHER));
        HUNGERER.addCollisionListener(new PotionEffectBroomCollision(MobEffects.HUNGER));
        KAMIKAZE.addCollisionListener(new BroomModifier.ICollisionListener() {
            @Override
            public void onCollide(EntityBroom broom, Entity entity, float modifierValue) {
                Level world = broom.level();
                float power = (modifierValue * (float) broom.getLastPlayerSpeed()) / 5F;
                if (power > 0 && broom.getControllingPassenger() != null) {
                    broom.lastMounted = null;
                    broom.getControllingPassenger().stopRiding();
                    world.explode(null, broom.getX(), broom.getY(), broom.getZ(), power, Level.ExplosionInteraction.TNT);
                }
            }
        });
        ICY.addCollisionListener(new PotionEffectBroomCollision(MobEffects.MOVEMENT_SLOWDOWN, 2));
        STICKY.addCollisionListener(new BroomModifier.ICollisionListener() {
            @Override
            public void onCollide(EntityBroom broom, Entity entity, float modifierValue) {
                if (!entity.level().isClientSide() && !entity.isPassenger() && entity.canRide(broom)) {
                    // Will fail if max passenger count is exceeded
                    entity.startRiding(broom);
                }
            }
        });
    }

    protected static void loadPost() {
        // Clear the registry, as this can be called multiple times
        REGISTRY.clearModifierItems();

        REGISTRY.registerModifiersItem(MODIFIER_COUNT, 1F, new ItemStack(Items.NETHER_STAR));
        REGISTRY.registerModifiersItem(MODIFIER_COUNT, 1F, new ItemStack(RegistryEntries.ITEM_GARMONBOZIA));

        REGISTRY.registerModifiersItem(SPEED, 1F, new ItemStack(Items.REDSTONE));
        REGISTRY.registerModifiersItem(SPEED, 9F, new ItemStack(Blocks.REDSTONE_BLOCK));

        REGISTRY.registerModifiersItem(ACCELERATION, 1F, new ItemStack(Items.COAL));
        REGISTRY.registerModifiersItem(ACCELERATION, 9F, new ItemStack(Blocks.COAL_BLOCK));

        REGISTRY.registerModifiersItem(MANEUVERABILITY, 2F, new ItemStack(Items.GLOWSTONE_DUST));
        REGISTRY.registerModifiersItem(MANEUVERABILITY, 8F, new ItemStack(Blocks.GLOWSTONE));

        REGISTRY.registerModifiersItem(LEVITATION, 1F, new ItemStack(Items.FEATHER));
        REGISTRY.registerModifiersItem(LEVITATION, 50F, new ItemStack(Items.PHANTOM_MEMBRANE));

        REGISTRY.registerModifiersItem(DAMAGE, 2F, new ItemStack(RegistryEntries.ITEM_DARK_SPIKE));
        REGISTRY.registerModifiersItem(DAMAGE, 1F, new ItemStack(Items.QUARTZ));

        REGISTRY.registerModifiersItem(PARTICLES, 1F, new ItemStack(Items.GUNPOWDER));

        REGISTRY.registerModifiersItem(FLAME, 1F, new ItemStack(Items.BLAZE_POWDER));

        REGISTRY.registerModifiersItem(SMASH, 1F, new ItemStack(Items.IRON_PICKAXE));
        REGISTRY.registerModifiersItem(SMASH, 5F, new ItemStack(Items.DIAMOND_PICKAXE));

        REGISTRY.registerModifiersItem(BOUNCY, 1F, new ItemStack(Items.SLIME_BALL));
        REGISTRY.registerModifiersItem(BOUNCY, 9F, new ItemStack(Blocks.SLIME_BLOCK));

        registerModifierTagItem(STURDYNESS, 1F, new ResourceLocation("forge", "stone"));
        REGISTRY.registerModifiersItem(STURDYNESS, 10F, new ItemStack(Blocks.OBSIDIAN));

        registerModifierTagItem(EFFICIENCY, 1F, new ResourceLocation(Reference.MOD_ID, "gems/dark_power"));

        REGISTRY.registerModifiersItem(SWIMMING, 1F, new ItemStack(Items.PRISMARINE_SHARD));
        REGISTRY.registerModifiersItem(SWIMMING, 4F, new ItemStack(Blocks.PRISMARINE));
        REGISTRY.registerModifiersItem(SWIMMING, 9F, new ItemStack(Blocks.DARK_PRISMARINE));
        REGISTRY.registerModifiersItem(SWIMMING, 20F, new ItemStack(Items.NAUTILUS_SHELL));
        REGISTRY.registerModifiersItem(SWIMMING, 25F, new ItemStack(Items.TURTLE_HELMET));

        REGISTRY.registerModifiersItem(ICY, 1F, new ItemStack(Blocks.ICE));
        REGISTRY.registerModifiersItem(ICY, 5F, new ItemStack(Blocks.PACKED_ICE));
        REGISTRY.registerModifiersItem(ICY, 10F, new ItemStack(Blocks.BLUE_ICE));

        REGISTRY.registerModifiersItem(STICKY, 1F, new ItemStack(Items.SEAGRASS));
        REGISTRY.registerModifiersItem(STICKY, 2F, new ItemStack(Items.KELP));
        REGISTRY.registerModifiersItem(STICKY, 4F, new ItemStack(Items.HONEY_BOTTLE));
        REGISTRY.registerModifiersItem(STICKY, 16F, new ItemStack(Items.HONEY_BLOCK));

        EvilCraft.clog(String.format("%s Broom modifiers can be applied!", REGISTRY.getModifiers().size()));
    }

    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() != null && event.getEntity().getVehicle() instanceof EntityBroom
                && event.getSource().getDirectEntity() instanceof Projectile) {
            EntityBroom broom = (EntityBroom) event.getEntity().getVehicle();
            float modifierValue = broom.getModifier(BroomModifiers.WITHERSHIELD);
            if (modifierValue > 0 && modifierValue > broom.level().random.nextInt((int) BroomModifiers.WITHERSHIELD.getMaxTierValue())) {
                event.setCanceled(true);
            }
        }
    }

    public static void registerModifierTagItem(BroomModifier modifier, float value, ResourceLocation name) {
        ITag<Item> tag = ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registries.ITEM, name));
        if (!tag.isEmpty()) {
            for (Item item : tag) {
                REGISTRY.registerModifiersItem(modifier, value, new ItemStack(item));
            }
        } else {
            EvilCraft.clog(String.format("Broom modifiers could not find a tag instance for %s", name), org.apache.logging.log4j.Level.WARN);
        }
    }

}
