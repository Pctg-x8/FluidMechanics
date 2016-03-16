package com.cterm2.mcfm1710

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.event._

@Mod(modid=FMEntry.ID, name=FMEntry.Name, version=FMEntry.Version, modLanguage="scala")
object FMEntry
{
	final val ID = "com.cterm2.mcfm1710"
	final val Name = "Fluid Mechanics"
	final val Version = "1.0-alpha"
	
	@EventHandler
	def preInit(e: FMLPreInitializationEvent) =
	{
		System.out.println(Name + " version " + Version + ".")
	}
	@EventHandler
	def init(e: FMLInitializationEvent) =
	{
		
	}
	@EventHandler
	def postInit(e: FMLPostInitializationEvent) =
	{
		
	}
}
