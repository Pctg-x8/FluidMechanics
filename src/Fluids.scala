package com.cterm2.mcfm1710

import net.minecraftforge.fluids.{FluidRegistry, Fluid, FluidStack}
import utils.LocalTranslationUtils._
import net.minecraft.init.{Blocks => VanillaBlocks}

object Fluids
{
	final class EnergeticFluid extends Fluid("lava")
	{
		// Fluid Properties //
		setBlock(VanillaBlocks.lava)
		setLuminosity(7)
		setUnlocalizedName("energeticFluid")
	}

	lazy val energeticFluid = new EnergeticFluid
	def newEnergeticFluidStack(amount: Int) = new FluidStack(energeticFluid, amount)

	def init() =
	{
		System.out.println("Registering Fluids...")

		FluidRegistry.registerFluid(this.energeticFluid)
	}
}
