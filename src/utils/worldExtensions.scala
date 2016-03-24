package com.cterm2.mcfm1710.utils

object WorldExtensions
{
	import net.minecraft.world.World, net.minecraft.block.Block

	implicit final class WorldExtensionDefs(val world: World) extends AnyVal
	{
		// Replace block to air
		final def replaceBlockToAir(pos: Vector3i, src: Block) =
			if(world.getBlock(pos.x, pos.y, pos.z) == src) world.setBlockToAir(pos.x, pos.y, pos.z)
		final def replaceBlock(pos: Vector3i, src: Block, dst: Block) =
			if(world.getBlock(pos.x, pos.y, pos.z) == src) world.setBlock(pos.x, pos.y, pos.z, dst)
		final def inClientSide = world.isRemote
	}
}
