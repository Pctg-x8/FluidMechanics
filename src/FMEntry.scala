package com.cterm2.mcfm1710

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
		System.out.println(s"${Name} version ${Version}.")
	}
	@EventHandler
	def init(e: FMLInitializationEvent) =
	{
		Items.init(this.ctab)
		Fluids.init()
		Blocks.init(this.ctab)
		Tiles.init()
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
			import cpw.mods.fml.client.registry.ClientRegistry
			import com.cterm2.mcfm1710.tiles._, com.cterm2.mcfm1710.client.renderer._

			Blocks.attachableEnergyInjector.renderType = this.registerBlockRenderer(new attachableEnergyInjector.BlockRenderer)
			sourceGenerator.CommonValues.renderType = this.registerBlockRenderer(new sourceGenerator.BlockRenderer)
			ClientRegistry.bindTileEntitySpecialRenderer(classOf[TEEnergyInjector], new attachableEnergyInjector.TileEntityRenderer)
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
