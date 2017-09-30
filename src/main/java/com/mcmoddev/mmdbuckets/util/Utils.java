package com.mcmoddev.mmdbuckets.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class Utils {
	private static RayTraceResult getMovingObjectPosWithReachDistance(World world, EntityPlayer player, double distance, boolean p1, boolean p2, boolean p3){
		float f = player.rotationPitch;
		float f1 = player.rotationYaw;
		double d0 = player.posX;
		double d1 = player.posY+(double)player.getEyeHeight();
		double d2 = player.posZ;
		Vec3d vec3 = new Vec3d(d0, d1, d2);
		float f2 = MathHelper.cos(-f1*0.017453292F-(float)Math.PI);
		float f3 = MathHelper.sin(-f1*0.017453292F-(float)Math.PI);
		float f4 = -MathHelper.cos(-f*0.017453292F);
		float f5 = MathHelper.sin(-f*0.017453292F);
		float f6 = f3*f4;
		float f7 = f2*f4;
		Vec3d vec31 = vec3.addVector((double)f6*distance, (double)f5*distance, (double)f7*distance);
		return world.rayTraceBlocks(vec3, vec31, p1, p2, p3);
	}

	public static RayTraceResult getNearestBlockWithDefaultReachDistance(World world, EntityPlayer player){
		return getNearestBlockWithDefaultReachDistance(world, player, false, true, false);
	}

	public static RayTraceResult getNearestBlockWithDefaultReachDistance(World world, EntityPlayer player, boolean stopOnLiquids, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock){
		return getMovingObjectPosWithReachDistance(world, player, player instanceof EntityPlayerMP ? ((EntityPlayerMP)player).interactionManager.getBlockReachDistance() : 5.0D, stopOnLiquids, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock);
	}

	public static void doFluidInteraction(TileEntity tileFrom, TileEntity tileTo, EnumFacing sideTo, int maxTransfer){
		if(maxTransfer > 0){
			if(tileFrom.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, sideTo) && tileTo.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, sideTo.getOpposite())){
				IFluidHandler handlerFrom = tileFrom.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, sideTo);
				IFluidHandler handlerTo = tileTo.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, sideTo.getOpposite());
				FluidStack drain = handlerFrom.drain(maxTransfer, false);
				if(drain != null){
					int filled = handlerTo.fill(drain.copy(), true);
					handlerFrom.drain(filled, true);
				}
			}
		}
	}	
}
