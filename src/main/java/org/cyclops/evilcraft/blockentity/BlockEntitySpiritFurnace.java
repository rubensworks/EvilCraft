package org.cyclops.evilcraft.blockentity;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.cyclops.cyclopscore.block.multi.AllowedBlock;
import org.cyclops.cyclopscore.block.multi.CubeDetector;
import org.cyclops.cyclopscore.block.multi.HollowCubeDetector;
import org.cyclops.cyclopscore.block.multi.MaximumBlockCountValidator;
import org.cyclops.cyclopscore.block.multi.MinimumSizeValidator;
import org.cyclops.cyclopscore.capability.item.ItemHandlerSlotMasked;
import org.cyclops.cyclopscore.fluid.SingleUseTank;
import org.cyclops.cyclopscore.helper.EntityHelpers;
import org.cyclops.cyclopscore.helper.FluidHelpers;
import org.cyclops.cyclopscore.helper.LocationHelpers;
import org.cyclops.cyclopscore.inventory.SimpleInventory;
import org.cyclops.cyclopscore.inventory.slot.SlotFluidContainer;
import org.cyclops.cyclopscore.persist.nbt.NBTPersist;
import org.cyclops.evilcraft.RegistryEntries;
import org.cyclops.evilcraft.block.BlockBoxOfEternalClosure;
import org.cyclops.evilcraft.blockentity.tickaction.EmptyFluidContainerInTankTickAction;
import org.cyclops.evilcraft.blockentity.tickaction.spiritfurnace.BoxCookTickAction;
import org.cyclops.evilcraft.core.blockentity.BlockEntityWorking;
import org.cyclops.evilcraft.core.blockentity.tickaction.ITickAction;
import org.cyclops.evilcraft.core.blockentity.tickaction.TickComponent;
import org.cyclops.evilcraft.core.blockentity.upgrade.IUpgradeSensitiveEvent;
import org.cyclops.evilcraft.core.blockentity.upgrade.UpgradeBehaviour;
import org.cyclops.evilcraft.core.blockentity.upgrade.Upgrades;
import org.cyclops.evilcraft.core.fluid.BloodFluidConverter;
import org.cyclops.evilcraft.core.fluid.ImplicitFluidConversionTank;
import org.cyclops.evilcraft.inventory.container.ContainerSpiritFurnace;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A furnace that is able to cook spirits for their inner entity drops.
 * @author rubensworks
 *
 */
public class BlockEntitySpiritFurnace extends BlockEntityWorking<BlockEntitySpiritFurnace, MutableDouble> implements MenuProvider {

    /**
     * The id of the fluid container drainer slot.
     */
    public static final int SLOT_CONTAINER = 0;
    /**
     * The id of the box slot.
     */
    public static final int SLOT_BOX = 1;
    /**
     * The id of the drops slot.
     */
    public static final int[] SLOTS_DROP = new int[]{2, 3, 4, 5};
    /**
     * The total amount of slots in this machine.
     */
    public static final int SLOTS = 2 + SLOTS_DROP.length;

    public static Metadata METADATA = new Metadata();

    /**
     * The capacity of the tank.
     */
    public static final int LIQUID_PER_SLOT = FluidHelpers.BUCKET_VOLUME * 10;

    protected static final MinimumSizeValidator minimumSizeValidator = new MinimumSizeValidator(new Vec3i(2, 2, 2));

    private static CubeDetector detector;

    private static final Map<Class<?>, ITickAction<BlockEntitySpiritFurnace>> BOX_COOK_TICK_ACTIONS = new LinkedHashMap<Class<?>, ITickAction<BlockEntitySpiritFurnace>>();
    static {
        BOX_COOK_TICK_ACTIONS.put(BlockBoxOfEternalClosure.class, new BoxCookTickAction());
    }

    private static final Map<Class<?>, ITickAction<BlockEntitySpiritFurnace>> EMPTY_IN_TANK_TICK_ACTIONS = new LinkedHashMap<Class<?>, ITickAction<BlockEntitySpiritFurnace>>();
    static {
        EMPTY_IN_TANK_TICK_ACTIONS.put(Item.class, new EmptyFluidContainerInTankTickAction<BlockEntitySpiritFurnace>());
    }
    public static int TICKERS = 2;

    public static final Upgrades.UpgradeEventType UPGRADEEVENT_SPEED = Upgrades.newUpgradeEventType();
    public static final Upgrades.UpgradeEventType UPGRADEEVENT_BLOODUSAGE = Upgrades.newUpgradeEventType();

    @NBTPersist(useDefaultValue = false)
    private Vec3i size = LocationHelpers.copyLocation(Vec3i.ZERO);
    @NBTPersist
    private Boolean forceHalt = false;
    @NBTPersist
    private Boolean caughtError = false;
    private int cookTicker;
    private Entity boxEntityCache = null;

    public BlockEntitySpiritFurnace(BlockPos blockPos, BlockState blockState) {
        super(
                RegistryEntries.BLOCK_ENTITY_SPIRIT_FURNACE.get(),
                blockPos,
                blockState,
                SLOTS,
                64,
                LIQUID_PER_SLOT,
                RegistryEntries.FLUID_BLOOD.get());
        cookTicker = addTicker(new TickComponent<>(this, BOX_COOK_TICK_ACTIONS, SLOT_BOX, true, false));
        addTicker(new TickComponent<>(this, EMPTY_IN_TANK_TICK_ACTIONS, SLOT_CONTAINER, false, true));
        assert getTickers().size() == TICKERS;

        // Upgrade behaviour
        upgradeBehaviour.put(Upgrades.UPGRADE_SPEED, new UpgradeBehaviour<BlockEntitySpiritFurnace, MutableDouble>(1) {
            @Override
            public void applyUpgrade(BlockEntitySpiritFurnace upgradable, Upgrades.Upgrade upgrade, int upgradeLevel,
                                     IUpgradeSensitiveEvent<MutableDouble> event) {
                if(event.getType() == UPGRADEEVENT_SPEED) {
                    double val = event.getObject().getValue();
                    val /= (1 + upgradeLevel / valueFactor);
                    event.getObject().setValue(val);
                }
                if(event.getType() == UPGRADEEVENT_BLOODUSAGE) {
                    double val = event.getObject().getValue();
                    val *= (1 + upgradeLevel / valueFactor);
                    event.getObject().setValue(val);
                }
            }
        });
        upgradeBehaviour.put(Upgrades.UPGRADE_EFFICIENCY, new UpgradeBehaviour<BlockEntitySpiritFurnace, MutableDouble>(2) {
            @Override
            public void applyUpgrade(BlockEntitySpiritFurnace upgradable, Upgrades.Upgrade upgrade, int upgradeLevel,
                                     IUpgradeSensitiveEvent<MutableDouble> event) {
                if(event.getType() == UPGRADEEVENT_BLOODUSAGE) {
                    double val = event.getObject().getValue();
                    val /= (1 + upgradeLevel / valueFactor);
                    event.getObject().setValue(val);
                }
            }
        });
    }

    public static CubeDetector getCubeDetector() {
        if (detector == null) {
            detector = new HollowCubeDetector(
                    new AllowedBlock[]{
                            new AllowedBlock(RegistryEntries.BLOCK_DARK_BLOOD_BRICK.get()),
                            new AllowedBlock(RegistryEntries.BLOCK_SPIRIT_FURNACE.get()).addCountValidator(new MaximumBlockCountValidator(1)),
                    },
                    Lists.newArrayList(RegistryEntries.BLOCK_SPIRIT_FURNACE.get(), RegistryEntries.BLOCK_DARK_BLOOD_BRICK.get())
            ).addSizeValidator(minimumSizeValidator);
        }
        return detector;
    }

    public static class CapabilityRegistrar extends BlockEntityWorking.CapabilityRegistrar<BlockEntitySpiritFurnace, MutableDouble> {
        public CapabilityRegistrar(Supplier<BlockEntityType<? extends BlockEntitySpiritFurnace>> blockEntityType) {
            super(blockEntityType);
        }

        @Override
        public void registerTankInventoryCapabilitiesItem() {
            add(
                    net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    (blockEntity, direction) -> new ItemHandlerSlotMasked(
                            blockEntity.getInventory(),
                            (direction == Direction.UP || direction == Direction.DOWN) ? SLOTS_DROP : new int[]{SLOT_BOX, SLOT_CONTAINER}
                    ) {
                        @Override
                        public ItemStack extractItem(int slot, int amount, boolean simulate) {
                            ItemStack extracted = super.extractItem(slot, amount, simulate);
                            if (slot > SLOT_BOX && !extracted.isEmpty() && !simulate) {
                                blockEntity.resetWork(false);
                            }
                            return extracted;
                        }
                    }
            );
        }
    }

    @Override
    protected SimpleInventory createInventory(int inventorySize, int stackSize) {
        return new Inventory<BlockEntitySpiritFurnace>(inventorySize, stackSize, this) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack itemStack) {
                if(slot == SLOT_BOX)
                    return getTileWorkingMetadata().canConsume(itemStack, getLevel());
                if(slot == SLOT_CONTAINER)
                    return SlotFluidContainer.checkIsItemValid(itemStack, RegistryEntries.FLUID_BLOOD.get());
                return super.canPlaceItem(slot, itemStack);
            }

            @Override
            public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, Direction side) {
                return slot < 2 && super.canPlaceItemThroughFace(slot, itemStack, side);
            }

            @Override
            public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction side) {
                return slot >= 2 && super.canTakeItemThroughFace(slot, itemStack, side);
            }
        };
    }

    @Override
    protected SingleUseTank createTank(int tankSize) {
        return new ImplicitFluidConversionTank(tankSize, BloodFluidConverter.getInstance());
    }

    @Override
    protected int getWorkTicker() {
        return cookTicker;
    }

    /**
     * Get the entity that is contained in a box.
     * @return The entity or null if no box or invalid box.
     */
    public Entity getEntity() {
        ItemStack boxStack = getInventory().getItem(getConsumeSlot());
        if(!boxStack.isEmpty() && boxStack.getItem() == getAllowedCookItem()) {
            EntityType<?> id = BlockBoxOfEternalClosure.getSpiritTypeWithFallbackSpirit(boxStack);
            if(id != null && id != RegistryEntries.ENTITY_VENGEANCE_SPIRIT.get()) {
                // We cache the entity inside 'boxEntityCache' for obvious efficiency reasons.
                if(boxEntityCache != null && id == boxEntityCache.getType()) {
                    return boxEntityCache;
                } else {
                    Entity entity = id.create(level);
                    boxEntityCache = entity;
                    return entity;
                }
            }
        }
        return null;
    }

    public String getPlayerId() {
        ItemStack boxStack = getInventory().getItem(getConsumeSlot());
        if(!boxStack.isEmpty() && boxStack.getItem() == getAllowedCookItem()) {
            return BlockBoxOfEternalClosure.getPlayerId(boxStack);
        }
        return "";
    }

    public String getPlayerName() {
        ItemStack boxStack = getInventory().getItem(getConsumeSlot());
        if(!boxStack.isEmpty() && boxStack.getItem() == getAllowedCookItem()) {
            return BlockBoxOfEternalClosure.getPlayerName(boxStack);
        }
        return "";
    }

    public boolean isPlayer() {
        return !getPlayerId().isEmpty();
    }

    /**
     * Get the size of the box entity.
     * @return The box entity size.
     */
    public Vec3i getEntitySize() {
        Entity entity = getEntity();
        if(entity == null) {
            return Vec3i.ZERO;
        }
        return EntityHelpers.getEntitySize(entity);
    }

    /**
     * If the size is valid for the contained entity to cook.
     * It will check the inner size of the furnace and the size of the entity.
     * @return If it is valid.
     */
    public boolean isSizeValidForEntity() {
        Entity entity = getEntity();
        if(entity == null) {
            return false;
        }
        Vec3i requiredSize = getEntitySize();
        return getInnerSize().compareTo(requiredSize) >= 0;
    }

    @Override
    public Metadata getTileWorkingMetadata() {
        return METADATA;
    }

    @Override
    public boolean canWork() {
        Vec3i size = getSize();
        return size.compareTo(minimumSizeValidator.getMinimumSize()) >= 0;
    }

    /**
     * Check if the spirit furnace on the given location is valid and can start working.
     * @param world The world.
     * @param location The location.
     * @return If it is valid.
     */
    public static boolean canWork(Level world, BlockPos location) {
        BlockEntity tile = world.getBlockEntity(location);
        if(tile != null) {
            return ((BlockEntitySpiritFurnace) tile).canWork();
        }
        return false;
    }

    /**
     * Get the allowed cooking item for this furnace.
     * @return The allowed item.
     */
    public static Item getAllowedCookItem() {
        return RegistryEntries.ITEM_BOX_OF_ETERNAL_CLOSURE.get();
    }

    /**
     * Callback for when a structure has been detected for a spirit furnace block.
     * @param world The world.
     * @param location The location of one block of the structure.
     * @param size The size of the structure.
     * @param valid If the structure is being validated(/created), otherwise invalidated.
     * @param originCorner The origin corner
     */
    public static void detectStructure(LevelReader world, BlockPos location, Vec3i size, boolean valid, BlockPos originCorner) {

    }

    /**
     * Get the id of the infusion slot.
     * @return id of the infusion slot.
     */
    public int getConsumeSlot() {
        return SLOT_BOX;
    }

    /**
     * Get the ids of the result slots.
     * @return ids of the result slots.
     */
    public int[] getProduceSlots() {
        return SLOTS_DROP;
    }

    /**
     * @return the size
     */
    public Vec3i getSize() {
        return size;
    }

    /**
     * @return the actual inner size.
     */
    public Vec3i getInnerSize() {
        return LocationHelpers.subtract(getSize(), new Vec3i(1, 1, 1));
    }

    /**
     * @param size the size to set
     */
    public void setSize(Vec3i size) {
        this.size = size;
        sendUpdate();
    }

    public void onItemDrop(ItemStack itemStack) {
        boolean placed = false;
        int[] slots = getProduceSlots();
        int i = 0;

        // Try placing the item inside the inventory slots.
        while(!placed && i < slots.length) {
            ItemStack produceStack = getInventory().getItem(slots[i]);
            if(produceStack.isEmpty()) {
                getInventory().setItem(slots[i], itemStack);
                placed = true;
            } else {
                if(ItemStack.isSameItemSameComponents(produceStack, itemStack)
                   && produceStack.getMaxStackSize() >= produceStack.getCount() + itemStack.getCount()) {
                    produceStack.grow(itemStack.getCount());
                    placed = true;
                }
            }
            i++;
        }

        // Halt the cooking if the item couldn't be placed
        forceHalt = !placed;
    }

    @Override
    public void resetWork(boolean hardReset) {
        forceHalt = false;
        caughtError = false;
        super.resetWork(hardReset);
    }

    /**
     * If the cooking is being halted because the inventory is full.
     * @return the forceHalt
     */
    public boolean isForceHalt() {
        return forceHalt;
    }

    /**
     * @return the caughtError
     */
    public boolean isCaughtError() {
        return caughtError;
    }

    /**
     * If an error was caught while killing a spirit.
     */
    public void caughtError() {
        this.caughtError = true;
    }

    @Override
    protected Direction transformFacingForRotation(Direction facing) {
        return facing;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory playerInventory, Player playerEntity) {
        return new ContainerSpiritFurnace(id, playerInventory, this.getInventory(), Optional.of(this));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.evilcraft.spirit_furnace");
    }

    public static class Metadata extends BlockEntityWorking.Metadata {
        private Metadata() {
            super(SLOTS);
        }

        @Override
        public boolean canConsume(ItemStack itemStack, Level world) {
            return !itemStack.isEmpty() && getAllowedCookItem() == itemStack.getItem();
        }

        @Override
        protected Block getBlock() {
            return RegistryEntries.BLOCK_SPIRIT_FURNACE.get();
        }
    }
}
