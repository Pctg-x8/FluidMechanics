package com.cterm2.mcfm1710

import org.apache.logging.log4j.{LogManager, Level}
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.ConsoleAppender

import cpw.mods.fml.common.{Mod, SidedProxy}
import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.event._

import net.minecraft.item.Item
import net.minecraft.init.{Blocks => VanillaBlocks}
import net.minecraft.creativetab.CreativeTabs
import cpw.mods.fml.relauncher.{Side, SideOnly}
import cpw.mods.fml.common.network.NetworkRegistry

@Mod(modid=FMEntry.ID, name=FMEntry.Name, version=FMEntry.Version, modLanguage="scala")
object FMEntry
{
	lazy val logger = LogManager.getLogger("Fluid Mechanics")

	final val ID = "mcfm1710"
	final val Name = "Fluid Mechanics"
	final val Version = "1.0-alpha"

	val ctab = new CreativeTabs("tabFluidMechanics")
	{
		@SideOnly(Side.CLIENT)
		override def getTabIconItem() = Item.getItemFromBlock(VanillaBlocks.anvil)
	}
	@SidedProxy(clientSide="com.cterm2.mcfm1710.FMEntry$ClientProxy", serverSide="com.cterm2.mcfm1710.FMEntry$ServerProxy")
	var proxy: IProxy = null

	@EventHandler
	def preInit(e: FMLPreInitializationEvent) =
	{
		// Enable Trace Logging
		val context = LogManager.getContext(false).asInstanceOf[LoggerContext]
		val config = context.getConfiguration
		val lconf = config.getLoggerConfig("Fluid Mechanics")
		lconf.addAppender(config.getAppenders.get("FmlSysOut").asInstanceOf[ConsoleAppender], Level.ALL, null)
		lconf.setLevel(Level.ALL)
		context.updateLoggers()

		logger.info(s"$Name version $Version.")
	}
	@EventHandler
	def init(e: FMLInitializationEvent) =
	{
		Fluids.init()
		AssemblyTable register this.ctab
		EnergyInjector register this.ctab
		ThermalGenerator register this.ctab
		ThermalFluidGenerator register this.ctab

		this.proxy.registerRenderers()

		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler)
	}

	// Proxies //
	sealed trait IProxy
	{
		def registerRenderers() = ()
	}
	@SideOnly(Side.CLIENT)
	final class ClientProxy extends IProxy
	{
		import cpw.mods.fml.client.registry.{RenderingRegistry, ISimpleBlockRenderingHandler}

		override def registerRenderers() =
		{
			AssemblyTable.registerClient
			EnergyInjector.registerClient
			SourceGenerator.registerClient
		}

		@inline
		private def registerBlockRenderer(render: ISimpleBlockRenderingHandler) =
		{
			val id = RenderingRegistry.getNextAvailableRenderId
			RenderingRegistry.registerBlockHandler(id, render)
			id
		}
	}
	@SideOnly(Side.SERVER)
	final class ServerProxy extends IProxy
	{

	}
}
