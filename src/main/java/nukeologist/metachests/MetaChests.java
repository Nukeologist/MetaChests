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

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ObjectHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MetaChests.MODID)
public class MetaChests {

    public static final String MODID = "metachests";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @ObjectHolder(MODID + ":metachest")
    public static Block metaChestBlock;

    @ObjectHolder(MODID + ":metachest")
    public static TileEntityType metaChestTile;

    @ObjectHolder(MODID + ":metachest")
    public static ContainerType metaChestContainer;

    public MetaChests() {
        // Register the setup method for modloading
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::setup);
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Registering gui.");
        DistExecutor.runWhenOn(Dist.CLIENT, () -> ClientHandler.INSTANCE::init);
        LOGGER.info("Finished registering gui.");
    }

    @SubscribeEvent
    public void onServerStarting(final FMLServerStartingEvent event) {
        // do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {

        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
            LOGGER.info("Registering blocks");
            blockRegistryEvent.getRegistry().registerAll(
                    new MetaChestBlock(Block.Properties.create(Material.WOOD)).setRegistryName(location("metachest"))
            );
            LOGGER.info("Finished registering blocks");
        }

        @SubscribeEvent
        public static void onItemsRegistry(final RegistryEvent.Register<Item> itemRegistryEvent) {
            LOGGER.info("Registering items");
            itemRegistryEvent.getRegistry().registerAll(
                    new BlockItem(metaChestBlock, new Item.Properties().group(ItemGroup.DECORATIONS)).setRegistryName(location("metachest"))
            );
            LOGGER.info("Finished registering items");
        }

        @SubscribeEvent
        public static void onTilesRegistry(final RegistryEvent.Register<TileEntityType<?>> tileEntityTypeRegister) {
            LOGGER.info("Registering tileEntities");
            tileEntityTypeRegister.getRegistry().registerAll(
                    TileEntityType.Builder.create(MetaChestTileEntity::new, metaChestBlock).build(null).setRegistryName(metaChestBlock.getRegistryName())
            );
            LOGGER.info("Finished registering tile entities");
        }

        @SubscribeEvent
        public static void onContainerRegistry(final RegistryEvent.Register<ContainerType<?>> containerTypeRegister) {
            LOGGER.info("Registering containers");
            containerTypeRegister.getRegistry().registerAll(
                    IForgeContainerType.create(MetaChestContainer::new).setRegistryName(location("metachest"))
            );
            LOGGER.info("Finished registering containers");
        }
    }

    public static ResourceLocation location(final String path) {
        return new ResourceLocation(MODID, path);
    }
}
