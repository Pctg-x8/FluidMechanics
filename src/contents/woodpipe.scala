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
			val (leftConnected, rightConnected, topConnected, bottomConnected, frontConnected, backConnected) =
				(Option(world.getTileEntity(x + 1, y, z)) exists { _.isInstanceOf[IFluidHandler] },
				Option(world.getTileEntity(x - 1, y, z)) exists { _.isInstanceOf[IFluidHandler] },
				Option(world.getTileEntity(x, y + 1, z)) exists { _.isInstanceOf[IFluidHandler] },
				Option(world.getTileEntity(x, y - 1, z)) exists { _.isInstanceOf[IFluidHandler] },
				Option(world.getTileEntity(x, y, z + 1)) exists { _.isInstanceOf[IFluidHandler] },
				Option(world.getTileEntity(x, y, z - 1)) exists { _.isInstanceOf[IFluidHandler] })
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
	@Optional.Interface(iface="com.cterm2.mcfm1710.interops.smartcursor.IInformationProvider", modid=SCModuleConnector.ID, striprefs = true)
	class TEPipe extends TileEntity with IInformationProvider with IFluidHandler
	{
		// Fluid Handler //
		private val internal = new Generics.FluidTank(1000)
		override def getTankInfo(from: ForgeDirection) = Array(internal.getInfo)
		override def canDrain(from: ForgeDirection, fluid: Fluid) = internal canDrain fluid
		override def canFill(from: ForgeDirection, fluid: Fluid) = internal canFill fluid
		override def drain(from: ForgeDirection, maxDrain: Int, perform: Boolean) =
			internal.drain(maxDrain, perform)
		override def drain(from: ForgeDirection, resource: FluidStack, perform: Boolean) =
			if(Option(resource) exists { internal canDrain _ }) internal.drain(resource.amount, perform) else null
		override def fill(from: ForgeDirection, resource: FluidStack, perform: Boolean) =
			internal.fill(resource, perform)

		// Information Provider //
		override def provideInformation(list: java.util.List[String])
		{
			list.add(s"Pipe Internal: ${this.internal.getFluidAmount} mb")
		}
		override def forceSynchronize() = ()
	}
}
