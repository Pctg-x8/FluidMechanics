package com.cterm2.mcfm1710

import cpw.mods.fml.relauncher.{Side, SideOnly}
import net.minecraft.world.World
import net.minecraftforge.fluids._

// Thermal Fluid Generator

package object ThermalFluidGenerator
{
	def register()
	{
		ContentRegistry register UnlitBlock.setCreativeTab(FluidTab) as "sourceGenerator.thermalFluid"
		ContentRegistry register LitBlock as "sourceGenerator.thermalFluid.lit"
		ContentRegistry register classOf[TileEntity] as "TEThermalFluidGenerator"
	}
	@SideOnly(Side.CLIENT)
	def registerClient() = ()

	// Change block state with replacing
	def updateLitState(world: World, x: Int, y: Int, z: Int, lit: Boolean, forceUpdate: Boolean = false) =
	{
		val content = Option(world.getTileEntity(x, y, z))
		val meta = world.getBlockMetadata(x, y, z)
		val block = world.getBlock(x, y, z)

		SourceGenerator.replacing = true
		val doReplace = if(lit && block == UnlitBlock) world.setBlock(x, y, z, LitBlock)
			else if(!lit && block == LitBlock) world.setBlock(x, y, z, UnlitBlock)
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
	// Check if fluid can be used as fuel
	def isValidFuel(fluid: FluidStack) = fluid.getFluid == FluidRegistry.LAVA
}

package ThermalFluidGenerator
{
	import net.minecraft.client.renderer.texture.IIconRegister
	import net.minecraft.world.World
	import net.minecraft.entity.player.EntityPlayer
	import utils.ActiveNBTRecord._

	object StoreKeys
	{
		final val Tank = "fuelTank"
		final val Injector = "injector"
	}

	// Blocks //
	sealed abstract class Block extends SourceGenerator.BlockBase("mcfm1710:thermalFluidGenerator")
	{
		// Block Traits //
		override final def createNewTileEntity(world: World, meta: Int) = new TileEntity

		// Block Interacts //
		override final def openContainerScreen(player: EntityPlayer, world: World, x: Int, y: Int, z: Int)
		{
			player.openGui(FMEntry, 0, world, x, y, z)
		}
		override final def breakContainer(world: World, x: Int, y: Int, z: Int, meta: Int){}

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
			this.icons(TextureIndices.Front) = register.registerIcon("mcfm1710:thermalFluidGenerator6")
		}

		override def getUnlocalizedName = UnlitBlock.getUnlocalizedName
	}

	// Tile Entity //
	import net.minecraftforge.fluids._
	import cpw.mods.fml.common.Optional
	import net.minecraft.nbt.NBTTagCompound
	import net.minecraft.network.NetworkManager, net.minecraft.network.play.server.S35PacketUpdateTileEntity
	import net.minecraftforge.common.util.ForgeDirection
	import interops.smartcursor._
	@Optional.Interface(iface="com.cterm2.mcfm1710.interops.smartcursor.IInformationProvider", modid=SCModuleConnector.ID, striprefs=true)
	final class TileEntity extends SourceGenerator.TileEntityBase with IFluidHandler with IInformationProvider
	{
		// Internal Data //
		val tank = new Generics.FluidTank(16000)

		// Data Synchronization //
		override def storePacketData(tag: NBTTagCompound) =
		{
			super.storePacketData(tag)
			tank.synchronizeData foreach { tag(StoreKeys.Tank) = _ }
			tag
		}
		override def loadPacketData(tag: NBTTagCompound)
		{
			super.loadPacketData(tag)
			tank.synchronizeData = tag[NBTTagCompound](StoreKeys.Tank)
		}

		// Fluid Handling //
		override def getTankInfo(from: ForgeDirection) =
			if(isPort(from)) Array(this.tank.getInfo) else this.injector map { _.getTankInfo(from) } getOrElse null
		override def canDrain(from: ForgeDirection, fluid: Fluid) =
			if(isPort(from)) this.tank canDrain fluid else this.injector exists { _.canDrain(from, fluid) }
		override def canFill(from: ForgeDirection, fluid: Fluid) =
			if(isPort(from)) this.tank canFill fluid else this.injector exists { _.canFill(from, fluid) }
		override def fill(from: ForgeDirection, resource: FluidStack, perform: Boolean) =
			if(this.injector exists { _.canFill(from, resource.getFluid) }) this.injector map { _.fill(from, resource, perform) } getOrElse 0
			else this.tank.fill(resource, perform)
		override def drain(from: ForgeDirection, resource: FluidStack, perform: Boolean) =
			if(Option(resource) exists { this.tank canDrain _.getFluid }) this.tank.drain(resource.amount, perform) else
			this.injector map { _.drain(from, resource, perform) } getOrElse null
		override def drain(from: ForgeDirection, maxDrain: Int, perform: Boolean) =
			if(this.injector exists { _.drain(from, maxDrain, false) != null }) this.injector map { _.drain(from, maxDrain, perform) } getOrElse null
			else this.tank.drain(maxDrain, perform)
		private def isPort(from: ForgeDirection) = from == ForgeDirection.UP || from == this.frontDirection.getOpposite

		// Updating //
		override def updateEntity()
		{
			import utils.WorldExtensions._

			if(!this.worldObj.inClientSide)
			{
				if(this.tank.getFluidOpt exists isValidFuel) Option(this.tank.drain(1, true)) match
				{
					case Some(_) =>
						this.injector foreach { _.injectFluids(1) }
						if(updateLitState(this.worldObj, this.xCoord, this.yCoord, this.zCoord, true)) this.markDirty()
					case None =>
						if(updateLitState(this.worldObj, this.xCoord, this.yCoord, this.zCoord, false)) this.markDirty()
				}
				else if(updateLitState(this.worldObj, this.xCoord, this.yCoord, this.zCoord, false)) this.markDirty()
			}
		}

		// Information Provider //
		override def provideInformation(list: java.util.List[String])
		{
			list add s"Fluid Fuel amount: ${this.tank.getFluidAmount} mb"
			this.injector foreach { _.provideInformation(list) }
		}
		// Send Description packet to client
		override def forceSynchronize() =
			updateLitState(this.worldObj, this.xCoord, this.yCoord, this.zCoord, this.tank.getFluidOpt exists isValidFuel, true)
	}

	// Container and Gui //
	import net.minecraft.entity.player.InventoryPlayer
	import net.minecraft.item.ItemStack
	import net.minecraft.client.gui.inventory.GuiContainer
	import net.minecraft.util.ResourceLocation
	object ProgressBar
	{
		final val FuelAmount = 0
	}
	final class Container(val te: TileEntity, val invPlayer: InventoryPlayer) extends Generics.Container
	{
		import utils.EntityLivingUtils._
		import net.minecraft.inventory.ICrafting
		import collection.JavaConversions._

		val (playerSlotStart, playerHotbarSlotStart, playerSlotEnd) = this.addPlayerSlots(invPlayer, 8, 84)

		override def canInteractWith(player: EntityPlayer) = player isUseable te
		override def mergeStackInTransferring(stack: ItemStack, index: Int) = index match
		{
			case n if playerSlotStart until playerHotbarSlotStart contains n => this.mergeItemStack(stack, playerHotbarSlotStart, playerSlotEnd, false)
			case _ => this.mergeItemStack(stack, playerSlotStart, playerHotbarSlotStart, false)
		}

		// Container Interacts with Crafters //
		var lastFuelAmount = this.te.tank.getFluidAmount
		override def addCraftingToCrafters(crafter: ICrafting)
		{
			super.addCraftingToCrafters(crafter)
			// initial syncing
			crafter.sendProgressBarUpdate(this, ProgressBar.FuelAmount, this.te.tank.getFluidAmount)
			this.lastFuelAmount = this.te.tank.getFluidAmount
		}
		override def detectAndSendChanges()
		{
			super.detectAndSendChanges()

			if(this.te.tank.getFluidAmount != this.lastFuelAmount)
			{
				this.lastFuelAmount = this.te.tank.getFluidAmount
				this.crafters map { _.asInstanceOf[ICrafting] } foreach
				{
					_.sendProgressBarUpdate(this, ProgressBar.FuelAmount, this.lastFuelAmount)
				}
			}
		}
		// receiver
		@SideOnly(Side.CLIENT)
		override def updateProgressBar(index: Int, value: Int) = index match
		{
			case ProgressBar.FuelAmount => this.te.tank.setAmount(value)
		}
	}
	final class Gui(val c: Container) extends GuiContainer(c)
	{
		import utils.LocalTranslationUtils._
		import net.minecraft.client.renderer.texture.TextureMap

		val backImage = new ResourceLocation(FMEntry.ID, "textures/guiBase/thermalFluidGenerator.png")

		override def drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int)
		{
			val caption = t"container.sourceGenerator.thermalFluid"
			val capWidth = this.fontRendererObj.getStringWidth(caption)
			this.fontRendererObj.drawString(caption, (this.xSize - capWidth) / 2, 6, 0x404040)
			this.fontRendererObj.drawString(t"container.inventory", 8, 72, 0x404040)

			if((this.guiLeft + 64 until this.guiLeft + 64 + 48 contains mouseX) && (this.guiTop + 20 until this.guiTop + 20 + 48 contains mouseY))
			{
				val list = new java.util.ArrayList[String]
				c.te.tank.getFluidOpt match
				{
					case Some(f) if f.amount > 0 =>
						val ted = t"${f.getFluid.getUnlocalizedName}"
						list add s"$ted: ${f.amount} mb"
					case _ => list add "Empty"
				}
				this.drawHoveringText(list, mouseX - this.guiLeft, mouseY - this.guiTop, this.fontRendererObj)
			}
		}
		override def drawGuiContainerBackgroundLayer(p1: Float, mouseX: Int, mouseY: Int)
		{
			this.mc.getTextureManager.bindTexture(this.backImage)
			this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize)
			c.te.tank.getFluidOpt foreach
			{
				case fluid if fluid.amount > 0 =>
					val pixels = (48 * (fluid.amount / 16000.0)).asInstanceOf[Int]
					this.mc.getTextureManager.bindTexture(TextureMap.locationBlocksTexture)
					for(left <- 0 until 3 map { this.guiLeft + 64 + 16 * _ })
					{
						for(top <- 0 until 3 map { this.guiTop + 20 + 48 - _ * 16 - 16 })
						{
							this.drawTexturedModelRectFromIcon(left, top, fluid.getFluid.getStillIcon, 16, 16)
						}
					}
					this.mc.getTextureManager.bindTexture(this.backImage)
					this.drawTexturedModalRect(this.guiLeft + 64, this.guiTop + 20, 64, 20, 48, 48 - pixels)
			}
			this.drawTexturedModalRect(this.guiLeft + 64, this.guiTop + 20, this.xSize, 0, 6, 48)
			this.drawTexturedModalRect(this.guiLeft + 64 + 48 - 8, this.guiTop + 20, this.xSize + 8, 0, 8, 48)
		}
	}
}
