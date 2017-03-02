package com.mcmoddev.mmdbuckets.items;


import com.mcmoddev.lib.material.MetalMaterial;
import com.mcmoddev.mmdbuckets.init.Materials;

import net.minecraft.block.BlockDispenser;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fluids.DispenseFluidContainer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;

import com.mcmoddev.basemetals.init.Fluids;
import com.mcmoddev.lib.material.IMetalObject;
import com.mcmoddev.lib.registry.IOreDictionaryEntry;
import com.mcmoddev.mmdbuckets.MMDBuckets;
import com.mcmoddev.mmdbuckets.init.Items;

@SuppressWarnings("deprecation")
public class ItemMMDBucket extends Item implements IFluidContainerItem, IOreDictionaryEntry, IMetalObject  {

	private static int numBuckets = Items.getCount();	
	private final MetalMaterial base;
	
	public ItemMMDBucket() {
		this(Materials.getMaterialByName("iron"));
	}

	public ItemMMDBucket(MetalMaterial mat) {
		this.base = mat;
		this.setMaxStackSize(1);
		this.setHasSubtypes(true);
		this.setCreativeTab(CreativeTabs.MISC);
		this.setMaxDamage(0);
		this.setRegistryName("metalbucket."+mat.getName());
		BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(this, DispenseFluidContainer.getInstance());
	}
	
	@Override
	public MetalMaterial getMaterial() {
		return this.base;
	}

	@Override
	public MetalMaterial getMetalMaterial() {
		return this.getMaterial();
	}

	@Override
	public String getOreDictionaryName() {
		return "bucket"+this.base.getCapitalizedName();
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return "item.mmdbuckets."+ Items.getNameFromMeta(stack.getMetadata())+".bucket.name";
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		MetalMaterial mat = Items.getBucketByMeta(stack.getMetadata()).getMetalMaterial();
		if( mat != null ) {
			String unloc = "item.mmdbuckets.bucket.name";
			String form = getUnlocalizedName(stack);
			String name = mat.getCapitalizedName();
			
			if( I18n.canTranslate(unloc) )
				return I18n.translateToLocalFormatted(unloc, name);
			
			return form;
		}
		return super.getItemStackDisplayName(stack);
	}
	
	@Override
	public void getSubItems(Item itemIn, CreativeTabs tab, List<ItemStack> subItems) {
		if( numBuckets == 0 ) {
			Items.init();
			numBuckets = Items.getCount();
		}
		
		subItems.add(new ItemStack(itemIn));
		for( int i = 0; i < numBuckets; i++ ) {
			ItemStack x = new ItemStack(itemIn, 1, i);
			if( !subItems.contains(x) ) {
				subItems.add( x );
			}
		}
	}
	
	/*
	 * Now for the fun stuff
	 */
	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack itemIn, World worldIn, EntityPlayer playerIn, EnumHand handIn ) {
		FluidStack fluidStack = getFluid(itemIn);
		if( fluidStack == null) {
			// we fill the bucket instead of empty it
			ItemStack stack = playerIn.getHeldItem(handIn);
			if( getFluid(stack) == null ) {
				ActionResult<ItemStack> ret = ForgeEventFactory.onBucketUse(playerIn, worldIn, itemIn, this.rayTrace(worldIn, playerIn, true));
				if( ret != null ) {
					return ret;
				}
			}
			
			return ActionResult.newResult(EnumActionResult.PASS, itemIn);
		}
		
		
        // clicked on a block?
        RayTraceResult mop = this.rayTrace(worldIn, playerIn, false);

        if(mop == null || mop.typeOfHit != RayTraceResult.Type.BLOCK) {
            return ActionResult.newResult(EnumActionResult.PASS, itemIn);
        }

        BlockPos clickPos = mop.getBlockPos();
        // can we place liquid there?
        if (worldIn.isBlockModifiable(playerIn, clickPos)) {
            // the block adjacent to the side we clicked on
            BlockPos targetPos = clickPos.offset(mop.sideHit);

            // can the player place there?
            if (playerIn.canPlayerEdit(targetPos, mop.sideHit, itemIn)) {
                // try placing liquid
                if (FluidUtil.tryPlaceFluid(playerIn, playerIn.getEntityWorld(), fluidStack, targetPos) && !playerIn.capabilities.isCreativeMode) {
                    // success!
                    playerIn.addStat(StatList.getObjectUseStats(this));

                    itemIn.stackSize--;
                    ItemStack emptyStack = new ItemStack(this, 1, Items.getBuckets().indexOf(base.getName()));

                    // check whether we replace the item or add the empty one to the inventory
                    if (itemIn.stackSize <= 0) {
                        return ActionResult.newResult(EnumActionResult.SUCCESS, emptyStack);
                    } else {
                        // add empty bucket to player inventory
                        ItemHandlerHelper.giveItemToPlayer(playerIn, emptyStack);
                        return ActionResult.newResult(EnumActionResult.SUCCESS, itemIn);
                    }
                }
            }
        }

        // couldn't place liquid there2
		return ActionResult.newResult(EnumActionResult.FAIL, itemIn);
	}
	
	
	public FluidStack getFluid(ItemStack itemIn) {
		NBTTagCompound tags = itemIn.getTagCompound();
		if( tags != null ) {
			return FluidStack.loadFluidStackFromNBT(tags);
		}
		return null;
	}

	@SubscribeEvent
	public void onFillBucket(FillBucketEvent ev) {
		if( ev.getResult() != Event.Result.DEFAULT ) return;
		
		ItemStack empty = ev.getEmptyBucket();
		if( empty == null || !empty.getItem().equals(this) ) return;
		
		ItemStack bucket = empty.copy();
		bucket.stackSize = 1;
		
		RayTraceResult target = ev.getTarget();		
		if( target == null || target.typeOfHit != RayTraceResult.Type.BLOCK ) return;
		
		World world = ev.getWorld();
		BlockPos targetPos = target.getBlockPos();
		
		ItemStack filled = FluidUtil.tryPickUpFluid(bucket, ev.getEntityPlayer(), world, targetPos, target.sideHit);
		
		if( filled != null ) {
			ev.setResult(Event.Result.ALLOW);
			ev.setFilledBucket(filled);
		} else {
			ev.setCanceled(true);
		}
	}

	@Override
	public int getCapacity(ItemStack container) {
		return getCapacity();
	}

	public int getCapacity() {
		return 1000;
	}
	
	@Override
	public int fill(ItemStack container, FluidStack resource, boolean doFill) {
		if( container.stackSize != 1 || resource == null || resource.amount < 1000 ) {
			return 0;
		}
		
		if( getFluid(container) != null ) {
			return 0;
		}
		
		if( FluidRegistry.getBucketFluids().contains(resource.getFluid())) {
			if( doFill ) {
				NBTTagCompound t = container.getTagCompound();
				if( t == null ) {
					t = new NBTTagCompound();
				}
				
				resource.writeToNBT(t);
				container.setTagCompound(t);
			}
			return getCapacity();
		} else if( resource.getFluid() == FluidRegistry.WATER ) {
			if( doFill ) {
				container.deserializeNBT(new ItemStack(net.minecraft.init.Items.WATER_BUCKET).serializeNBT());
			}
			return getCapacity();
		} else if( resource.getFluid() == FluidRegistry.LAVA ) {
			if( doFill ) {
				container.deserializeNBT(new ItemStack(net.minecraft.init.Items.LAVA_BUCKET).serializeNBT());
			}
			return getCapacity();
		}
		return 0;
	}

	@Override
	public FluidStack drain(ItemStack container, int maxDrain, boolean doDrain) {
		if( container.stackSize != 1 || maxDrain < getCapacity(container) ) {
			return null;
		}
		
		FluidStack fluidStack = getFluid(container);
		if( doDrain && fluidStack != null ) {
			container.stackSize = 0;
		}
		return fluidStack;
	}
	
}
