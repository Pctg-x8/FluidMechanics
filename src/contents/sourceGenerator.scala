package com.cterm2.mcfm1710

import cpw.mods.fml.relauncher.{SideOnly, Side}

package object SourceGenerator
{
	@SideOnly(Side.CLIENT)
	def registerClient()
	{
		import cpw.mods.fml.client.registry.{RenderingRegistry, ClientRegistry}

		CommonValues.renderType = RenderingRegistry.getNextAvailableRenderId
		RenderingRegistry.registerBlockHandler(CommonValues.renderType, BlockRenderer)
		ClientRegistry.bindTileEntitySpecialRenderer(classOf[TileEntityBase], TileEntityRenderer)
	}

	// BreakBlock supressor
	var replacing = false
}

// Source Generator Base Classes //
package SourceGenerator
{
    import net.minecraft.block.BlockContainer
    import net.minecraft.block.material.Material
    import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler
    import net.minecraft.client.renderer.EntityRenderer
    import net.minecraft.client.renderer.texture.IIconRegister
    import net.minecraft.entity.EntityLivingBase
    import net.minecraft.item.ItemStack
    import net.minecraft.util.{EnumFacing, IIcon}
    import net.minecraft.world.{IBlockAccess, World}

	object MetaValueUtils
	{
		def hasInfuser(meta: Int) = (meta & MetaValue.HasInfuserBit) != 0
		def facing(meta: Int) = (meta & MetaValue.FacingBitsMask) >> 1
	}
	object MetaValue
	{
		final val HasInfuserBit = 0x01
		final val FacingBitsMask = 0x03 << 1

		implicit class BooleanConverter(val b: Boolean) extends AnyVal
		{
			def toInt() = if(b) 1 else 0
		}

		def apply(hasInfuser: Boolean, facing: Int) = hasInfuser.toInt | ((facing << 1) & FacingBitsMask)
		def unapply(meta: Int) = Option((MetaValueUtils.hasInfuser(meta), MetaValueUtils.facing(meta)))
	}
	object CommonValues
    {
        var renderType: Int = 0
    }
	object StoreKeys
	{
		final val FrontDirection = "front"
		final val Injector = "injector"
	}
    // Block //
    abstract class BlockBase(textureNameBase: String) extends BlockContainer(Material.iron) with interfaces.ITextureIndicesProvider
    {
	    import utils.EntityLivingUtils._, utils.WorldExtensions._, net.minecraft.entity.player.EntityPlayer

        setHardness(1.0f)

        // Rendering Configurations //
        @SideOnly(Side.CLIENT)
        override final val renderAsNormalBlock = false
        override final val isOpaqueCube = false
        override final val isNormalCube = false
	    protected final val icons = new Array[IIcon](6)
	    @SideOnly(Side.CLIENT)
	    override def registerBlockIcons(register: IIconRegister)
	    {
		    for((i, tex) <- 0 until 6 map { x => (x, s"$textureNameBase$x") })
			    this.icons(i) = register.registerIcon(tex)
	    }
	    @SideOnly(Side.CLIENT)
	    override final def getIcon(side: Int, meta: Int) = this.icons(side)

        override final def getRenderType = CommonValues.renderType

	    // Block Interacts //
	    override final def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, placer: EntityLivingBase, stack: ItemStack)
	    {
		    world.setBlockMetadataWithNotify(x, y, z, MetaValue(false, (placer.facingInt + 2) & 0x03), 3)
			Option(world.getTileEntity(x, y, z).asInstanceOf[TileEntityBase]) foreach { _.updateMetaInfo() }
	    }
		override final def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, xo: Float, yo: Float, zo: Float) =
		{
			if(!world.inClientSide)
			{
				val MetaValue(hasInfuser, facing) = world.getBlockMetadata(x, y, z)
				if(!hasInfuser && (Option(player.inventory.getCurrentItem) exists { _.getItem == EnergyInjector.ItemModuled }))
				{
					// Server Side & Activated with Attachable Energy Injector & No infuser has attached
					world.setBlockMetadataWithNotify(x, y, z, MetaValue(true, facing), 2)
					Option(world.getTileEntity(x, y, z).asInstanceOf[TileEntityBase]) foreach { _.updateMetaInfo() }
					player.useCurrentItem()
					world.notifyBlockChange(x, y, z, this)
					true
				}
				else
				{
					openContainerScreen(player, world, x, y, z)
					true
				}
			}
			else true
		}
	    override final def breakBlock(world: World, x: Int, y: Int, z: Int, block: net.minecraft.block.Block, meta: Int)
		{
			import utils.EntityExtensions._

			if(!SourceGenerator.replacing)
			{
				val MetaValue(hasInfuser, _) = meta
				if(hasInfuser) new ItemStack(EnergyInjector.BlockModuled, 1).dropAsEntityRandom(world, x, y, z)
				this.breakContainer(world, x, y, z, meta)
			}
		}

		// Custom Interacts //
		def openContainerScreen(player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Unit
		def breakContainer(world: World, x: Int, y: Int, z: Int, meta: Int): Unit
    }

	// TileEntity Commons //
	abstract class TileEntityBase extends base.NetworkTileEntity
	{
		import net.minecraftforge.common.util.ForgeDirection

		// Internal Injector Entity //
		private var _injector: Option[EnergyInjector.TEModuled] = None
		def injector = this._injector
		def injector_=(te: EnergyInjector.TEModuled)
		{
			this._injector = Option(te)
		}

		// Meta Settings //
		def updateMetaInfo()
		{
			val MetaValue(hasInfuser, facing) = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord)
			if(hasInfuser && this.injector.isEmpty) this.attachNewInjector()
			else if(!hasInfuser && this.injector.isDefined) this.detachInjector()
			this.facing = facing
		}

		// Facing Settings //
		private var _frontDirection = ForgeDirection.UNKNOWN
		private var facingInt = 0
		def frontDirection = this._frontDirection
		def facing_=(facing: Int)
		{
			this.facingInt = facing
			this._frontDirection = facing match
			{
				case 0 => ForgeDirection.SOUTH
				case 1 => ForgeDirection.WEST
				case 2 => ForgeDirection.NORTH
				case 3 => ForgeDirection.EAST
			}
			this.injector foreach { _.dir = this.frontDirection }
		}
		def facing = this.facingInt
		private def attachNewInjector()
		{
			this._injector = Some(new EnergyInjector.TEModuled)
			this.injector foreach
			{ x =>
				x.dir = this.frontDirection
				// share objects
				x.setWorldObj(this.worldObj)
				x.xCoord = this.xCoord
				x.yCoord = this.yCoord
				x.zCoord = this.zCoord
			}
			// Update Block for Re-rendering
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord)
		}
		private def detachInjector()
		{
			this._injector = None
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord)
		}

		// Data Synchronizing //
		import net.minecraft.nbt.NBTTagCompound, utils.ActiveNBTRecord._
        override def storePacketData(tag: NBTTagCompound) =
		{
			tag(StoreKeys.FrontDirection) = this.facingInt.asInstanceOf[Byte]
			this.injector map { _.storeSpecificDataTo(new NBTTagCompound) } foreach { tag(StoreKeys.Injector) = _ }
			tag
		}
        override def loadPacketData(tag: NBTTagCompound)
		{
			this.facing = tag[Byte](StoreKeys.FrontDirection) map { _.toInt } getOrElse 0
			this._injector = tag[NBTTagCompound](StoreKeys.Injector) map
			{ t =>
				val te = new EnergyInjector.TEModuled
				te.loadSpecificDataFrom(t)
				// share objects
				te.setWorldObj(this.worldObj)
				te.xCoord = this.xCoord
				te.yCoord = this.yCoord
				te.zCoord = this.zCoord
				te.dir = this.frontDirection
				te
			}
		}

		override def validate()
		{
			super.validate()
			this.injector foreach
			{ t =>
				t.validate()
				// share objects
				t.setWorldObj(this.worldObj)
				t.xCoord = this.xCoord
				t.yCoord = this.yCoord
				t.zCoord = this.zCoord
				t.dir = this.frontDirection
			}
		}
	}

	// TileEntity Renderer //
	import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
	@SideOnly(Side.CLIENT)
	object TileEntityRenderer extends TileEntitySpecialRenderer
	{
		import net.minecraft.tileentity.TileEntity

		override def renderTileEntityAt(entity: TileEntity, x: Double, y: Double, z: Double, f: Float)
		{
			entity match
			{
				case t: TileEntityBase => t.injector foreach
				{
					EnergyInjector.TileEntityRenderer.renderContent(_, x, y, z, EnergyInjector.FluidLimits.Module, 0.5f)
				}
			}
		}
	}

    // Block Renderer //
	@SideOnly(Side.CLIENT)
    object BlockRenderer extends ISimpleBlockRenderingHandler
    {
        import net.minecraft.block.Block, net.minecraft.world.IBlockAccess
        import net.minecraft.client.renderer.{RenderBlocks, Tessellator}

        override def renderInventoryBlock(block: Block, modelId: Int, meta: Int, renderer: RenderBlocks) =
        {
            import org.lwjgl.opengl.GL11._

            val tess = Tessellator.instance
            renderer.setRenderBoundsFromBlock(block)
            glRotatef(90.0f, 0.0f, 1.0f, 0.0f)
            glTranslatef(-0.5f, -0.5f, -0.5f)

            // for combining
            @inline
            def dispatch(tess: Tessellator) = { tess.draw(); tess }
            @inline
            def startDrawingQuadsWithNormal(x: Float, y: Float, z: Float)(tess: Tessellator) =
            {
                tess.startDrawingQuads(); tess.setNormal(x, y, z); tess
            }
            @inline
            def renderFaceYPos(tess: Tessellator) = { renderer.renderFaceYPos(block, 0.0d, 0.0d, 0.0d, block.getBlockTextureFromSide(1)); tess }
            @inline
            def renderPole(sx: Double, sz: Double, dx: Double, dz: Double)(tess: Tessellator) =
            {
                renderer.renderMinX = sx; renderer.renderMaxX = dx
                renderer.renderMinZ = sz; renderer.renderMaxZ = dz

                @inline
                def renderFaceXPos(tess: Tessellator) = { renderer.renderFaceXPos(block, 0.0d, 0.0d, 0.0d, block.getBlockTextureFromSide(4)); tess }
                @inline
                def renderFaceXNeg(tess: Tessellator) = { renderer.renderFaceXNeg(block, 0.0d, 0.0d, 0.0d, block.getBlockTextureFromSide(5)); tess }
                @inline
                def renderFaceZNeg(tess: Tessellator) = { renderer.renderFaceZNeg(block, 0.0d, 0.0d, 0.0d, block.getBlockTextureFromSide(2)); tess }
                @inline
                def renderFaceZPos(tess: Tessellator) = { renderer.renderFaceZPos(block, 0.0d, 0.0d, 0.0d, block.getBlockTextureFromSide(3)); tess }
                @inline
                def renderFaceYNeg(tess: Tessellator) = { renderer.renderFaceYNeg(block, 0.0d, 0.0d, 0.0d, block.getBlockTextureFromSide(0)); tess }

                startDrawingQuadsWithNormal(1.0f, 0.0f, 0.0f) _ andThen renderFaceXPos andThen dispatch andThen
                startDrawingQuadsWithNormal(-1.0f, 0.0f, 0.0f) andThen renderFaceXNeg andThen dispatch andThen
                startDrawingQuadsWithNormal(0.0f, 0.0f, -1.0f) andThen renderFaceZNeg andThen dispatch andThen
                startDrawingQuadsWithNormal(0.0f, 0.0f,  1.0f) andThen renderFaceZPos andThen dispatch andThen
                startDrawingQuadsWithNormal(0.0f, -1.0f, 0.0f) andThen renderFaceYNeg andThen dispatch apply tess
            }

            // Shrink render with RenderBlocks
            renderer.renderMinY = 0.5d; renderer.renderMaxY = 1.0d
            renderPole(0.0d, 0.0d, 1.0d, 1.0d) _ andThen startDrawingQuadsWithNormal(0.0f, 1.0f, 0.0f) andThen renderFaceYPos andThen dispatch apply tess
            // Render under 4 poles
            renderer.renderMinY = 0.0d; renderer.renderMaxY = 0.5d
			(for
			{
				x <- Seq(0.0d, 7.0d / 8.0d)
				z <- Seq(0.0d, 7.0d / 8.0d)
			} yield renderPole(x, z, x + 1.0d / 8.0d, z + 1.0d / 8.0d) _) reduceLeft { _ andThen _ } apply tess

            glTranslatef(0.5f, 0.5f, 0.5f)
        }
        override def renderWorldBlock(world: IBlockAccess, x: Int, y: Int, z: Int, block: Block, modelId: Int, renderer: RenderBlocks) =
        {
			val MetaValue(hasInjector, facing) = world.getBlockMetadata(x, y, z)
			val rp = new utils.RenderPipeline(renderer, block.asInstanceOf[Block with interfaces.ITextureIndicesProvider], x, y, z, facing)

			if(hasInjector)
			{
				// Full body
				rp.renderFaceYPos(0.0f, 0.5f, 0.0f, 1.0f, 1.0f, 1.0f)
				rp.render4Faces(0.0f, 0.5f, 0.0f, 1.0f, 1.0f, 1.0f)
				val rp2 = new utils.RenderPipeline(renderer, EnergyInjector.BlockModuled, x, y, z, facing)
				rp2.render4Faces(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f)
				rp2.renderFaceYNeg(0.0f, 1.0f / 256.0f, 0.0f, 1.0f, 0.5f, 1.0f)
				EnergyInjector.BlockRenderer.renderHull(rp2, renderer, true)
				EnergyInjector.BlockRenderer.renderSeparator(rp2, facing)
				rp2.close()
			}
			else
			{
			    // render -Y Faces(Pole under and body under)
			    rp.renderFaceYNeg(0.0f, 0.5f, 0.0f, 1.0f, 1.0f, 1.0f)
			    for(xo <- Seq(0.0f, 7.0f / 8.0f); zo <- Seq(0.0f, 7.0f / 8.0f))
				    rp.renderFaceYNeg(xo, 0.0f, zo, xo + 1.0f / 8.0f, 0.5f, zo + 1.0f / 8.0f)
			    // render +Y Faces(body top)
			    rp.renderFaceYPos(0.0f, 0.5f, 0.0f, 1.0f, 1.0f, 1.0f)
			    // render 4 Faces(body and pole back)
			    rp.render4Faces(0.0f, 0.5f, 0.0f, 1.0f, 1.0f, 1.0f)
			    for(xo <- Seq(0.0f, 7.0f / 8.0f); zo <- Seq(0.0f, 7.0f / 8.0f))
				    rp.render4Faces(xo, 0.0f, zo, xo + 1.0f / 8.0f, 0.5f, zo + 1.0f / 8.0f)
			}
			rp.close()
            true
        }
        override def shouldRender3DInInventory(meta: Int) = true
        override def getRenderId = CommonValues.renderType
    }
}
