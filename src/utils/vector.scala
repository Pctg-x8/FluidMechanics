package com.cterm2.mcfm1710.utils

final case class Vector3i(val x: Int, val y: Int, val z: Int)
{
	final def withOffset(o: Vector3i) = Vector3i(x + o.x, y + o.y, z + o.z)
	final def opposite = Vector3i(-x, -y, -z)
	final def *(scalar: Int) = Vector3i(x * scalar, y * scalar, z * scalar)
}
final case class Vector3f(val x: Float, val y: Float, val z: Float)
{
	final def +(scalar: Float) = Vector3f(x + scalar, y + scalar, z + scalar)
	final def +(v: Vector3f) = Vector3f(x + v.x, y + v.y, z + v.z) 
	final def *(scalar: Float) = Vector3f(x * scalar, y * scalar, z * scalar)
}
object Vector3f
{
	final def randomUnit = Vector3f(RandomGenerator.nextFloat, RandomGenerator.nextFloat, RandomGenerator.nextFloat)
}

object ForgeDirectionExtensions
{
	import net.minecraftforge.common.util.ForgeDirection

	implicit final class ImplicitClass(val d: ForgeDirection)
	{
		final val offset = Vector3i(d.offsetX, d.offsetY, d.offsetZ)
	}
}
