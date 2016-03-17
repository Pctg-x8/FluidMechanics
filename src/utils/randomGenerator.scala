package com.cterm2.mcfm1710.utils

import java.util.Random

final object RandomGenerator
{
	// internal object
	private final val randomizer = new Random

	// Returns next float value
	final def nextFloat = this.randomizer.nextFloat
	// Returns next value in normal distribution
	final def nextGaussian = this.randomizer.nextGaussian
	// Returns ranged random int value
	final def range(min: Int, max: Int) = this.randomizer.nextInt(max - min) + min
}
