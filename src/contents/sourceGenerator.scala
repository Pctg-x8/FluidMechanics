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
    // Block //
    abstract class BlockBase(textureNameBase: String) extends BlockContainer(Material.iron) with interfaces.ITextureIndicesProvider
    {
	    import utils.EntityLivingUtils._

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
	    @SideOnly(Side.CLIENT)
	    override final def colorMultiplier(world: IBlockAccess, x: Int, y: Int, z: Int) = 0xe0e0e0    // little darker

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
			val MetaValue(hasInjector, facing) = world.getBlockMetadata(x, y, z)
			val rp = new utils.RenderPipeline(renderer, block.asInstanceOf[Block with interfaces.ITextureIndicesProvider], x, y, z, facing)

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
            true
        }
        override def shouldRender3DInInventory(meta: Int) = true
        override def getRenderId = CommonValues.renderType
    }
}
