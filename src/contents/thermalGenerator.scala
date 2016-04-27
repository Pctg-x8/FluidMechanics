package com.cterm2.mcfm1710

import interops.smartcursor._
import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.world.World

package object ThermalGenerator
{
	def register()
	{
		ContentRegistry register UnlitBlock.setCreativeTab(FluidTab) as "sourceGenerator.thermal"
		ContentRegistry register LitBlock as "sourceGenerator.thermal.lit"
		ContentRegistry register classOf[TileEntity] as "TEThermalGenerator"
	}

	def setBlockLitState(world: World, x: Int, y: Int, z: Int, lit: Boolean, forceUpdate: Boolean = false) =
	{
		val content = Option(world.getTileEntity(x, y, z))
		val meta = world.getBlockMetadata(x, y, z)

		SourceGenerator.replacing = true
		val doReplace = if(lit && world.getBlock(x, y, z) == UnlitBlock) world.setBlock(x, y, z, LitBlock)
		else if(!lit && world.getBlock(x, y, z) == LitBlock) world.setBlock(x, y, z, UnlitBlock)
		else false
		SourceGenerator.replacing = false

		if(doReplace)
		{
			world.setBlockMetadataWithNotify(x, y, z, meta, 2)
			for(c <- content)
			{
				c.validate()
				world.setTileEntity(x, y, z, c)
			}
		}
		else if(forceUpdate) world.markBlockForUpdate(x, y, z)
		doReplace
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
    import net.minecraft.client.renderer.texture.IIconRegister
    import net.minecraft.util.IIcon
    import utils.WorldExtensions._

	object StoreKeys
	{
		final val FuelStack = "fuelStack"
		final val BurnTime = "burnTimeLast"
		final val FullBurnTime = "burnTimeFull"
		final val Injector = "injector"
	}
	object ProgressBar
	{
		final val BurnTime = 0
		final val FullBurnTime = 1
	}

    sealed class Block extends SourceGenerator.BlockBase("mcfm1710:thermalGenerator")
    {
        // Block Traits //
        override def createNewTileEntity(world: World, meta: Int) = new TileEntity

		// Block Interacts //
		override def openContainerScreen(player: EntityPlayer, world: World, x: Int, y: Int, z: Int)
		{
			player.openGui(FMEntry, 0, world, x, y, z)
		}
	    override def breakContainer(world: World, x: Int, y: Int, z: Int, meta: Int)
	    {
			Option(world.getTileEntity(x, y, z).asInstanceOf[TileEntity]) foreach { _.dropItems() }
	    }

		import net.minecraft.item.Item, java.util.Random
		override final def getItem(world: World, x: Int, y: Int, z: Int) = Item.getItemFromBlock(UnlitBlock)
		override final def getItemDropped(meta: Int, random: Random, fortune: Int) = Item.getItemFromBlock(UnlitBlock)
    }
	object UnlitBlock extends Block { this.setLightLevel(0.0f) }
	object LitBlock extends Block
	{
		this.setLightLevel(0.875f)

		override def registerBlockIcons(register: IIconRegister)
		{
			super.registerBlockIcons(register)
			this.icons(TextureIndices.Front) = register.registerIcon("mcfm1710:thermalGenerator6")
		}

		override def getUnlocalizedName = UnlitBlock.getUnlocalizedName
	}

	import net.minecraftforge.fluids._, net.minecraftforge.common.util.ForgeDirection
	@Optional.Interface(iface="com.cterm2.mcfm1710.interops.smartcursor.IInformationProvider", modid=SCModuleConnector.ID, striprefs=true)
    final class TileEntity extends SourceGenerator.TileEntityBase with IInventory with IInformationProvider with IFluidHandler
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
        override def storePacketData(tag: NBTTagCompound) =
        {
			super.storePacketData(tag)
			tag.setShort(StoreKeys.BurnTime, this.burnTimeLast.asInstanceOf[Short])
			tag.setShort(StoreKeys.FullBurnTime, this.fullBurnTime.asInstanceOf[Short])
			this.slotItem map { x => val tag = new NBTTagCompound; x.writeToNBT(tag); tag } foreach { tag.setTag(StoreKeys.FuelStack, _) }
			tag
        }
        override def loadPacketData(tag: NBTTagCompound)
        {
			super.loadPacketData(tag)
			this.burnTimeLast = Option(tag.getShort(StoreKeys.BurnTime).asInstanceOf[Int]) getOrElse 0
			this.fullBurnTime = Option(tag.getShort(StoreKeys.FullBurnTime).asInstanceOf[Int]) getOrElse 0
			this.slotItem = Option(tag.getTag(StoreKeys.FuelStack).asInstanceOf[NBTTagCompound]) map ItemStack.loadItemStackFromNBT
        }

		// Fluid Handling //
		override def getTankInfo(from: ForgeDirection) = this.injector map { _.getTankInfo(from) } getOrElse null
		override def canDrain(from: ForgeDirection, fluid: Fluid) = this.injector exists { _.canDrain(from, fluid) }
		override def canFill(from: ForgeDirection, fluid: Fluid) = this.injector exists { _.canFill(from, fluid) }
		override def fill(from: ForgeDirection, resource: FluidStack, perform: Boolean) =
			this.injector map { _.fill(from, resource, perform) } getOrElse 0
		override def drain(from: ForgeDirection, resource: FluidStack, perform: Boolean) =
			this.injector map { _.drain(from, resource, perform) } getOrElse null
		override def drain(from: ForgeDirection, maxDrain: Int, perform: Boolean) =
			this.injector map { _.drain(from, maxDrain, perform) } getOrElse null

		// Inventory Configurations //
		override val getSizeInventory = 1
		override val getInventoryName = "container.fuelStack"
		override val hasCustomInventoryName = false
		override val getInventoryStackLimit = 64
		override def isUseableByPlayer(player: EntityPlayer) = player isUseable this
		override def isItemValidForSlot(index: Int, stack: ItemStack) = index == 0 && TileEntityFurnace.getItemBurnTime(stack) > 0

		private def limitStack(stack: Option[ItemStack]) = stack map
		{
			x => if(x.stackSize > this.getInventoryStackLimit) new ItemStack(x.getItem, this.getInventoryStackLimit, x.getItemDamage)
			else x
		}

		// Inventory Interacts //
		override def getStackInSlot(index: Int) = index match
		{
			case 0 => this.slotItem.orNull
			case _ => null
		}
		override def getStackInSlotOnClosing(index: Int) = index match
		{
			case 0 =>
				val item = this.slotItem
				this.slotItem = None
				item.orNull
			case _ => null
		}
		override def setInventorySlotContents(index: Int, stack: ItemStack) = if(index == 0) this.slotItem = this.limitStack(Option(stack))
		override def decrStackSize(index: Int, amount: Int) = (index, this.slotItem) match
		{
			case (0, Some(s)) =>
				if(s.stackSize <= amount)
				{
					val ret = s
					this.slotItem = None
					ret
				}
				else s splitStack amount
			case _ => null
		}
		override def openInventory() = ()
		override def closeInventory() = ()

		// TileEntity Ticking //
		override def updateEntity()
		{
			if(this.worldObj.inServerSide)
			{
				if(this.burnTimeLast > 0) this.burnTimeLast = this.processBurn()
				if(this.burnTimeLast <= 0) this.burnTimeLast = this.useNextFuel()
			}
		}
		private def processBurn() =
		{
			this.injector foreach { _.injectFluids(1) }
			this.burnTimeLast - 24
		}
		private def useNextFuel() = (this.worldObj.inClientSide, this.slotItem) match
		{
			case (true, _) => 0
			case (false, Some(item)) =>
				val burnTimeNext = TileEntityFurnace.getItemBurnTime(item)
				if(burnTimeNext > 0)
				{
					item.stackSize -= 1
					if(item.stackSize <= 0) this.slotItem = Option(item.getItem.getContainerItem(item))
					this.markDirty()
					this.fullBurnTime = burnTimeNext
					this.litBlock()
					this.burnTimeLast + burnTimeNext
				}
				else { this.unlitBlock(); 0 }
			case _ => this.unlitBlock(); 0
		}
		private def litBlock()
		{
			// Lit block
			if(setBlockLitState(this.worldObj, this.xCoord, this.yCoord, this.zCoord, true)) this.markDirty()
		}
		private def unlitBlock()
		{
			// Unlit block(actual all fuels burned?)
			if(setBlockLitState(this.worldObj, this.xCoord, this.yCoord, this.zCoord, false)) this.markDirty()
		}
		def burnRemainingPercent = if(this.fullBurnTime <= 0) 0.0 else this.burnTimeLast.toDouble / this.fullBurnTime

		// Information Provider //
		override def provideInformation(list: java.util.List[String])
		{
			for(slot <- this.slotItem) list add s"Stacking ${slot.getItem.getItemStackDisplayName(slot)}x${slot.stackSize}"
			val burnTimePercent = (this.burnRemainingPercent * 100.0).asInstanceOf[Int]
			list add s"Last BurnTime: $burnTimeLast tick(s) [$burnTimePercent%]"
			this.injector foreach { _.provideInformation(list) }
		}
		// Send Description packet to client
		override def forceSynchronize() =
			setBlockLitState(this.worldObj, this.xCoord, this.yCoord, this.zCoord,
				(this.slotItem exists { TileEntityFurnace.getItemBurnTime(_) > 0 }) || this.burnTimeLast > 0, true)

		// Drop all contained items from specified coordinate
		def dropItems() =
		{
			import net.minecraft.entity.item.EntityItem
			import utils._, utils.EntityExtensions._

			this.slotItem foreach { _.dropAsEntityRandom(this.worldObj, this.xCoord, this.yCoord, this.zCoord) }
		}
    }

	// Container and Gui //
	import net.minecraft.inventory.Slot
	import net.minecraft.entity.player.InventoryPlayer
	import net.minecraft.client.gui.inventory.GuiContainer
	import net.minecraft.util.ResourceLocation
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
		import utils.LocalTranslationUtils._

		val backImage = new ResourceLocation(FMEntry.ID, "textures/guiBase/thermalGenerator.png")

		// Draw GUI Foreground Layer(caption layer)
		override def drawGuiContainerForegroundLayer(p1: Int, p2: Int)
		{
			val caption = t"container.sourceGenerator.thermal"
			val capWidth = this.fontRendererObj.getStringWidth(caption)
			this.fontRendererObj.drawString(caption, (this.xSize - capWidth) / 2, 10, 0x404040)
			this.fontRendererObj.drawString(t"container.inventory", 8, 72, 0x404040)
		}
		// Draw GUI Background Layer(backimage layer)
		override def drawGuiContainerBackgroundLayer(p1: Float, p2: Int, p3: Int)
		{
			this.mc.getTextureManager.bindTexture(this.backImage)
			this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize)
			val burnTextureHeight = (16 - con.te.burnRemainingPercent * 16).asInstanceOf[Int]
			this.drawTexturedModalRect(this.guiLeft + 80, this.guiTop + 24 + burnTextureHeight, this.xSize, burnTextureHeight, 16, 16)
		}
	}
}
