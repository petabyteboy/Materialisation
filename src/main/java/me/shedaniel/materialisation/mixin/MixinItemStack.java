package me.shedaniel.materialisation.mixin;

import me.shedaniel.materialisation.MaterialisationUtils;
import me.shedaniel.materialisation.items.MaterialisedMiningTool;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class MixinItemStack {
    
    @Shadow
    public abstract Item getItem();
    
    @Shadow
    public abstract CompoundTag getTag();
    
    @Shadow public abstract boolean hasEnchantments();
    
    /**
     * Disable italic on tools
     */
    @Inject(method = "hasCustomName", at = @At("HEAD"), cancellable = true)
    public void hasDisplayName(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (getItem() instanceof MaterialisedMiningTool)
            callbackInfo.setReturnValue(false);
    }
    
    @Inject(method = "isDamageable", at = @At("HEAD"), cancellable = true)
    public void isDamageable(CallbackInfoReturnable<Boolean> returnable) {
        if (getItem() instanceof MaterialisedMiningTool) {
            CompoundTag compoundTag_1 = getTag();
            returnable.setReturnValue(compoundTag_1 == null || !compoundTag_1.getBoolean("Unbreakable"));
        }
    }
    
    @Inject(method = "isEnchantable", at = @At("HEAD"), cancellable = true)
    public void isEnchantable(CallbackInfoReturnable<Boolean> returnable) {
        if (getItem() instanceof MaterialisedMiningTool)
            returnable.setReturnValue(!hasEnchantments());
    }
    
    @Inject(method = "getDamage", at = @At("HEAD"), cancellable = true)
    public void getDamage(CallbackInfoReturnable<Integer> returnable) {
        if (getItem() instanceof MaterialisedMiningTool) {
            int maxDurability = MaterialisationUtils.getToolMaxDurability((ItemStack) (Object) this);
            returnable.setReturnValue(maxDurability - MaterialisationUtils.getToolDurability((ItemStack) (Object) this) - 1);
        }
    }
    
    @Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
    public void getMaxDamage(CallbackInfoReturnable<Integer> returnable) {
        if (getItem() instanceof MaterialisedMiningTool)
            returnable.setReturnValue(MaterialisationUtils.getToolMaxDurability((ItemStack) (Object) this));
    }
    
    @Inject(method = "setDamage", at = @At("HEAD"), cancellable = true)
    public void setDamage(int damage, CallbackInfo info) {
        if (getItem() instanceof MaterialisedMiningTool) {
            int maxDurability = MaterialisationUtils.getToolMaxDurability((ItemStack) (Object) this);
            MaterialisationUtils.setToolDurability((ItemStack) (Object) this, maxDurability - MathHelper.clamp(damage, 0, maxDurability));
            info.cancel();
        }
    }
    
}
