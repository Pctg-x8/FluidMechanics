package com.cterm2.mcfm1710.tiles

import net.minecraft.tileentity.TileEntity
import net.minecraft.item.ItemStack
import net.minecraft.inventory.IInventory
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.{NBTTagCompound, NBTTagList}
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants.NBT

// Indices in Assembly Table
object AssemblySlotIndices
{
	case object Core extends AssemblySlotIndex(0)
	case object TopCasing extends AssemblySlotIndex(1)
	case object BottomCasing extends AssemblySlotIndex(2)
	case object Attachment extends AssemblySlotIndex(3)
}
sealed abstract class AssemblySlotIndex(val index: Int){}

// companion
object TEAssemblyTable
{
	// NBT Keys //
	private final val craftingItemsKey = "craftingItems"
	private final val assemblyItemsKey = "assemblyItems"
}
// TileEntity for Assembly Table that holding items on crafting grid and assembly grid
final class TEAssemblyTable extends TileEntity with IInventory
{
	import TEAssemblyTable._

	// Inventory Entities //
	private val craftingGridItems = new Array[ItemStack](9)		// x * y
	private val assemblyGridItems = new Array[ItemStack](4)		// offset = 9, core, topcase, bottomcase, attachment

	// Inventory Entity Accessors //
	final def getCraftingGridItem(x: Int, y: Int) = this.craftingGridItems(x + y * 3)
	final def getAssemblyGridItem(index: AssemblySlotIndex) = this.assemblyGridItems(index.index)

	// Inventory Configurations //
	// Size of inventory
	override val getSizeInventory = this.craftingGridItems.length + this.assemblyGridItems.length
	// Name of inventory(customname is not allowed)
	override val getInventoryName = "container.asmTable";
	// has custom name(always false)
	override val hasCustomInventoryName = false
	// Stack limit in inventory
	override val getInventoryStackLimit = 64
	// No items can insert to inventory
	override def isItemValidForSlot(index: Int, stack: ItemStack) = false

	// Inventory Interface Actions //
	// Get itemstack in inventory(null means nothing and empty)
	override def getStackInSlot(index: Int) = index match
	{
		case x if 0 until 9 contains x => this.craftingGridItems(x)
		case x if 9 until 13 contains x => this.assemblyGridItems(x - 9)
		case _ => null
	}
	// Set itemstack in inventory
	override def setInventorySlotContents(index: Int, stack: ItemStack) =
	{
		// clamp stack size
		if(stack != null && stack.stackSize > this.getInventoryStackLimit) stack.stackSize = this.getInventoryStackLimit
		index match
		{
			case x if 0 until 9 contains x => this.craftingGridItems(x) = stack
			case x if 9 until 13 contains x => this.assemblyGridItems(x - 9) = stack
		}
		this.markDirty()
	}
	// Decrease itemstack's stack size
	override def decrStackSize(index: Int, amount: Int) =
	{
		val stack = this.getStackInSlot(index)
		if(stack == null) null
		else
		{
			if(stack.stackSize <= amount)
			{
				// less than, or equal to amount
				this.setInventorySlotContents(index, null)
				stack
			}
			else
			{
				// greater
				val returnStack = stack.splitStack(amount)
				if(stack.stackSize == 0) this.setInventorySlotContents(index, null)
				returnStack
			}
		}
	}
	// When some containers are closed they call this on each slot, then drop whatever it returns as an EntityItem(like when you close a workbench GUI)
	override def getStackInSlotOnClosing(index: Int) = if(this.getStackInSlot(index) == null) null else
	{
		val stack = this.getStackInSlot(index)
		this.setInventorySlotContents(index, null)
		stack
	}
	override def isUseableByPlayer(player: EntityPlayer) =
		if(this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord) != this) false
		else player.getDistanceSq(this.xCoord.toDouble + 0.5d, this.yCoord.toDouble + 0.5d, this.zCoord.toDouble + 0.5d) <= 64.0d
	override def openInventory() = {}
	override def closeInventory() = {}

	// Data Synchronizing //
	// Read entity data from NBT
	override def readFromNBT(tag: NBTTagCompound) =
	{
		super.readFromNBT(tag)

		// loading to crafting grid
		val tagList = tag.getTagList(craftingItemsKey, NBT.TAG_COMPOUND)
		for(tag <- 0 until tagList.tagCount map { tagList.getCompoundTagAt(_) })
		{
			val slotIndex = tag.getByte("slotIndex")
			if(0 until 9 contains slotIndex) this.craftingGridItems(slotIndex) = ItemStack.loadItemStackFromNBT(tag)
		}
		// loading to assembly grid
		val tagListA = tag.getTagList(assemblyItemsKey, NBT.TAG_COMPOUND)
		for(tag <- (0 until tagListA.tagCount) map { tagListA.getCompoundTagAt(_) })
		{
			val slotIndex = tag.getByte("slotIndex")
			if(0 until 4 contains slotIndex) this.assemblyGridItems(slotIndex) = ItemStack.loadItemStackFromNBT(tag)
		}
	}
	// Write entity data to NBT
	override def writeToNBT(tag: NBTTagCompound) =
	{
		super.writeToNBT(tag)
		val craftingItemList = new NBTTagList
		val assemblyItemList = new NBTTagList

		for((stack, index) <- this.craftingGridItems.zipWithIndex if stack != null)
		{
			val itemTag = new NBTTagCompound
			itemTag.setByte("slotIndex", index.asInstanceOf[Byte])
			stack.writeToNBT(itemTag)
			craftingItemList.appendTag(itemTag)
		}
		for((stack, index) <- this.assemblyGridItems.zipWithIndex if stack != null)
		{
			val itemTag = new NBTTagCompound
			itemTag.setByte("slotIndex", index.asInstanceOf[Byte])
			stack.writeToNBT(itemTag)
			assemblyItemList.appendTag(itemTag)
		}
		tag.setTag(craftingItemsKey, craftingItemList)
		tag.setTag(assemblyItemsKey, assemblyItemList)
	}

	// Extra Utilities //
	// Restore crafting matrix inventory contents(tile -> inventory)
	final def restoreCraftingInventoryContents(inv: IInventory) =
		0 until 9 foreach { (x: Int) => inv.setInventorySlotContents(x, this.craftingGridItems(x)) }
	// Restore assembly matrix inventory contents
	final def restoreAssemblyInventoryContents(inv: IInventory) =
		0 until 4 foreach { (x: Int) => inv.setInventorySlotContents(x, this.assemblyGridItems(x)) }
	// Save crafting matrix inventory contents(inventory -> tile)
	final def saveCraftingInventoryContents(inv: IInventory) =
		0 until 9 foreach { (x: Int) => this.craftingGridItems(x) = inv.getStackInSlot(x) }
	// Save assembly matrix inventory contents
	final def saveAssemblyInventoryContents(inv: IInventory) =
		0 until 4 foreach { (x: Int) => this.assemblyGridItems(x) = inv.getStackInSlot(x) }
	// Drop all contained items from specified coordinate
	final def dropItemsAtRandom(world: World, x: Int, y: Int, z: Int) =
	{
		import com.cterm2.mcfm1710.utils.{Vector3f, RandomGenerator}
		import net.minecraft.entity.item.EntityItem

		System.out.println("Dropping items...")

		for(stack <- Seq(this.craftingGridItems, this.assemblyGridItems).flatten if stack != null)
		{
			val randomVector = Vector3f.randomUnit * 0.8f + 0.1f
			val dropOffsetted = Vector3f(x, y, z) + randomVector

			while(stack.stackSize > 0)
			{
				val limit = (x: Int) => if(x > stack.stackSize) stack.stackSize else x
				val dropCount = limit(RandomGenerator.range(10, 31))
				stack.stackSize -= dropCount

				val droppedStack = new ItemStack(stack.getItem(), dropCount, stack.getItemDamage())
				val entityItem = new EntityItem(world, dropOffsetted.x.toDouble, dropOffsetted.y.toDouble, dropOffsetted.z.toDouble, droppedStack)
				if(stack.hasTagCompound()) entityItem.getEntityItem().setTagCompound(stack.getTagCompound().copy().asInstanceOf[NBTTagCompound])

				val motionConst = 0.05f
				entityItem.motionX = (RandomGenerator.nextGaussian.toFloat * motionConst).toDouble
				entityItem.motionY = (RandomGenerator.nextGaussian.toFloat * motionConst + 0.2f).toDouble
				entityItem.motionZ = (RandomGenerator.nextGaussian.toFloat * motionConst).toDouble
				world.spawnEntityInWorld(entityItem)
			}
		}
	}
}
