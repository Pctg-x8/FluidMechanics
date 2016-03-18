package com.cterm2.mcfm1710.blocks

import net.minecraft.block.BlockContainer, net.minecraft.block.material.Material
import cpw.mods.fml.relauncher.{SideOnly, Side}
import com.cterm2.mcfm1710.tiles.TEEnergyInjector
import net.minecraft.world.World
import com.cterm2.mcfm1710.client.renderer.RenderIds

object EnergyInjectorFluidLimits
{
	// unit: milli-buckets
	final val Attachable = 4 * 1000
	final val Standalone = 16 * 1000
}
// Attachable Energy Injector
final class BlockAttachableEnergyInjector extends BlockContainer(Material.iron)
{
	this.setHardness(2.0f)
	this.applyBlockBoundsAsHalf()

	// Rendering Configurations //
	@SideOnly(Side.CLIENT)
	override val renderAsNormalBlock = false
	override val isOpaqueCube = false
	override val isNormalCube = false
	override val getRenderType = RenderIds.EnergyInjector
	private def applyBlockBoundsAsHalf() = this.setBlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f)

	// Block Traits //
	override def createNewTileEntity(world: World, meta: Int) = new TEEnergyInjector(EnergyInjectorFluidLimits.Attachable)
}
