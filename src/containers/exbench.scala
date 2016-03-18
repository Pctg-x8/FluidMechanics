package com.cterm2.mcfm1710.containers

import net.minecraft.inventory.{Container, Slot, SlotCrafting, InventoryCrafting, InventoryCraftResult, IInventory}
import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}
import net.minecraft.item.ItemStack, net.minecraft.world.World
import net.minecraft.item.crafting.CraftingManager
import com.cterm2.mcfm1710.tiles.{TEAssemblyTable, AssemblySlotIndices}

final class ContainerAssemblyTable(val worldObj: World, val tile: TEAssemblyTable, val invPlayer: InventoryPlayer) extends Container
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
