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

package nukeologist.metachests.tiles;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import nukeologist.metachests.container.MetaChestContainer;
import nukeologist.metachests.MetaChests;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MetaChestTileEntity extends TileEntity implements INamedContainerProvider {

    private static final int SIZE = 45;

    private final ItemStackHandler itemHandler;
    private final LazyOptional<IItemHandler> INVENTORY;

    private ItemGroup itemGroup;
    private boolean keepContent = false;

    public MetaChestTileEntity(TileEntityType<?> type) {
        super(type);
        this.itemHandler = createHandler();
        this.INVENTORY = LazyOptional.of(() -> this.itemHandler);
    }

    public MetaChestTileEntity() {
        this(MetaChests.metaChestTile);
    }

    //This method is where the magic happens. Creates an itemhandler that accepts and maintains itemgroup functionality.
    public ItemStackHandler createHandler() {
        return new ItemStackHandler(getSize()) {
            @Override
            protected void onContentsChanged(int slot) {
                MetaChestTileEntity.this.markDirty();
                if (MetaChestTileEntity.this.getStackOfSlots().isEmpty())
                    MetaChestTileEntity.this.setItemGroup(null);
            }

            @Override
            public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
                super.setStackInSlot(slot, stack);
                if (stack.getItem().getGroup() != null && MetaChestTileEntity.this.getItemGroup() == null)
                    MetaChestTileEntity.this.setItemGroup(stack.getItem().getGroup());
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                final ItemGroup group = MetaChestTileEntity.this.getItemGroup();
                if (group != null) {
                    return group == stack.getItem().getGroup();
                } else {
                    return stack.getItem().getGroup() != null;
                }
            }

            @Nonnull
            @Override
            public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                final ItemGroup stackGroup = stack.getItem().getGroup();
                if (stackGroup == null) return stack;
                if (MetaChestTileEntity.this.getItemGroup() == null) {
                    if (!simulate) {
                        MetaChestTileEntity.this.setItemGroup(stackGroup);
                    }
                    return super.insertItem(slot, stack, simulate);
                } else {
                    if (MetaChestTileEntity.this.getItemGroup() == stackGroup) {
                        return super.insertItem(slot, stack, simulate);
                    } else {
                        return stack;
                    }
                }
            }
        };
    }

    @Override
    public void read(CompoundNBT compound) {
        CompoundNBT invTag = compound.getCompound("inv");
        this.itemHandler.deserializeNBT(invTag);
        this.keepContent = compound.getBoolean("keepContent");
        super.read(compound);
        if (this.getItemGroup() == null) {
            final ItemStack stack = this.getStackOfSlots(); //if there is an item here, its item group SHOULD NOT be null.
            if (!stack.isEmpty()) this.setItemGroup(stack.getItem().getGroup());
        }
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        CompoundNBT invTag = this.itemHandler.serializeNBT();
        compound.put("inv", invTag);
        compound.putBoolean("keepContent", this.keepContent);
        return super.write(compound);
    }

    @Override
    public void remove() {
        super.remove();
        this.INVENTORY.invalidate();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? INVENTORY.cast() : super.getCapability(cap, side);
    }

    @Override
    public ITextComponent getDisplayName() {
        return new StringTextComponent(getType().getRegistryName().getPath());
    }

    @Nullable
    @Override
    public Container createMenu(int id, PlayerInventory playerInv, PlayerEntity player) {
        return new MetaChestContainer(id, playerInv, this.getPos());
    }

    public ItemGroup getItemGroup() {
        return itemGroup;
    }

    public void setItemGroup(final ItemGroup itemGroup) {
        this.itemGroup = itemGroup;
    }

    public boolean keepsContent() {
        return this.keepContent;
    }

    public void setKeepContent(final boolean keepContent) {
        this.keepContent = keepContent;
    }

    private ItemStack getStackOfSlots() {
        final int slots = itemHandler.getSlots();
        for (int i = 0; i < slots; i++) {
            final ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty())
                return stack;
        }
        return ItemStack.EMPTY;
    }

    public int getSize() {
        return SIZE;
    }
}
