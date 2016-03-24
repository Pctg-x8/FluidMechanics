package com.cterm2.mcfm1710

// Generic Objects(Tanks)
package Generics
{
	import net.minecraftforge.fluids.{IFluidTank, FluidStack, FluidTankInfo, Fluid}
	import net.minecraft.nbt.NBTTagCompound
	final class FluidTank(val capacity: Int) extends IFluidTank
	{
		// Tank Traits //
		override val getCapacity = capacity
		override lazy val getInfo = new FluidTankInfo(this)

		// Tank Exports //
		private var stack: Option[FluidStack] = None
		override def getFluid = this.stack getOrElse null
		override def getFluidAmount = this.stack map (_.amount) getOrElse 0
		def canFill(input: Fluid) = this.stack map { _.getFluid == input } getOrElse true
		def canDrain(input: Fluid) = this.stack map { _.getFluid == input } getOrElse false

		// Tank Interacts //
		override def fill(resource: FluidStack, perform: Boolean) = resource match
		{
			case null => 0
			case _ if this.stack map { _ isFluidEqual resource } getOrElse true =>
			{
				val incrAmount = Math.min(resource.amount, this.capacity - this.getFluidAmount)
				if(perform && incrAmount > 0)
				{
					this.stack match
					{
						case None => this.stack = Some(new FluidStack(resource, incrAmount))
						case Some(s) => s.amount += incrAmount
					}
				}
				incrAmount
			}
			case _ => 0
		}
		override def drain(maxDrain: Int, perform: Boolean) = this.stack match
		{
			case None => null
			case Some(s) =>
			{
				val drained = Math.min(maxDrain, s.amount)
				if(perform && drained > 0)
				{
					s.amount -= drained
					if(s.amount <= 0) this.stack = None
				}
				new FluidStack(s, drained)
			}
		}

		// Data Synchronizations //
		def synchronizeData = this.stack map { x => { val tag = new NBTTagCompound; x.writeToNBT(tag); tag } }
		def synchronizeDataFrom(tag: Option[NBTTagCompound])
		{
			this.stack = tag map FluidStack.loadFluidStackFromNBT
		}
	}

	// Generic Container supports shift-clicking and player slot implementation
	abstract class Container extends net.minecraft.inventory.Container
	{
		import net.minecraft.inventory.Slot
		import net.minecraft.item.ItemStack
		import net.minecraft.entity.player.{EntityPlayer, InventoryPlayer}

		// Container Interacts //
		// Merge item stack to other stack in transferStackInSlot(Returns if merging succeeded)
		protected def mergeStackInTransferring(stack: ItemStack, index: Int): Boolean
		// Transfer stack between slot with shortcut(shift-clicking)
		override final def transferStackInSlot(player: EntityPlayer, slotIndex: Int) =
			Option(this.inventorySlots.get(slotIndex).asInstanceOf[Slot]) flatMap
			{
				case slotObject if slotObject.getHasStack =>
				{
					// available slot and contains item
					val slotStack = slotObject.getStack
					val stackOld = slotStack.copy

					if(this.mergeStackInTransferring(slotStack, slotIndex))
					{
						// raise slotChanged event or clear slot content
						if(slotStack.stackSize == 0) slotObject.putStack(null) else slotObject.onSlotChanged()
						if(slotStack.stackSize == stackOld.stackSize) null else
						{
							// Transferred(raise picked-up event)
							slotObject.onPickupFromSlot(player, slotStack)
							Some(stackOld)
						}
					} else None
				}
				case _ => None
			} getOrElse null

		// Place Player Slots(Returns tuple of (startSlotIndex, lastSecondaryIndex, lastSlotIndex))
		protected final def addPlayerSlots(invPlayer: InventoryPlayer, left: Int, top: Int) =
		{
			val startSlotIndex = this.inventorySlots.size
			for(i <- 0 until 3; j <- 0 until 9) this.addSlotToContainer(new Slot(invPlayer, 9 + j + i * 9, left + j * 18, top + i * 18))
			val lastSecondaryIndex = this.inventorySlots.size
			for(j <- 0 until 9) this.addSlotToContainer(new Slot(invPlayer, j, left + j * 18, top + 3 * 18 + 2))
			(startSlotIndex, lastSecondaryIndex, this.inventorySlots.size)
		}
	}
}
