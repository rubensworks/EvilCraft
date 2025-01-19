package org.cyclops.evilcraft.entity.item;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.network.NetworkHooks;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.cyclopscore.helper.MinecraftHelpers;
import org.cyclops.evilcraft.RegistryEntries;
import org.cyclops.evilcraft.api.broom.BroomModifier;
import org.cyclops.evilcraft.api.broom.BroomModifiers;
import org.cyclops.evilcraft.api.broom.IBroom;
import org.cyclops.evilcraft.client.particle.ParticleColoredSmokeData;
import org.cyclops.evilcraft.core.broom.BroomParts;
import org.cyclops.evilcraft.core.helper.MathHelpers;
import org.cyclops.evilcraft.item.ItemBroomConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Entity for a broom
 *
 * @author immortaleeb
 *
 */
public class EntityBroom extends Entity {

    private static final EntityDataAccessor<ItemStack> ITEMSTACK_INDEX = SynchedEntityData.<ItemStack>defineId(EntityBroom.class, EntityDataSerializers.ITEM_STACK);

    /**
     * Speed for the broom (in all directions)
     */
    public static final double SPEED = 0.4;

    /**
     * Maximum angle of the broom between the XZ-plane and the Y-axis
     * (in degrees, -90 = completely up, +90 = completely down)
     * This limits the angle under which the player can move up or down
     */
    public static final float MAX_ANGLE = 60.0F;
    /**
     * Minimum angle of the broom between the XZ-plane and the Y-axis
     * (in degrees, -90 = completely up, +90 = completely down)
     * This limits the angle under which the player can move up or down
     */
    public static final float MIN_ANGLE = -60.0F;

    /**
     * Maximum amplitude of the cosine functions which generate the hovering effect
     */
    private static final float MAX_COS_AMPLITUDE = 0.2f;

    /**
     * The player that last mounted this broom (used to detect dismounting)
     */
    public LivingEntity lastMounted = null;

    private boolean setLast = false;
    private double lastPlayerSpeed = 0D;
    private float lastRotationPitch = -1F;
    private float lastRotationYaw = -1F;

    private double newPosX;
    private double newPosY;
    private double newPosZ;
    private double newRotationYaw;
    private double newRotationPitch;
    private int newPosRotationIncrements;

    // This value adds a random value to the world tick in the calculations of the hover offset of a broom
    // This makes sure that all brooms don't reach the highest and lowest hovering points at the same time
    private int broomHoverTickOffset;

    /**
     * This variable holds the last hover offset
     * that was set by this broom during its last
     * update.
     */
    @OnlyIn(Dist.CLIENT)
    private double oldHoverOffset;

    private Map<BroomModifier, Float> cachedModifiers = null;

    public EntityBroom(EntityType<? extends EntityBroom> type, Level world) {
        this(type, world, 0.0, 0.0, 0.0);
        initBroomHoverTickOffset();
    }

    public EntityBroom(EntityType<? extends EntityBroom> type, Level world, double x, double y, double z) {
        super(type, world);
        setPos(x, y, z);
        setDeltaMovement(0, 0, 0);
        refreshDimensions();
        this.xo = x;
        this.yo = y;
        this.zo = z;
        initBroomHoverTickOffset();
    }

    public EntityBroom(Level world, double x, double y, double z) {
        this(RegistryEntries.ENTITY_BROOM, world, x, y, z);
    }

    @Nonnull
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public EntityDimensions getDimensions(Pose poseIn) {
        return EntityDimensions.scalable(1.5f, 0.6f);
    }

    protected void initBroomHoverTickOffset() {
        broomHoverTickOffset = random.nextInt((int)Math.PI);
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0;
    }

    @Override
    public boolean isPickable() {
        return isAlive();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && !isVehicle() && !player.isCrouching()) {
            player.startRiding(this);
            lastMounted = player;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void push(Entity entityIn) {
        if (!this.level().isClientSide()) {
            if (!entityIn.noPhysics && !this.noPhysics) {
                Entity controlling = this.getControllingPassenger();
                if (entityIn != controlling) {
                    if (entityIn instanceof LivingEntity
                            && !(entityIn instanceof Player)
                            && controlling == null
                            && !entityIn.isPassenger()) {
                        entityIn.startRiding(this);
                    }
                }
            }
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean isTeleport) {
        posRotationIncrements += 6;

        //this.yOffset = 0.0F;
        this.newPosX = x;
        this.newPosY = y;
        this.newPosZ = z;
        this.newRotationYaw = (double)yaw;
        this.newRotationPitch = (double)pitch;
        this.newPosRotationIncrements = posRotationIncrements;
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        List<Entity> list = this.getPassengers();
        return list.isEmpty() ? null : (LivingEntity) list.get(0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide() && this.isAlive()) {
            if (this.isInvulnerableTo(source)) {
                return false;
            }
            if (source.getEntity() instanceof Player && source.getEntity() != lastMounted) {
                if (isVehicle()) {
                    this.stopRiding();
                } else {
                    remove(RemovalReason.DISCARDED);
                    Player player = (Player) source.getEntity();
                    if (!player.isCreative()) {
                        ItemStack itemStack = getBroomStack().copy();
                        this.spawnAtLocation(itemStack, 0.0F);
                    }
                }
            }
        }
        return true;
    }

    public void startAllowFlying(LivingEntity entity) {
        if(entity instanceof Player) {
            ((Player) entity).getAbilities().mayfly = true;
        }
    }

    public void stopAllowFlying(LivingEntity entity) {
        if(entity instanceof Player) {
            ((Player) entity).getAbilities().mayfly = false;
        }
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if(!level().isClientSide() && passenger instanceof LivingEntity) {
            startAllowFlying((LivingEntity) passenger);
        }
    }

    @Override
    public void tick() {
        super.tick();

        Entity rider = getControllingPassenger();
        if (!level().isClientSide() && !isVehicle() && lastMounted != null) {
            onDismount();

        } else if (rider instanceof LivingEntity) {
            /*
             * If we ever have the problem that a player can dismount without
             * getting the broom back in his inventory and removing the entity from the world
             * its probably because of this next line of code because onUpdate() is called AFTER
             * the player is dismounted, thus lastMounted is not updated before the player dismounts
             * and thus the dismounting code is never executed
             */
            lastMounted = (LivingEntity) rider;

            xo = getX();
            yo = getY();
            zo = getZ();
            xRotO = getXRot();
            yRotO = getYRot();

            if (!level().isClientSide() || Minecraft.getInstance().player == lastMounted) {
                updateMountedServer();
            } else {
                updateMountedClient();
            }

            if(MinecraftHelpers.isClientSide() && getModifier(BroomModifiers.PARTICLES) > 0) {
                showParticles(this);
            }

            // Apply collisions
            List<Entity> list = this.level().getEntities(this, this.getBoundingBox().inflate(0.2, 0.0, 0.2));
            if (list != null && !list.isEmpty()) {
                for (int l = 0; l < list.size(); ++l) {
                    Entity entity = list.get(l);
                    if (entity != rider && entity.isPushable() && !(entity instanceof EntityBroom)) {
                        for (Map.Entry<BroomModifier, Float> entry : getModifiers().entrySet()) {
                            for (BroomModifier.ICollisionListener listener : entry.getKey().getCollisionListeners()) {
                                listener.onCollide(this, entity, entry.getValue());
                            }
                        }
                        entity.push(this);

                        // Slow the broom down a bit
                        float toughnessModifier = 0.5F + (getModifier(BroomModifiers.STURDYNESS) / BroomModifiers.STURDYNESS.getMaxTierValue()  / 2F);
                        setLastPlayerSpeed(getLastPlayerSpeed() * toughnessModifier);
                    }
                }
            }

            for (Map.Entry<BroomModifier, Float> entry : getModifiers().entrySet()) {
                for (BroomModifier.ITickListener listener : entry.getKey().getTickListeners()) {
                    listener.onTick(this, entry.getValue());
                }
            }
        } else {
            if(!this.level().isClientSide() && rider == null) {
                this.collideWithNearbyEntities();
            }
            updateUnmounted();
        }
    }

    private void onDismount() {
        // The player dismounted, give him his broom back if he's not in creative mode
        if (lastMounted instanceof Player) {
            stopAllowFlying(lastMounted);
            Player player = (Player) lastMounted;
            // Return to inventory if we have space and the player is not dead, otherwise drop it on the ground
            if (player.isAlive() && (!MinecraftHelpers.isPlayerInventoryFull(player) || player.isCreative())) {
                // Return to inventory if he's not in creative mode
                if (!player.isCreative()) {
                    player.getInventory().add(getBroomStack());
                }
                this.remove(RemovalReason.DISCARDED);
            }
        }

        lastMounted = null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < (1 + (getModifier(BroomModifiers.STICKY) / BroomModifiers.STICKY.getMaxTierValue()) * 3);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        if (!getCommandSenderWorld().isClientSide() && getControllingPassenger() == passenger) {
            onDismount();
        }
        super.removePassenger(passenger);
    }

    @OnlyIn(Dist.CLIENT)
    public void showParticles(EntityBroom broom) {
        Level world = broom.level();
        if (world.isClientSide() && broom.lastMounted.zza != 0) {
            // Emit particles
            int particles = (int) (broom.getModifier(BroomModifiers.PARTICLES) * (float) broom.getLastPlayerSpeed());
            Triple<Float, Float, Float> color = BroomModifier.getAverageColor(broom.getModifiers());
            for (int i = 0; i < particles; i++) {
                float r = color.getLeft();
                float g = color.getMiddle();
                float b = color.getRight();
                Vec3 motion = broom.getDeltaMovement();
                Minecraft.getInstance().levelRenderer.addParticle(
                        new ParticleColoredSmokeData(r, g, b), false,
                        broom.getX() - motion.x * 1.5D + Math.random() * 0.4D - 0.2D,
                        broom.getY() - motion.y * 1.5D + Math.random() * 0.4D - 0.2D,
                        broom.getZ() - motion.z * 1.5D + Math.random() * 0.4D - 0.2D,
                        motion.x / 10, motion.y / 10, motion.z / 10);
            }
        }
    }

    protected void collideWithNearbyEntities() {
        List<Entity> list = this.level().getEntities(this, this.getBoundingBox().inflate(0.20000000298023224D, 0.0D, 0.20000000298023224D),
                EntitySelector.NO_SPECTATORS.and(Entity::isPushable));
        if (!list.isEmpty()){
            for (Entity entity : list) {
                this.push(entity);
            }
        }
    }

    protected void updateMountedClient() {
        if (newPosRotationIncrements > 0) {
            double x = getX() + (newPosX - getX()) / newPosRotationIncrements;
            double y = getY() + (newPosY - getY()) / newPosRotationIncrements;
            double z = getZ() + (newPosZ - getZ()) / newPosRotationIncrements;

            float yaw = MathHelpers.normalizeAngle_180((float)(newRotationYaw - getYRot()));
            setYRot(getYRot() + yaw / newPosRotationIncrements);
            setXRot((float) (getXRot() + (newRotationPitch - getXRot()) / newPosRotationIncrements));

            newPosRotationIncrements--;

            setPos(x, y, z);
            setRot(getYRot(), getXRot());
        }

        move(MoverType.SELF, new Vec3(0, getHoverOffset(), 0));
    }

    public boolean canConsume(int amount, LivingEntity entityLiving) {
        ItemStack broomStack = getBroomStack();
        return broomStack != null && broomStack.getItem() instanceof IBroom
                && ((IBroom) broomStack.getItem()).canConsumeBroomEnergy(amount, broomStack, entityLiving);
    }

    public void consume(int amount, LivingEntity entityLiving) {
        float efficiencyFactor = Math.min(0.9F, Math.max(0.0F, getModifier(BroomModifiers.EFFICIENCY) / BroomModifiers.EFFICIENCY.getMaxTierValue()));
        if(level().random.nextFloat() > efficiencyFactor) {
            ItemStack broomStack = getBroomStack();
            ((IBroom) broomStack.getItem()).consumeBroom(amount, broomStack, entityLiving);
            setBroomStack(broomStack);
        }
    }

    /**
     * Called on the server side for all players or on the client side when the
     * player mounted on the broom is the local player, so movement is as smooth as
     * possible.
     */
    protected void updateMountedServer() {
        if (!setLast) {
            setLast = true;
            lastRotationYaw = getYRot();
            lastRotationPitch = getXRot();
        }

        if (lastMounted instanceof Player) {
            if (lastMounted instanceof ServerPlayer) {
                ((ServerPlayer) lastMounted).connection.aboveGroundVehicleTickCount = 0;
            }
        } else {
            lastMounted.setYRot(lastMounted.yHeadRot);
            // We have to hardcode this speed because the entity may already have ticked,
            // so we can't count on it having a predictable speed
            lastMounted.zza = 0.5F;
        }

        // Rotate broom
        setXRot(MathHelpers.normalizeAngle_180(lastMounted.getXRot()));
        setYRot(MathHelpers.normalizeAngle_180(lastMounted.getYRot()));

        // Apply maneuverability modifier
        float maneuverabilityFactor = 1F - (getModifier(BroomModifiers.MANEUVERABILITY) / 2000F);
        setXRot(getXRot() * (1F - maneuverabilityFactor) + lastRotationPitch * maneuverabilityFactor);
        // These if's are necessary to fix rotation when the yaw goes over the border of -180F;+180F
        if(lastRotationYaw - getYRot() > 180F) {
            lastRotationYaw -= 360F;
        }
        if(lastRotationYaw - getYRot() < -180F) {
            lastRotationYaw += 360F;
        }
        setYRot(getYRot() * (1F - maneuverabilityFactor) + lastRotationYaw * maneuverabilityFactor);
        lastRotationPitch = getXRot();
        lastRotationYaw = getYRot();

        // Limit the angle under which the player can move up or down
        if (getXRot() > MAX_ANGLE)
            setXRot(MAX_ANGLE);
        else if (getXRot() < MIN_ANGLE)
            setXRot(MIN_ANGLE);

        setRot(getYRot(), getXRot());

        // Handle player movement
        double pitch = ((getXRot() + 90) * Math.PI) / 180;
        double yaw = ((getYRot() + 90) * Math.PI) / 180;

        double x = Math.sin(pitch) * Math.cos(yaw);
        double z = Math.sin(pitch) * Math.sin(yaw);
        double y = Math.cos(pitch);

        // Apply speed modifier
        double playerSpeed = lastMounted.getAttribute(Attributes.MOVEMENT_SPEED).getValue();
        playerSpeed += getModifier(BroomModifiers.SPEED) / 100;
        int amount = ItemBroomConfig.bloodUsage;
        LivingEntity currentRidingEntity = getControllingPassenger() instanceof LivingEntity ? (LivingEntity) getControllingPassenger() : null;
        float moveForward = canConsume(amount, currentRidingEntity) ? lastMounted.zza : lastMounted.zza / 10F;
        playerSpeed *= moveForward;
        if(moveForward != 0) {
            consume(amount, currentRidingEntity);
        }

        // Apply acceleration modifier
        float slowingFactor = 1F - ((getModifier(BroomModifiers.ACCELERATION) + 1F) / 2500F);
        playerSpeed = playerSpeed * (1D - slowingFactor) + lastPlayerSpeed * slowingFactor;

        // Apply levitation modifier
        float levitation = getModifier(BroomModifiers.LEVITATION);
        float levitationModifier = (levitation / BroomModifiers.LEVITATION.getMaxTierValue()) * 1.5F;
        levitationModifier = Math.max(0.2F, levitationModifier);
        if (y < 0) {
            levitationModifier = Math.max(1.0F, levitationModifier);
        }

        // Save last speed (don't take into account water modifier)
        lastPlayerSpeed = playerSpeed;

        // Apply water speed
        if (this.wasTouchingWater) {
            // Apply a log-scale
            float waterMovementFactor = Mth.clamp(
                    getModifier(BroomModifiers.SWIMMING) / BroomModifiers.SWIMMING.getMaxTierValue(), 0F, 1F);
            waterMovementFactor = (float) Math.log10(1 + waterMovementFactor * 9);
            playerSpeed *= waterMovementFactor;
        }

        setDeltaMovement(getDeltaMovement()
                .multiply(0.1, 0.1, 0.1)
                .add(x * SPEED * playerSpeed, y * SPEED * playerSpeed * levitationModifier, z * SPEED * playerSpeed));

        // Update motion on client side to provide a hovering effect
        if (level().isClientSide()) {
            setDeltaMovement(getDeltaMovement().add(0, getHoverOffset(), 0));
        }

        move(MoverType.SELF, getDeltaMovement());
    }

    public double getLastPlayerSpeed() {
        return lastPlayerSpeed;
    }

    public void setLastPlayerSpeed(double lastPlayerSpeed) {
        this.lastPlayerSpeed = lastPlayerSpeed;
    }

    protected void updateUnmounted() {
        if (level().isClientSide()) {
            move(MoverType.SELF, new Vec3(0, getHoverOffset(), 0));
        }
    }

    protected double getHoverOffset() {
        float x = level().getGameTime();
        float t = broomHoverTickOffset;
        double newHoverOffset = Math.cos(x / 10 + t) * Math.cos(x / 12 + t) * Math.cos(x / 15 + t) * MAX_COS_AMPLITUDE;

        double newHoverDifference = newHoverOffset - oldHoverOffset;
        oldHoverOffset += newHoverDifference;

        return newHoverDifference;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource damageSource) {
        // Makes sure the player doesn't get any fall damage when on the broom
        return false;
    }

    @Override
    public boolean canBeRiddenUnderFluidType(FluidType type, Entity rider) {
        return getModifier(BroomModifiers.SWIMMING) > 0;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(ITEMSTACK_INDEX, new ItemStack(RegistryEntries.ITEM_BROOM));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbttagcompound) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbttagcompound) {

    }

    public void setBroomStack(ItemStack itemStack) {
        entityData.set(ITEMSTACK_INDEX, itemStack.copy());
    }

    public ItemStack getBroomStack() {
        ItemStack itemStack = entityData.get(ITEMSTACK_INDEX);
        if (itemStack.isEmpty()) {
            itemStack = new ItemStack(RegistryEntries.ITEM_BROOM);
        }
        itemStack.setCount(1);
        return itemStack;
    }

    public Map<BroomModifier, Float> getModifiers() {
        if (cachedModifiers == null) {
            cachedModifiers = Maps.newHashMap();
            ItemStack broomStack = getBroomStack();
            Map<BroomModifier, Float> modifiers = BroomModifiers.REGISTRY.getModifiers(broomStack);
            Map<BroomModifier, Float> baseModifiers = BroomParts.REGISTRY.getBaseModifiersFromBroom(broomStack);
            Set<BroomModifier> modifierTypes = Sets.newHashSet();
            modifierTypes.addAll(modifiers.keySet());
            modifierTypes.addAll(baseModifiers.keySet());

            for (BroomModifier modifier : modifierTypes) {
                float value = modifier.getDefaultValue();
                if (baseModifiers.containsKey(modifier)) {
                    value = baseModifiers.get(modifier);
                }
                if (modifiers.containsKey(modifier)) {
                    value = modifier.apply(value, Lists.newArrayList(modifiers.get(modifier)));
                }
                cachedModifiers.put(modifier, value);
            }
        }
        return cachedModifiers;
    }

    public float getModifier(BroomModifier modifier) {
        Float value = getModifiers().get(modifier);
        if(value == null) {
            return modifier.getDefaultValue();
        }
        return value;
    }

    @Override
    public CompoundTag saveWithoutId(CompoundTag tag) {
        tag = super.saveWithoutId(tag);
        CompoundTag broomItemTag = new CompoundTag();
        getBroomStack().save(broomItemTag);
        tag.put("broomItem", broomItemTag);
        return tag;
    }

    @Override
    public void load(CompoundTag tagCompound) {
        super.load(tagCompound);
        ItemStack broomStack = ItemStack.of(tagCompound.getCompound("broomItem"));
        if (!broomStack.isEmpty()) {
            setBroomStack(broomStack);
        }
    }
}
