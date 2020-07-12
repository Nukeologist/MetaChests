/*
 *
 *  *     Copyright © 2019 Nukeologist
 *  *     This file is part of MetaChests.
 *  *
 *  *     MetaChests is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License, or
 *  *     (at your option) any later version.
 *  *
 *  *     MetaChests is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU General Public License
 *  *     along with MetaChests.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package nukeologist.metachests.block;

import net.minecraft.block.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import nukeologist.metachests.tiles.LargeMetaChestTileEntity;
import nukeologist.metachests.tiles.MetaChestTileEntity;
import nukeologist.metachests.MetaChests;

import javax.annotation.Nullable;
import java.util.Random;

import static net.minecraft.block.ChestBlock.WATERLOGGED;

public class MetaChestBlock extends Block implements IWaterLoggable {

    public static final DirectionProperty FACING = HorizontalBlock.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.makeCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);

    public MetaChestBlock(Properties properties) {
        super(properties);
        this.setDefaultState(this.stateContainer.getBaseState().with(FACING, Direction.NORTH).with(WATERLOGGED, Boolean.FALSE));
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new MetaChestTileEntity();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (worldIn.isRemote) return ActionResultType.SUCCESS;
        final TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof MetaChestTileEntity) {
            NetworkHooks.openGui((ServerPlayerEntity) player, (INamedContainerProvider) te, buf -> buf.writeBlockPos(pos));
            return ActionResultType.SUCCESS;
        }
        return super.onBlockActivated(state, worldIn, pos, player, handIn, hit);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean eventReceived(BlockState state, World worldIn, BlockPos pos, int id, int param) {
        super.eventReceived(state, worldIn, pos, id, param);
        final TileEntity tileentity = worldIn.getTileEntity(pos);
        return tileentity != null && tileentity.receiveClientEvent(id, param);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            final TileEntity tileentity = worldIn.getTileEntity(pos);
            if (tileentity instanceof MetaChestTileEntity) {
                if (!((MetaChestTileEntity) tileentity).keepsContent())
                    InventoryHelper.dropItems(worldIn, pos, dropItemHandlerContents(tileentity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElseGet(() -> new ItemStackHandler()), new Random()));
            }
            super.onReplaced(state, worldIn, pos, newState, isMoving);
        }
    }

    @Override
    public void harvestBlock(World worldIn, PlayerEntity player, BlockPos pos, BlockState state, @Nullable TileEntity te, ItemStack stack) {
        player.addStat(Stats.BLOCK_MINED.get(this));
        player.addExhaustion(0.005F);
        if (te instanceof MetaChestTileEntity) {
            final MetaChestTileEntity meta = (MetaChestTileEntity) te;
            if (meta.keepsContent()) {
                meta.setKeepContent(false);
                CompoundNBT nbt = new CompoundNBT();
                nbt = meta.write(nbt);
                final ItemStack is = meta instanceof LargeMetaChestTileEntity ? new ItemStack(MetaChests.largeMetaChestItem) : new ItemStack(MetaChests.metaChestItem);
                is.setTagInfo("BlockEntityTag", nbt);
                final ItemEntity itemEntity = new ItemEntity(worldIn, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, is);
                worldIn.addEntity(itemEntity);

            } else {
                spawnDrops(state, worldIn, pos, te, player, stack);
            }
        }
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        final TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof MetaChestTileEntity && placer != null) {
            if (stack.hasTag()) {
                te.read(state, stack.getTag().getCompound("BlockEntityTag"));
                te.setPos(pos); //when it reads, it reads the old pos.
            }
        }

    }

    /**
     * Get a list of the {@link IItemHandler}'s contents with the stacks randomly split.
     * <p>
     * Adapted from {@link InventoryHelper}.
     * <p>
     * Originally from https://github.com/Choonster-Minecraft-Mods/TestMod3/blob/21ca711b2d69c8ba91f88ffc609e0c36e2fa2ac4/src/main/java/choonster/testmod3/util/InventoryUtils.java#L110-L134
     *
     * @param itemHandler The inventory
     * @param random      The Random object
     * @return The drops list
     */
    public static NonNullList<ItemStack> dropItemHandlerContents(IItemHandler itemHandler, Random random) {
        final NonNullList<ItemStack> drops = NonNullList.create();
        for (int slot = 0; slot < itemHandler.getSlots(); ++slot) {
            while (!itemHandler.getStackInSlot(slot).isEmpty()) {
                final int amount = random.nextInt(21) + 10;
                if (!itemHandler.extractItem(slot, amount, true).isEmpty()) {
                    final ItemStack itemStack = itemHandler.extractItem(slot, amount, false);
                    drops.add(itemStack);
                }
            }
        }

        return drops;
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return SHAPE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (stateIn.get(WATERLOGGED)) {
            worldIn.getPendingFluidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickRate(worldIn));
        }

        return super.updatePostPlacement(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        final Direction direction = context.getPlacementHorizontalFacing().getOpposite();
        final FluidState ifluidstate = context.getWorld().getFluidState(context.getPos());
        return this.getDefaultState().with(FACING, direction).with(WATERLOGGED, ifluidstate.getFluid() == Fluids.WATER);
    }

    @SuppressWarnings("deprecation")
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state);
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.with(FACING, rot.rotate(state.get(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.toRotation(state.get(FACING)));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean allowsMovement(BlockState state, IBlockReader worldIn, BlockPos pos, PathType type) {
        return false;
    }
}
