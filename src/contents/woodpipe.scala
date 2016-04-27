package com.cterm2.mcfm1710

import cpw.mods.fml.relauncher.{Side, SideOnly}

package object WoodPipe
{
	def register()
	{
		ContentRegistry register Block.setCreativeTab(FluidTab) as "woodpipe"
		ContentRegistry register classOf[TEPipe] as "TEPipe"
	}
	@SideOnly(Side.CLIENT)
	def registerClient()
	{

	}
}
package WoodPipe
{
	import net.minecraft.block, net.minecraft.block.material.Material
	import net.minecraft.world.{IBlockAccess, World}
	import net.minecraft.tileentity.TileEntity
	import cpw.mods.fml.common.Optional
	import net.minecraftforge.fluids._, net.minecraftforge.common.util.ForgeDirection
	import interops.smartcursor._
	import net.minecraft.nbt.NBTTagCompound
	import utils.ActiveNBTRecord._, utils.LocalTranslationUtils._
	import com.cterm2.mcfm1710.interops.smartcursor._

	object StoreKeys
	{
		final val Tank = "tpbuffer"
	}

	object Block extends block.BlockContainer(Material.wood)
	{
		final val pipeThickness = 0.5f

		this.setHardness(0.5f)
		this.setBlockBounds(0.0f, (1.0f - pipeThickness) * 0.5f, (1.0f - pipeThickness) * 0.5f,
			1.0f, (1.0f + pipeThickness) * 0.5f, (1.0f + pipeThickness) * 0.5f)

		// Rendering Configurations //
		override val renderAsNormalBlock = false
		override val isOpaqueCube = false

		// Shape Configurations //
		override def setBlockBoundsBasedOnState(world: IBlockAccess, x: Int, y: Int, z: Int)
		{
			def foundTankInfos(from: ForgeDirection) =
				(x: TileEntity) => (Option(x.asInstanceOf[IFluidHandler]) flatMap {
					(x: IFluidHandler) => Option(x.getTankInfo(from))
				} exists { _.nonEmpty })

			val (leftConnected, rightConnected, topConnected, bottomConnected, frontConnected, backConnected) =
				(Option(world.getTileEntity(x + 1, y, z)) exists foundTankInfos(ForgeDirection.WEST),
				Option(world.getTileEntity(x - 1, y, z)) exists foundTankInfos(ForgeDirection.EAST),
				Option(world.getTileEntity(x, y + 1, z)) exists foundTankInfos(ForgeDirection.DOWN),
				Option(world.getTileEntity(x, y - 1, z)) exists foundTankInfos(ForgeDirection.UP),
				Option(world.getTileEntity(x, y, z + 1)) exists foundTankInfos(ForgeDirection.NORTH),
				Option(world.getTileEntity(x, y, z - 1)) exists foundTankInfos(ForgeDirection.SOUTH))
			def getExtentNeg(connected: Boolean) = if(connected) 0.0f else (1.0f - pipeThickness) * 0.5f
			def getExtentPos(connected: Boolean) = if(connected) 1.0f else (1.0f + pipeThickness) * 0.5f

			this.setBlockBounds(getExtentNeg(rightConnected), getExtentNeg(bottomConnected), getExtentNeg(backConnected),
				getExtentPos(leftConnected), getExtentPos(topConnected), getExtentPos(frontConnected))
		}
		override def setBlockBoundsForItemRender()
		{
			this.setBlockBounds((1.0f - pipeThickness) * 0.5f, 0.0f, (1.0f - pipeThickness) * 0.5f,
				(1.0f + pipeThickness) * 0.5f, 1.0f, (1.0f + pipeThickness) * 0.5f)
		}

		override def createNewTileEntity(world: World, meta: Int) = new TEPipe
	}
	@Optional.Interface(iface="com.cterm2.mcfm1710.interops.smartcursor.IInformationProvider", modid=SCModuleConnector.ID, striprefs=true)
	class TEPipe extends base.NetworkTileEntity with IFluidHandler with IInformationProvider
	{
		// Data Synchronize //
		override def storePacketData(tag: NBTTagCompound) =
		{
			this.tank.synchronizeData foreach { tag(StoreKeys.Tank) = _ }
			tag
		}
		override def loadPacketData(tag: NBTTagCompound)
		{
			this.tank.synchronizeData = tag[NBTTagCompound](StoreKeys.Tank)
		}

		// FluidTank //
		private val tank = new Generics.FluidTank(1000)
		override def getTankInfo(from: ForgeDirection) = Array(this.tank.getInfo)
		override def canFill(from: ForgeDirection, fluid: Fluid) = this.tank canFill fluid
		override def canDrain(from: ForgeDirection, fluid: Fluid) = this.tank canDrain fluid
		override def fill(from: ForgeDirection, resource: FluidStack, perform: Boolean) =
			if(this.tank canFill resource) this.tank.fill(resource, perform) else 0
		override def drain(from: ForgeDirection, resource: FluidStack, perform: Boolean) =
			if(this.tank canDrain resource) this.tank.drain(resource.amount, perform) else null
		override def drain(from: ForgeDirection, maxDrain: Int, perform: Boolean) =
			if(this.tank.canDrain) this.tank.drain(maxDrain, perform) else null

		// Information Proivider //
		override final def provideInformation(list: java.util.List[String])
		{
			val fluidname = Option(this.tank.getFluid) map { _.getFluid } map { x => t"${x.getUnlocalizedName}" } getOrElse "None"
			val amount = this.tank.getFluidAmount
			val cap = this.tank.getCapacity

			list add s"Transporting ${fluidname}"
			list add s"Pending ${amount}/${cap} mb"
		}
		override final def forceSynchronize()
		{
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord)
		}
	}
}
