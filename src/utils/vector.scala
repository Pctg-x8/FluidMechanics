package com.cterm2.mcfm1710

package object utils
{
	// Implicit Converters of Int => Vector3i and Float => Vector3f
	implicit def toV3(x: Int) = Vector3i(x, x, x)
	implicit def toV3(f: Float) = Vector3f(f, f, f)
}

package utils
{
	final case class Vector3i(val x: Int, val y: Int, val z: Int)
	{
		final def withOffset(o: Vector3i) = Vector3i(x + o.x, y + o.y, z + o.z)
		final def opposite = Vector3i(-x, -y, -z)
		final def *(v: Vector3i) = Vector3i(x * v.x, y * v.y, z * v.z)
		final def +(v: Vector3i) = Vector3i(x + v.x, y + v.y, z + v.z)
		final def unary_- = Vector3i(-x, -y, -z)
	}
	final case class Vector3f(val x: Float, val y: Float, val z: Float)
	{
		final def +(v: Vector3f) = Vector3f(x + v.x, y + v.y, z + v.z)
		final def *(v: Vector3f) = Vector3f(x * v.x, y * v.y, z * v.z)
	}
	object Vector3f
	{
		final def randomUnit = Vector3f(RandomGenerator.nextFloat, RandomGenerator.nextFloat, RandomGenerator.nextFloat)
	}

	object ForgeDirectionExtensions
	{
		import net.minecraftforge.common.util.ForgeDirection

		implicit final class ImplicitClass(val d: ForgeDirection) extends AnyVal
		{
			final def offset = Vector3i(d.offsetX, d.offsetY, d.offsetZ)
		}
	}
}
