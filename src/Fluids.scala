package com.cterm2.mcfm1710

import net.minecraftforge.fluids
import utils.LocalTranslationUtils._
import net.minecraft.init.{Blocks => VanillaBlocks}
import net.minecraft.block.material.Material
import net.minecraft.world.{World, IBlockAccess}
import net.minecraft.util.IIcon
import net.minecraft.client.renderer.texture.IIconRegister
import cpw.mods.fml.relauncher.{Side, SideOnly}

package EnergeticFluid
{
	object Fluid extends fluids.Fluid("water")
	{
		// Fluid Properties //
		setLuminosity(7)
	}
	object Block extends fluids.BlockFluidClassic(Fluid, Material.water)
	{
		// Rendering Configurations //
		@SideOnly(Side.CLIENT)
		private var stillIcon: IIcon = null
		@SideOnly(Side.CLIENT)
		private var flowingIcon: IIcon = null
		override def getIcon(side: Int, meta: Int) = if(side == 0 || side == 1) this.stillIcon else this.flowingIcon
		@SideOnly(Side.CLIENT)
		override def registerBlockIcons(register: IIconRegister)
		{
			this.stillIcon = register.registerIcon("water_still")
			this.flowingIcon = register.registerIcon("water_flow")
		}

		override def canDisplace(world: IBlockAccess, x: Int, y: Int, z: Int) = false
		override def displaceIfPossible(world: World, x: Int, y: Int, z: Int) = false
	}
}

object Fluids
{
	def newEnergeticFluidStack(amount: Int) = new fluids.FluidStack(EnergeticFluid.Fluid, amount)

	def init()
	{
		System.out.println("Registering Fluids...")

		fluids.FluidRegistry.registerFluid(EnergeticFluid.Fluid)
		ContentRegistry register EnergeticFluid.Block.setCreativeTab(FluidTab) as "energeticFluid"
	}
}
