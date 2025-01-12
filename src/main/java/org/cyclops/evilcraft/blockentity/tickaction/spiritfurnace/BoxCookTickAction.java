package org.cyclops.evilcraft.blockentity.tickaction.spiritfurnace;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.cyclops.cyclopscore.helper.ItemStackHelpers;
import org.cyclops.cyclopscore.item.WeightedItemStack;
import org.cyclops.evilcraft.EvilCraft;
import org.cyclops.evilcraft.block.BlockBoxOfEternalClosure;
import org.cyclops.evilcraft.block.BlockSpiritFurnaceConfig;
import org.cyclops.evilcraft.blockentity.BlockEntitySpiritFurnace;
import org.cyclops.evilcraft.core.blockentity.tickaction.ITickAction;
import org.cyclops.evilcraft.core.blockentity.upgrade.UpgradeSensitiveEvent;
import org.cyclops.evilcraft.core.blockentity.upgrade.Upgrades;
import org.cyclops.evilcraft.core.helper.MathHelpers;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.NoSuchElementException;

/**
 * {@link ITickAction} that is able to cook boxes with spirits.
 * @author rubensworks
 *
 */
public class BoxCookTickAction implements ITickAction<BlockEntitySpiritFurnace> {

    public static final Map<Class<? extends LivingEntity>, List<WeightedItemStack>> MOBDROP_OVERRIDES = Maps.newHashMap();
    public static final Map<UUID, List<WeightedItemStack>> PLAYERDROP_OVERRIDES_INNER = Maps.newHashMap();
    static {
        if(BlockSpiritFurnaceConfig.villagerDropEmeraldChance > 0) {
            overrideMobDrop(Villager.class, Sets.newHashSet(
                    new WeightedItemStack(new ItemStack(Items.EMERALD), 1),
                    new WeightedItemStack(ItemStack.EMPTY, BlockSpiritFurnaceConfig.villagerDropEmeraldChance - 1)
            ));
        }
        overrideMobDrop(WitherBoss.class, Sets.newHashSet(
                new WeightedItemStack(new ItemStack(Items.NETHER_STAR), 1)
        ));
        overridePlayerDropInner("068d4de0-3a75-4c6a-9f01-8c37e16a394c", new ItemStack(Items.EMERALD)); // kroeserr
        overridePlayerDropInner("e1dc75c6-dcf9-4e0c-8fbf-9c6e5e44527c", new ItemStack(Items.WOODEN_SWORD)); // _EeB_
        overridePlayerDropInner("777e7aa3-9373-4511-8d75-f99d23ebe252", withName(new ItemStack(Items.BROWN_DYE), Component.literal("Lekkere Stront"))); // Davivs69
        overridePlayerDropInner("3e13f558-fb72-4949-a842-07879924bc49", new ItemStack(Items.QUARTZ)); // Jona
        overridePlayerDropInner("b5c31e33-8224-4f96-a4bf-73721be9d2ec", new ItemStack(Blocks.COBBLESTONE)); // dodo3231
        overridePlayerDropInner("b2faeaab-fc87-4f91-98d3-836024f268ae", withName(new ItemStack(Blocks.FURNACE), Component.literal("Fuurnas"))); // _KillaH229_
        overridePlayerDropInner("069a79f4-44e9-4726-a5be-fca90e38aaf5", new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)); // Notch
        overridePlayerDropInner("853c80ef-3c37-49fd-aa49-938b674adae6", withName(new ItemStack(Items.SHEEP_SPAWN_EGG), Component.literal("jeb_"))); // jeb_
        overridePlayerDropInner("61699b2e-d327-4a01-9f1e-0ea8c3f06bc6", withName(new ItemStack(Items.SHEEP_SPAWN_EGG), Component.literal("Dinnerbone"))); // Dinnerbone
        overridePlayerDropInner("bbb87dbe-690f-4205-bdc5-72ffb8ebc29d", withName(new ItemStack(Blocks.COBBLESTONE, 45), Component.literal("direwolf20"))); // direwolf20
        overridePlayerDropInner("0b7509f0-2458-4160-9ce1-2772b9a45ac2", new ItemStack(Items.PORKCHOP)); // iChun
    }
    public static ItemStack withName(ItemStack itemStack, Component name) {
        itemStack.set(DataComponents.CUSTOM_NAME, name);
        return itemStack;
    }
    public static final ItemStack[] PLAYERDROP_RANDOM = new ItemStack[] {
            new ItemStack(Items.BOOK),
            new ItemStack(Items.BLAZE_POWDER),
            new ItemStack(Items.QUARTZ),
            new ItemStack(Items.CHAINMAIL_CHESTPLATE),
            new ItemStack(Items.FERMENTED_SPIDER_EYE),
            new ItemStack(Items.GLASS_BOTTLE),
            new ItemStack(Items.ITEM_FRAME),
            new ItemStack(Items.MINECART),
            new ItemStack(Items.SHEARS),
            new ItemStack(Items.SUGAR_CANE),
            new ItemStack(Items.PUMPKIN_PIE),
            new ItemStack(Items.MAGMA_CREAM),
            new ItemStack(Items.SADDLE),
            new ItemStack(Items.GLISTERING_MELON_SLICE),
            new ItemStack(Items.IRON_HOE),
            new ItemStack(Items.CARROT_ON_A_STICK),
            new ItemStack(Items.REDSTONE),
            new ItemStack(Blocks.COAL_BLOCK),
            new ItemStack(Blocks.LAPIS_BLOCK),
            new ItemStack(Blocks.SOUL_SAND),
            new ItemStack(Blocks.GRAVEL),
            new ItemStack(Blocks.HOPPER),
    };

    /**
     * Override an entity's drops inside the spirit furnace.
     * @param entity The entity class.
     * @param drops A map of drops to relative frequency, with the second pair of the map key representing the min-max
     *              amount of drops (both inclusive)
     */
    public static void overrideMobDrop(Class<? extends LivingEntity> entity, Set<WeightedItemStack> drops) {
        MOBDROP_OVERRIDES.put(entity, WeightedItemStack.createWeightedList(drops));
    }

    /**
     * Override a player's drops inside the spirit furnace.
     * @param playerId The player id.
     * @param drop The drop
     */
    public static void overridePlayerDropInner(String playerId, ItemStack drop) {
        PLAYERDROP_OVERRIDES_INNER.put(UUID.fromString(playerId), WeightedItemStack.createWeightedList(Sets.newHashSet(new WeightedItemStack(drop, 1))));
    }

    @Override
    public boolean canTick(BlockEntitySpiritFurnace tile, ItemStack itemStack, int slot, int tick) {
        if(!tile.isForceHalt() && !tile.isCaughtError() && tile.canWork()
                && tile.getTank().getFluidAmount() >= getRequiredMb(tile, 0)
                && !getCookStack(tile).isEmpty() && tile.getTileWorkingMetadata().canConsume(getCookStack(tile), tile.getLevel())) {
            for(int slotId : tile.getProduceSlots()) {
                ItemStack production = tile.getInventory().getItem(slotId);
                if(production == null || production.getCount() < production.getMaxStackSize()) {
                    return tile.isSizeValidForEntity();
                }
            }
        }
        return false;
    }

    protected ItemStack getCookStack(BlockEntitySpiritFurnace tile) {
        return tile.getInventory().getItem(tile.getConsumeSlot());
    }

    protected ItemStack getPlayerSkull(String playerName) {
        ItemStack itemStack = new ItemStack(Items.PLAYER_HEAD);
        try {
            itemStack.set(DataComponents.PROFILE, new ResolvableProfile(SkullBlockEntity.fetchGameProfile(playerName).get(1, TimeUnit.SECONDS).get()));
        } catch (InterruptedException | ExecutionException | NullPointerException | TimeoutException | 	NoSuchElementException e) {
            e.printStackTrace();
        }
        return itemStack;
    }

    protected ItemStack getPlayerDeterminedDrop(String playerId) {
        return PLAYERDROP_RANDOM[Math.abs(playerId.hashCode() % PLAYERDROP_RANDOM.length)].copy();
    }

    protected void doNextDrop(BlockEntitySpiritFurnace tile) {
        Entity entityRaw = tile.getEntity();
        if(entityRaw instanceof LivingEntity) {
            LivingEntity entity = (LivingEntity) entityRaw;
            Level world = tile.getLevel();

            // Send sound to client
            SoundEvent deathSound = entity.getDeathSound();
            if(BlockSpiritFurnaceConfig.mobDeathSounds && deathSound != null) {
                BlockPos pos = tile.getBlockPos();
                world.playSound(null, pos, deathSound, entity.getSoundSource(), 0.5F + world.random.nextFloat() * 0.2F, 1.0F);
            }

            if(tile.isPlayer()) {
                UUID playerUuid = UUID.fromString(tile.getPlayerId());
                List<WeightedItemStack> possibleDrops = Lists.newLinkedList();
                possibleDrops.add(new WeightedItemStack(getPlayerDeterminedDrop(tile.getPlayerId()), 1));
                List<WeightedItemStack> overridenDrops = PLAYERDROP_OVERRIDES_INNER.get(playerUuid);
                if(overridenDrops == null) {
                    Map<String, List<WeightedItemStack>> playerDropOverrides = getPlayerDropOverrides();
                    overridenDrops = playerDropOverrides.get(playerUuid.toString());
                }
                if(overridenDrops != null) {
                    possibleDrops.addAll(overridenDrops);
                }
                if(!BlockBoxOfEternalClosure.FORGOTTEN_PLAYER.equals(tile.getPlayerName())) {
                    possibleDrops.add(new WeightedItemStack(getPlayerSkull(tile.getPlayerName()), 1));
                }
                WeightedItemStack weightedItemStack = WeightedItemStack.getRandomWeightedItemStack(possibleDrops, world.random);
                ItemStack drop = weightedItemStack.getItemStackWithRandomizedSize(world.random);
                if (!drop.isEmpty()) {
                    tile.onItemDrop(drop);
                }
            } else {
                if (MOBDROP_OVERRIDES.containsKey(entity.getClass())) {
                    List<WeightedItemStack> possibleDrops = MOBDROP_OVERRIDES.get(entity.getClass());
                    WeightedItemStack weightedItemStack = WeightedItemStack.getRandomWeightedItemStack(possibleDrops, world.random);
                    ItemStack drop = weightedItemStack.getItemStackWithRandomizedSize(world.random);
                    if (!drop.isEmpty()) {
                        tile.onItemDrop(drop);
                    }
                } else {
                    ResourceKey<LootTable> deathLootTable;
                    Map<EntityType<?>, ResourceLocation> mobDropTablesOverrides = getMobDropTablesOverrides();
                    if (mobDropTablesOverrides.containsKey(entity.getType())) {
                        deathLootTable = ResourceKey.create(Registries.LOOT_TABLE, mobDropTablesOverrides.get(entity.getType()));
                    } else {
                        deathLootTable = entity.getLootTable();
                    }
                    if(deathLootTable != null) {
                        LootTable loottable = tile.getLevel().getServer().reloadableRegistries().getLootTable(deathLootTable);
                        FakePlayer killerEntity = FakePlayerFactory.getMinecraft((ServerLevel) tile.getLevel());
                        LootParams.Builder lootcontext$builder = (new LootParams.Builder((ServerLevel) tile.getLevel()))
                                .withParameter(LootContextParams.THIS_ENTITY, entity)
                                .withParameter(LootContextParams.ORIGIN, new Vec3(tile.getBlockPos().getX(), tile.getBlockPos().getY(), tile.getBlockPos().getZ()))
                                .withParameter(LootContextParams.ATTACKING_ENTITY, killerEntity)
                                .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killerEntity)
                                .withParameter(LootContextParams.DAMAGE_SOURCE, killerEntity.damageSources().generic())
                                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killerEntity);

                        // If we want to do something with fortune later...
                        /*if (p_184610_1_ && this.attackingPlayer != null) {
                            lootcontext$builder = lootcontext$builder.withPlayer(this.attackingPlayer).withLuck(this.attackingPlayer.getLuck());
                        }*/

                        for (ItemStack itemstack : loottable.getRandomItems(lootcontext$builder.create(LootContextParamSets.ENTITY))) {
                            tile.onItemDrop(itemstack);
                        }
                    }
                }
            }
        }
    }

    private Map<String, List<WeightedItemStack>> getPlayerDropOverrides() {
        Map<String, List<WeightedItemStack>> map = Maps.newIdentityHashMap();
        for(String line : BlockSpiritFurnaceConfig.playerDrops) {
            String[] split = line.split(BlockSpiritFurnaceConfig.DELIMITER);
            if(split.length != 2) {
                throw new IllegalArgumentException("Invalid line '" + line + "' found for "
                        + "a Spirit Furnace player drop config.");
            }
            try {
                String playerId = split[0];
                boolean validId = true;
                try {
                    UUID.fromString(playerId);
                } catch (IllegalArgumentException e) {
                    validId = false;
                }
                if(!validId) {
                    EvilCraft.clog("Invalid line '" + line + "' found for "
                            + "a Spirit Furnace player drop config: " + split[0] + " does not refer to a valid player UUID; skipping.");
                    continue;
                }
                try {
                    ItemStack itemStack = ItemStackHelpers.parseItemStack(split[1]);
                    map.put(playerId, Lists.newArrayList(new WeightedItemStack(itemStack, 1)));
                } catch (IllegalArgumentException e) {
                    EvilCraft.clog("Invalid item '" + split[1] + "' in "
                            + "a Spirit Furnace player drop config; skipping:" + e.getMessage(), org.apache.logging.log4j.Level.ERROR);
                }
            } catch (NumberFormatException e) {
                EvilCraft.clog("Invalid line '" + line + "' found for "
                        + "a Spirit Furnace player drop config: " + split[0] + " is not a number; skipping.");
            }
        }
        return map;
    }

    private Map<EntityType<?>, ResourceLocation> getMobDropTablesOverrides() {
        Map<EntityType<?>, ResourceLocation> map = Maps.newIdentityHashMap();
        for(String line : BlockSpiritFurnaceConfig.mobDrops) {
            String[] split = line.split(BlockSpiritFurnaceConfig.DELIMITER);
            if(split.length != 2) {
                throw new IllegalArgumentException("Invalid line '" + line + "' found for "
                        + "a Spirit Furnace mob drop config.");
            }
            ResourceLocation entityName = ResourceLocation.parse(split[0]);
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entityName)) {
                EvilCraft.clog("Invalid line '" + line + "' found for "
                        + "a Spirit Furnace mob drop config: " + split[0] + " does not refer to a valid entity name; skipping.");
                continue;
            }
            ResourceLocation resourceLocation = ResourceLocation.parse(split[1]);
            map.put(BuiltInRegistries.ENTITY_TYPE.get(entityName), resourceLocation);
        }
        return map;
    }

    protected int getRequiredMb(BlockEntitySpiritFurnace tile, int tick) {
        int baseUsage;
        if(tile.isPlayer()) {
            baseUsage = BlockSpiritFurnaceConfig.playerMBPerTick;
        } else if (tile.getEntity() != null && !tile.getEntity().canChangeDimensions(tile.getLevel(), tile.getLevel())) {
            baseUsage = BlockSpiritFurnaceConfig.bossMBPerTick;
        } else {
            baseUsage = BlockSpiritFurnaceConfig.mBPerTick;
        }
        MutableDouble drain = new MutableDouble(baseUsage);
        Upgrades.sendEvent(tile, new UpgradeSensitiveEvent<MutableDouble>(drain, BlockEntitySpiritFurnace.UPGRADEEVENT_BLOODUSAGE));
        return MathHelpers.factorToBursts(drain.getValue(), tick);
    }

    @Override
    public void onTick(BlockEntitySpiritFurnace tile, ItemStack itemStack, int slot,
                       int tick) {
        // Drain the tank a bit.
        tile.getTank().drain(getRequiredMb(tile, tick), IFluidHandler.FluidAction.EXECUTE);
        if(tick >= getRequiredTicks(tile, slot, tick)) {
            doNextDrop(tile);
        }
    }

    @Override
    public float getRequiredTicks(BlockEntitySpiritFurnace tile, int slot, int tick) {
        int requiredTicksBase;
        Entity entity = tile.getEntity();
        if(entity == null) {
            requiredTicksBase = BlockSpiritFurnaceConfig.requiredTicksPerHp;
        } else {
            try {
                LivingEntity livingEntity = (LivingEntity) entity;
                requiredTicksBase = (int) ((livingEntity.getHealth() + livingEntity.getArmorValue()) * BlockSpiritFurnaceConfig.requiredTicksPerHp);
            } catch (RuntimeException e) {
                requiredTicksBase = 40 * BlockSpiritFurnaceConfig.requiredTicksPerHp;
            }
        }
        MutableDouble duration = new MutableDouble(requiredTicksBase);
        Upgrades.sendEvent(tile, new UpgradeSensitiveEvent<MutableDouble>(duration, BlockEntitySpiritFurnace.UPGRADEEVENT_SPEED));
        return (int) (double) duration.getValue();
    }

}
