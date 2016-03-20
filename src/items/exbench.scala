package com.cterm2.mcfm1710.items

import net.minecraft.item.{Item, ItemStack}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import com.cterm2.mcfm1710.utils.EntityLivingUtils._
import com.cterm2.mcfm1710.utils.{Vector3i, BlockPlacementInfo}
import com.cterm2.mcfm1710.utils.ForgeDirectionExtensions._
import com.cterm2.mcfm1710.Blocks, com.cterm2.mcfm1710.blocks.BlockAssemblyTablePart

final class ItemAssemblyTable extends Item
{
	setUnlocalizedName("assemblyTable_caller")

	// called when using item(place actual blocks if possible)
	override def onItemUse(stack: ItemStack, player: EntityPlayer, world: World, x: Int, y: Int, z: Int, side: Int, xOffs: Float, yOffs: Float, zOffs: Float) =
		(world.isRemote, ForgeDirection.values()(side)) match
		{
			case (false, ForgeDirection.UP) =>
			{
				val placeY = y + 1
				val blockSlaveDirection = player.facing.getRotation(ForgeDirection.UP)
				val slavePos = Vector3i(x, placeY, z) withOffset blockSlaveDirection.offset
				val placeInfo = BlockPlacementInfo(player, world, Vector3i(x, placeY, z))
				val placeInfoSlave = BlockPlacementInfo(player, world, slavePos)
				if(BlockAssemblyTablePart.canPlaceBlock(placeInfo, ForgeDirection.UP, stack) &&
					BlockAssemblyTablePart.canPlaceBlock(placeInfoSlave, ForgeDirection.UP, stack))
				{
					world.setBlock(placeInfo.pos.x, placeInfo.pos.y, placeInfo.pos.z, Blocks.asmTable, blockSlaveDirection.ordinal, 3)
					if(world.getBlock(placeInfo.pos.x, placeInfo.pos.y, placeInfo.pos.z) == Blocks.asmTable)
					{
						world.setBlock(slavePos.x, slavePos.y, slavePos.z, Blocks.asmTable, blockSlaveDirection.ordinal | BlockAssemblyTablePart.SlaveBit, 3)
						world.setBlock(placeInfo.pos.x, placeInfo.pos.y + 1, placeInfo.pos.z, Blocks.asmTableTop, blockSlaveDirection.ordinal, 3)
						world.setBlock(slavePos.x, slavePos.y + 1, slavePos.z, Blocks.asmTableTop, blockSlaveDirection.ordinal | BlockAssemblyTablePart.SlaveBit, 3)
					}
					stack.stackSize -= 1
					true
				}
				else false
			}
			case (true, _) => true
			case _ => false
		}
}
