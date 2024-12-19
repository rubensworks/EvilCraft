package org.cyclops.evilcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.cyclops.evilcraft.Reference;
import org.cyclops.evilcraft.RegistryEntries;
import org.cyclops.evilcraft.blockentity.BlockEntityDarkTank;

@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsItemEternalWater {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final BlockPos POS = BlockPos.ZERO.offset(2, 0, 2);

    @GameTest(template = TEMPLATE_EMPTY)
    public void testItemEternalWaterPlaceWorld(GameTestHelper helper) {
        // Give bucket to player
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(POS).getCenter());
        player.setXRot(90);
        ItemStack itemStack = new ItemStack(RegistryEntries.ITEM_ETERNAL_WATER);
        player.setItemInHand(InteractionHand.MAIN_HAND, itemStack);

        // Surround target location with blocks to avoid spilling
        helper.setBlock(POS.north(), Blocks.STONE);
        helper.setBlock(POS.east(), Blocks.STONE);
        helper.setBlock(POS.south(), Blocks.STONE);
        helper.setBlock(POS.west(), Blocks.STONE);

        // Right click with bucket as player
        InteractionResultHolder<ItemStack> result = itemStack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        helper.succeedWhen(() -> {
            // Check result
            helper.assertTrue(result.getResult() == InteractionResult.SUCCESS, "Result is not successful");
            helper.assertFalse(result.getObject().isEmpty(), "Result is empty");
            helper.assertTrue(result.getObject().getItem() == RegistryEntries.ITEM_ETERNAL_WATER.get(), "Result item is incorrect");

            // Check placed water block
            helper.assertBlockPresent(Blocks.WATER, POS);

            // Check if player still has the bucket
            helper.assertFalse(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "Item in hand is empty");
            helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getItem() == RegistryEntries.ITEM_ETERNAL_WATER.get(), "Item in hand is incorrect");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public void testItemEternalWaterPickupWorld(GameTestHelper helper) {
        // Give bucket to player
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(POS).getCenter());
        player.setXRot(90);
        ItemStack itemStack = new ItemStack(RegistryEntries.ITEM_ETERNAL_WATER);
        player.setItemInHand(InteractionHand.MAIN_HAND, itemStack);

        // Surround target location with blocks to avoid spilling
        helper.setBlock(POS.north(), Blocks.STONE);
        helper.setBlock(POS.east(), Blocks.STONE);
        helper.setBlock(POS.south(), Blocks.STONE);
        helper.setBlock(POS.west(), Blocks.STONE);

        // Place water block
        helper.setBlock(POS, Blocks.WATER);

        // Right click with bucket as player
        InteractionResultHolder<ItemStack> result = itemStack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        helper.succeedWhen(() -> {
            // Check result
            helper.assertTrue(result.getResult() == InteractionResult.SUCCESS, "Result is not successful");
            helper.assertFalse(result.getObject().isEmpty(), "Result is empty");
            helper.assertTrue(result.getObject().getItem() == RegistryEntries.ITEM_ETERNAL_WATER.get(), "Result item is incorrect");

            // Check removed water block
            helper.assertBlockNotPresent(Blocks.WATER, POS);

            // Check if player still has the bucket
            helper.assertFalse(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "Item in hand is empty");
            helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getItem() == RegistryEntries.ITEM_ETERNAL_WATER.get(), "Item in hand is incorrect");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public void testItemEternalWaterFillTank(GameTestHelper helper) {
        // Give bucket to player
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack itemStack = new ItemStack(RegistryEntries.ITEM_ETERNAL_WATER);
        player.setItemInHand(InteractionHand.MAIN_HAND, itemStack);

        // Place tank
        helper.setBlock(POS, RegistryEntries.BLOCK_DARK_TANK.get());

        // Right click with bucket as player
        InteractionResult result = itemStack.onItemUseFirst(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(helper.absolutePos(POS).getCenter(), Direction.NORTH, helper.absolutePos(POS), false)));

        helper.succeedWhen(() -> {
            // Check result
            helper.assertTrue(result == InteractionResult.SUCCESS, "Result is not successful");

            // Check tank has water
            BlockEntityDarkTank tank = helper.getBlockEntity(POS);
            helper.assertTrue(FluidStack.matches(tank.getTank().getFluid(), new FluidStack(Fluids.WATER, 1000)), "Fluid in tank is incorrect");

            // Check if player still has the bucket
            helper.assertFalse(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "Item in hand is empty");
            helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getItem() == RegistryEntries.ITEM_ETERNAL_WATER.get(), "Item in hand is incorrect");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public void testItemEternalWaterFillCauldron(GameTestHelper helper) {
        // Give bucket to player
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack itemStack = new ItemStack(RegistryEntries.ITEM_ETERNAL_WATER);
        player.setItemInHand(InteractionHand.MAIN_HAND, itemStack);

        // Place cauldron
        helper.setBlock(POS, Blocks.CAULDRON);

        // Right click with bucket as player
        InteractionResult result = itemStack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(helper.absolutePos(POS).getCenter(), Direction.NORTH, helper.absolutePos(POS), false)));

        helper.succeedWhen(() -> {
            // Check result
            helper.assertTrue(result == InteractionResult.SUCCESS, "Result is not successful");

            // Check filled cauldron
            helper.assertBlockPresent(Blocks.WATER_CAULDRON, POS);
            helper.assertBlockProperty(POS, LayeredCauldronBlock.LEVEL, 3);

            // Check if player still has the bucket
            helper.assertFalse(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "Item in hand is empty");
            helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getItem() == RegistryEntries.ITEM_ETERNAL_WATER.get(), "Item in hand is incorrect");
        });
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public void testItemEternalWaterFillCauldronPartial(GameTestHelper helper) {
        // Give bucket to player
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack itemStack = new ItemStack(RegistryEntries.ITEM_ETERNAL_WATER);
        player.setItemInHand(InteractionHand.MAIN_HAND, itemStack);

        // Place cauldron
        helper.setBlock(POS, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1));

        // Right click with bucket as player
        InteractionResult result = itemStack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(helper.absolutePos(POS).getCenter(), Direction.NORTH, helper.absolutePos(POS), false)));

        helper.succeedWhen(() -> {
            // Check result
            helper.assertTrue(result == InteractionResult.SUCCESS, "Result is not successful");

            // Check filled cauldron
            helper.assertBlockPresent(Blocks.WATER_CAULDRON, POS);
            helper.assertBlockProperty(POS, LayeredCauldronBlock.LEVEL, 3);

            // Check if player still has the bucket
            helper.assertFalse(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "Item in hand is empty");
            helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getItem() == RegistryEntries.ITEM_ETERNAL_WATER.get(), "Item in hand is incorrect");
        });
    }

}
