package com.cterm2.mcfm1710

import interfaces.ISourceGenerator
import interops.smartcursor._
import cpw.mods.fml.relauncher.{SideOnly, Side}

package object ThermalGenerator
{
	def register(ctab: net.minecraft.creativetab.CreativeTabs) =
	{
		ContentRegistry register Block.setCreativeTab(ctab) as "sourceGenerator.thermal"
		ContentRegistry register classOf[TileEntity] as "TEThermalGenerator"
		ctab
	}
}

package ThermalGenerator
{
    import net.minecraft.world.World
    import net.minecraft.tileentity.{TileEntity => TileEntityBase}
    import net.minecraft.nbt.NBTTagCompound
	import net.minecraft.inventory.IInventory
	import net.minecraft.item.ItemStack
	import net.minecraft.entity.player.EntityPlayer
	import cpw.mods.fml.common.Optional

	final object StoreKeys
	{
		final val FuelStack = "fuelStack"
		final val BurnTime = "burnTimeLast"
		final val FullBurnTime = "burnTimeFull"
	}

    final object Block extends SourceGenerator.BlockBase
    {
        // Block Traits //
        override def createNewTileEntity(world: World, meta: Int) = new TileEntity

		// Block Interacts //
		override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, xo: Float, yo: Float, zo: Float) =
			(world.isRemote, world.getTileEntity(x, y, z).asInstanceOf[TileEntity]) match
			{
				case (true, _) => true
				case (false, tile) => { player.openGui(FMEntry, 0, world, x, y, z); true }
				case _ => false
			}
    }

	@Optional.Interface(iface="com.cterm2.mcfm1710.interops.smartcursor.IInformationProvider", modid=SCModuleConnector.ID, striprefs=true)
    final class TileEntity extends TileEntityBase with ISourceGenerator with IInventory with IInformationProvider
    {
		import net.minecraft.tileentity.TileEntityFurnace
		import net.minecraft.network.play.server.S35PacketUpdateTileEntity
		import net.minecraft.network.NetworkManager

		// Internal Data //
		private var slotItem: Option[ItemStack] = None
		private var burnTimeLast = 0
		private var fullBurnTime = 0

		// Data Synchronization //
        override final def storeSpecificDataTo(tag: NBTTagCompound) =
        {
			tag.setShort(StoreKeys.BurnTime, this.burnTimeLast.asInstanceOf[Short])
			tag.setShort(StoreKeys.FullBurnTime, this.fullBurnTime.asInstanceOf[Short])
			this.slotItem map { x => { val tag = new NBTTagCompound; x.writeToNBT(tag); tag } } foreach { tag.setTag(StoreKeys.FuelStack, _) }
			tag
        }
        override final def loadSpecificDataFrom(tag: NBTTagCompound) =
        {
			this.burnTimeLast = Option(tag.getShort(StoreKeys.BurnTime).asInstanceOf[Int]) getOrElse 0
			this.fullBurnTime = Option(tag.getShort(StoreKeys.FullBurnTime).asInstanceOf[Int]) getOrElse 0
			this.slotItem = Option(tag.getTag(StoreKeys.FuelStack).asInstanceOf[NBTTagCompound]) map { ItemStack.loadItemStackFromNBT(_) }
			tag
        }
		override final def writeToNBT(tag: NBTTagCompound)
		{
			super.writeToNBT(tag)
			this.storeSpecificDataTo(tag)
		}
		override final def readFromNBT(tag: NBTTagCompound)
		{
			super.readFromNBT(tag)
			this.loadSpecificDataFrom(tag)
		}
		override final def getDescriptionPacket =
			this.storeSpecificDataTo _ andThen (new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, _)) apply (new NBTTagCompound)
		override final def onDataPacket(net: NetworkManager, packet: S35PacketUpdateTileEntity)
		{
			((_: S35PacketUpdateTileEntity).func_148857_g) andThen this.loadSpecificDataFrom apply packet
		}

		// Inventory Configurations //
		override final val getSizeInventory = 1
		override final val getInventoryName = "container.fuelStack"
		override final val hasCustomInventoryName = false
		override final val getInventoryStackLimit = 64
		override final def isUseableByPlayer(player: EntityPlayer) =
			if(this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord) != this) false
			else player.getDistanceSq(this.xCoord.toDouble + 0.5d, this.yCoord.toDouble + 0.5d, this.zCoord.toDouble + 0.5d) <= 64.0d
		override final def isItemValidForSlot(index: Int, stack: ItemStack) =
			index == 0 && TileEntityFurnace.getItemBurnTime(stack) > 0

		private final def limitStack(stack: Option[ItemStack]) = stack map
		{
			x => if(x.stackSize > this.getInventoryStackLimit) new ItemStack(x.getItem(), this.getInventoryStackLimit, x.getItemDamage())
			else x
		}

		// Inventory Interacts //
		override final def getStackInSlot(index: Int) = index match
		{
			case 0 => this.slotItem getOrElse null
			case _ => null
		}
		override final def getStackInSlotOnClosing(index: Int) = index match
		{
			case 0 =>
			{
				val item = this.slotItem
				this.slotItem = None
				item getOrElse null
			}
			case _ => null
		}
		override final def setInventorySlotContents(index: Int, stack: ItemStack) =
			if(index == 0) this.slotItem = this.limitStack(Option(stack))
		override final def decrStackSize(index: Int, amount: Int) = (index, this.slotItem) match
		{
			case (0, Some(s)) =>
			{
				if(s.stackSize <= amount)
				{
					val ret = s
					this.slotItem = None
					ret
				}
				else s splitStack amount
			}
			case _ => null
		}
		override final def openInventory() = ()
		override final def closeInventory() = ()

		// TileEntity Ticking //
		override final val canUpdate = true
		override final def updateEntity()
		{
			val updateChecker = this.burnTimeLast
			this.burnTimeLast = if(this.burnTimeLast > 0) this.processBurn() else this.useNextFuel()
			if(this.burnTimeLast != updateChecker)
			{
				this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord)
				this.markDirty()
			}
		}
		private def processBurn() = this.burnTimeLast - 1
		private def useNextFuel() = this.slotItem map { TileEntityFurnace.getItemBurnTime _ } map
		{
			case itemBurnTime if itemBurnTime > 0 =>
			{
				for(x <- this.slotItem)
				{
					x.stackSize -= 1
					if(x.stackSize <= 0) this.slotItem = Option(x.getItem.getContainerItem(x))
				}
				this.fullBurnTime = itemBurnTime
				itemBurnTime
			}
			case _ => 0
		} getOrElse 0
		def burnRemainingPercent = if(this.fullBurnTime <= 0) 0.0 else this.burnTimeLast.toDouble / this.fullBurnTime

		// Information Provider //
		override final def provideInformation(list: java.util.List[String])
		{
			for(slot <- this.slotItem) list add s"Stacking ${slot.getItem.getItemStackDisplayName(slot)}x${slot.stackSize}"
			val burnTimePercent = (this.burnRemainingPercent * 100.0).asInstanceOf[Int]
			list add s"Last BurnTime: $burnTimeLast tick(s) [${burnTimePercent}%]"
		}
    }

	// Container and Gui //
	import net.minecraft.inventory.{Container => ContainerBase, Slot}
	import net.minecraft.entity.player.InventoryPlayer
	import net.minecraft.client.gui.inventory.GuiContainer
	import net.minecraft.util.ResourceLocation
	import com.cterm2.mcfm1710.utils.LocalTranslationUtils._
	final class Container(val te: TileEntity, val invPlayer: InventoryPlayer) extends ContainerBase
	{
		// initialize slots
		this.addSlotToContainer(new Slot(te, 0, 80, 44))			// fuel slot
		this.addPlayerSlots(invPlayer, 8, 84)

		// Container Configurations //
		// can container interact with the player
		override def canInteractWith(player: EntityPlayer) = te isUseableByPlayer player

		// Container Interacts //
		// Transfer stack between slot with shortcut(shift-clicking)
		override def transferStackInSlot(player: EntityPlayer, slotIndex: Int) =
			Option(this.inventorySlots.get(slotIndex).asInstanceOf[Slot]) map
			{
				case slotObject if slotObject.getHasStack =>
				{
					// available slot and contains item
					val slotStack = slotObject.getStack
					val stackOld = slotStack.copy

					(slotIndex match
					{
						// fuel slot -> player slot
						case 0 => this.mergeItemStack(slotStack, 1, 1 + 9 * 4, false)
						case n if 1 until (1 + 9 * 3) contains n => this.mergeItemStack(slotStack, 1 + 9 * 3, 1 + 9 * 4, false)
						case _ => this.mergeItemStack(slotStack, 1, 1 + 9 * 3, false)
					}) match
					{
						case true =>
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
						case _ => null
					}
				}
				case _ => null
			} getOrElse null

		def addPlayerSlots(invPlayer: InventoryPlayer, left: Int, top: Int) =
		{
			for(i <- 0 until 3; j <- 0 until 9) this.addSlotToContainer(new Slot(invPlayer, 9 + j + i * 9, left + j * 18, top + i * 18))
			for(j <- 0 until 9) this.addSlotToContainer(new Slot(invPlayer, j, left + j * 18, top + 3 * 18 + 2))
		}
	}
	@SideOnly(Side.CLIENT)
	final class Gui(val con: Container) extends GuiContainer(con)
	{
		val backImage = new ResourceLocation(FMEntry.ID, "textures/guiBase/thermalGenerator.png")

		// Draw GUI Foreground Layer(caption layer)
		override final def drawGuiContainerForegroundLayer(p1: Int, p2: Int)
		{
			val caption = t"container.sourceGenerator.thermal"
			val capWidth = this.fontRendererObj.getStringWidth(caption)
			this.fontRendererObj.drawString(caption, (this.xSize - capWidth) / 2, 10, 0x404040)
			this.fontRendererObj.drawString(t"container.inventory", 8, 72, 0x404040)
		}
		// Draw GUI Background Layer(backimage layer)
		override final def drawGuiContainerBackgroundLayer(p1: Float, p2: Int, p3: Int)
		{
			this.mc.getTextureManager.bindTexture(this.backImage)
			this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize)
			val burnTextureHeight = (16 - con.te.burnRemainingPercent * 16).asInstanceOf[Int]
			this.drawTexturedModalRect(this.guiLeft + 80, this.guiTop + 24 + burnTextureHeight, this.xSize, burnTextureHeight, 16, 16)
		}
	}
}