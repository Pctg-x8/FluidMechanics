package com.cterm2.mcfm1710

import cpw.mods.fml.relauncher.{Side, SideOnly}

package object AssemblyTable
{
	def register(ctab: net.minecraft.creativetab.CreativeTabs) =
	{
		ContentRegistry register Item.setCreativeTab(ctab) as "asmTable"
		ContentRegistry register Block as "assemblyTable_part"
		ContentRegistry register BlockTopPart as "assemblyTable.top"
		ContentRegistry register classOf[TileEntity] as "TEAssemblyTable"
		ctab
	}
	@SideOnly(Side.CLIENT)
	def registerClient() = ()
}

package AssemblyTable
{
	import net.minecraft.item.{ItemStack, Item => ItemBase}, net.minecraft.block.{BlockContainer, Block => BlockBase}
	import net.minecraft.block.material.Material
	import net.minecraftforge.common.util.ForgeDirection
	import com.cterm2.mcfm1710.utils.BlockPlacementInfo
	import net.minecraft.world.World
	import com.cterm2.mcfm1710.utils.ForgeDirectionExtensions._
	import com.cterm2.mcfm1710.utils.EntityLivingUtils._
	import com.cterm2.mcfm1710.utils.WorldExtensions._
	import com.cterm2.mcfm1710.utils.Vector3i
	import net.minecraft.entity.player.EntityPlayer
	import java.util.Random

	// Common Values/Utils
	final object CommonUtils
	{
		// MetaBits //
		final val SlaveBit = 0x08

		// Utilities //
		final def canPlaceBlock(placeinfo: BlockPlacementInfo, side: ForgeDirection, stack: ItemStack) =
			placeinfo.player.canPlayerEdit(placeinfo.pos.x, placeinfo.pos.y, placeinfo.pos.z, side.ordinal(), stack) &&
			placeinfo.player.canPlayerEdit(placeinfo.pos.x, placeinfo.pos.y + 1, placeinfo.pos.z, side.ordinal(), stack) &&
			placeinfo.world.isAirBlock(placeinfo.pos.x, placeinfo.pos.y, placeinfo.pos.z) &&
			placeinfo.world.isAirBlock(placeinfo.pos.x, placeinfo.pos.y + 1, placeinfo.pos.z) &&
			World.doesBlockHaveSolidTopSurface(placeinfo.world, placeinfo.pos.x, placeinfo.pos.y - 1, placeinfo.pos.z)
		// Determine this block is master(primary) block
		final def isMaster(meta: Int) = (meta & 0x08) == 0
		// Get direction from metadata
		final def getDirectionOffset(meta: Int) = ForgeDirection.values()(meta & 0x07).offset
	}
	object AssemblySlotIndices
	{
		case object Core extends AssemblySlotIndex(0)
		case object TopCasing extends AssemblySlotIndex(1)
		case object BottomCasing extends AssemblySlotIndex(2)
		case object Attachment extends AssemblySlotIndex(3)
	}
	sealed abstract class AssemblySlotIndex(val index: Int){}
	object NBTKeys
	{
		// NBT Keys //
		final val craftingItemsKey = "craftingItems"
		final val assemblyItemsKey = "assemblyItems"
		final val slotIndexKey = "slotIndex"
	}

	final object Item extends ItemBase
	{
		setUnlocalizedName("assemblyTable_caller")

		// called when using item(place actual blocks if possible)
		override def onItemUse(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, xOffs: Float, yOffs: Float, zOffs: Float) =
			(world.isRemote, ForgeDirection.values()(side)) match
			{
				case (false, ForgeDirection.UP) =>
				{
					val placeY = y + 1
					val blockSlaveDirection = player.facing.getRotation(ForgeDirection.UP)
					val slavePos = Vector3i(x, placeY, z) withOffset blockSlaveDirection.offset
					val placeInfo = BlockPlacementInfo(player, world, Vector3i(x, placeY, z))
					val placeInfoSlave = BlockPlacementInfo(player, world, slavePos)
					if(CommonUtils.canPlaceBlock(placeInfo, ForgeDirection.UP, stack) && CommonUtils.canPlaceBlock(placeInfoSlave, ForgeDirection.UP, stack))
					{
						world.setBlock(placeInfo.pos.x, placeInfo.pos.y, placeInfo.pos.z, Block, blockSlaveDirection.ordinal, 3)
						if(world.getBlock(placeInfo.pos.x, placeInfo.pos.y, placeInfo.pos.z) == Block)
						{
							world.setBlock(slavePos.x, slavePos.y, slavePos.z, Block, blockSlaveDirection.ordinal | CommonUtils.SlaveBit, 3)
							world.setBlock(placeInfo.pos.x, placeInfo.pos.y + 1, placeInfo.pos.z, BlockTopPart, blockSlaveDirection.ordinal, 3)
							world.setBlock(slavePos.x, slavePos.y + 1, slavePos.z, BlockTopPart, blockSlaveDirection.ordinal | CommonUtils.SlaveBit, 3)
						}
						stack.stackSize -= 1
						true
					}
					else false
				}
				case (true, _) => true
				case _ => false
			}
	}

	// Blocks //
	final object Block extends BlockContainer(Material.rock)
	{
		setHardness(1.0f)

		// Render Settings //
		// render as normal(solid cube) block flag
		@SideOnly(Side.CLIENT)
		override val renderAsNormalBlock = false
		// render as opaque(no transparency) block flag
		override val isOpaqueCube = false
		override val isNormalCube = false		// set false if block rendered by custom renderer

		// Block Actions //
		// Called when block is right-clicked(activated)
		override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, xo: Float, yo: Float, zo: Float) =
			if(world.isRemote) true else
			{
				val meta = world.getBlockMetadata(x, y, z)
				if(!CommonUtils.isMaster(meta))
				{
					// Slave: Delegate process to master
					val masterPos = Vector3i(x, y, z) withOffset -CommonUtils.getDirectionOffset(meta)
					val masterMeta = world.getBlockMetadata(masterPos.x, masterPos.y, masterPos.z)
					this.onBlockActivated(world, masterPos.x, masterPos.y, masterPos.z, player, masterMeta, xo, yo, zo)
				}
				else
				{
					// Master: Open Table's GUI
					// System.out.println(s"Open Table's GUI on Coordinate (${x}, ${y}, ${z})")
					val entity = world.getTileEntity(x, y, z).asInstanceOf[TileEntity]
					// System.out.println(s"TileEntity associated coordinate: ${entity}")
					if(entity != null) player.openGui(FMEntry, 0, world, x, y, z)
					true
				}
			}
		// Called when neighbor block changed(broken, placed new or pushed by pistons, passed coordinate is mine)
		override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, neighbor: BlockBase) =
		{
			val meta = world.getBlockMetadata(x, y, z)
			val dir = CommonUtils.getDirectionOffset(meta)

			// flip direction if slave block
			if(CommonUtils.isMaster(meta)) this.breakOnIllegalAdjacents(world, Vector3i(x, y, z), dir)
			else this.breakOnIllegalAdjacents(world, Vector3i(x, y, z), -dir, true)
		}
		// Called when the block is attempted to be harvested
		override def onBlockHarvested(world: World, x: Int, y: Int, z: Int, meta: Int, player: EntityPlayer) =
			if(player.capabilities.isCreativeMode && CommonUtils.isMaster(meta))
			{
				// on master
				val dir = CommonUtils.getDirectionOffset(meta)
				val slavePos = Vector3i(x, y, z) withOffset dir
				// break slaves too
				world.replaceBlockToAir(slavePos, this)
				world.replaceBlockToAir(Vector3i(x, y + 1, z), BlockTopPart)
				world.replaceBlockToAir(slavePos + Vector3i(0, 1, 0), BlockTopPart)
			}
		// Called when the block is broken
		override def breakBlock(world: World, x: Int, y: Int, z: Int, block: BlockBase, meta: Int) =
			Option(world.getTileEntity(x, y, z).asInstanceOf[TileEntity]) foreach
			{
				_.dropItemsAtRandom(world, x, y, z)
			}

		// Configures //
		// Configure dropped item
		override def getItemDropped(meta: Int, randomizer: Random, quantity: Int) =
			if(CommonUtils.isMaster(meta)) ItemBase.getItemById(0) else Item
		// Drops the block items with a specified chance of dropping the specified items(only from slave block)
		override def dropBlockAsItemWithChance(world: World, x: Int, y: Int, z: Int, meta: Int, f1: Float, i4: Int) =
			if(!CommonUtils.isMaster(meta)) super.dropBlockAsItemWithChance(world, x, y, z, meta, f1, 0)
		// Set as immobility
		override val getMobilityFlag = 2
		// Gets an item for the block being called on
		@SideOnly(Side.CLIENT)
		override def getItem(world: World, x: Int, y: Int, z: Int) = Item
		// TileEntity associated to block(have only in master)
		override def createNewTileEntity(world: World, meta: Int) = if(CommonUtils.isMaster(meta)) new TileEntity else null

		// Miscs //
		// Break on illegal adjacents found
		private final def breakOnIllegalAdjacents(world: World, thisPos: Vector3i, pairDir: Vector3i, dropItem: Boolean = false) =
			if(world.getBlock(thisPos.x + pairDir.x, thisPos.y, thisPos.z + pairDir.z) != this ||
				world.getBlock(thisPos.x, thisPos.y + 1, thisPos.z) != BlockTopPart ||
				world.getBlock(thisPos.x + pairDir.x, thisPos.y + 1, thisPos.z + pairDir.z) != BlockTopPart)
			{
				// illegal adjacents -> break
				world.setBlockToAir(thisPos.x, thisPos.y, thisPos.z)
				if(!world.isRemote && dropItem) this.dropBlockAsItem(world, thisPos.x, thisPos.y, thisPos.z, CommonUtils.SlaveBit, 0)
			}
	}

	// Additional Block
	final object BlockTopPart extends BlockBase(Material.rock)
	{
		this.setHardness(1.0f)
		this.setBlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f)

		// Rendering Configurations //
		override val renderAsNormalBlock = false
		override val isOpaqueCube = false
		override val isNormalCube = false

		// Block Interacts //
		// Called when block is right-clicked(activated, delegate process to lower blocks)
		override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, xo: Float, yo: Float, zo: Float) =
			world.getBlock(x, y - 1, z).asInstanceOf[Block.type].onBlockActivated(world, x, y - 1, z, player, side, xo, yo, zo)
		// Called when neighbor block changed(broken, placed new or pushed by pistons, passed coordinate is mine)
		override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, neighbor: BlockBase) =
		{
			val meta = world.getBlockMetadata(x, y, z)
			val dir = CommonUtils.getDirectionOffset(meta)

			// flip direction if slave block
			if(CommonUtils.isMaster(meta)) this.breakOnIllegalAdjacents(world, Vector3i(x, y, z), dir)
			else this.breakOnIllegalAdjacents(world, Vector3i(x, y, z), -dir)
		}
		// Called when the block is attempted to be harvested(creative item dropping supression)
		override def onBlockHarvested(world: World, x: Int, y: Int, z: Int, meta: Int, player: EntityPlayer) =
			if(player.capabilities.isCreativeMode)
			{
				// on master
				val dir = CommonUtils.getDirectionOffset(meta)
				val slavePos = if(CommonUtils.isMaster(meta)) Vector3i(x, y, z) withOffset dir else Vector3i(x, y, z) withOffset -dir
				// break slaves too
				world.replaceBlockToAir(slavePos, this)
				world.replaceBlockToAir(Vector3i(x, y - 1, z), Block)
				world.replaceBlockToAir(slavePos + Vector3i(0, -1, 0), Block)
			}

		// Block Configurations //
		override val getMobilityFlag = 2
		override def getItemDropped(meta: Int, randomizer: Random, quantity: Int) = null

		// illegal adjacents -> break
		private final def breakOnIllegalAdjacents(world: World, thisPos: Vector3i, pairDir: Vector3i) =
			if(world.getBlock(thisPos.x + pairDir.x, thisPos.y, thisPos.z + pairDir.z) != this ||
				world.getBlock(thisPos.x, thisPos.y - 1, thisPos.z) != Block ||
				world.getBlock(thisPos.x + pairDir.x, thisPos.y - 1, thisPos.z + pairDir.z) != Block)
					world.setBlockToAir(thisPos.x, thisPos.y, thisPos.z)
	}

	// TileEntity for Assembly Table that holding items on crafting grid and assembly grid //
	import net.minecraft.inventory.IInventory
	import net.minecraft.nbt.{NBTTagCompound, NBTTagList}
	import net.minecraft.tileentity.{TileEntity => TileEntityBase}
	import net.minecraftforge.common.util.Constants.NBT
	final class TileEntity extends TileEntityBase with IInventory
	{
		import net.minecraft.nbt.{NBTTagCompound, NBTTagList}

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
			val tagList = tag.getTagList(NBTKeys.craftingItemsKey, NBT.TAG_COMPOUND)
			for(tag <- 0 until tagList.tagCount map { tagList.getCompoundTagAt _ })
			{
				val slotIndex = tag.getByte(NBTKeys.slotIndexKey)
				if(0 until 9 contains slotIndex) this.craftingGridItems(slotIndex) = ItemStack.loadItemStackFromNBT(tag)
			}
			// loading to assembly grid
			val tagListA = tag.getTagList(NBTKeys.assemblyItemsKey, NBT.TAG_COMPOUND)
			for(tag <- 0 until tagListA.tagCount map { tagListA.getCompoundTagAt _ })
			{
				val slotIndex = tag.getByte(NBTKeys.slotIndexKey)
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
				itemTag.setByte(NBTKeys.slotIndexKey, index.asInstanceOf[Byte])
				stack.writeToNBT(itemTag)
				craftingItemList.appendTag(itemTag)
			}
			for((stack, index) <- this.assemblyGridItems.zipWithIndex if stack != null)
			{
				val itemTag = new NBTTagCompound
				itemTag.setByte(NBTKeys.slotIndexKey, index.asInstanceOf[Byte])
				stack.writeToNBT(itemTag)
				assemblyItemList.appendTag(itemTag)
			}
			tag.setTag(NBTKeys.craftingItemsKey, craftingItemList)
			tag.setTag(NBTKeys.assemblyItemsKey, assemblyItemList)
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

	// Containers //
	import net.minecraft.entity.player.InventoryPlayer
	import net.minecraft.inventory.{InventoryCrafting, InventoryCraftResult, Slot, SlotCrafting, Container => ContainerBase}
	import net.minecraft.item.crafting.CraftingManager
	final class Container(val worldObj: World, val tile: TileEntity, val invPlayer: InventoryPlayer) extends ContainerBase
	{
		// initialize with copying from TEAssemblyTable
		val craftMatrix = new InventoryCrafting(this, 3, 3)
		val craftResult = new InventoryCraftResult()
		tile.restoreCraftingInventoryContents(this.craftMatrix)
		val asmResult = new InventoryAssembleResult()

		// initialize slots
		initCraftingSlots(); initAssemblySlots(); initPlayerSlots()

		// initial fire
		this.onCraftMatrixChanged(this.craftMatrix)

		// Container Configurations //
		// can container interact with the player
		override def canInteractWith(player: EntityPlayer) = tile.isUseableByPlayer(player)

		// Container Interacts //
		// Transfer stack between slot with shortcut(shift-clicking)
		override def transferStackInSlot(player: EntityPlayer, slotIndex: Int) =
		{
			val slotObject = this.inventorySlots.get(slotIndex).asInstanceOf[Slot]

			if(slotObject == null || !slotObject.getHasStack) null else
			{
				// available slot and contains item
				val slotStack = slotObject.getStack
				val stackOld = slotStack.copy()

				def postProcess() =
				{
					// raise slotChanged event or clear slot content
					if(slotStack.stackSize == 0) slotObject.putStack(null) else slotObject.onSlotChanged()
					if(slotStack.stackSize == stackOld.stackSize) null else
					{
						// Transferred(raise picked-up event)
						slotObject.onPickupFromSlot(player, slotStack)
						stackOld
					}
				}

				val containerSlotCount = 15
				if(0 until containerSlotCount contains slotIndex)
				{
					// tile slot -> player slot
					if(!this.mergeItemStack(slotStack, containerSlotCount, containerSlotCount + 9 * 4, false)) null else postProcess()
				}
				else if(containerSlotCount until (containerSlotCount + 9 * 3) contains slotIndex)
				{
					// player secondary slot -> player primary slot
					if(!this.mergeItemStack(slotStack, containerSlotCount + 9 * 3, containerSlotCount + 9 * 4, false)) null else postProcess()
				}
				else if(!this.mergeItemStack(slotStack, containerSlotCount, containerSlotCount + 9 * 3, false)) null
				else postProcess()
			}
		}
		// Called when the container is closed(save items to TileEntity)
		override def onContainerClosed(player: EntityPlayer) =
		{
			super.onContainerClosed(player)

			if(!this.worldObj.isRemote)
			{
				this.tile.saveCraftingInventoryContents(this.craftMatrix)
			}
		}
		// Called when the crafting matrix is changed
		override def onCraftMatrixChanged(matrix: IInventory) =
		{
			this.craftResult.setInventorySlotContents(0, CraftingManager.getInstance.findMatchingRecipe(this.craftMatrix, this.worldObj))
		}

		// Private Constructions //
		// Setup slots for crafting
		private def initCraftingSlots() =
		{
			val (craftingLeft, craftingTop) = (8, 22)
			for(i <- 0 until 3; j <- 0 until 3)
			{
				this.addSlotToContainer(new Slot(this.craftMatrix, j + i * 3, craftingLeft + j * 18, craftingTop + i * 18))
			}
			this.addSlotToContainer(new SlotCrafting(invPlayer.player, this.craftMatrix, this.craftResult, 0, craftingLeft + 86 + 4, craftingTop + 14 + 4))
		}
		// Setup slots for assembling
		private def initAssemblySlots() =
		{
			val (coreLeft, coreTop) = (140 + 32, 18 + 22)
			this.addSlotToContainer(new Slot(tile, AssemblySlotIndices.Core.index + 9, coreLeft, coreTop))
			this.addSlotToContainer(new Slot(tile, AssemblySlotIndices.TopCasing.index + 9, coreLeft, coreTop - 22))
			this.addSlotToContainer(new Slot(tile, AssemblySlotIndices.BottomCasing.index + 9, coreLeft, coreTop + 22))
			this.addSlotToContainer(new Slot(tile, AssemblySlotIndices.Attachment.index + 9, coreLeft - 32, coreTop))
			this.addSlotToContainer(new SlotAssembleOutput(invPlayer.player, this.asmResult, 0, coreLeft + 48 + 4, coreTop))
		}
		// Setup slots for inventory of player
		private def initPlayerSlots() =
		{
			val (playerLeft, playerTop) = (45, 92)
			for(i <- 0 until 3; j <- 0 until 9) this.addSlotToContainer(new Slot(invPlayer, 9 + j + i * 9, playerLeft + j * 18, playerTop + i * 18))
			for(j <- 0 until 9) this.addSlotToContainer(new Slot(invPlayer, j, playerLeft + j * 18, playerTop + 3 * 18 + 2))
		}
	}
	// Slot for Assembling Result
	final class SlotAssembleOutput(val player: EntityPlayer, val source: IInventory, val index: Int, val x: Int, val y: Int)
		extends Slot(source, index, x, y)
	{
		// player for achievements
		// Internal Counter
		private var amountAssembled: Int = 0

		// Slot Configurations //
		// No items are valid(Output only)
		override def isItemValid(stack: ItemStack) = false

		// Slot Interactions //
		// Decrease stack size
		override def decrStackSize(amount: Int) =
		{
			import java.lang.Math.min

			if(this.getHasStack) this.amountAssembled += min(amount, this.getStack.stackSize)
			super.decrStackSize(amount)
		}
		// Called when picked up from slot(stub)
		override def onPickupFromSlot(player: EntityPlayer, stack: ItemStack) = ???
	}
	// Inventory for Assemblying Result
	final class InventoryAssembleResult extends IInventory
	{
		private var stack: Option[ItemStack] = None

		// Inventory Configurations //
		override val getSizeInventory = 1										// Size of inventory
		override val getInventoryName = "asmResult"								// Name of inventory
		override val hasCustomInventoryName = false								// Custom names cannot have
		override val getInventoryStackLimit = 64								// Stack limit of inventory
		override def isUseableByPlayer(player: EntityPlayer) = true				// Useable by player
		override def isItemValidForSlot(index: Int, stack: ItemStack) = true	// Any items are valid

		// Inventory Interactions //
		// Gets itemStack in slot
		override def getStackInSlot(index: Int) = this.stack.orNull
		// Gets itemStack in slot on closing
		override def getStackInSlotOnClosing(index: Int) = this.stack.map(x =>
		{
			this.stack = None; x
		}).orNull
		// Sets itemStack in slot
		override def setInventorySlotContents(index: Int, stack: ItemStack) = this.stack = Some(stack)
		// Decreases stack size in slot
		override def decrStackSize(index: Int, amount: Int) = this.stack.map(x =>
		{
			this.stack = None; x
		}).orNull
		// OpenInventory/CloseInventory/markDirty(Nothing to do)
		override def openInventory() = {}
		override def closeInventory() = {}
		override def markDirty() = {}
	}

	// Guis //
	import net.minecraft.client.gui.inventory.GuiContainer
	import net.minecraft.util.ResourceLocation
	import com.cterm2.mcfm1710.utils.LocalTranslationUtils._
	class Gui(val con: Container) extends GuiContainer(con)
	{
		val backImage = new ResourceLocation(FMEntry.ID, "textures/guiBase/asmTable.png")

		// initializer
		override def initGui() =
		{
			super.initGui()

			this.xSize = 252; this.ySize = 170
			this.guiLeft = (this.width - this.xSize) / 2
			this.guiTop = (this.height - this.ySize) / 2
		}

		// Draw GUI Foreground Layer(caption layer)
		override protected def drawGuiContainerForegroundLayer(p1: Int, p2: Int) =
		{
			this.fontRendererObj.drawString(t"container.crafting", 8, 10, 0x404040)
			this.fontRendererObj.drawString(t"container.inventory", 45, 80, 0x404040)
			this.fontRendererObj.drawString(t"container.assembling", 140, 6, 0x404040)
		}

		// Draw GUI Background Layer(backimage layer)
		override protected def drawGuiContainerBackgroundLayer(p1: Float, p2: Int, p3: Int) =
		{
			import org.lwjgl.opengl.GL11._

			this.mc.getTextureManager.bindTexture(this.backImage)
			glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
			this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize)
		}
	}
}
