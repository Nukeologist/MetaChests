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

package nukeologist.metachests.client;

import net.minecraft.client.gui.ScreenManager;
import nukeologist.metachests.MetaChests;

public enum ClientHandler {

    INSTANCE;

    public void init() {
        ScreenManager.registerFactory(MetaChests.metaChestContainer, MetaChestScreen::new);
        ScreenManager.registerFactory(MetaChests.largeMetaChestContainer, LargeMetaChestScreen::new);
    }
}
