package nukeologist.metachests;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ISearchTree;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.util.SearchTreeManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagCollection;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static nukeologist.metachests.MetaChests.location;

public class MetaChestScreen extends ContainerScreen<MetaChestContainer> {

    private static final ResourceLocation GUI = location("textures/gui/metachestgui.png");

    //taken from creative tab
    private TextFieldWidget searchField;
    private final Map<ResourceLocation, Tag<Item>> tagSearchResults = Maps.newTreeMap();
    private boolean field_195377_F;

    public final NonNullList<ItemStack> searchedStacks = NonNullList.create();

    public MetaChestScreen(MetaChestContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn);
        this.ySize = 195;
        this.xSize = 195;
    }

    @Override
    protected void init() {
        super.init();
        this.searchField = new TextFieldWidget(this.font, this.guiLeft + 82, this.guiTop + 6, 80, 9, I18n.format("itemGroup.search"));
        this.searchField.setMaxStringLength(50);
        this.searchField.setEnableBackgroundDrawing(false);
        this.searchField.setVisible(true);
        this.searchField.setCanLoseFocus(false);
        this.searchField.setFocused2(true);
        this.searchField.setTextColor(16777215);
        this.searchField.setWidth(89); //default 89
        this.searchField.x = this.guiLeft + (82 /*default left*/ + 89 /*default width*/) - this.searchField.getWidth();
        this.children.add(this.searchField);
    }

    @Override
    public void tick() {
        super.tick();
        this.searchField.tick();
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

    @Override   //copied
    protected void renderTooltip(ItemStack stack, int p_renderTooltip_2_, int p_renderTooltip_3_) {
        List<ITextComponent> list = stack.getTooltip(this.minecraft.player, this.minecraft.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
        List<String> list1 = Lists.newArrayListWithCapacity(list.size());

        for (ITextComponent itextcomponent : list) {
            list1.add(itextcomponent.getFormattedText());
        }

        Item item = stack.getItem();
        ItemGroup itemgroup1 = item.getGroup();
        if (itemgroup1 == null && item == Items.ENCHANTED_BOOK) {
            Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(stack);
            if (map.size() == 1) {
                Enchantment enchantment = map.keySet().iterator().next();

                for (ItemGroup itemgroup : ItemGroup.GROUPS) {
                    if (itemgroup.hasRelevantEnchantmentType(enchantment.type)) {
                        itemgroup1 = itemgroup;
                        break;
                    }
                }
            }
        }

        this.tagSearchResults.forEach((p_214083_2_, p_214083_3_) -> {
            if (p_214083_3_.contains(item)) {
                list1.add(1, "" + TextFormatting.BOLD + TextFormatting.DARK_PURPLE + "#" + p_214083_2_);
            }

        });
        if (itemgroup1 != null) {
            list1.add(1, "" + TextFormatting.BOLD + TextFormatting.BLUE + I18n.format(itemgroup1.getTranslationKey()));
        }

        for (int i = 0; i < list1.size(); ++i) {
            if (i == 0) {
                list1.set(i, stack.getRarity().color + list1.get(i));
            } else {
                list1.set(i, TextFormatting.GRAY + list1.get(i));
            }
        }

        net.minecraft.client.gui.FontRenderer font = stack.getItem().getFontRenderer(stack);
        net.minecraftforge.fml.client.config.GuiUtils.preItemToolTip(stack);
        this.renderTooltip(list1, p_renderTooltip_2_, p_renderTooltip_3_, (font == null ? this.font : font));
        net.minecraftforge.fml.client.config.GuiUtils.postItemToolTip();

    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.renderBackground();
        super.render(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(GUI);
        this.blit(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        this.searchField.render(mouseX, mouseY, partialTicks);
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
                isearchtree = this.minecraft.getSearchTree(SearchTreeManager.TAGS);
                this.searchTags(s);
            } else {
                isearchtree = this.minecraft.getSearchTree(SearchTreeManager.ITEMS);
            }

            searchedStacks.addAll(isearchtree.search(s.toLowerCase(Locale.ROOT)));
        }

        //this.currentScroll = 0.0F;
        //this.container.scrollTo(0.0F);
    }

    //copied
    private void searchTags(String search) {
        int i = search.indexOf(58);
        Predicate<ResourceLocation> predicate;
        if (i == -1) {
            predicate = (rs) -> rs.getPath().contains(search);
        } else {
            String s = search.substring(0, i).trim();
            String s1 = search.substring(i + 1).trim();
            predicate = (rs) -> rs.getNamespace().contains(s) && rs.getPath().contains(s1);
        }

        TagCollection<Item> tagcollection = ItemTags.getCollection();
        tagcollection.getRegisteredTags().stream().filter(predicate).forEach((rs) -> this.tagSearchResults.put(rs, tagcollection.get(rs)));
    }
}