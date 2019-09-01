package nukeologist.metachests;

import net.minecraft.client.gui.ScreenManager;

public enum ClientHandler {

    INSTANCE;

    public void init() {
        ScreenManager.registerFactory(MetaChests.metaChestContainer, MetaChestScreen::new);
    }
}
