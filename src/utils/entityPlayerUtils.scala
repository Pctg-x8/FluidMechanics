package com.cterm2.mcfm1710.utils

import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.util.ForgeDirection
import net.minecraft.util.MathHelper

object EntityPlayerUtils
{
	// Converts facing direction integer to ForgeDirection
	def convertFacingDirection(i: Int) = i match
	{
		case 0 => ForgeDirection.SOUTH
		case 1 => ForgeDirection.WEST
		case 2 => ForgeDirection.NORTH
		case 3 => ForgeDirection.EAST
	}
	implicit final class ImplicitWrapper(val p: EntityPlayer)
	{
		// Gets player facing direction as integer
		def directionInt = MathHelper.floor_double((p.rotationYaw * 4.0f / 360.0f).asInstanceOf[Double] + 0.5d).asInstanceOf[Int] & 3
		// Gets player facing direction as ForgeDirection
		def direction = convertFacingDirection(this.directionInt)
		// aliasing to direction
		def facing = direction
		// aliasing to directionInt
		def facingInt = directionInt
	}
}
