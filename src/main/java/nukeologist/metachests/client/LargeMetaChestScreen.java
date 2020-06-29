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

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import nukeologist.metachests.container.MetaChestContainer;

import static nukeologist.metachests.MetaChests.location;

public class LargeMetaChestScreen extends MetaChestScreen {

    private static final ResourceLocation GUI = location("textures/gui/largemetachestgui.png");

    public LargeMetaChestScreen(MetaChestContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.ySize = 236;
        this.xSize = 247;
    }

    @Override
    protected void func_230450_a_(MatrixStack matrix, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.getMinecraft().getTextureManager().bindTexture(GUI);
        this.func_238474_b_(matrix, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        this.searchField.func_230430_a_(matrix, mouseX, mouseY, partialTicks);
    }

    @Override
    protected int getSearchX() {
        return this.guiLeft + (171 /*new left*/ + 65 /*default width*/) - this.searchField.func_230998_h_();
    }
}
