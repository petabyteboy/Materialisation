package me.shedaniel.materialisation.containers;

import com.mojang.blaze3d.systems.RenderSystem;
import io.netty.buffer.Unpooled;
import me.shedaniel.materialisation.Materialisation;
import me.shedaniel.materialisation.ModReference;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

@Environment(EnvType.CLIENT)
public class MaterialisingTableScreen extends HandledScreen<MaterialisingTableContainer> implements ScreenHandlerListener {
    
    private static final Identifier BG_TEX = new Identifier(ModReference.MOD_ID, "textures/gui/container/materialising_table.png");
    private TextFieldWidget nameField;
    
    public MaterialisingTableScreen(MaterialisingTableContainer container, PlayerInventory inventory, Text title) {
        super(container, inventory, title);
    }
    
    @Override
    protected void init() {
        super.init();
        this.client.keyboard.setRepeatEvents(true);
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        this.nameField = new TextFieldWidget(this.textRenderer, x + 38, y + 24, 103, 12, new TranslatableText("container.repair"));
        this.nameField.setFocusUnlocked(false);
        this.nameField.changeFocus(true);
        this.nameField.setEditableColor(-1);
        this.nameField.setUneditableColor(-1);
        this.nameField.setHasBorder(false);
        this.nameField.setMaxLength(35);
        this.nameField.setChangedListener(this::onChangeName);
        this.children.add(this.nameField);
        this.handler.addListener(this);
        this.setInitialFocus(this.nameField);
    }
    
    @Override
    public void resize(MinecraftClient minecraftClient_1, int int_1, int int_2) {
        String string_1 = this.nameField.getText();
        this.init(minecraftClient_1, int_1, int_2);
        this.nameField.setText(string_1);
    }
    
    @Override
    public void removed() {
        super.removed();
        this.client.keyboard.setRepeatEvents(false);
        this.handler.removeListener(this);
    }
    
    @Override
    public boolean keyPressed(int int_1, int int_2, int int_3) {
        if (int_1 == 256) {
            this.client.player.closeHandledScreen();
        }
        return this.nameField.keyPressed(int_1, int_2, int_3) || this.nameField.isActive() || super.keyPressed(int_1, int_2, int_3);
    }
    
    @Override
    protected void drawForeground(MatrixStack matrixStack, int i, int j) {
        RenderSystem.disableBlend();
        this.textRenderer.draw(matrixStack, this.title, 6f, 6f, 4210752);
    }
    
    private void onChangeName(String string_1) {
        if (!string_1.isEmpty()) {
            String string_2 = string_1;
            Slot slot_1 = this.handler.getSlot(2);
            if (slot_1 != null && slot_1.hasStack() && !slot_1.getStack().hasCustomName() && string_1.equals(slot_1.getStack().getName().getString())) {
                string_2 = "";
            }
            
            this.handler.setNewItemName(string_2);
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeString(string_2);
            ClientSidePacketRegistry.INSTANCE.sendToServer(Materialisation.MATERIALISING_TABLE_RENAME, buf);
        }
    }
    
    @Override
    protected void drawBackground(MatrixStack matrixStack, float v, int i, int i1) {
        this.client.getTextureManager().bindTexture(BG_TEX);
        int int_3 = x;
        int int_4 = y;
        this.drawTexture(matrixStack, int_3, int_4, 0, 0, this.backgroundWidth, this.backgroundHeight);
        this.drawTexture(matrixStack, int_3 + 34, int_4 + 20, 0, this.backgroundHeight + (this.handler.getSlot(0).hasStack() ? 0 : 16), 110, 16);
        if ((this.handler.getSlot(0).hasStack() || this.handler.getSlot(1).hasStack()) && !this.handler.getSlot(2).hasStack()) {
            this.drawTexture(matrixStack, int_3 + 99, int_4 + 45, this.backgroundWidth, 0, 28, 21);
        }
    }
    
    @Override
    public void render(MatrixStack matrixStack, int int_1, int int_2, float float_1) {
        renderBackground(matrixStack);
        super.render(matrixStack, int_1, int_2, float_1);
        RenderSystem.disableBlend();
        this.nameField.render(matrixStack, int_1, int_2, float_1);
        this.drawMouseoverTooltip(matrixStack, int_1, int_2);
    }
    
    @Override
    public void onHandlerRegistered(ScreenHandler container, DefaultedList<ItemStack> defaultedList) {
        this.onSlotUpdate(container, 2, container.getSlot(2).getStack());
    }
    
    @Override
    public void onSlotUpdate(ScreenHandler container, int i, ItemStack itemStack) {
        if (i == 2) {
            this.nameField.setChangedListener(null);
            this.nameField.setText(!container.getSlot(i).hasStack() ? "" : itemStack.getName().getString());
            this.nameField.setEditable(!itemStack.isEmpty());
            this.nameField.setChangedListener(this::onChangeName);
        }
    }
    
    @Override
    public void onPropertyUpdate(ScreenHandler container, int i, int i1) {
        
    }
    
}
