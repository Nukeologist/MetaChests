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

package nukeologist.metachests;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import java.util.function.BiFunction;

import static nukeologist.metachests.MetaChests.MODID;

public enum Upgrade {

    CHEST_TO_META(Upgrade::chestToMeta, Upgrade.INFO + ".chestupgrade"),
    META_TO_LARGE_META(Upgrade::metaToLarge, Upgrade.INFO + ".metaupgrade"),
    CHEST_TO_LARGE_META(Upgrade::chestToLarge, Upgrade.INFO + ".chestlargeupgrade"),
    KEEP_CONTENT(Upgrade::keepContent, Upgrade.INFO + ".keepupgrade");

    private static final String INFO = "info." + MODID;

    private final BiFunction<ItemStack, ItemUseContext, ActionResultType> USE;
    private final String INFO_KEY;

    Upgrade(final BiFunction<ItemStack, ItemUseContext, ActionResultType> use, final String info) {
        this.USE = use;
        this.INFO_KEY = info;
    }

    public final ActionResultType onItemUse(final ItemStack stack, final ItemUseContext context) {
        return this.USE.apply(stack, context);
    }

    public final String getInfoKey() {
        return INFO_KEY;
    }

    private static ActionResultType chestToMeta(final ItemStack stack, final ItemUseContext ctx) {
        return chestToMeta(stack, ctx, MetaChests.metaChestBlock.getDefaultState());
    }

    private static ActionResultType chestToMeta(final ItemStack stack, final ItemUseContext ctx, final BlockState chestState) {
        final World world = ctx.getWorld();
        final BlockPos pos = ctx.getPos();
        final PlayerEntity player = ctx.getPlayer();
        final BlockState state = world.getBlockState(pos);
        if (state.getBlock().getDefaultState() != Blocks.CHEST.getDefaultState())
            return ActionResultType.PASS;
        final TileEntity te = world.getTileEntity(pos);
        if (te instanceof ChestTileEntity) {
            if (player == null || !player.abilities.isCreativeMode)
                stack.shrink(1);
            final ChestTileEntity chest = (ChestTileEntity) te;
            final int size = chest.getSizeInventory();
            final NonNullList<ItemStack> inv = NonNullList.withSize(size, ItemStack.EMPTY);
            for (int slot = 0; slot < size; slot++)
                inv.set(slot, chest.getStackInSlot(slot));
            chest.updateContainingBlockInfo();
            world.removeTileEntity(pos);
            world.removeBlock(pos, false);
            final BlockState ib = chestState.with(MetaChestBlock.FACING, state.get(ChestBlock.FACING));
            world.setBlockState(pos, ib, 3);
            world.notifyBlockUpdate(pos, ib, ib, 3);
            final TileEntity newTile = world.getTileEntity(pos);
            if (newTile != null)
                newTile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
                    final IItemHandlerModifiable mod = (IItemHandlerModifiable) handler;
                    ItemGroup group = null;
                    for (int i = 0; i < size; i++) {
                        final ItemStack is = inv.get(i);
                        if (!is.isEmpty() && is.getItem().getGroup() != null) {
                            group = is.getItem().getGroup();
                            break;
                        }
                    }
                    if (group == null) {
                        InventoryHelper.dropItems(world, pos.add(0, 1, 0), inv);
                    } else {
                        for (int i = 0; i < size; i++) {
                            final ItemStack is = inv.get(i);
                            if (is.getItem().getGroup() != group) {
                                InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY() + 1, pos.getZ(), is);
                            } else {
                                mod.setStackInSlot(i, is);
                            }
                        }
                    }
                });
        }
        return ActionResultType.SUCCESS;
    }

    private static ActionResultType metaToLarge(final ItemStack stack, final ItemUseContext ctx) {
        final World world = ctx.getWorld();
        final BlockPos pos = ctx.getPos();
        final PlayerEntity player = ctx.getPlayer();
        final BlockState state = world.getBlockState(pos);
        if (state.getBlock().getDefaultState() != MetaChests.metaChestBlock.getDefaultState())
            return ActionResultType.PASS;
        final TileEntity te = world.getTileEntity(pos);
        if (te instanceof MetaChestTileEntity) {
            if (player == null || !player.abilities.isCreativeMode)
                stack.shrink(1);
            final int size = 45;
            final IItemHandler old = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElseGet(ItemStackHandler::new);
            final NonNullList<ItemStack> inv = NonNullList.withSize(size, ItemStack.EMPTY);
            for (int slot = 0; slot < size; slot++)
                inv.set(slot, old.getStackInSlot(slot));
            world.removeTileEntity(pos);
            world.removeBlock(pos, false);
            final BlockState ib = MetaChests.largeMetaChestBlock.getDefaultState().with(MetaChestBlock.FACING, state.get(ChestBlock.FACING));
            world.setBlockState(pos, ib, 3);
            world.notifyBlockUpdate(pos, ib, ib, 3);
            final TileEntity newTile = world.getTileEntity(pos);
            if (newTile != null)
                newTile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
                    final IItemHandlerModifiable mod = (IItemHandlerModifiable) handler;
                    for (int i = 0; i < size; i++)
                        mod.setStackInSlot(i, inv.get(i));
                });
        }
        return ActionResultType.SUCCESS;
    }

    private static ActionResultType chestToLarge(final ItemStack stack, final ItemUseContext ctx) {
        return chestToMeta(stack, ctx, MetaChests.largeMetaChestBlock.getDefaultState());
    }

    private static ActionResultType keepContent(final ItemStack stack, final ItemUseContext ctx) {
        final World world = ctx.getWorld();
        final BlockPos pos = ctx.getPos();
        final PlayerEntity player = ctx.getPlayer();
        final TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof MetaChestTileEntity))
            return ActionResultType.PASS;
        final MetaChestTileEntity meta = (MetaChestTileEntity) te;
        if (!meta.keepsContent()) {
            if ((player == null || !player.abilities.isCreativeMode))
                stack.shrink(1);
            meta.setKeepContent(true);
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }
}