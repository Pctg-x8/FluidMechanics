package com.cterm2.mcfm1710

object TextureIndices
{
	final val Bottom	= 0
	final val Top		= 1
	final val Back		= 2
	final val Front		= 3
	final val Right		= 4
	final val Left		= 5
}
package interfaces
{
	trait ITextureIndicesProvider
	{
		def getIndices(facing: Int) = facing match
		{
			// -z, +z, -x, +x
			case 0 => (TextureIndices.Back, TextureIndices.Front, TextureIndices.Right, TextureIndices.Left)
			case 1 => (TextureIndices.Right, TextureIndices.Left, TextureIndices.Front, TextureIndices.Back)
			case 2 => (TextureIndices.Front, TextureIndices.Back, TextureIndices.Left, TextureIndices.Right)
			case 3 => (TextureIndices.Left, TextureIndices.Right, TextureIndices.Back, TextureIndices.Front)
		}
	}
}

import net.minecraft.client.renderer.{RenderBlocks, Tessellator, EntityRenderer}
import net.minecraft.block.Block
import interfaces.ITextureIndicesProvider

package utils
{
	object RenderPipeline
	{
		// Shading Const
		final val (underFactor, xFactor, zFactor) = (0.5f, 0.6f, 0.8f)
	}
	class RenderPipeline(val renderer: RenderBlocks, val block: Block with ITextureIndicesProvider, val x: Float, val y: Float, val z: Float, val facing: Int)
	{
		import RenderPipeline._

		final val (world, tess) = (renderer.blockAccess, Tessellator.instance)
		final val (xi, yi, zi) = (x.toInt, y.toInt, z.toInt)

		final val (r, g, b) =
		{
			val l = block.colorMultiplier(world, xi, yi, zi)
			val (rt, gt, bt) = (((l >> 16) & 0xff) / 255.0f, ((l >> 8) & 0xff) / 255.0f, (l & 0xff) / 255.0f)
			if(EntityRenderer.anaglyphEnable)
			{
				val ra = (rt * 30.0f + gt * 59.0f + bt * 11.0f) / 100.0f
				val ga = (rt * 30.0f + gt * 70.0f) / 100.0f
				val ba = (rt * 30.0f + bt * 70.0f) / 100.0f
				(ra, ga, ba)
			}
			else (rt, gt, bt)
		}

		renderer.uvRotateTop = Array(0, 1, 3, 2)(facing); renderer.uvRotateBottom = Array(0, 1, 3, 2)(facing)
		final val (backIndex, frontIndex, rightIndex, leftIndex) = block.getIndices(facing)

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
		// render 2 faces (z)
		@inline
		def render2FacesZ(sx: Float, sy: Float, sz: Float, dx: Float, dy: Float, dz: Float, texIndex: Int = -1)
		{
			renderer.setRenderBounds(sx, sy, sz, dx, dy, dz)

			val tex = if(texIndex >= 0) Some(renderer.getBlockIcon(block, world, xi, yi, zi, texIndex)) else None

			tess.setColorOpaque_F(r * zFactor, g * zFactor, b * zFactor)
			if(renderer.renderMinZ > 0.0d || backRendered)
			{
				tess.setBrightness(if(renderer.renderMinZ > 0.0d) brightness else backBrightness)
				renderer.renderFaceZNeg(block, x, y, z, tex getOrElse backIcon)
			}
			if(renderer.renderMaxZ < 1.0d || frontRendered)
			{
				// pos
				tess.setBrightness(if(renderer.renderMaxZ < 1.0d) brightness else frontBrightness)
				renderer.renderFaceZPos(block, x, y, z, tex getOrElse frontIcon)
			}
		}
		// x
		@inline
		def render2FacesX(sx: Float, sy: Float, sz: Float, dx: Float, dy: Float, dz: Float, texIndex: Int = -1)
		{
			renderer.setRenderBounds(sx, sy, sz, dx, dy, dz)

			val tex = if(texIndex >= 0) Some(renderer.getBlockIcon(block, world, xi, yi, zi, texIndex)) else None

			// x
			tess.setColorOpaque_F(r * xFactor, g * xFactor, b * xFactor)
			if(renderer.renderMinX > 0.0d || rightRendered)
			{
				// neg
				tess.setBrightness(if(renderer.renderMinX > 0.0d) brightness else rightBrightness)
				renderer.renderFaceXNeg(block, x, y, z, tex getOrElse rightIcon)
			}
			if(renderer.renderMaxX < 1.0d || leftRendered)
			{
				// pos
				tess.setBrightness(if(renderer.renderMaxX < 1.0d) brightness else leftBrightness)
				renderer.renderFaceXPos(block, x, y, z, tex getOrElse leftIcon)
			}
		}
		// y
		@inline
		def render2FacesY(sx: Float, sy: Float, sz: Float, dx: Float, dy: Float, dz: Float, texIndex: Int = -1)
		{
			renderer.setRenderBounds(sx, sy, sz, dx, dy, dz)
			val tex = if(texIndex >= 0) Some(renderer.getBlockIcon(block, world, xi, yi, zi, texIndex)) else None

			// under
			if(renderer.renderMinY > 0.0d || underRendered)
			{
				tess.setBrightness(if(renderer.renderMinY > 0.0d) brightness else underBrightness)
				tess.setColorOpaque_F(r * underFactor, g * underFactor, b * underFactor)
				renderer.renderFaceYNeg(block, x, y, z, tex getOrElse underIcon)
			}
			// top
			if(renderer.renderMaxY > 1.0d || topRendered)
			{
				tess.setBrightness(if(renderer.renderMaxY < 1.0d) brightness else topBrightness)
				tess.setColorOpaque_F(r, g, b)
				renderer.renderFaceYPos(block, x, y, z, tex getOrElse topIcon)
			}
		}

		def close()
		{
			renderer.uvRotateTop = 0; renderer.uvRotateBottom = 0
		}
	}
}
