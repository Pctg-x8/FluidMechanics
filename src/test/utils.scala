package com.cterm2.mcfm1710.test.utils

import org.scalatest._, org.scalatest.Matchers._
import com.cterm2.mcfm1710.utils.{Vector3i, Vector3f}

class UtilsTest extends FlatSpec with ShouldMatchers
{
	// Int Vector
	"(0, 0, 0) of integers" should "equal (-1, 1, 1)" in
	{
		assert((Vector3i(0, 0, 0) withOffset Vector3i(-1, 1, 1)) == Vector3i(-1, 1, 1))
		assert((Vector3i(0, 0, 0) + Vector3i(-1, 1, 1)) == Vector3i(-1, 1, 1))
	}
	"(1, 2, 3) of integers" should "equal (0, 0, 0)" in
	{
		assert(Vector3i(1, 2, 3) * 0 == Vector3i(0, 0, 0))
		assert(Vector3i(1, 2, 3) * Vector3i(0, 0, 0) == Vector3i(0, 0, 0))
	}

	// Float Vector
	"(0, 0, 0) of float" should "equal (1, 1.5, 2)" in
	{
		assert(Vector3f(0.0f, 0.0f, 0.0f) + Vector3f(1.0f, 1.5f, 2.0f) == Vector3f(1.0f, 1.5f, 2.0f))
	}
	"(5, -2.3, 0.0f) of float" should "equal (0, 0, 0)" in
	{
		assert(Vector3f(5.0f, -2.3f, 0.0f) * 0.0f == Vector3f(0.0f, 0.0f, 0.0f))
		assert(Vector3f(5.0f, -2.3f, 0.0f) * Vector3f(0.0f, 0.0f, 0.0f) == Vector3f(0.0f, 0.0f, 0.0f))
	}
}
