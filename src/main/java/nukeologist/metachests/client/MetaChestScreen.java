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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ISearchTree;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.util.SearchTreeManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagCollection;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import nukeologist.metachests.container.MetaChestContainer;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static nukeologist.metachests.MetaChests.location;

public class MetaChestScreen extends ContainerScreen<MetaChestContainer> {

    private static final ResourceLocation GUI = location("textures/gui/metachestgui.png");

    //taken from creative tab
    protected TextFieldWidget searchField;
    private final Map<ResourceLocation, ITag<Item>> tagSearchResults = Maps.newTreeMap();
    private ItemGroup group;
    private boolean field_195377_F;

    public final NonNullList<ItemStack> searchedStacks = NonNullList.create();

    public MetaChestScreen(MetaChestContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.ySize = 195;
        this.xSize = 176;
    }

    @Override
    protected void init() {
        super.init();
        this.searchField = new TextFieldWidget(this.font, this.guiLeft + 100, this.guiTop + 6, 70, 9, new TranslationTextComponent("itemGroup.search"));
        this.searchField.setMaxStringLength(50);
        this.searchField.setEnableBackgroundDrawing(false);
        this.searchField.setVisible(true);
        this.searchField.setCanLoseFocus(false);
        this.searchField.setFocused2(true);
        this.searchField.setTextColor(16777215);
        this.searchField.setWidth(65); //default 89
        this.searchField.x = getSearchX();
        this.children.add(this.searchField);
        this.group = this.getItemGroup();
    }

    @Override
    protected void func_230451_b_(MatrixStack p_230451_1_, int p_230451_2_, int p_230451_3_) {
        //do not draw title
    }

    protected int getSearchX() {
        return this.guiLeft + (100 /*default left*/ + 65 /*default width*/) - this.searchField.getWidth();
    }

    @Override
    public void tick() {
        super.tick();
        this.searchField.tick();
    }

    private void highLightSlots(MatrixStack matrix) {
        final List<Item> itemList = searchedStacks.stream().map(ItemStack::getItem).collect(Collectors.toList());
        this.container.inventorySlots.forEach(slot -> {
            if (!itemList.contains(slot.getStack().getItem())) {
                int slotColor;
                //RenderSystem.disableLighting(); Was here in 1.14
                RenderSystem.disableDepthTest();
                int j1 = slot.xPos + this.guiLeft;
                int k1 = slot.yPos + this.guiTop;
                RenderSystem.colorMask(true, true, true, false);
                slotColor = this.getSlotColor(slot.slotNumber);
                this.blit(matrix, j1, k1, j1 + 16, k1 + 16, slotColor, slotColor);
                RenderSystem.colorMask(true, true, true, true);
                //RenderSystem.enableLighting(); Was here in 1.14
                RenderSystem.enableDepthTest();
            }
        });

    }

    @Override
    public boolean charTyped(char p_charTyped_1_, int p_charTyped_2_) {
        if (this.field_195377_F) {
            return false;
        }
        String s = this.searchField.getText();
        if (this.searchField.charTyped(p_charTyped_1_, p_charTyped_2_)) {
            if (!Objects.equals(s, this.searchField.getText())) {
                this.updateCreativeSearch();
            }
            return true;
        }
        return super.charTyped(p_charTyped_1_, p_charTyped_2_);
    }

    @Override
    public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
        this.field_195377_F = false;
        //boolean flag = !this.hasTmpInventory(this.hoveredSlot) || this.hoveredSlot != null && this.hoveredSlot.getHasStack();
        boolean flag = this.hoveredSlot != null && this.hoveredSlot.getHasStack();
        if (flag && this.func_195363_d(p_keyPressed_1_, p_keyPressed_2_)) {
            this.field_195377_F = true;
            return true;
        } else {
            String s = this.searchField.getText();
            if (this.searchField.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_)) {
                if (!Objects.equals(s, this.searchField.getText())) {
                    this.updateCreativeSearch();
                }

                return true;
            } else {
                return this.searchField.isFocused() && this.searchField.getVisible() && p_keyPressed_1_ != 256 || super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
            }
        }
        //return super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        this.field_195377_F = false;
        return super.keyReleased(keyCode, scanCode, slotColor);
    }

    @Override   //copied
    protected void renderTooltip(MatrixStack matrix, ItemStack stack, int p_renderTooltip_2_, int p_renderTooltip_3_) {
        List<ITextComponent> list = stack.getTooltip(this.minecraft.player, this.minecraft.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
        List<ITextComponent> list1 = Lists.newArrayList(list);
        Item item = stack.getItem();
        ItemGroup itemgroup = item.getGroup();
        if (itemgroup == null && item == Items.ENCHANTED_BOOK) {
            Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(stack);
            if (map.size() == 1) {
                Enchantment enchantment = map.keySet().iterator().next();

                for(ItemGroup itemgroup1 : ItemGroup.GROUPS) {
                    if (itemgroup1.hasRelevantEnchantmentType(enchantment.type)) {
                        itemgroup = itemgroup1;
                        break;
                    }
                }
            }
        }

        this.tagSearchResults.forEach((p_214083_2_, p_214083_3_) -> {
            if (p_214083_3_.contains(item)) {
                list1.add(1, (new StringTextComponent("#" + p_214083_2_)).func_240699_a_(TextFormatting.DARK_PURPLE));
            }

        });
        if (itemgroup != null) {
            list1.add(1, (new TranslationTextComponent(itemgroup.getTranslationKey())).func_240699_a_(TextFormatting.BLUE));
        }

        net.minecraft.client.gui.FontRenderer font = stack.getItem().getFontRenderer(stack);
        net.minecraftforge.fml.client.gui.GuiUtils.preItemToolTip(stack);
        this.renderTooltip(matrix, list1, p_renderTooltip_2_, p_renderTooltip_3_, (font == null ? this.font : font));
        net.minecraftforge.fml.client.gui.GuiUtils.postItemToolTip();

    }

    @Override
    public void render (MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrix);

        super.render(matrix, mouseX, mouseY, partialTicks);
        if (!this.searchField.getText().isEmpty()) {
            this.highLightSlots(matrix);
        }
        this.getItemGroup();
        if (this.group != null) {
            this.font.drawString(matrix, I18n.format(this.group.getTranslationKey()), this.guiLeft + 4, this.guiTop + 6, 4210752);
        }
        this.func_230459_a_(matrix, mouseX, mouseY);

    }

    @Override
    protected void func_230450_a_(MatrixStack matrix, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.getMinecraft().getTextureManager().bindTexture(GUI);
        this.blit(matrix, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        this.searchField.render(matrix, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    //copied and modified
    private void updateCreativeSearch() {
        searchedStacks.clear();
        this.tagSearchResults.clear();

        String s = this.searchField.getText();
        if (!s.isEmpty()) {

            ISearchTree<ItemStack> isearchtree;
            if (s.startsWith("#")) {
                s = s.substring(1);
                isearchtree = this.getMinecraft().getSearchTree(SearchTreeManager.TAGS);
                this.searchTags(s);
            } else {
                isearchtree = this.getMinecraft().getSearchTree(SearchTreeManager.ITEMS);
            }

            searchedStacks.addAll(isearchtree.search(s.toLowerCase(Locale.ROOT)));
        }

        //this.currentScroll = 0.0F;
        //this.container.scrollTo(0.0F);
    }

    private ItemGroup getItemGroup() {
        final int containerSlots = this.container.getTeSlotsSize();
        for (int i = 0; i < containerSlots; i++) {
            final Slot slot = this.container.inventorySlots.get(i);
            final ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                this.group = stack.getItem().getGroup();
                return stack.getItem().getGroup();
            }
        }
        this.group = null;
        return null;
    }

    //copied
    private void searchTags(String search) {
        int i = search.indexOf(58);
        Predicate<ResourceLocation> predicate;
        if (i == -1) {
            predicate = (p_214084_1_) -> p_214084_1_.getPath().contains(search);
        } else {
            String s = search.substring(0, i).trim();
            String s1 = search.substring(i + 1).trim();
            predicate = (p_214081_2_) -> p_214081_2_.getNamespace().contains(s) && p_214081_2_.getPath().contains(s1);
        }

        TagCollection<Item> tagcollection = ItemTags.getCollection();
        tagcollection.getRegisteredTags().stream().filter(predicate).forEach((p_214082_2_) -> this.tagSearchResults.put(p_214082_2_, tagcollection.get(p_214082_2_)));
    }
}