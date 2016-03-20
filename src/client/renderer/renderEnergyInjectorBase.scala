package com.cterm2.mcfm1710.client.renderer

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler
import net.minecraft.world.IBlockAccess
import net.minecraft.client.renderer.{RenderBlocks, Tessellator}
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.block.Block, net.minecraft.tileentity.TileEntity
import com.cterm2.mcfm1710.Blocks

// AttachableEnergyInjector Renderers
package attachableEnergyInjector
{
	// TileEntity Renderer
	final class TileEntityRenderer extends TileEntitySpecialRenderer
	{
		override def renderTileEntityAt(entity: TileEntity, x: Double, y: Double, z: Double, f: Float) =
		{
			
		}
	}
	// Block Renderer
	final class BlockRenderer extends ISimpleBlockRenderingHandler
	{
		override def renderInventoryBlock(block: Block, modelId: Int, meta: Int, renderer: RenderBlocks) =
		{
			import org.lwjgl.opengl.GL11._

			renderer.setRenderBoundsFromBlock(block)
			glRotatef(90.0f, 0.0f, 1.0f, 0.0f)
			glTranslatef(-0.5f, -0.5f, -0.5f)
			this.renderBlockWithNormals(block, 0.0d, 0.0d, 0.0d, renderer)
			glTranslatef(0.5f, 0.5f, 0.5f)
		}
		override def renderWorldBlock(world: IBlockAccess, x: Int, y: Int, z: Int, block: Block, modelId: Int, renderer: RenderBlocks) =
		{
			this.renderBlock(block, world.getBlockMetadata(x, y, z), x.toDouble, y.toDouble, z.toDouble, renderer)
			false
		}
		override def shouldRender3DInInventory(meta: Int) = true
		override def getRenderId = Blocks.attachableEnergyInjector.renderType

		// Render Codes //
		private def renderBlock(blk: Block, meta: Int, x: Double, y: Double, z: Double, renderer: RenderBlocks) =
		{
			import net.minecraft.init.Blocks._

			val margin = 1.0d / 16.0d
			val tex = iron_block.getBlockTextureFromSide(0)

			// shrinked render with RenderBlocks
			def renderShell() =
			{
				renderer.renderFaceYNeg(blk, x, y + 1.0d / 256.0d, z, tex)
				// renderer.renderFaceYPos(blk, x, y - 1.0d / 256.0d, z, tex)
				renderer.renderMinZ += margin; renderer.renderMaxZ -= margin
				renderer.renderFaceXNeg(blk, x, y, z, tex)
				renderer.renderFaceXPos(blk, x, y, z, tex)
				renderer.renderMinZ -= margin; renderer.renderMaxZ += margin
				renderer.renderMinX += margin; renderer.renderMaxX -= margin
				renderer.renderFaceZNeg(blk, x, y, z, tex)
				renderer.renderFaceZPos(blk, x, y, z, tex)
				renderer.renderMinX -= margin; renderer.renderMaxX += margin
				renderer.renderMinX = 0; renderer.renderMaxX = margin
				renderer.renderFaceZPos(blk, x, y, z - margin, tex)
				renderer.renderFaceZNeg(blk, x, y, z + margin, tex)
				renderer.renderMinX = 1.0d - margin; renderer.renderMaxX = 1.0d
				renderer.renderFaceZPos(blk, x, y, z - margin, tex)
				renderer.renderFaceZNeg(blk, x, y, z + margin, tex)
				renderer.renderMinX = 0.0d
				renderer.renderMinZ = 0; renderer.renderMaxZ = margin
				renderer.renderFaceXPos(blk, x - margin, y, z, tex)
				renderer.renderFaceXNeg(blk, x + margin, y, z, tex)
				renderer.renderMinZ = 1.0d - margin; renderer.renderMaxZ = 1.0d
				renderer.renderFaceXPos(blk, x - margin, y, z, tex)
				renderer.renderFaceXNeg(blk, x + margin, y, z, tex)
				renderer.renderMinZ = 0.0d
			}
			renderer.renderFromInside = true; renderShell()
			renderer.renderFromInside = false; renderShell()
			// separator
			if((meta & 0x01) == 0)
			{
				renderer.renderFaceXPos(blk, x - 0.5d + 0.5d * margin, y, z, tex)
				renderer.renderFaceXNeg(blk, x + 0.5d - 0.5d * margin, y, z, tex)
			}
			else
			{
				renderer.renderFaceZPos(blk, x, y, z - 0.5d + 0.5d * margin, tex)
				renderer.renderFaceZNeg(blk, x, y, z + 0.5d - 0.5d * margin, tex)
			}
		}
		private def renderBlockWithNormals(blk: Block, x: Double, y: Double, z: Double, renderer: RenderBlocks) =
		{
			import net.minecraft.init.Blocks._

			val margin = 1.0d / 16.0d
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
