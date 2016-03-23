package com.cterm2.mcfm1710

import net.minecraft.creativetab.CreativeTabs
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.block.Block

object Blocks
{
	def init(ctab: CreativeTabs) =
	{
		System.out.println("Init Blocks...")

		this.registerWithName(assemblyTable.Block, "assemblyTable_part")
		this.registerWithName(assemblyTable.BlockTopPart, "assemblyTable.top")
		this.registerWithName(energyInjector.BlockModuled.setCreativeTab(ctab), "energyInjector.attachable")
		this.registerWithName(energyInjector.BlockStandalone.setCreativeTab(ctab), "energyInjector.standalone")
		this.registerWithName(thermalGenerator.Block.setCreativeTab(ctab), "sourceGenerator.thermal")
	}

	@inline
	private final def registerWithName[B <: Block](b: B, ulName: String) = GameRegistry.registerBlock(b.setBlockName(ulName), ulName)
}
