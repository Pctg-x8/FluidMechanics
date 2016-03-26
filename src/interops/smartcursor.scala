package com.cterm2.mcfm1710.interops.smartcursor

import com.asaskevich.smartcursor.api.{ModuleConnector, IBlockProcessor}
import cpw.mods.fml.common.{Mod, Loader}
import cpw.mods.fml.common.event._
import cpw.mods.fml.common.network.{NetworkRegistry, ByteBufUtils}
import cpw.mods.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}
import cpw.mods.fml.relauncher.Side
import io.netty.buffer.ByteBuf

// Module Connector
@Mod(modid = SCModuleConnector.ID, name = SCModuleConnector.Name, version = SCModuleConnector.Version, modLanguage = "scala")
object SCModuleConnector
{
	final val ID = "mcfm1710#SmartCursorModuleConnector"
	final val Name = "SmartCursor Module Connector[Fluid Mechanics]"
	final val Version = "1.0-alpha"

	final val network = NetworkRegistry.INSTANCE.newSimpleChannel(ID)
	var forceUpdate = true
	def startForceUpdate()
	{
		if(Loader.isModLoaded(ID)) SCModuleConnector.forceUpdate = true
	}
	def stopForceUpdate()
	{
		if(Loader.isModLoaded(ID)) SCModuleConnector.forceUpdate = false
	}

	@Mod.EventHandler
	def preInit(e: FMLPreInitializationEvent) =
	{
		network.registerMessage(classOf[SCForceSyncPacketHandler], classOf[SCPacketForceSync], 0, Side.SERVER)
	}
	@Mod.EventHandler
	def init(e: FMLInitializationEvent) =
	{
		System.out.println("mcfm1710.ModInterop: Applying Interoperation with SmartCursor...")
		ModuleConnector.connectModule(new BlockInformationProvider)
	}
}

// Packet for Force Synchronization
final class SCPacketForceSync(var xCoord: Int, var yCoord: Int, var zCoord: Int) extends IMessage
{
	import ByteBufUtils._

	def this() = this(0, 0, 0)

	override def fromBytes(buffer: ByteBuf)
	{
		this.xCoord = readVarInt(buffer, 5)
		this.yCoord = readVarInt(buffer, 5)
		this.zCoord = readVarInt(buffer, 5)
	}
	override def toBytes(buffer: ByteBuf)
	{
		writeVarInt(buffer, this.xCoord, 5)
		writeVarInt(buffer, this.yCoord, 5)
		writeVarInt(buffer, this.zCoord, 5)
	}
}
// Packet Handler for Force Synchronization
final class SCForceSyncPacketHandler extends IMessageHandler[SCPacketForceSync, IMessage]
{
	override def onMessage(message: SCPacketForceSync, context: MessageContext) =
		Option(context.getServerHandler().playerEntity.worldObj.getTileEntity(message.xCoord, message.yCoord, message.zCoord)) match
		{
			case Some(ip: IInformationProvider) =>
			{
				ip.forceSynchronize()
				null
			}
			case _ => null
		}
}

// Fluid Mechanics block information provider module
class BlockInformationProvider extends IBlockProcessor
{
	import net.minecraft.world.World, net.minecraft.block.Block

	// Module Informations //
	override val getModuleName = "Fluid Mechanics"
	override val getAuthor = "S.Percentage"

	override def process(list: java.util.List[String], block: Block, meta: Int, world: World, x: Int, y: Int, z: Int) =
		world.getTileEntity(x, y, z) match
		{
			case ip: IInformationProvider =>
			{
				if(SCModuleConnector.forceUpdate) SCModuleConnector.network.sendToServer(new SCPacketForceSync(x, y, z))
				ip.provideInformation(list)
			}
			case _ => ()
		}
}

// Tile as Information Provider
trait IInformationProvider
{
	def provideInformation(list: java.util.List[String]): Unit
	def forceSynchronize(): Unit
}
