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

package nukeologist.metachests.container;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import nukeologist.metachests.MetaChests;

public class LargeMetaChestContainer extends MetaChestContainer {

    public LargeMetaChestContainer(int windowId, PlayerInventory playerInv, PacketBuffer extraData) {
        this(windowId, playerInv, extraData.readBlockPos());
    }

    public LargeMetaChestContainer(int windowId, PlayerInventory playerInv, BlockPos pos) {
        super(MetaChests.largeMetaChestContainer, windowId, playerInv, pos);
    }

    @Override
    public int getTeSlotsSize() {
        return 91;
    }

    @Override
    public void createPlayerSlots() {
        //player inventory
        for (int k = 0; k < 3; ++k) {
            for (int i1 = 0; i1 < 9; ++i1) {
                this.addSlot(new SlotItemHandler(this.playerInventory, i1 + k * 9 + 9, 44 + i1 * 18, 157 + k * 18));
            }
        }
        //hotbar
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new SlotItemHandler(this.playerInventory, k, 44 + k * 18, 215));
        }
    }

    @Override
    public void createTileEntitySlots() {
        this.te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(inv -> {
            for (int i = 0; i < 7; ++i) {
                for (int j = 0; j < 13; ++j) {
                    this.addSlot(new SlotItemHandler(inv, i * 13 + j, 8 + j * 18, 18 + i * 18) {
                        @Override
                        public void onSlotChanged() {
                            super.onSlotChanged();
                            LargeMetaChestContainer.this.sorted = false;
                        }
                    });
                }
            }
        });
    }
}
