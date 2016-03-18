package com.cterm2.mcfm1710.client.renderer

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler
import net.minecraft.world.IBlockAccess
import net.minecraft.client.renderer.{RenderBlocks, Tessellator}
import net.minecraft.block.Block

// AttachableEnergyInjector Renderer
final class EnergyInjectorRenderer extends ISimpleBlockRenderingHandler
{
	import EnergyInjectorRenderer._

	override def renderInventoryBlock(block: Block, meta: Int, modelId: Int, renderer: RenderBlocks) =
	{

	}
	override def renderWorldBlock(world: IBlockAccess, x: Int, y: Int, z: Int, block: Block, modelId: Int, renderer: RenderBlocks) =
	{
		this.renderBlock(block, x.toDouble, y.toDouble, z.toDouble)
		false
	}
	override def shouldRender3DInInventory(meta: Int) = true
	override val getRenderId = RenderIds.EnergyInjector

	// Render Codes //
	private def renderBlock(blk: Block, x: Double, y: Double, z: Double) =
	{
		import net.minecraft.init.Blocks

		val margin = 1.0d / 16.0d
		val tess = Tessellator.instance

		val tex = Blocks.iron_block.getBlockTextureFromSide(0)
		val (u, v, u2, v2) = (tex.getMinU, tex.getMinV, tex.getMaxU, tex.getMaxV)
		val sideYZPlane = tess.addYZPlane(u, v, u2, v2) _
		val sideXYPlane = tess.addXYPlane(u, v, u2, v2) _

		// YZ
		sideYZPlane(x, y + 0.5d, z + margin)(y, z + 1.0d - margin)
		sideYZPlane(x + margin, y + 0.5d, z)(y, z + margin)
		sideYZPlane(x + margin, y + 0.5d, z + 1.0 - margin)(y, z + 1.0)
		sideYZPlane(x + 1.0d, y + 0.5d, z + 1.0d - margin)(y, z + margin)
		sideYZPlane(x + 1.0d - margin, y + 0.5d, z + 1.0d)(y, z + 1.0 - margin)
		sideYZPlane(x + 1.0d - margin, y + 0.5d, z + margin)(y, z)

		// XY
		sideXYPlane(x + margin, y + 0.5d, z)(x + 1.0d - margin, y)
		sideXYPlane(x, y + 0.5d, z + margin)(x + margin, y)
		sideXYPlane(x + 1.0 - margin, y + 0.5d, z + margin)(x + 1.0d, y)
		sideXYPlane(x + 1.0d - margin, y + 0.5d, z + 1.0d)(x + margin, y)
		sideXYPlane(x + 1.0d, y + 0.5d, z + 1.0d - margin)(x + 1.0 - margin, y)
		sideXYPlane(x + margin, y + 0.5d, z + 1.0d - margin)(x, y)
	}
}
object EnergyInjectorRenderer
{
	implicit class TesselatorHelper(val tess: Tessellator) extends AnyVal
	{
		def addYZPlane(u: Float, v: Float, u2: Float, v2: Float)(x: Double, y: Double, z: Double)(y2: Double, z2: Double) =
		{
			tess.addVertexWithUV(x, y , z , u , v )
			tess.addVertexWithUV(x, y2, z , u , v2)
			tess.addVertexWithUV(x, y2, z2, u2, v2)
			tess.addVertexWithUV(x, y , z2, u2, v )
		}
		def addXYPlane(u: Float, v: Float, u2: Float, v2: Float)(x: Double, y: Double, z: Double)(x2: Double, y2: Double) =
		{
			tess.addVertexWithUV(x2, y , z, u , v )
			tess.addVertexWithUV(x2, y2, z, u , v2)
			tess.addVertexWithUV(x , y2, z, u2, v2)
			tess.addVertexWithUV(x , y , z, u2, v )
		}
	}
}
