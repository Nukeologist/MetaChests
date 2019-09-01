package nukeologist.metachests;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;

public class MetaChestTileEntity extends TileEntity implements INamedContainerProvider {

    private static final int SIZE = 45;

    private final ItemStackHandler itemHandler;
    private final LazyOptional<IItemHandler> INVENTORY;

    private ItemGroup itemGroup;

    public MetaChestTileEntity() {
        super(MetaChests.metaChestTile);
        this.itemHandler = new ItemStackHandler(SIZE) {
            @Override
            protected void onContentsChanged(int slot) {
                MetaChestTileEntity.this.markDirty();
                if (MetaChestTileEntity.this.getStackOfSlots().isEmpty())
                    MetaChestTileEntity.this.setItemGroup(null);
                    //this.stacks.sort(Comparator.comparing(stack -> stack.getDisplayName().getString()));
            }

            @Override
            public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
                super.setStackInSlot(slot, stack);
                if (stack.getItem().getGroup() != null && MetaChestTileEntity.this.getItemGroup() == null)
                    MetaChestTileEntity.this.setItemGroup(stack.getItem().getGroup());
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                ItemGroup group = MetaChestTileEntity.this.getItemGroup();
                if (group != null) {
                    return group == stack.getItem().getGroup();
                }
                return true;
            }

            @Nonnull
            @Override
            public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                ItemGroup stackGroup = stack.getItem().getGroup();
                if (MetaChestTileEntity.this.getItemGroup() == null) {
                    if (stackGroup == null) {
                        return stack;
                    } else {
                        if (!simulate) {
                            MetaChestTileEntity.this.setItemGroup(stackGroup);
                        }
                        return super.insertItem(slot, stack, simulate);
                    }
                } else {
                    if (stackGroup == null) {
                        return stack;
                    } else {
                        if (MetaChestTileEntity.this.getItemGroup() == stackGroup) {
                            return super.insertItem(slot, stack, simulate);
                        } else {
                            return stack;
                        }
                    }
                }
            }
        };
        this.INVENTORY = LazyOptional.of(() -> this.itemHandler);
    }

    @Override
    public void read(CompoundNBT compound) {
        CompoundNBT invTag = compound.getCompound("inv");
        itemHandler.deserializeNBT(invTag);
        super.read(compound);
        if (this.getItemGroup() == null) {
            ItemStack stack = this.getStackOfSlots(); //if there is an item here, its item group SHOULD NOT be null.
            if (!stack.isEmpty()) this.setItemGroup(stack.getItem().getGroup());
        }
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        CompoundNBT invTag = itemHandler.serializeNBT();
        compound.put("inv", invTag);
        return super.write(compound);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? INVENTORY.cast() : super.getCapability(cap);
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

    public void setItemGroup(ItemGroup itemGroup) {
        this.itemGroup = itemGroup;
    }

    private ItemStack getStackOfSlots() {
        int slots = itemHandler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty())
                return stack;
        }
        return ItemStack.EMPTY;
    }
}
