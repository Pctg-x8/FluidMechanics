package com.cterm2.mcfm1710.utils

import net.minecraft.entity.EntityLivingBase, net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.util.ForgeDirection
import net.minecraft.util.{EnumFacing, MathHelper}

object EntityLivingUtils
{
	// Converts facing direction integer to ForgeDirection
	def convertFacingDirection(i: Int) = i match
	{
		case 0 => ForgeDirection.SOUTH
		case 1 => ForgeDirection.WEST
		case 2 => ForgeDirection.NORTH
		case 3 => ForgeDirection.EAST
	}
	def convertFacingDirectionE(i: Int) = i match
	{
		case 0 => EnumFacing.SOUTH
		case 1 => EnumFacing.WEST
		case 2 => EnumFacing.NORTH
		case 3 => EnumFacing.EAST
	}
	implicit final class ImplicitWrapper(val p: EntityLivingBase) extends AnyVal
	{
		// Gets player facing direction as integer
		def directionInt = MathHelper.floor_double((p.rotationYaw * 4.0f / 360.0f).asInstanceOf[Double] + 0.5d).asInstanceOf[Int] & 3
		// Gets player facing direction as ForgeDirection
		def direction = convertFacingDirection(this.directionInt)
		// aliasing to direction
		def facing = convertFacingDirectionE(this.directionInt)
		// aliasing to directionInt
		def facingInt = directionInt
		// is useable containers
		def isUseable(e: net.minecraft.tileentity.TileEntity) = p.worldObj.getTileEntity(e.xCoord, e.yCoord, e.zCoord) == e &&
			p.getDistanceSq(e.xCoord.toDouble + 0.5d, e.yCoord.toDouble + 0.5d, e.zCoord.toDouble + 0.5d) <= 64.0d
	}
	implicit final class PlayerWrapper(val p: EntityPlayer) extends AnyVal
	{
		// Use item in holding
		def useCurrentItem()
		{
			if(!p.capabilities.isCreativeMode && p.inventory.getCurrentItem != null)
			{
				p.inventory.getCurrentItem.stackSize -= 1
				if(p.inventory.getCurrentItem.stackSize <= 0)
				{
					p.inventory.setInventorySlotContents(p.inventory.currentItem, null)
				}
			}
		}
	}
}
object EntityExtensions
{
	import net.minecraft.item.ItemStack
	import net.minecraft.entity.item.EntityItem
	import net.minecraft.world.World
	import net.minecraft.nbt.NBTTagCompound

	implicit final class ItemStackImplicitWrapper(val stack: ItemStack) extends AnyVal
	{
		def dropAsEntityRandom(world: World, x: Int, y: Int, z: Int)
		{
			val randomVector = Vector3f.randomUnit * 0.8f + 0.1f
			val dropPoint = Vector3f(x, y, z) + randomVector

			while(stack.stackSize > 0)
			{
				val limit = (x: Int) => if(x > stack.stackSize) stack.stackSize else x
				val dropCount = limit(RandomGenerator.range(10, 31))
				stack.stackSize -= dropCount

				val droppedStack = new ItemStack(stack.getItem, dropCount, stack.getItemDamage)
				val entityItem = new EntityItem(world, dropPoint.x.toDouble, dropPoint.y.toDouble, dropPoint.z.toDouble, droppedStack)
				if(stack.hasTagCompound) entityItem.getEntityItem.setTagCompound(stack.getTagCompound.copy().asInstanceOf[NBTTagCompound])

				val motionConst = 0.05f
				entityItem.motionX = (RandomGenerator.nextGaussian.toFloat * motionConst).toDouble
				entityItem.motionY = (RandomGenerator.nextGaussian.toFloat * motionConst + 0.2f).toDouble
				entityItem.motionZ = (RandomGenerator.nextGaussian.toFloat * motionConst).toDouble
				world.spawnEntityInWorld(entityItem)
			}
		}
	}
}
