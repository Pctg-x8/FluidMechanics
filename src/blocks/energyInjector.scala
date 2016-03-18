package com.cterm2.mcfm1710.blocks

import net.minecraft.block.Block, net.minecraft.block.material.Material
import cpw.mods.fml.relauncher.{SideOnly, Side}

// Attachable Energy Injector
final class BlockAttachableEnergyInjector extends Block(Material.iron)
{
	this.setHardness(2.0f)
	this.applyBlockBoundsAsHalf()

	// Rendering Configurations //
	@SideOnly(Side.CLIENT)
	override val renderAsNormalBlock = false
	override val isOpaqueCube = false
	override val isNormalCube = false
	private def applyBlockBoundsAsHalf() = this.setBlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f)
}
