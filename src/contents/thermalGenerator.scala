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
	import utils.WorldExtensions._

	final object StoreKeys
	{
		final val FuelStack = "fuelStack"
		final val BurnTime = "burnTimeLast"
		final val FullBurnTime = "burnTimeFull"
	}
	final object ProgressBar
	{
		final val BurnTime = 0
		final val FullBurnTime = 1
	}

    final object Block extends SourceGenerator.BlockBase
    {
        // Block Traits //
        override def createNewTileEntity(world: World, meta: Int) = new TileEntity

		// Block Interacts //
		override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, xo: Float, yo: Float, zo: Float) =
			(world.inClientSide, world.getTileEntity(x, y, z).asInstanceOf[TileEntity]) match
			{
				case (true, _) => true
				case (false, tile) if tile != null => { player.openGui(FMEntry, 0, world, x, y, z); true }
				case _ => false
			}
    }

	@Optional.Interface(iface="com.cterm2.mcfm1710.interops.smartcursor.IInformationProvider", modid=SCModuleConnector.ID, striprefs=true)
    final class TileEntity extends TileEntityBase with ISourceGenerator with IInventory with IInformationProvider
    {
		import net.minecraft.tileentity.TileEntityFurnace
		import net.minecraft.network.play.server.S35PacketUpdateTileEntity
		import net.minecraft.network.NetworkManager
		import utils.EntityLivingUtils._

		// Internal Data //
		private var slotItem: Option[ItemStack] = None
		var burnTimeLast = 0
		var fullBurnTime = 0

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
		/*override final def getDescriptionPacket =
			this.storeSpecificDataTo _ andThen (new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, _)) apply (new NBTTagCompound)
		override final def onDataPacket(net: NetworkManager, packet: S35PacketUpdateTileEntity)
		{
			((_: S35PacketUpdateTileEntity).func_148857_g) andThen this.loadSpecificDataFrom apply packet
		}*/

		// Inventory Configurations //
		override final val getSizeInventory = 1
		override final val getInventoryName = "container.fuelStack"
		override final val hasCustomInventoryName = false
		override final val getInventoryStackLimit = 64
		override final def isUseableByPlayer(player: EntityPlayer) = player isUseable this
		override final def isItemValidForSlot(index: Int, stack: ItemStack) = index == 0 && TileEntityFurnace.getItemBurnTime(stack) > 0

		private final def limitStack(stack: Option[ItemStack]) = stack map
		{
			x => if(x.stackSize > this.getInventoryStackLimit) new ItemStack(x.getItem, this.getInventoryStackLimit, x.getItemDamage)
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
		override final def setInventorySlotContents(index: Int, stack: ItemStack) = if(index == 0) this.slotItem = this.limitStack(Option(stack))
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
		override final def updateEntity()
		{
			this.burnTimeLast = if(this.burnTimeLast > 0) this.processBurn() else this.useNextFuel()
		}
		private def processBurn() = this.burnTimeLast - 1
		private def useNextFuel() = (this.worldObj.inClientSide, this.slotItem) match
		{
			case (true, _) => 0
			case (false, Some(item)) =>
			{
				val burnTimeNext = TileEntityFurnace.getItemBurnTime(item)
				if(burnTimeNext > 0)
				{
					item.stackSize -= 1
					if(item.stackSize <= 0) this.slotItem = Option(item.getItem.getContainerItem(item))
					this.markDirty()
					this.fullBurnTime = burnTimeNext
					burnTimeNext
				}
				else { unlitBlock(); 0 }
			}
			case _ => { unlitBlock(); 0 }
		}
		private def unlitBlock()
		{
			// Unlit block(actual all fuels burned?)
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord)
			this.markDirty()
		}
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
	import net.minecraft.inventory.Slot
	import net.minecraft.entity.player.InventoryPlayer
	import net.minecraft.client.gui.inventory.GuiContainer
	import net.minecraft.util.ResourceLocation
	import com.cterm2.mcfm1710.utils.LocalTranslationUtils._
	final class Container(val te: TileEntity, val invPlayer: InventoryPlayer) extends Generics.Container
	{
		import utils.EntityLivingUtils._
		import net.minecraft.inventory.ICrafting
		import collection.JavaConversions._

		// initialize slots
		this.addSlotToContainer(new Slot(te, 0, 80, 44))			// fuel slot
		val (playerSlotStart, playerHotbarSlotStart, playerSlotEnd) = this.addPlayerSlots(invPlayer, 8, 84)

		// Container Configurations //
		// can container interact with the player
		override def canInteractWith(player: EntityPlayer) = player isUseable te

		// Container Interacts //
		override def mergeStackInTransferring(stack: ItemStack, index: Int) = index match
		{
			// fuel slot -> player slot
			case 0 => this.mergeItemStack(stack, playerSlotStart, playerSlotEnd, false)
			case n if playerSlotStart until playerHotbarSlotStart contains n => this.mergeItemStack(stack, playerHotbarSlotStart, playerSlotEnd, false)
			case _ => this.mergeItemStack(stack, playerSlotStart, playerHotbarSlotStart, false)
		}

		// Container Interacts with Crafters //
		var lastBurnTimeLast = this.te.burnTimeLast
		var lastFullBurnTime = this.te.fullBurnTime
		override def addCraftingToCrafters(crafter: ICrafting)
		{
			super.addCraftingToCrafters(crafter)
			// initial syncing
			crafter.sendProgressBarUpdate(this, ProgressBar.BurnTime, this.te.burnTimeLast)
			crafter.sendProgressBarUpdate(this, ProgressBar.FullBurnTime, this.te.fullBurnTime)
			this.lastBurnTimeLast = this.te.burnTimeLast
			this.lastFullBurnTime = this.te.fullBurnTime
		}
		override def detectAndSendChanges()
		{
			super.detectAndSendChanges()

			for(crafter <- this.crafters map { _.asInstanceOf[ICrafting] })
			{
				if(this.te.burnTimeLast != this.lastBurnTimeLast) crafter.sendProgressBarUpdate(this, ProgressBar.BurnTime, this.te.burnTimeLast)
				if(this.te.fullBurnTime != this.lastFullBurnTime) crafter.sendProgressBarUpdate(this, ProgressBar.FullBurnTime, this.te.fullBurnTime)
			}
			this.lastBurnTimeLast = this.te.burnTimeLast
			this.lastFullBurnTime = this.te.fullBurnTime
		}
		// receiver
		@SideOnly(Side.CLIENT)
		override def updateProgressBar(index: Int, value: Int) = index match
		{
			case ProgressBar.BurnTime => this.te.burnTimeLast = value
			case ProgressBar.FullBurnTime => this.te.fullBurnTime = value
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
