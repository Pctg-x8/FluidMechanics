package com.cterm2.mcfm1710

import cpw.mods.fml.relauncher.{SideOnly, Side}

// Thermal Fluid Generator

package object ThermalFluidGenerator
{
	def register(ctab: net.minecraft.creativetab.CreativeTabs)
	{
		ContentRegistry register Block.setCreativeTab(ctab) as "sourceGenerator.thermalFluid"
		ContentRegistry register classOf[TileEntity] as "TEThermalFluidGenerator"
	}
	@SideOnly(Side.CLIENT)
	def registerClient()
	{

	}
}

package ThermalFluidGenerator
{
	import net.minecraft.world.World
	import net.minecraft.entity.player.EntityPlayer

	final object StoreKeys
	{
		final val Tank = "fuelTank"
	}

	// Blocks //
	final object Block extends SourceGenerator.BlockBase
	{
		// Block Traits //
		override final def createNewTileEntity(world: World, meta: Int) = new TileEntity

		// Block Interacts //
		override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, xo: Float, yo: Float, zo: Float) =
			(world.isRemote, world.getTileEntity(x, y, z).asInstanceOf[TileEntity]) match
			{
				case (true, _) => true
				case (false, tile) => { player.openGui(FMEntry, 0, world, x, y, z); true }
				case _ => false
			}
	}

	// Tile Entity //
	import net.minecraftforge.fluids._
	import cpw.mods.fml.common.Optional
	import net.minecraft.nbt.NBTTagCompound
	import net.minecraft.network.NetworkManager, net.minecraft.network.play.server.S35PacketUpdateTileEntity
	import net.minecraftforge.common.util.ForgeDirection
	import interfaces.ISourceGenerator, interops.smartcursor._
	@Optional.Interface(iface="com.cterm2.mcfm1710.interops.smartcursor.IInformationProvider", modid=SCModuleConnector.ID, striprefs=true)
	final class TileEntity extends net.minecraft.tileentity.TileEntity with ISourceGenerator with IFluidHandler with IInformationProvider
	{
		// Internal Data //
		val tank = new Generics.FluidTank(16000)

		// Data Synchronization //
		override final def storeSpecificDataTo(tag: NBTTagCompound) =
		{
			tank.synchronizeData foreach { tag.setTag(StoreKeys.Tank, _) }
			tag
		}
		override final def loadSpecificDataFrom(tag: NBTTagCompound) =
		{
			tank.synchronizeDataFrom(Option(tag.getTag(StoreKeys.Tank).asInstanceOf[NBTTagCompound]))
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
			{ (_: S35PacketUpdateTileEntity).func_148857_g } andThen this.loadSpecificDataFrom apply packet
		}

		// Fluid Handling //
		override final def getTankInfo(from: ForgeDirection) = Array(this.tank.getInfo)
		override final def canDrain(from: ForgeDirection, fluid: Fluid) = this.tank canDrain fluid
		override final def canFill(from: ForgeDirection, fluid: Fluid) = this.tank canFill fluid
		override final def fill(from: ForgeDirection, resource: FluidStack, perform: Boolean) = this.tank.fill(resource, perform)
		override final def drain(from: ForgeDirection, resource: FluidStack, perform: Boolean) =
			if(Option(resource) map { this.tank canDrain _.getFluid } getOrElse false) this.tank.drain(resource.amount, perform) else null
		override final def drain(from: ForgeDirection, maxDrain: Int, perform: Boolean) = this.tank.drain(maxDrain, perform)

		// Information Provider //
		override final def provideInformation(list: java.util.List[String])
		{
			list add s"Fluid Fuel amount: ${this.tank.getFluidAmount} mb"
		}
		// Send Description packet to client
		override final def forceSynchronize() = this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord)
	}

	// Container and Gui //
	import net.minecraft.entity.player.InventoryPlayer
	import net.minecraft.item.ItemStack
	import net.minecraft.client.gui.inventory.GuiContainer
	import net.minecraft.util.ResourceLocation
	final class Container(val te: TileEntity, val invPlayer: InventoryPlayer) extends Generics.Container
	{
		import utils.EntityLivingUtils._

		val (playerSlotStart, playerHotbarSlotStart, playerSlotEnd) = this.addPlayerSlots(invPlayer, 8, 84)

		override def canInteractWith(player: EntityPlayer) = player isUseable te
		override def mergeStackInTransferring(stack: ItemStack, index: Int) = index match
		{
			case n if playerSlotStart until playerHotbarSlotStart contains n => this.mergeItemStack(stack, playerHotbarSlotStart, playerSlotEnd, false)
			case _ => this.mergeItemStack(stack, playerSlotStart, playerHotbarSlotStart, false)
		}
	}
	final class Gui(val c: Container) extends GuiContainer(c)
	{
		import utils.LocalTranslationUtils._

		final val backImage = new ResourceLocation(FMEntry.ID, "textures/guiBase/thermalFluidGenerator.png")

		override def drawGuiContainerForegroundLayer(p1: Int, p2: Int)
		{
			val caption = t"container.sourceGenerator.thermalFluid"
			val capWidth = this.fontRendererObj.getStringWidth(caption)
			this.fontRendererObj.drawString(caption, (this.xSize - capWidth) / 2, 6, 0x404040)
			this.fontRendererObj.drawString(t"container.inventory", 8, 72, 0x404040)
		}
		override def drawGuiContainerBackgroundLayer(p1: Float, p2: Int, p3: Int)
		{
			this.mc.getTextureManager.bindTexture(this.backImage)
			this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize)
			this.drawTexturedModalRect(this.guiLeft + 64, this.guiTop + 20, this.xSize, 0, 6, 48)
			this.drawTexturedModalRect(this.guiLeft + 64 + 48 - 8, this.guiTop + 20, this.xSize + 8, 0, 8, 48)
		}
	}
}
