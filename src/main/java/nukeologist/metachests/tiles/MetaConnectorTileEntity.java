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

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.Tuple;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import nukeologist.metachests.MetaChests;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class MetaConnectorTileEntity extends TileEntity implements ITickableTileEntity {

    private final ControllerHandler HANDLER;
    private final LazyOptional<IItemHandler> CAP;
    private final Set<MetaChestTileEntity> TILES;

    public MetaConnectorTileEntity(TileEntityType<?> type) {
        super(type);
        HANDLER = new ControllerHandler();
        CAP = LazyOptional.of(() -> HANDLER);
        TILES = new LinkedHashSet<>();
    }

    public MetaConnectorTileEntity() {
        this(MetaChests.metaConnectorTile);
    }

    @Override
    public void remove() {
        super.remove();
        CAP.invalidate();
    }

    @Override
    public void tick() {
        if (this.hasWorld() && !this.world.isRemote() && this.world.getGameTime() % 25 == 0) {
            this.refresh();
        }
    }

    private void refresh() {
        TILES.clear();
        HANDLER.handlers.clear();
        HANDLER.slots = 0;
        final Deque<MetaChestTileEntity> stack = new ArrayDeque<>();
        for (final Direction facing : Direction.values()) {
            final TileEntity te = world.getTileEntity(this.getPos().offset(facing));
            if (te instanceof MetaChestTileEntity) {
                stack.push((MetaChestTileEntity) te);
            }
        }
        while (!stack.isEmpty()) {
            final MetaChestTileEntity tile = stack.pop();
            final boolean notContained = putHandler(tile);
            if (!notContained) continue;
            Arrays.stream(Direction.values())
                    .map(dir -> tile.getPos().offset(dir))
                    .filter(pos -> pos.distanceSq(this.pos) <= 64D)
                    .map(pos -> world.getTileEntity(pos))
                    .filter(t -> t instanceof MetaChestTileEntity)
                    .map(t -> (MetaChestTileEntity) t)
                    .filter(t -> !TILES.contains(t))
                    .forEach(stack::push);
        }
    }

    private boolean putHandler(final MetaChestTileEntity te) {
        final boolean notContained = TILES.add(te);
        if (notContained) {
            final LazyOptional<IItemHandler> cap = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
            cap.ifPresent(h -> {
                HANDLER.slots += te.getSize();
                HANDLER.handlers.add(cap);
            });
            cap.addListener(h -> {
                HANDLER.slots -= te.getSize();
                HANDLER.handlers.remove(h);
                TILES.remove(te);
            });
        }
        return notContained;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? CAP.cast() : super.getCapability(cap, side);
    }

    private final class ControllerHandler implements IItemHandler {

        private int slots;

        private final List<LazyOptional<IItemHandler>> handlers = new ArrayList<>();

        //This method will throw if there are no more handlers,
        //but it SHOULD be handled by the invalidation consumers.
        private List<IItemHandler> getHandlers() {
            return handlers.stream().map(op -> op.orElseThrow(IllegalStateException::new)).collect(Collectors.toList());
        }

        //The tuple serves as a double return, with the integer being the "real" slot.
        private Tuple<IItemHandler, Integer> getHandlerForSlot(int slot) {
            final List<IItemHandler> hand = getHandlers();
            for (final IItemHandler handler : hand) {
                final int size = handler.getSlots();
                if (slot < size) {
                    return new Tuple<>(handler, slot);
                } else {
                    slot -= size;
                }
            }
            throw new IllegalStateException("This shouldn't happen...");
        }

        @Override
        public int getSlots() {
            return slots;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            final Tuple<IItemHandler, Integer> tup = getHandlerForSlot(slot);
            return tup.getA().getStackInSlot(tup.getB());
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            final Tuple<IItemHandler, Integer> tup = getHandlerForSlot(slot);
            return tup.getA().insertItem(tup.getB(), stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            final Tuple<IItemHandler, Integer> tup = getHandlerForSlot(slot);
            return tup.getA().extractItem(tup.getB(), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            final Tuple<IItemHandler, Integer> tup = getHandlerForSlot(slot);
            return tup.getA().isItemValid(tup.getB(), stack);
        }
    }
}
