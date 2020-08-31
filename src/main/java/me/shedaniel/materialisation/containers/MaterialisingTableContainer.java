package me.shedaniel.materialisation.containers;

import io.netty.buffer.Unpooled;
import me.shedaniel.materialisation.Materialisation;
import me.shedaniel.materialisation.MaterialisationUtils;
import me.shedaniel.materialisation.api.BetterIngredient;
import me.shedaniel.materialisation.api.Modifier;
import me.shedaniel.materialisation.api.ModifierIngredient;
import me.shedaniel.materialisation.api.PartMaterial;
import me.shedaniel.materialisation.items.MaterialisedMiningTool;
import me.shedaniel.materialisation.modifiers.Modifiers;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Pair;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;

public class MaterialisingTableContainer extends ScreenHandler {
    
    private final Inventory main, result;
    private final PlayerEntity player;
    private ScreenHandlerContext context;
    private String itemName;
    private int nextDecrease;
    
    public MaterialisingTableContainer(int syncId, PlayerInventory main) {
        this(syncId, main, ScreenHandlerContext.EMPTY);
    }
    
    public MaterialisingTableContainer(int syncId, PlayerInventory playerInventory, final ScreenHandlerContext context) {
        super(null, syncId);
        this.context = context;
        this.result = new CraftingResultInventory();
        this.main = new SimpleInventory(2) {
            public void markDirty() {
                super.markDirty();
                onContentChanged(this);
            }
        };
        this.player = playerInventory.player;
        this.addSlot(new Slot(this.main, 0, 27, 47));
        this.addSlot(new Slot(this.main, 1, 76, 47));
        this.addSlot(new Slot(this.result, 2, 134, 47) {
            public boolean canInsert(ItemStack itemStack_1) {
                return false;
            }
            
            public boolean canTakeItems(PlayerEntity playerEntity_1) {
                return hasStack();
            }
            
            public ItemStack onTakeItem(PlayerEntity playerEntity_1, ItemStack itemStack_1) {
                ItemStack stack = main.getStack(0).copy();
                stack.decrement(1);
                main.setStack(0, stack);
                stack = main.getStack(1).copy();
                stack.decrement(nextDecrease);
                main.setStack(1, stack);
                context.run((world, blockPos) -> {
                    ServerSidePacketRegistry.INSTANCE.sendToPlayer(playerEntity_1, Materialisation.MATERIALISING_TABLE_PLAY_SOUND, new PacketByteBuf(Unpooled.buffer()));
                });
                return itemStack_1;
            }
        });
        int int_4;
        for (int_4 = 0; int_4 < 3; ++int_4)
            for (int int_3 = 0; int_3 < 9; ++int_3)
                this.addSlot(new Slot(playerInventory, int_3 + int_4 * 9 + 9, 8 + int_3 * 18, 84 + int_4 * 18));
        for (int_4 = 0; int_4 < 9; ++int_4)
            this.addSlot(new Slot(playerInventory, int_4, 8 + int_4 * 18, 142));
    }
    
    @Override
    public boolean canUse(PlayerEntity playerEntity) {
        return this.context.run((world, blockPos) -> {
            return world.getBlockState(blockPos).getBlock() == Materialisation.MATERIALISING_TABLE && playerEntity.squaredDistanceTo(blockPos.getX() + .5D, blockPos.getY() + .5D, blockPos.getZ() + .5D) < 64D;
        }, true);
    }
    
    public void setNewItemName(String string_1) {
        this.itemName = string_1;
        if (this.getSlot(2).hasStack()) {
            ItemStack itemStack_1 = this.getSlot(2).getStack();
            if (StringUtils.isBlank(string_1)) {
                itemStack_1.removeCustomName();
            } else {
                itemStack_1.setCustomName(new LiteralText(this.itemName));
            }
        }
        this.updateResult();
    }
    
    @Override
    public void onContentChanged(Inventory inventory_1) {
        super.onContentChanged(inventory_1);
        if (inventory_1 == this.main) {
            this.updateResult();
        }
    }
    
    private void updateResult() {
        ItemStack first = this.main.getStack(0);
        ItemStack second = this.main.getStack(1);
        if (first.isEmpty()) {
            this.result.setStack(0, ItemStack.EMPTY);
        } else if (first.getItem() instanceof MaterialisedMiningTool
                   && first.getOrCreateTag().contains("mt_0_material")
                   && first.getOrCreateTag().contains("mt_1_material")
        ) {
            // Modifiers
            if (!second.isEmpty()) {
                ItemStack copy = first.copy();
                Map<Modifier, Integer> modifierIntegerMap = MaterialisationUtils.getToolModifiers(copy);
                for (Modifier modifier : Materialisation.MODIFIERS) {
                    Integer currentLevel = modifierIntegerMap.getOrDefault(modifier, 0);
                    if (modifier.isApplicableTo(copy) && modifier.getMaximumLevel(copy) > currentLevel) {
                        int nextLevel = currentLevel + 1;
                        Optional<Pair<Modifier, Pair<ModifierIngredient, BetterIngredient>>> modifierOptional
                                = Modifiers.getModifierByIngredient(second, modifier, nextLevel);
                        if (modifierOptional.isPresent()) {
                            MaterialisedMiningTool tool = (MaterialisedMiningTool) copy.getItem();
                            int maximumLevel = modifier.getMaximumLevel(first);
                            int level = tool.getModifierLevel(first, modifier);
                            tool.setModifierLevel(copy, modifier, level + 1);
                            if (level + 1 <= maximumLevel || MaterialisationUtils.getToolMaxDurability(copy) >= 1) {
                                nextDecrease = modifierOptional.get().getRight().getRight().count;
                                this.result.setStack(0, copy);
                            } else {
                                this.result.setStack(0, ItemStack.EMPTY);
                            }
                            this.sendContentUpdates();
                            return;
                        }
                    }
                }
            }
            
            
            // Fixing Special
            ItemStack copy = first.copy();
            int toolDurability = MaterialisationUtils.getToolDurability(first);
            int maxDurability = MaterialisationUtils.getToolMaxDurability(first);
            if (!second.isEmpty()) {
                if (toolDurability >= maxDurability) {
                    this.result.setStack(0, ItemStack.EMPTY);
                    this.sendContentUpdates();
                    return;
                }
                PartMaterial material = null;
                if (copy.getOrCreateTag().contains("mt_1_material"))
                    material = MaterialisationUtils.getMaterialFromString(copy.getOrCreateTag().getString("mt_1_material"));
                if (material == null) {
                    this.result.setStack(0, ItemStack.EMPTY);
                    this.sendContentUpdates();
                    return;
                }
                int repairAmount = material.getRepairAmount(second);
                if (repairAmount <= 0) {
                    this.result.setStack(0, ItemStack.EMPTY);
                    this.sendContentUpdates();
                    return;
                }
                MaterialisationUtils.setToolDurability(copy, Math.min(maxDurability, toolDurability + repairAmount));
            }
            if (StringUtils.isBlank(this.itemName)) {
                if (copy.hasCustomName())
                    copy.removeCustomName();
            } else if (!this.itemName.equals(copy.getName().getString()))
                if (itemName.equals(copy.getItem().getName(copy).getString()))
                    copy.removeCustomName();
                else
                    copy.setCustomName(new LiteralText(this.itemName));
            nextDecrease = 1;
            this.result.setStack(0, copy);
        } else if ((first.getItem() == Materialisation.PICKAXE_HEAD && second.getItem() == Materialisation.HANDLE) || (first.getItem() == Materialisation.HANDLE && second.getItem() == Materialisation.PICKAXE_HEAD)) {
            // Crafting a pickaxe
            int handle = 0, head = 0;
            if (first.getItem() == Materialisation.HANDLE)
                head = 1;
            else if (first.getItem() == Materialisation.PICKAXE_HEAD)
                handle = 1;
            PartMaterial handleMaterial = MaterialisationUtils.getMaterialFromPart(main.getStack(handle));
            PartMaterial headMaterial = MaterialisationUtils.getMaterialFromPart(main.getStack(head));
            if (handleMaterial == null || headMaterial == null) {
                this.result.setStack(0, ItemStack.EMPTY);
            } else {
                ItemStack copy = MaterialisationUtils.createPickaxe(handleMaterial, headMaterial);
                if (StringUtils.isBlank(this.itemName)) {
                    if (copy.hasCustomName())
                        copy.removeCustomName();
                } else if (!this.itemName.equals(copy.getName().getString()))
                    if (itemName.equals(copy.getItem().getName(copy).getString()))
                        copy.removeCustomName();
                    else
                        copy.setCustomName(new LiteralText(this.itemName));
                nextDecrease = 1;
                this.result.setStack(0, copy);
            }
        } else if ((first.getItem() == Materialisation.AXE_HEAD && second.getItem() == Materialisation.HANDLE) || (first.getItem() == Materialisation.HANDLE && second.getItem() == Materialisation.AXE_HEAD)) {
            // Crafting an axe
            int handle = 0, head = 0;
            if (first.getItem() == Materialisation.HANDLE)
                head = 1;
            else if (first.getItem() == Materialisation.AXE_HEAD)
                handle = 1;
            PartMaterial handleMaterial = MaterialisationUtils.getMaterialFromPart(main.getStack(handle));
            PartMaterial headMaterial = MaterialisationUtils.getMaterialFromPart(main.getStack(head));
            if (handleMaterial == null || headMaterial == null) {
                this.result.setStack(0, ItemStack.EMPTY);
            } else {
                ItemStack copy = MaterialisationUtils.createAxe(handleMaterial, headMaterial);
                if (StringUtils.isBlank(this.itemName)) {
                    if (copy.hasCustomName())
                        copy.removeCustomName();
                } else if (!this.itemName.equals(copy.getName().getString()))
                    if (itemName.equals(copy.getItem().getName(copy).getString()))
                        copy.removeCustomName();
                    else
                        copy.setCustomName(new LiteralText(this.itemName));
                nextDecrease = 1;
                this.result.setStack(0, copy);
            }
        } else if ((first.getItem() == Materialisation.SHOVEL_HEAD && second.getItem() == Materialisation.HANDLE) || (first.getItem() == Materialisation.HANDLE && second.getItem() == Materialisation.SHOVEL_HEAD)) {
            // Crafting a shovel
            int handle = 0, head = 0;
            if (first.getItem() == Materialisation.HANDLE)
                head = 1;
            else if (first.getItem() == Materialisation.SHOVEL_HEAD)
                handle = 1;
            PartMaterial handleMaterial = MaterialisationUtils.getMaterialFromPart(main.getStack(handle));
            PartMaterial headMaterial = MaterialisationUtils.getMaterialFromPart(main.getStack(head));
            if (handleMaterial == null || headMaterial == null) {
                this.result.setStack(0, ItemStack.EMPTY);
            } else {
                ItemStack copy = MaterialisationUtils.createShovel(handleMaterial, headMaterial);
                if (StringUtils.isBlank(this.itemName)) {
                    if (copy.hasCustomName())
                        copy.removeCustomName();
                } else if (!this.itemName.equals(copy.getName().getString()))
                    if (itemName.equals(copy.getItem().getName(copy).getString()))
                        copy.removeCustomName();
                    else
                        copy.setCustomName(new LiteralText(this.itemName));
                nextDecrease = 1;
                this.result.setStack(0, copy);
            }
        } else if ((first.getItem() == Materialisation.SWORD_BLADE && second.getItem() == Materialisation.HANDLE) || (first.getItem() == Materialisation.HANDLE && second.getItem() == Materialisation.SWORD_BLADE)) {
            // Crafting a sword
            int handle = 0, head = 0;
            if (first.getItem() == Materialisation.HANDLE)
                head = 1;
            else if (first.getItem() == Materialisation.SWORD_BLADE)
                handle = 1;
            PartMaterial handleMaterial = MaterialisationUtils.getMaterialFromPart(main.getStack(handle));
            PartMaterial headMaterial = MaterialisationUtils.getMaterialFromPart(main.getStack(head));
            if (handleMaterial == null || headMaterial == null) {
                this.result.setStack(0, ItemStack.EMPTY);
            } else {
                ItemStack copy = MaterialisationUtils.createSword(handleMaterial, headMaterial);
                if (StringUtils.isBlank(this.itemName)) {
                    if (copy.hasCustomName())
                        copy.removeCustomName();
                } else if (!this.itemName.equals(copy.getName().getString()))
                    if (itemName.equals(copy.getItem().getName(copy).getString()))
                        copy.removeCustomName();
                    else
                        copy.setCustomName(new LiteralText(this.itemName));
                nextDecrease = 1;
                this.result.setStack(0, copy);
            }
        } else if ((first.getItem() == Materialisation.MEGAAXE_HEAD && second.getItem() == Materialisation.HANDLE) || (first.getItem() == Materialisation.HANDLE && second.getItem() == Materialisation.MEGAAXE_HEAD)) {
            // Crafting a mega axe
            int handle = 0, head = 0;
            if (first.getItem() == Materialisation.HANDLE)
                head = 1;
            else if (first.getItem() == Materialisation.MEGAAXE_HEAD)
                handle = 1;
            PartMaterial handleMaterial = MaterialisationUtils.getMaterialFromPart(main.getStack(handle));
            PartMaterial headMaterial = MaterialisationUtils.getMaterialFromPart(main.getStack(head));
            if (handleMaterial == null || headMaterial == null) {
                this.result.setStack(0, ItemStack.EMPTY);
            } else {
                ItemStack copy = MaterialisationUtils.createMegaAxe(handleMaterial, headMaterial);
                if (StringUtils.isBlank(this.itemName)) {
                    if (copy.hasCustomName())
                        copy.removeCustomName();
                } else if (!this.itemName.equals(copy.getName().getString()))
                    if (itemName.equals(copy.getItem().getName(copy).getString()))
                        copy.removeCustomName();
                    else
                        copy.setCustomName(new LiteralText(this.itemName));
                nextDecrease = 1;
                this.result.setStack(0, copy);
            }
        } else if ((first.getItem() == Materialisation.HAMMER_HEAD && second.getItem() == Materialisation.HANDLE) || (first.getItem() == Materialisation.HANDLE && second.getItem() == Materialisation.HAMMER_HEAD)) {
            // Crafting a hammer
            int handle = 0, head = 0;
            if (first.getItem() == Materialisation.HANDLE)
                head = 1;
            else if (first.getItem() == Materialisation.HAMMER_HEAD)
                handle = 1;
            PartMaterial handleMaterial = MaterialisationUtils.getMaterialFromPart(main.getStack(handle));
            PartMaterial headMaterial = MaterialisationUtils.getMaterialFromPart(main.getStack(head));
            if (handleMaterial == null || headMaterial == null) {
                this.result.setStack(0, ItemStack.EMPTY);
            } else {
                ItemStack copy = MaterialisationUtils.createHammer(handleMaterial, headMaterial);
                if (StringUtils.isBlank(this.itemName)) {
                    if (copy.hasCustomName())
                        copy.removeCustomName();
                } else if (!this.itemName.equals(copy.getName().getString()))
                    if (itemName.equals(copy.getItem().getName(copy).getString()))
                        copy.removeCustomName();
                    else
                        copy.setCustomName(new LiteralText(this.itemName));
                nextDecrease = 1;
                this.result.setStack(0, copy);
            }
        } else {
            this.result.setStack(0, ItemStack.EMPTY);
        }
        this.sendContentUpdates();
    }
    
    @Override
    public void close(PlayerEntity playerEntity_1) {
        super.close(playerEntity_1);
        this.context.run((world_1, blockPos_1) -> {
            this.dropInventory(playerEntity_1, world_1, this.main);
        });
    }
    
    @Override
    public ItemStack transferSlot(PlayerEntity playerEntity_1, int int_1) {
        ItemStack itemStack_1 = ItemStack.EMPTY;
        Slot slot_1 = this.slots.get(int_1);
        if (slot_1 != null && slot_1.hasStack()) {
            ItemStack itemStack_2 = slot_1.getStack();
            itemStack_1 = itemStack_2.copy();
            if (int_1 == 2) {
                if (!this.insertItem(itemStack_2, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                
                slot_1.onStackChanged(itemStack_2, itemStack_1);
            } else if (int_1 != 0 && int_1 != 1) {
                if (int_1 >= 3 && int_1 < 39 && !this.insertItem(itemStack_2, 0, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(itemStack_2, 3, 39, false)) {
                return ItemStack.EMPTY;
            }
            
            if (itemStack_2.isEmpty()) {
                slot_1.setStack(ItemStack.EMPTY);
            } else {
                slot_1.markDirty();
            }
            
            if (itemStack_2.getCount() == itemStack_1.getCount()) {
                return ItemStack.EMPTY;
            }
            
            slot_1.onTakeItem(playerEntity_1, itemStack_2);
        }
        
        return itemStack_1;
    }
    
}
