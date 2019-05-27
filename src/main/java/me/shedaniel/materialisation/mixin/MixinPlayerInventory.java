package me.shedaniel.materialisation.mixin;

import me.shedaniel.cloth.api.ReflectionUtils;
import me.shedaniel.materialisation.Materialisation;
import me.shedaniel.materialisation.MaterialisationUtils;
import me.shedaniel.materialisation.items.MaterialisedAxeItem;
import me.shedaniel.materialisation.items.MaterialisedPickaxeItem;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.fabric.impl.tools.ToolManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.Tag;
import net.minecraft.util.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public class MixinPlayerInventory {
    
    @Shadow @Final public DefaultedList<ItemStack> main;
    
    @Shadow public int selectedSlot;
    
    private static TriState mt_handleIsEffectiveOn(ItemStack stack, BlockState state) {
        ToolManager.Entry entry = (ToolManager.Entry) ToolManager.entry(state.getBlock());
        Tag<Item>[] tags = Materialisation.getReflectionField(entry, Tag[].class, 0).orElse(new Tag[0]);
        int[] tagLevels = Materialisation.getReflectionField(entry, int[].class, 1).orElse(new int[tags.length]);
        Item item = stack.getItem();
        for(int i = 0; i < tags.length; ++i)
            if (item.matches(tags[i]))
                return TriState.of(MaterialisationUtils.getToolMiningLevel(stack) >= tagLevels[i]);
        return ReflectionUtils.getField(entry, TriState.class, 2).orElse(TriState.DEFAULT);
    }
    
    /**
     * Applies the block breaking speed of tools, using the fabric api
     */
    @Inject(method = "getBlockBreakingSpeed", at = @At("HEAD"), cancellable = true)
    public void getBlockBreakingSpeed(BlockState state, CallbackInfoReturnable<Float> callbackInfo) {
        ItemStack itemStack = main.get(selectedSlot);
        if (itemStack.getItem() instanceof MaterialisedPickaxeItem) {
            if (MaterialisationUtils.getToolDurability(itemStack) <= 0) {
                // If there is not durability left
                callbackInfo.setReturnValue(-1f);
            } else {
                TriState triState = mt_handleIsEffectiveOn(itemStack, state);
                if (triState != TriState.DEFAULT) {
                    // If we are dealing with 3rd party blocks
                    callbackInfo.setReturnValue(triState.get() ? MaterialisationUtils.getToolBreakingSpeed(itemStack) : 1.0F);
                } else {
                    // Lastly if we are not dealing with 3rd party blocks with durability left
                    callbackInfo.setReturnValue(MaterialisedPickaxeItem.getToolBlockBreakingSpeed(itemStack, state));
                }
            }
        } else if (itemStack.getItem() instanceof MaterialisedAxeItem) {
            if (MaterialisationUtils.getToolDurability(itemStack) <= 0) {
                // If there is not durability left
                callbackInfo.setReturnValue(-1f);
            } else {
                TriState triState = mt_handleIsEffectiveOn(itemStack, state);
                if (triState != TriState.DEFAULT) {
                    // If we are dealing with 3rd party blocks
                    callbackInfo.setReturnValue(triState.get() ? MaterialisationUtils.getToolBreakingSpeed(itemStack) : 1.0F);
                } else {
                    // Lastly if we are not dealing with 3rd party blocks with durability left
                    callbackInfo.setReturnValue(MaterialisedAxeItem.getToolBlockBreakingSpeed(itemStack, state));
                }
            }
        }
    }
    
    /**
     * Checks the mining level of the tool, using the fabric api
     */
    @Inject(method = "isUsingEffectiveTool", at = @At("HEAD"), cancellable = true)
    public void isUsingEffectiveTool(BlockState state, CallbackInfoReturnable<Boolean> callbackInfo) {
        ItemStack itemStack = main.get(selectedSlot);
        if (itemStack.getItem() instanceof MaterialisedPickaxeItem) {
            if (MaterialisationUtils.getToolDurability(itemStack) <= 0) {
                // If there is not durability left
                callbackInfo.setReturnValue(false);
            } else {
                TriState triState = mt_handleIsEffectiveOn(itemStack, state);
                if (triState != TriState.DEFAULT) {
                    // If we are dealing with 3rd party blocks
                    callbackInfo.setReturnValue(triState.get());
                } else {
                    // Lastly if we are not dealing with 3rd party blocks with durability left
                    callbackInfo.setReturnValue(MaterialisedPickaxeItem.canEffectivelyBreak(itemStack, state));
                }
            }
        } else if (itemStack.getItem() instanceof MaterialisedAxeItem) {
            if (MaterialisationUtils.getToolDurability(itemStack) <= 0) {
                // If there is not durability left
                callbackInfo.setReturnValue(false);
            } else {
                TriState triState = mt_handleIsEffectiveOn(itemStack, state);
                if (triState != TriState.DEFAULT) {
                    // If we are dealing with 3rd party blocks
                    callbackInfo.setReturnValue(triState.get());
                } else {
                    // Lastly if we are not dealing with 3rd party blocks with durability left
                    callbackInfo.setReturnValue(MaterialisedAxeItem.canEffectivelyBreak(itemStack, state));
                }
            }
        }
    }
    
}