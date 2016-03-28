package com.cterm2.mcfm1710

import net.minecraft.nbt.NBTTagCompound
import cpw.mods.fml.relauncher.{SideOnly, Side}
import interops.smartcursor._

package interfaces
{
    trait IModuledInjector
    {
        def storeSpecificData(nbt: NBTTagCompound)
        def loadSpecificData(nbt: NBTTagCompound)
    }
}

package object EnergyInjector
{
	def register(ctab: net.minecraft.creativetab.CreativeTabs)
	{
		ContentRegistry register BlockModuled.setCreativeTab(ctab) as "energyInjector.attachable"
		ContentRegistry register BlockStandalone.setCreativeTab(ctab) as "energyInjector.standalone"
		ContentRegistry register classOf[TEModuled] as "TEEnergyInjectorModuled"
		ContentRegistry register classOf[TEStandalone] as "TEEnergyInjector"
	}
	@SideOnly(Side.CLIENT)
	def registerClient()
	{
		import cpw.mods.fml.client.registry.{RenderingRegistry, ClientRegistry}

		BlockModuled.renderType = RenderingRegistry.getNextAvailableRenderId()
		RenderingRegistry.registerBlockHandler(BlockModuled.renderType, BlockRenderer)
		ClientRegistry.bindTileEntitySpecialRenderer(classOf[TEModuled], TileEntityRenderer)
		ClientRegistry.bindTileEntitySpecialRenderer(classOf[TEStandalone], TileEntityRenderer)
	}

	lazy val ItemModuled = net.minecraft.item.Item.getItemFromBlock(BlockModuled)
}

package EnergyInjector
{
	import net.minecraft.entity.player.EntityPlayer
    import net.minecraft.block.BlockContainer, net.minecraft.block.material.Material
    import net.minecraft.world.World, net.minecraft.entity.EntityLivingBase
    import net.minecraftforge.fluids._
    import net.minecraft.tileentity.TileEntity
    import net.minecraftforge.common.util.ForgeDirection
	import net.minecraft.network.NetworkManager
    import net.minecraft.network.play.server.S35PacketUpdateTileEntity
    import net.minecraft.item.{ItemStack, ItemBlock}
	import com.cterm2.mcfm1710.utils.EntityLivingUtils._
	import cpw.mods.fml.common.Optional
	import com.cterm2.mcfm1710.Fluids
	import net.minecraft.util.IIcon
    import net.minecraft.client.renderer.texture.IIconRegister
	import utils.WorldExtensions._, utils.DeobfuscatorSupport._, utils.EntityLivingUtils._

    // Common Values
    final object EnergyInjectorSynchronizeDataKeys
    {
    	final val WaterKey = "WaterData"
    	final val EnergeticKey = "EnergyFluidsData"
    	final val BlockDirectionKey = "BlockDirection"
    }
    final object FluidLimits
    {
    	// unit: milli-buckets
    	final val Module = 4 * 1000
    	final val Standalone = 16 * 1000
    }

    // Block //
    // Energy Injector Block Base
    abstract class BlockBase extends BlockContainer(Material.iron) with interfaces.ITextureIndicesProvider
    {
        this.setHardness(2.0f)

        // Rendering Configurations //
        @SideOnly(Side.CLIENT)
        override final val renderAsNormalBlock = false
        override final val isOpaqueCube = false
	    protected final val icons = new Array[IIcon](7)
	    @SideOnly(Side.CLIENT)
	    override final def getIcon(side: Int, meta: Int) = this.icons(side)
        override def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, placer: EntityLivingBase, stack: ItemStack) =
        {
            world.setBlockMetadataWithNotify(x, y, z, (placer.facingInt + 2) & 0x03, 2)
            Option(world.getTileEntity(x, y, z).asInstanceOf[TEBase]) foreach
            {
                _.dir = convertFacingDirection((placer.facingInt + 2) & 0x03)
            }
        }
    }
    // Attachable Modification
    final object BlockModuled extends BlockBase
    {
        this.setBlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f)

        // Overrided Configurations //
        override final val isNormalCube = false
		var renderType: Int = 0
        override final def getRenderType = renderType
	    @SideOnly(Side.CLIENT)
	    override def registerBlockIcons(register: IIconRegister)
	    {
		    for((i, tex) <- 0 until 7 map { x => (x, s"mcfm1710:energyInjectorModuled$x") })
			    this.icons(i) = register.registerIcon(tex)
	    }
        override def createNewTileEntity(world: World, meta: Int) = new TEModuled
		override final def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, xo: Float, yo: Float, zo: Float) =
		{
			if(world.inServerSide)
			{
				Option(player.inventory.getCurrentItem) map { _.getItem } foreach
				{
					case cistack: ItemBlock if cistack.block.isInstanceOf[SourceGenerator.BlockBase] =>
						val entity = world.getTileEntity(x, y, z).asInstanceOf[TEModuled]
						val meta = world.getBlockMetadata(x, y, z)
						world.setBlock(x, y, z, cistack.block)
						world.setBlockMetadataWithNotify(x, y, z, SourceGenerator.MetaValue(true, meta), 2)
						Option(world.getTileEntity(x, y, z).asInstanceOf[SourceGenerator.TileEntityBase]) foreach
						{ newtile =>
							newtile.injector = entity
							newtile.updateMetaInfo()
						}
						world.notifyBlockChange(x, y, z, cistack.block)
						player.useCurrentItem()
						true
					case _ => true
				}
				true
			}
			else true
		}
    }
    // Standalone
    final object BlockStandalone extends BlockBase
    {
        // Overrided Configurations //
        var renderType: Int = 0
        override final def getRenderType = renderType
	    @SideOnly(Side.CLIENT)
	    override def registerBlockIcons(register: IIconRegister)
	    {
		    for((i, tex) <- 0 until 7 map { x => (x, s"mcfm1710:energyInjectorModuled$x") })
			    this.icons(i) = register.registerIcon(tex)
	    }
        override def createNewTileEntity(world: World, meta: Int) = new TEStandalone
    }

    // TileEntity //
    // Energy Injector Tile Entity Base
	@Optional.Interface(iface="com.cterm2.mcfm1710.interops.smartcursor.IInformationProvider", modid=SCModuleConnector.ID, striprefs=true)
    abstract class TEBase(val maxFluidAmount: Int) extends TileEntity with IFluidHandler with IInformationProvider
    {
    	tankHolder =>
    	import EnergyInjectorSynchronizeDataKeys._

    	// External Values //
    	var dir = ForgeDirection.UNKNOWN
		def facingDegree = this.dir match
		{
			case ForgeDirection.SOUTH => 0.0d
			case ForgeDirection.WEST => 90.0d
			case ForgeDirection.NORTH => 180.0d
			case ForgeDirection.EAST => -90.0d
			case _ => 0.0d
		}
		def isFacingXAxis = this.dir == ForgeDirection.WEST || this.dir == ForgeDirection.EAST

    	// Make as Combinated Fluid Tanks //
    	// Content restricted fluid tank
    	private class RestrictedFluidTank(val acceptType: Fluid) extends IFluidTank
    	{
    		// Tank Traits //
    		override def getCapacity = tankHolder.maxFluidAmount
    		override lazy val getInfo = new FluidTankInfo(this)
    		private[TEBase] def canAccept(fluid: Fluid) = fluid == this.stack.getFluid
    		private[TEBase] def canAccept(fluid: FluidStack): Boolean = this.canAccept(fluid.getFluid)

    		// Tank Exports //
    		private lazy val stack = new FluidStack(acceptType, 0)
    		override def getFluid = this.stack
    		override def getFluidAmount = this.stack.amount

    		// Tank Interacts //
    		// called when attempting to fill fluids
    		override def fill(resource: FluidStack, perform: Boolean) = resource match
    		{
    			case null => 0
    			case _ if !this.canAccept(resource) => 0
    			case _ =>
    			{
    				val newAmount = Math.min(resource.amount, this.getCapacity - this.getFluidAmount)
					if(perform && newAmount > 0)
					{
						this.stack.amount += newAmount
						tankHolder.updateTileInfo()
					}
					newAmount
    			}
    		}
    		// Called when attempting to drain fluids
    		override def drain(maxDrain: Int, perform: Boolean) =
    		{
    			val drained = Math.min(maxDrain, this.stack.amount)
    			if(perform && drained > 0)
    			{
    				this.stack.amount -= drained
    				tankHolder.updateTileInfo()
    			}
				if(drained <= 0) null else new FluidStack(this.stack, drained)
    		}

    		// Data Synchronizations //
    		private[TEBase] def synchronizeData =
    		{
    			val tag = new NBTTagCompound
    			this.stack.writeToNBT(tag)
    			tag
    		}
    		private[TEBase] def synchronizeDataFrom(tag: NBTTagCompound) =
    		{
    			val newFluid = FluidStack.loadFluidStackFromNBT(tag)
    			if(!this.canAccept(newFluid)) throw new RuntimeException("Restriction Error")
    			this.stack.amount = newFluid.amount
    		}
    	}
    	private lazy val waterTank = new RestrictedFluidTank(FluidRegistry.WATER)
    	private lazy val energeticTank = new RestrictedFluidTank(Fluids.energeticFluid)
    	private final def getTank(from: ForgeDirection) = from match
    	{
    		case ForgeDirection.EAST if dir == ForgeDirection.NORTH => Some(this.waterTank)
    		case ForgeDirection.EAST if dir == ForgeDirection.SOUTH => Some(this.energeticTank)
    		case ForgeDirection.WEST if dir == ForgeDirection.NORTH => Some(this.energeticTank)
    		case ForgeDirection.WEST if dir == ForgeDirection.SOUTH => Some(this.waterTank)
    		case ForgeDirection.NORTH if dir == ForgeDirection.WEST => Some(this.waterTank)
    		case ForgeDirection.NORTH if dir == ForgeDirection.EAST => Some(this.energeticTank)
    		case ForgeDirection.SOUTH if dir == ForgeDirection.WEST => Some(this.energeticTank)
    		case ForgeDirection.SOUTH if dir == ForgeDirection.EAST => Some(this.waterTank)
    		case _ => None
    	}
    	override final def getTankInfo(from: ForgeDirection) = Array(this.getTank(from) map { _.getInfo } getOrElse null)
    	override final def canDrain(from: ForgeDirection, fluid: Fluid) = this.getTank(from).isDefined
    	override final def canFill(from: ForgeDirection, fluid: Fluid) = this.getTank(from) map { _ canAccept fluid } getOrElse false
    	override final def fill(from: ForgeDirection, resource: FluidStack, perform: Boolean) = this.getTank(from) map
    	{
    		x => if(x canAccept resource) x.fill(resource, perform) else 0
    	} getOrElse 0
    	override final def drain(from: ForgeDirection, resource: FluidStack, perform: Boolean) = this.getTank(from) map
    	{
    		x => if(x.getFluid == resource.getFluid) x.drain(resource.amount, perform) else null
    	} getOrElse null
    	override final def drain(from: ForgeDirection, maxDrain: Int, perform: Boolean) =
    		this.getTank(from) map { _.drain(maxDrain, perform) } getOrElse null
		def getWaterTankState = this.waterTank.getFluid
		def getEnergeticTankState = this.energeticTank.getFluid

    	// Data Synchronizations //
    	final def storeSpecificDataTo(tag: NBTTagCompound) =
    	{
    		tag.setTag(WaterKey, this.waterTank.synchronizeData)
    		tag.setTag(EnergeticKey, this.energeticTank.synchronizeData)
    		tag
    	}
    	final def loadSpecificDataFrom(tag: NBTTagCompound) =
    	{
    		this.waterTank synchronizeDataFrom tag.getTag(WaterKey).asInstanceOf[NBTTagCompound]
    		this.energeticTank synchronizeDataFrom tag.getTag(EnergeticKey).asInstanceOf[NBTTagCompound]
    		this
    	}
    	override final def writeToNBT(tag: NBTTagCompound) =
    	{
    		super.writeToNBT(tag)
    		tag.setInteger(BlockDirectionKey, this.dir.ordinal)
    		this.storeSpecificDataTo(tag)
    	}
    	override final def readFromNBT(tag: NBTTagCompound) =
    	{
    		super.readFromNBT(tag)
    		this.dir = ForgeDirection.values()(tag.getInteger(BlockDirectionKey))
    		this.loadSpecificDataFrom(tag)
    	}
    	override final def getDescriptionPacket() =
    		this.storeSpecificDataTo _ andThen
    		(new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, _)) apply new NBTTagCompound
    	override final def onDataPacket(net: NetworkManager, packet: S35PacketUpdateTileEntity) =
    		((_: S35PacketUpdateTileEntity).func_148857_g()) andThen this.loadSpecificDataFrom apply packet
    	final def updateTileInfo() =
    	{
			if(this.worldObj == null) FMEntry.logger.warn("World Object for EnergyInjector.TEBase is null!")
			else
			{
	    		this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord)
	    		this.markDirty()
			}
    	}

    	// TileEntity Interacts //
    	// Called when power transmitted(Unit: RF/t)(Returns used energy)
    	final def injectFluids(amount: Int) =
    	{
    		// 1:1 = energy:water => energeticFluid
    		val newEnergeticFluid = Fluids.newEnergeticFluidStack(amount)
    		val drainable = Option(this.waterTank.drain(amount, false)) map { _.amount } getOrElse 0
    		val acceptable = this.energeticTank.fill(newEnergeticFluid, false)
    		val converted = Math.min(drainable, acceptable)
    		if(converted > 0)
    		{
    			newEnergeticFluid.amount = converted
    			this.waterTank.drain(converted, true); this.energeticTank.fill(newEnergeticFluid, true)
    			converted
    		}
    		else 0
    	}

    	// Information Provider //
    	override final def provideInformation(list: java.util.List[String]) =
    	{
    		list add s"Facing on ${this.dir}"
    		list add s"Input(Water) Tank amount: ${this.waterTank.getFluidAmount} mb"
    		list add s"Output(EnergeticFluid) Tank amount: ${this.energeticTank.getFluidAmount} mb"
    	}
		// unnecessary because this is always synchronized for TileEntityRenderer
		override final def forceSynchronize(){}
    }

    // Energy Injector Module Tile Entity
    final class TEModuled extends TEBase(FluidLimits.Module)

    // Energy Injector Tile Entity
    @Optional.Interface(iface = "mekanism.api.energy.IStrictEnergyAcceptor", modid="Mekanism", striprefs = true)
    final class TEStandalone extends TEBase(FluidLimits.Standalone)
		with mekanism.api.energy.IStrictEnergyAcceptor
    {
    	// Energy Acceptor Interacts //
    	override def transferEnergyToAcceptor(side: ForgeDirection, amount: Double) = side match
    	{
    		case ForgeDirection.UP => this.injectFluids(amount.asInstanceOf[Int]).toDouble
    		case _ => 0.0d
    	}
    	override def canReceiveEnergy(side: ForgeDirection) = side == ForgeDirection.UP

    	// Energy Storage Exports(Note: EnergyInjector does not store any energies) //
    	override val getEnergy = 0.0d
    	override def setEnergy(newValue: Double) = ()
    	override val getMaxEnergy = FluidLimits.Standalone.toDouble	// provides max acceptable energy in EnergyAcceptor
    }

	// Renderers //
	import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
	import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler

	@SideOnly(Side.CLIENT)
	object TileEntityRenderer extends TileEntitySpecialRenderer
	{
		import net.minecraft.tileentity.TileEntity

		override def renderTileEntityAt(entity: TileEntity, x: Double, y: Double, z: Double, f: Float)
		{
			entity match
			{
				case t: TEModuled => renderContent(t, x, y, z, FluidLimits.Module, 0.5f)
				case t: TEStandalone => renderContent(t, x, y, z, FluidLimits.Standalone, 1.0f)
			}
		}
		def renderContent(entity: TEBase, x: Double, y: Double, z: Double, capacity: Int, height: Float)
		{
			import org.lwjgl.opengl.GL11._, net.minecraft.client.renderer.Tessellator
			import net.minecraft.client.renderer.texture.TextureMap
			import utils.RenderPipeline

			val tess = Tessellator.instance
			this.bindTexture(TextureMap.locationBlocksTexture)
			glPushMatrix()
			glTranslated(x + 0.5d, y, z + 0.5d)
			glRotated(entity.facingDegree, 0.0d, -1.0d, 0.0d)
			glScaled(1.0f - 1.0f / 256.0f, 1.0f - 1.0f / 256.0f, 1.0f - 1.0f / 256.0f)
			glTranslated(-0.5d, 0.0d, -0.5d)
			glEnable(GL_BLEND)
			glDepthMask(false)
			glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
			tess.startDrawingQuads()
			tess.setColorRGBA_F(1.0f, 1.0f, 1.0f, 1.0f)
			// val colFactor = if(entity.isFacingXAxis) RenderPipeline.xFactor else RenderPipeline.zFactor

			if(entity.getWaterTankState.amount > 0)
			{
				val amountPercent = (entity.getWaterTankState.amount.toFloat / capacity) * height
				val icon = entity.getWaterTankState.getFluid.getStillIcon
				val (u1, v1, u2, v2) = (icon.getMinU, icon.getMinV, icon.getInterpolatedU(7.0d), icon.getMaxV)
				val (u0, v0, v4, v3) =
					(icon.getInterpolatedU(2.0d), icon.getInterpolatedV(2.0d), icon.getInterpolatedV(14.0d), icon.getInterpolatedV(amountPercent * 16.0d))
				val (margin, limit) = (1.0f / 8.0f, 0.5f - 1.0f / 16.0f)

				// top
				// tess.setColorRGBA_F(1.0f, 1.0f, 1.0f, 1.0f)
				tess.setNormal(0.0f, 1.0f, 0.0f)
				tess.addVertexWithUV(  0.0f, amountPercent,        margin, u1, v0)
				tess.addVertexWithUV(  0.0f, amountPercent, 1.0f - margin, u1, v4)
				tess.addVertexWithUV(margin, amountPercent, 1.0f - margin, u0, v4)
				tess.addVertexWithUV(margin, amountPercent,        margin, u0, v0)
				tess.addVertexWithUV(margin, amountPercent, 0.0f, u0, v1)
				tess.addVertexWithUV(margin, amountPercent, 1.0f, u0, v2)
				tess.addVertexWithUV( limit, amountPercent, 1.0f, u2, v2)
				tess.addVertexWithUV( limit, amountPercent, 0.0f, u2, v1)
				// front
				// tess.setColorRGBA_F(colFactor, colFactor, colFactor, 1.0f)
				tess.setNormal(0.0f, 0.0f, -1.0f)
				tess.addVertexWithUV(margin,          0.0f, 0.0f, u0, v1)
				tess.addVertexWithUV(margin, amountPercent, 0.0f, u0, v3)
				tess.addVertexWithUV( limit, amountPercent, 0.0f, u2, v3)
				tess.addVertexWithUV( limit,          0.0f, 0.0f, u2, v1)
				// back
				// tess.setColorRGBA_F(colFactor, colFactor, colFactor, 1.0f)
				tess.setNormal(0.0f, 0.0f, 1.0f)
				tess.addVertexWithUV( limit,          0.0f, 1.0f, u2, v1)
				tess.addVertexWithUV( limit, amountPercent, 1.0f, u2, v3)
				tess.addVertexWithUV(margin, amountPercent, 1.0f, u0, v3)
				tess.addVertexWithUV(margin,          0.0f, 1.0f, u0, v1)
			}

			tess.draw()
			glDepthMask(true)
			glDisable(GL_BLEND)
			glPopMatrix()
		}
	}
	@SideOnly(Side.CLIENT)
	object BlockRenderer extends ISimpleBlockRenderingHandler
	{
		import org.lwjgl.opengl.GL11._
		import net.minecraft.block.Block, net.minecraft.client.renderer.{RenderBlocks, Tessellator}
		import net.minecraft.world.IBlockAccess

		override def renderInventoryBlock(block: Block, modelId: Int, meta: Int, renderer: RenderBlocks) =
		{
			renderer.setRenderBoundsFromBlock(block)
			glRotatef(90.0f, 0.0f, 1.0f, 0.0f)
			glTranslatef(-0.5f, -0.5f, -0.5f)
			this.renderBlockWithNormals(block, 0.0d, 0.0d, 0.0d, renderer)
			glTranslatef(0.5f, 0.5f, 0.5f)
		}
		def renderHull(rp: utils.RenderPipeline, renderer: RenderBlocks, insideRender: Boolean)
		{
			val margin = 1.0f / 8.0f

			renderer.renderFromInside = insideRender
			// render Y Faces(under box)
			rp.render2FacesY(0.0f, 1.0f / 256.0f, margin, margin, 0.5f, 1.0f - margin)
			rp.render2FacesY(margin, 1.0f / 256.0f, 0.0f, 1.0f - margin, 0.5f, 1.0f)
			rp.render2FacesY(1.0f - margin, 1.0f / 256.0f, margin, 1.0f, 0.5f, 1.0f - margin)
			// render Z Faces
			renderer.flipTexture = insideRender
			rp.render2FacesZ(margin, 0.0f, 0.0f, 1.0f - margin, 0.5f, 1.0f)
			renderer.flipTexture = false
			rp.render2FacesZ(0.0f, 0.0f, margin, margin, 0.5f, 1.0f - margin)
			rp.render2FacesZ(1.0f - margin, 0.0f, margin, 1.0f, 0.5f, 1.0f - margin)
			// render X Faces
			renderer.flipTexture = insideRender
			rp.render2FacesX(0.0f, 0.0f, margin, 1.0f, 0.5f, 1.0f - margin)
			renderer.flipTexture = false
			rp.render2FacesX(margin, 0.0f, 0.0f, 1.0f - margin, 0.5f, margin)
			rp.render2FacesX(margin, 0.0f, 1.0f - margin, 1.0f - margin, 0.5f, 1.0f)
			renderer.renderFromInside = false
		}
		def renderSeparator(rp: utils.RenderPipeline, facing: Int)
		{
			val margin = 1.0f / 8.0f

			if((facing & 0x01) != 0)
			{
				rp.render2FacesZ(0.0f, 0.0f, 0.5f - margin * 0.5f, 1.0f, 0.5f, 0.5f + margin * 0.5f, 6)
			}
			else
			{
				rp.render2FacesX(0.5f - margin * 0.5f, 0.0f, 0.0f, 0.5f + margin * 0.5f, 0.5f, 1.0f, 6)
			}
		}
		override def renderWorldBlock(world: IBlockAccess, x: Int, y: Int, z: Int, block: Block, modelId: Int, renderer: RenderBlocks) =
		{
			val facing = world.getBlockMetadata(x, y, z) & 0x03
			val rp = new utils.RenderPipeline(renderer, block.asInstanceOf[BlockBase], x, y, z, facing)
			val margin = 1.0f / 8.0f

			renderHull(rp, renderer, false)
			renderHull(rp, renderer, true)
			renderSeparator(rp, facing)

			rp.close()
			true
		}
		override def shouldRender3DInInventory(meta: Int) = true
		override def getRenderId = BlockModuled.renderType

		private def renderBlockWithNormals(blk: Block, x: Double, y: Double, z: Double, renderer: RenderBlocks) =
		{
			import net.minecraft.init.Blocks._

			val margin = 1.0d / 8.0d
			val tex = iron_block.getBlockTextureFromSide(0)

			// shrinked render with RenderBlocks
			val tess = Tessellator.instance
			def renderShell() =
			{
				tess.startDrawingQuads(); tess.setNormal(0.0f, -1.0f, 0.0f)
				renderer.renderFaceYNeg(blk, x, y + 1.0d / 256.0d, z, tex)
				// tess.draw(); tess.startDrawingQuads(); tess.setNormal(0.0f, 1.0f, 0.0f)
				// renderer.renderFaceYPos(blk, x, y - 1.0d / 256.0d, z, tex)
				renderer.renderMinZ += margin; renderer.renderMaxZ -= margin
				tess.draw(); tess.startDrawingQuads(); tess.setNormal(-1.0f, 0.0f, 0.0f)
				renderer.renderFaceXNeg(blk, x, y, z, tex)
				tess.draw(); tess.startDrawingQuads(); tess.setNormal(1.0f, 0.0f, 0.0f)
				renderer.renderFaceXPos(blk, x, y, z, tex)
				renderer.renderMinZ -= margin; renderer.renderMaxZ += margin
				renderer.renderMinX += margin; renderer.renderMaxX -= margin
				tess.draw(); tess.startDrawingQuads(); tess.setNormal(0.0f, 0.0f, -1.0f)
				renderer.renderFaceZNeg(blk, x, y, z, tex)
				tess.draw(); tess.startDrawingQuads(); tess.setNormal(0.0f, 0.0f, 1.0f)
				renderer.renderFaceZPos(blk, x, y, z, tex)
				renderer.renderMinX -= margin; renderer.renderMaxX += margin
				renderer.renderMinX = 0; renderer.renderMaxX = margin
				tess.draw(); tess.startDrawingQuads(); tess.setNormal(0.0f, 0.0f, 1.0f)
				renderer.renderFaceZPos(blk, x, y, z - margin, tex)
				tess.draw(); tess.startDrawingQuads(); tess.setNormal(0.0f, 0.0f, -1.0f)
				renderer.renderFaceZNeg(blk, x, y, z + margin, tex)
				renderer.renderMinX = 1.0d - margin; renderer.renderMaxX = 1.0d
				tess.draw(); tess.startDrawingQuads(); tess.setNormal(0.0f, 0.0f, 1.0f)
				renderer.renderFaceZPos(blk, x, y, z - margin, tex)
				tess.draw(); tess.startDrawingQuads(); tess.setNormal(0.0f, 0.0f, -1.0f)
				renderer.renderFaceZNeg(blk, x, y, z + margin, tex)
				renderer.renderMinX = 0.0d
				renderer.renderMinZ = 0; renderer.renderMaxZ = margin
				tess.draw(); tess.startDrawingQuads(); tess.setNormal(1.0f, 0.0f, 0.0f)
				renderer.renderFaceXPos(blk, x - margin, y, z, tex)
				tess.draw(); tess.startDrawingQuads(); tess.setNormal(-1.0f, 0.0f, 0.0f)
				renderer.renderFaceXNeg(blk, x + margin, y, z, tex)
				renderer.renderMinZ = 1.0d - margin; renderer.renderMaxZ = 1.0d
				tess.draw(); tess.startDrawingQuads(); tess.setNormal(1.0f, 0.0f, 0.0f)
				renderer.renderFaceXPos(blk, x - margin, y, z, tex)
				tess.draw(); tess.startDrawingQuads(); tess.setNormal(-1.0f, 0.0f, 0.0f)
				renderer.renderFaceXNeg(blk, x + margin, y, z, tex)
				tess.draw()
				renderer.renderMinZ = 0.0d
			}
			renderer.renderFromInside = true; renderShell()
			renderer.renderFromInside = false; renderShell()
			// separator
			tess.startDrawingQuads(); tess.setNormal(1.0f, 0.0f, 0.0f)
			renderer.renderFaceXPos(blk, x - 0.5d + 0.5d * margin, y, z, tex)
			tess.draw(); tess.startDrawingQuads(); tess.setNormal(-1.0f, 0.0f, 0.0f)
			renderer.renderFaceXNeg(blk, x + 0.5d - 0.5d * margin, y, z, tex)
			tess.draw()
		}
	}
}
