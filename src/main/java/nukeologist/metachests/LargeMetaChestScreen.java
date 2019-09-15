package nukeologist.metachests;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import static nukeologist.metachests.MetaChests.location;

public class LargeMetaChestScreen extends MetaChestScreen {

    private static final ResourceLocation GUI = location("textures/gui/largemetachestgui.png");

    public LargeMetaChestScreen(MetaChestContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.ySize = 236;
        this.xSize = 247;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(GUI);
        this.blit(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        this.searchField.render(mouseX, mouseY, partialTicks);
    }

    @Override
    protected int getSearchX() {
        return this.guiLeft + (171 /*new left*/ + 65 /*default width*/) - this.searchField.getWidth();
    }
}
