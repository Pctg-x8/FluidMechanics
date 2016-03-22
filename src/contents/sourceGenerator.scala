package com.cterm2.mcfm1710

package interfaces
{
    import net.minecraft.nbt.NBTTagCompound

    trait ISourceGenerator
    {
        def storeSpecificData(tag: NBTTagCompound)
        def loadSpecificData(tag: NBTTagCompound)
    }
}

// Source Generator Base Classes //
package sourceGenerator
{
    import net.minecraft.block.BlockContainer, net.minecraft.block.material.Material
    import cpw.mods.fml.relauncher.{SideOnly, Side}
    import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler
    import net.minecraft.client.renderer.{RenderBlocks, Tessellator}
    import net.minecraft.world.{World, IBlockAccess}

    final object CommonValues
    {
        var renderType: Int = 0
    }
    // Block //
    abstract class BlockBase extends BlockContainer(Material.iron)
    {
        setHardness(1.0f)

        // Rendering Configurations //
        @SideOnly(Side.CLIENT)
        override final val renderAsNormalBlock = false
        override final val isOpaqueCube = false
        override final val isNormalCube = false

        override final def getRenderType = CommonValues.renderType
    }

    // Block Renderer //
    final class BlockRenderer extends ISimpleBlockRenderingHandler
    {
        import com.cterm2.mcfm1710.Blocks
        import net.minecraft.block.Block
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
                def renderFaceXPos(tess: Tessellator) = { renderer.renderFaceXPos(block, 0.0d, 0.0d, 0.0d, block.getBlockTextureFromSide(2)); tess }
                @inline
                def renderFaceXNeg(tess: Tessellator) = { renderer.renderFaceXNeg(block, 0.0d, 0.0d, 0.0d, block.getBlockTextureFromSide(3)); tess }
                @inline
                def renderFaceZNeg(tess: Tessellator) = { renderer.renderFaceZNeg(block, 0.0d, 0.0d, 0.0d, block.getBlockTextureFromSide(4)); tess }
                @inline
                def renderFaceZPos(tess: Tessellator) = { renderer.renderFaceZPos(block, 0.0d, 0.0d, 0.0d, block.getBlockTextureFromSide(5)); tess }
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
            Seq(0.0d, 7.0d / 8.0d) flatMap { x => Seq(0.0d, 7.0d / 8.0d) map { z => renderPole(x, z, x + 1.0d / 8.0d, z + 1.0d / 8.0d) _ } } reduceLeft { _ andThen _ } apply tess

            glTranslatef(0.5f, 0.5f, 0.5f)
        }
        override def renderWorldBlock(world: IBlockAccess, x: Int, y: Int, z: Int, block: Block, modelId: Int, renderer: RenderBlocks) =
        {
            @inline
            def renderPole(sx: Double, sz: Double, dx: Double, dz: Double) =
            {
                renderer.renderMinX = sx; renderer.renderMaxX = dx
                renderer.renderMinZ = sz; renderer.renderMaxZ = dz
                renderer.renderFaceXPos(block, x, y, z, block.getBlockTextureFromSide(2))
                renderer.renderFaceXNeg(block, x, y, z, block.getBlockTextureFromSide(3))
                renderer.renderFaceZNeg(block, x, y, z, block.getBlockTextureFromSide(4))
                renderer.renderFaceZPos(block, x, y, z, block.getBlockTextureFromSide(5))
                renderer.renderFaceYNeg(block, x, y, z, block.getBlockTextureFromSide(0))
            }

            Tessellator.instance.setColorOpaque_F(1.0f, 1.0f, 1.0f)
            // Shrink render with RenderBlocks
            renderer.renderMinY = 0.5d; renderer.renderMaxY = 1.0d
            renderPole(0.0d, 0.0d, 1.0d, 1.0d)
            renderer.renderFaceYPos(block, x, y, z, block.getBlockTextureFromSide(1))
            renderer.renderMinY = 0.0d; renderer.renderMaxY = 0.5d
            Seq(0.0d, 7.0d / 8.0d) map { x => Seq(0.0d, 7.0d / 8.0d) foreach { z => renderPole(x, z, x + 1.0d / 8.0d, z + 1.0d / 8.0d) } }

            false
        }
        override def shouldRender3DInInventory(meta: Int) = true
        override def getRenderId = CommonValues.renderType
    }
}
