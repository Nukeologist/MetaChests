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

import com.google.common.collect.*;
import com.google.common.primitives.Ints;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import java.util.Comparator;

public class MetaChestContainer extends Container {

    private final BlockPos pos;
    private final Vec3d addedPos;
    private final PlayerEntity player;

    protected final TileEntity te;
    protected final IItemHandler playerInventory;

    private static final Comparator<ItemStack> COMPARATOR = new ItemStackComparator();

    private int tick = 0;
    protected boolean sorted = false;

    public MetaChestContainer(int windowId, PlayerInventory playerInv, PacketBuffer extraData) {
        this(windowId, playerInv, extraData.readBlockPos());
    }

    public MetaChestContainer(ContainerType<?> type, int windowId, PlayerInventory playerInv, BlockPos pos) {
        super(type, windowId);
        this.pos = pos;
        this.te = playerInv.player.getEntityWorld().getTileEntity(pos);
        this.playerInventory = new InvWrapper(playerInv);
        this.player = playerInv.player;
        this.addedPos = new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);

        //meta chest inventory
        this.createTileEntitySlots();

        //player
        this.createPlayerSlots();
    }

    public MetaChestContainer(int windowId, PlayerInventory playerInv, BlockPos pos) {
        super(MetaChests.metaChestContainer, windowId);
        this.pos = pos;
        this.te = playerInv.player.getEntityWorld().getTileEntity(pos);
        this.playerInventory = new InvWrapper(playerInv);
        this.player = playerInv.player;
        this.addedPos = new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);

        //meta chest inventory
        this.createTileEntitySlots();

        //player
        this.createPlayerSlots();
    }

    public void createTileEntitySlots() {
        this.te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(inv -> {
            for (int i = 0; i < 5; ++i) {
                for (int j = 0; j < 9; ++j) {
                    this.addSlot(new SlotItemHandler(inv, i * 9 + j, 9 + j * 18, 18 + i * 18) {
                        @Override
                        public void onSlotChanged() {
                            super.onSlotChanged();
                            MetaChestContainer.this.sorted = false;
                        }
                    });
                }
            }
        });
    }

    public void createPlayerSlots() {
        //player inventory
        for (int k = 0; k < 3; ++k) {
            for (int i1 = 0; i1 < 9; ++i1) {
                this.addSlot(new SlotItemHandler(this.playerInventory, i1 + k * 9 + 9, 9 + i1 * 18, 113 + k * 18));
            }
        }
        //hotbar
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new SlotItemHandler(this.playerInventory, k, 9 + k * 18, 171));
        }
    }

    public int getTeSlotsSize() {
        return 45;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        tick++;
        if (tick == 120) {
            tick = 0;
            if (!sorted) {
                this.sortItems(player);
                sorted = true;
            }
        }
    }

    //based on inventory sorter (by cpw) (modified) https://github.com/cpw/inventorysorter
    //changed to not use a itemstackholder. May or may not break the contract of equals,
    //must test to see. (see SortedMultiSet)
    private void sortItems(PlayerEntity player) {
        final int slotLow = 0;
        final int slotHigh = this.getTeSlotsSize();
        final Multiset<ItemStack> itemcounts = getInventoryContent(slotLow, slotHigh, player);

        final UnmodifiableIterator<Multiset.Entry<ItemStack>> itemsIterator;
        try {
            itemsIterator = Multisets.copyHighestCountFirst(itemcounts).entrySet().iterator();
        } catch (Exception e) {
            MetaChests.LOGGER.warn("Something weird happened ", e);
            return;
        }

        Multiset.Entry<ItemStack> stackHolder = itemsIterator.hasNext() ? itemsIterator.next() : null;
        int itemCount = stackHolder != null ? stackHolder.getCount() : 0;
        for (int i = slotLow; i < slotHigh; i++) {
            final Slot slot = player.openContainer.getSlot(i);
            if (!slot.canTakeStack(player) && slot.getHasStack()) {
                continue;
            }
            slot.putStack(ItemStack.EMPTY);
            ItemStack target = ItemStack.EMPTY;
            if (itemCount > 0 && stackHolder != null) {
                target = stackHolder.getElement().copy();
                target.setCount(Math.min(itemCount, target.getMaxStackSize()));
            }
            // The item isn't valid for this slot
            if (!target.isEmpty() && !slot.isItemValid(target)) {
                continue;
            }
            slot.putStack(target.isEmpty() ? ItemStack.EMPTY : target);
            itemCount -= !target.isEmpty() ? target.getCount() : 0;
            if (itemCount == 0) {
                stackHolder = itemsIterator.hasNext() ? itemsIterator.next() : null;
                itemCount = stackHolder != null ? stackHolder.getCount() : 0;
            }
        }
    }

    public Multiset<ItemStack> getInventoryContent(int slotLow, int end, PlayerEntity player) {
        SortedMultiset<ItemStack> itemcounts = TreeMultiset.create(COMPARATOR);
        for (int i = slotLow; i < end; i++) {
            final Slot slot = player.openContainer.getSlot(i);
            if (!slot.canTakeStack(player)) continue;
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                itemcounts.add(stack, stack.getCount());
            }
        }
        final HashMultiset<ItemStack> entries = HashMultiset.create();
        for (Multiset.Entry<ItemStack> entry : itemcounts.descendingMultiset().entrySet()) {
            entries.add(entry.getElement(), entry.getCount());
        }
        return entries;
    }

    public static class ItemStackComparator implements Comparator<ItemStack> {
        @Override
        public int compare(ItemStack o1, ItemStack o2) {
            if (o1 == o2) return 0;
            if (o1.getItem() != o2.getItem())
                return String.valueOf(o1.getItem().getRegistryName()).compareTo(String.valueOf(o2.getItem().getRegistryName()));
            if (ItemStack.areItemStackTagsEqual(o1, o2))
                return 0;
            return Ints.compare(System.identityHashCode(o1), System.identityHashCode(o2));
        }
    }

    //end inventory sorter

    @Override
    public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            int containerSlots = inventorySlots.size() - playerIn.inventory.mainInventory.size();

            if (index < containerSlots) {
                if (!this.mergeItemStack(itemstack1, containerSlots, inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(itemstack1, 0, containerSlots, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.getCount() == 0) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(playerIn, itemstack1);
        }
        return itemstack;
    }

    @Override
    public boolean canInteractWith(PlayerEntity player) {
        return player.getDistanceSq(addedPos) <= 64D;
    }
}