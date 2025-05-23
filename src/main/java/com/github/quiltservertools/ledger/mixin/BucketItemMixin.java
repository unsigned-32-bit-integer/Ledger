package com.github.quiltservertools.ledger.mixin;

import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback;
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback;
import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback;
import com.github.quiltservertools.ledger.utility.Sources;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.block.Waterloggable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.BucketItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin {
    @Shadow
    @Final
    private Fluid fluid;

    @Inject(method = "placeFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;breakBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    private void logFluidBreak(LivingEntity user, World world, BlockPos pos, BlockHitResult hitResult, CallbackInfoReturnable<Boolean> cir) {
        var blockstate = world.getBlockState(pos);
        if (!blockstate.isAir() && user instanceof PlayerEntity player) {
            BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, world.getBlockState(pos), world.getBlockEntity(pos), Sources.FLUID, player);
        }
    }

    @Inject(method = "placeFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BucketItem;playEmptyingSound(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;)V"))
    private void logFluidPlace(LivingEntity user, World world, BlockPos pos, BlockHitResult hitResult, CallbackInfoReturnable<Boolean> cir) {
        if (user instanceof PlayerEntity player) {
            BlockPlaceCallback.EVENT.invoker().place(world, pos, this.fluid.getDefaultState().getBlockState(), null, player);
        } else {
            BlockPlaceCallback.EVENT.invoker().place(world, pos, this.fluid.getDefaultState().getBlockState(), null, Sources.REDSTONE);
        }
    }

    @Inject(
            method = "placeFluid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/BucketItem;playEmptyingSound(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;)V",
                    ordinal = 0
            )
    )
    private void logWaterlog(LivingEntity user, World world, BlockPos pos, BlockHitResult hitResult, CallbackInfoReturnable<Boolean> cir, @Local BlockState blockState) {
        if (user instanceof PlayerEntity player) {
            BlockChangeCallback.EVENT.invoker().changeBlock(
                    world,
                    pos,
                    blockState,
                    world.getBlockState(pos),
                    world.getBlockEntity(pos),
                    world.getBlockEntity(pos),
                    player
            );
        } else {
            BlockChangeCallback.EVENT.invoker().changeBlock(
                    world,
                    pos,
                    blockState,
                    world.getBlockState(pos),
                    world.getBlockEntity(pos),
                    world.getBlockEntity(pos),
                    Sources.REDSTONE
            ); //TODO This is dumb. Make some sort of Source wrapper
        }
    }

    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;incrementStat(Lnet/minecraft/stat/Stat;)V", ordinal = 0))
    private void logFluidPickup(World world, PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir, @Local(ordinal = 0) BlockPos pos, @Local BlockState blockState) {
        if (blockState.getBlock() instanceof Waterloggable) {
            BlockChangeCallback.EVENT.invoker().changeBlock(
                    world,
                    pos,
                    blockState,
                    world.getBlockState(pos),
                    world.getBlockEntity(pos),
                    world.getBlockEntity(pos),
                    player
            );
        } else {
            BlockBreakCallback.EVENT.invoker().breakBlock(world, pos, blockState, world.getBlockEntity(pos), player);
        }

    }


}
