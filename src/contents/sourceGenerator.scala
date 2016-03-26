package com.cterm2.mcfm1710

import cpw.mods.fml.relauncher.{SideOnly, Side}

package interfaces
{
    import net.minecraft.nbt.NBTTagCompound

    trait ISourceGenerator
    {
        def storeSpecificDataTo(tag: NBTTagCompound): NBTTagCompound
        def loadSpecificDataFrom(tag: NBTTagCompound): NBTTagCompound
    }
}

package object SourceGenerator
{
	@SideOnly(Side.CLIENT)
	def registerClient()
	{
		import cpw.mods.fml.client.registry.RenderingRegistry

		CommonValues.renderType = RenderingRegistry.getNextAvailableRenderId
		RenderingRegistry.registerBlockHandler(CommonValues.renderType, BlockRenderer)
	}
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
	object TextureIndices
	{
		final val Bottom	= 0
		final val Top		= 1
		final val Back		= 2
		final val Front		= 3
		final val Right		= 4
		final val Left		= 5
	}
    // Block //
    abstract class BlockBase(textureNameBase: String) extends BlockContainer(Material.iron)
    {
	    import utils.EntityLivingUtils._

        setHardness(1.0f)

        // Rendering Configurations //
        @SideOnly(Side.CLIENT)
        override final val renderAsNormalBlock = false
        override final val isOpaqueCube = false
        override final val isNormalCube = false
	    // Rendering Configurations //
	    protected final val icons = new Array[IIcon](6)
	    @SideOnly(Side.CLIENT)
	    override def registerBlockIcons(register: IIconRegister)
	    {
		    for((i, tex) <- 0 until 6 map { x => (x, s"$textureNameBase$x") })
			    this.icons(i) = register.registerIcon(tex)
	    }
	    @SideOnly(Side.CLIENT)
	    override final def getIcon(side: Int, meta: Int) = this.icons(side)
	    @SideOnly(Side.CLIENT)
	    override final def colorMultiplier(world: IBlockAccess, x: Int, y: Int, z: Int) = 0xe0e0e0    // little darker
	    final def getIndices(facing: Int) = facing match
	    {
			// -z, +z, -x, +x
		    case 0 => (TextureIndices.Back, TextureIndices.Front, TextureIndices.Right, TextureIndices.Left)
			case 1 => (TextureIndices.Right, TextureIndices.Left, TextureIndices.Front, TextureIndices.Back)
			case 2 => (TextureIndices.Front, TextureIndices.Back, TextureIndices.Left, TextureIndices.Right)
			case 3 => (TextureIndices.Left, TextureIndices.Right, TextureIndices.Back, TextureIndices.Front)
	    }

        override final def getRenderType = CommonValues.renderType

	    // Block Interacts //
	    override final def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, placer: EntityLivingBase, stack: ItemStack)
	    {
		    world.setBlockMetadataWithNotify(x, y, z, MetaValue(false, (placer.facingInt + 2) & 0x03), 3)
			Option(world.getTileEntity(x, y, z).asInstanceOf[TileEntityBase]) foreach { _.setFacing((placer.facingInt + 2) & 0x03) }
	    }
    }

	// TileEntity Commons //
	abstract class TileEntityBase extends net.minecraft.tileentity.TileEntity
	{
		import net.minecraftforge.common.util.ForgeDirection
		
		// Facing Settings //
		protected var frontDirection = ForgeDirection.UNKNOWN
		def setFacing(facing: Int)
		{
			this.frontDirection = facing match
			{
				case 0 => ForgeDirection.SOUTH
				case 1 => ForgeDirection.WEST
				case 2 => ForgeDirection.NORTH
				case 3 => ForgeDirection.EAST
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
            val l = block.colorMultiplier(world, x, y, z)
            val (r, g, b) = applyAnaglyph(((l >> 16) & 0xff) / 255.0f, ((l >> 8) & 0xff) / 255.0f, (l & 0xff) / 255.0f)

	        this.renderWithColorMultiplier(renderer, block, x.toFloat, y.toFloat, z.toFloat, r, g, b)
            true
        }
        override def shouldRender3DInInventory(meta: Int) = true
        override def getRenderId = CommonValues.renderType

	    private class RenderPipeline(val renderer: RenderBlocks, val block: Block, val x: Float, val y: Float, val z: Float, val r: Float, val g: Float, val b: Float)
	    {
		    final val (world, tess) = (renderer.blockAccess, Tessellator.instance)
			final val (xi, yi, zi) = (x.toInt, y.toInt, z.toInt)
			final val MetaValue(hasInjector, facing) = world.getBlockMetadata(xi, yi, zi)

			renderer.uvRotateTop = Array(0, 1, 3, 2)(facing); renderer.uvRotateBottom = Array(0, 1, 3, 2)(facing)
			final val (backIndex, frontIndex, rightIndex, leftIndex) = block.asInstanceOf[BlockBase].getIndices(facing)

		    // brightness values
		    lazy val brightness = block.getMixedBrightnessForBlock(world, xi, yi, zi)
		    lazy val underBrightness = block.getMixedBrightnessForBlock(world, xi, yi - 1, zi)
		    lazy val topBrightness = block.getMixedBrightnessForBlock(world, xi, yi + 1, zi)
		    lazy val backBrightness = block.getMixedBrightnessForBlock(world, xi, yi, zi - 1)
		    lazy val frontBrightness = block.getMixedBrightnessForBlock(world, xi, yi, zi + 1)
		    lazy val rightBrightness = block.getMixedBrightnessForBlock(world, xi - 1, yi, zi)
		    lazy val leftBrightness = block.getMixedBrightnessForBlock(world, xi + 1, yi, zi)

		    // block icons
		    lazy val underIcon = renderer.getBlockIcon(block, world, xi, yi, zi, 0)
		    lazy val topIcon = renderer.getBlockIcon(block, world, xi, yi, zi, 1)
		    lazy val backIcon = renderer.getBlockIcon(block, world, xi, yi, zi, backIndex)
		    lazy val frontIcon = renderer.getBlockIcon(block, world, xi, yi, zi, frontIndex)
		    lazy val rightIcon = renderer.getBlockIcon(block, world, xi, yi, zi, rightIndex)
		    lazy val leftIcon = renderer.getBlockIcon(block, world, xi, yi, zi, leftIndex)

		    // side properties
		    lazy val underRendered = renderer.renderAllFaces || block.shouldSideBeRendered(world, xi, yi - 1, zi, 0)
		    lazy val topRendered = renderer.renderAllFaces || block.shouldSideBeRendered(world, xi, yi + 1, zi, 1)
		    lazy val backRendered = renderer.renderAllFaces || block.shouldSideBeRendered(world, xi, yi, zi - 1, backIndex)
		    lazy val frontRendered = renderer.renderAllFaces || block.shouldSideBeRendered(world, xi, yi, zi + 1, frontIndex)
		    lazy val rightRendered = renderer.renderAllFaces || block.shouldSideBeRendered(world, xi - 1, yi, zi, rightIndex)
		    lazy val leftRendered = renderer.renderAllFaces || block.shouldSideBeRendered(world, xi + 1, yi, zi, leftIndex)

		    // shading const
		    final val (underFactor, xFactor, zFactor) = (0.5f, 0.6f, 0.8f)

		    // render -Y Face
		    @inline
		    def renderFaceYNeg(sx: Float, sy: Float, sz: Float, dx: Float, dy: Float, dz: Float)
		    {
			    if(sy > 0.0d || underRendered)
			    {
				    renderer.setRenderBounds(sx, sy, sz, dx, dy, dz)
				    tess.setBrightness(if(renderer.renderMinY > 0.0d) brightness else underBrightness)
				    tess.setColorOpaque_F(r * underFactor, g * underFactor, b * underFactor)
				    renderer.renderFaceYNeg(block, x, y, z, underIcon)
			    }
		    }
		    // render +Y Face
		    @inline
		    def renderFaceYPos(sx: Float, sy: Float, sz: Float, dx: Float, dy: Float, dz: Float)
		    {
			    if(dy < 1.0d || topRendered)
			    {
				    renderer.setRenderBounds(sx, sy, sz, dx, dy, dz)
				    tess.setBrightness(if(renderer.renderMaxY < 1.0d) brightness else topBrightness)
				    tess.setColorOpaque_F(r, g, b)
				    renderer.renderFaceYPos(block, x, y, z, topIcon)
			    }
		    }
		    // render 4 faces
		    @inline
		    def render4Faces(sx: Float, sy: Float, sz: Float, dx: Float, dy: Float, dz: Float)
		    {
			    renderer.setRenderBounds(sx, sy, sz, dx, dy, dz)

			    // z
			    tess.setColorOpaque_F(r * zFactor, g * zFactor, b * zFactor)
			    if(renderer.renderMinZ > 0.0d || backRendered)
			    {
				    // neg
				    tess.setBrightness(if(renderer.renderMinZ > 0.0d) brightness else backBrightness)
				    renderer.renderFaceZNeg(block, x, y, z, backIcon)
			    }
			    if(renderer.renderMaxZ < 1.0d || frontRendered)
			    {
				    // pos
				    tess.setBrightness(if(renderer.renderMaxZ < 1.0d) brightness else frontBrightness)
				    renderer.renderFaceZPos(block, x, y, z, frontIcon)
			    }
			    // x
			    tess.setColorOpaque_F(r * xFactor, g * xFactor, b * xFactor)
			    if(renderer.renderMinX > 0.0d || rightRendered)
			    {
				    // neg
				    tess.setBrightness(if(renderer.renderMinX > 0.0d) brightness else rightBrightness)
				    renderer.renderFaceXNeg(block, x, y, z, rightIcon)
			    }
			    if(renderer.renderMaxX < 1.0d || leftRendered)
			    {
				    // pos
				    tess.setBrightness(if(renderer.renderMaxX < 1.0d) brightness else leftBrightness)
				    renderer.renderFaceXPos(block, x, y, z, leftIcon)
			    }
		    }

			def close()
			{
				renderer.uvRotateTop = 0; renderer.uvRotateBottom = 0
			}
	    }

	    private def renderWithColorMultiplier(renderer: RenderBlocks, block: Block, x: Float, y: Float, z: Float, r: Float, g: Float, b: Float)
	    {
		    val rp = new RenderPipeline(renderer, block, x, y, z, r, g, b)

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
			rp.close()
	    }

	    private def applyAnaglyph(r: Float, g: Float, b: Float) = if(!EntityRenderer.anaglyphEnable) (r, g, b) else
		    ((r * 30.0f + g * 59.0f + b * 11.0f) / 100.0f, (r * 30.0f + g * 70.0f) / 100.0f, (r * 30.0f + b * 70.0f) / 100.0f)
    }
}
