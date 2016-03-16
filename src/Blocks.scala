package com.cterm2.mcfm1710

import net.minecraft.creativetab.CreativeTabs
import com.cterm2.mcfm1710.blocks._
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.block.Block

object Blocks
{
	lazy val asmTable = new BlockAssemblyTablePart
	
	def init(ctab: CreativeTabs) = 
	{
		System.out.println("Init Blocks...")
		
		this.registerWithName(this.asmTable, "assemblyTable_part")
	}
	
	private final def registerWithName(b: Block, ulName: String) =
		GameRegistry.registerBlock(b.setBlockName(ulName), ulName)
}
