package com.cterm2.mcfm1710.blocks

import net.minecraft.block.BlockContainer, net.minecraft.block.material.Material
import cpw.mods.fml.relauncher.{SideOnly, Side}
import com.cterm2.mcfm1710.tiles.TEEnergyInjector
import net.minecraft.world.World, net.minecraft.entity.EntityLivingBase
import com.cterm2.mcfm1710.utils.EntityLivingUtils._
import net.minecraft.item.ItemStack

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
	private def applyBlockBoundsAsHalf() = this.setBlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f)

	var renderType: Int = 0
	override def getRenderType = renderType

	// Block Traits //
	override def createNewTileEntity(world: World, meta: Int) = new TEEnergyInjector(EnergyInjectorFluidLimits.Attachable)

	// Block Interacts //
	// Called when the block is placed in the world
	override def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, placer: EntityLivingBase, stack: ItemStack) =
		world.setBlockMetadataWithNotify(x, y, z, placer.facingInt, 2)
}
