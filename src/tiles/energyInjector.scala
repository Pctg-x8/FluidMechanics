package com.cterm2.mcfm1710.tiles

import net.minecraft.tileentity.TileEntity
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.play.server.S35PacketUpdateTileEntity
import net.minecraft.network.NetworkManager
import net.minecraftforge.fluids.{IFluidTank, FluidStack, FluidTankInfo, IFluidHandler, FluidRegistry, Fluid}
import net.minecraftforge.common.util.ForgeDirection
import cpw.mods.fml.common.Optional
import com.cterm2.mcfm1710.Fluids
import mekanism.api.energy.IStrictEnergyAcceptor

object EnergyInjectorSynchronizeDataKeys
{
	final val WaterKey = "WaterData"
	final val EnergeticKey = "EnergyFluidsData"
	final val BlockDirectionKey = "BlockDirection"
	final val MaxAmountKey = "MaxFluidAmount"
}
// Energy Injector Tile Entities
@Optional.Interface(iface = "mekanism.api.energy.IStrictEnergyAcceptor", modid="Mekanism", striprefs = true)
final class TEEnergyInjector extends TileEntity with IFluidHandler with IStrictEnergyAcceptor
{
	tankHolder =>
	import EnergyInjectorSynchronizeDataKeys._

	// External Values //
	var dir: ForgeDirection = ForgeDirection.UNKNOWN
	var maxFluidAmount: Int = 0

	// Make as Combinated Fluid Tanks //
	// Content restricted fluid tank
	private class RestrictedFluidTank(val acceptType: Fluid) extends IFluidTank
	{
		// Tank Traits //
		// Capacity
		override val getCapacity = tankHolder.maxFluidAmount
		// Information provider
		override lazy val getInfo = new FluidTankInfo(this)

		// Tank Exports //
		private val stack = new FluidStack(acceptType, 0)
		override def getFluid = this.stack
		override def getFluidAmount = this.stack.amount

		// Tank Interacts //
		// called when attempting to fill fluids
		override def fill(resource: FluidStack, perform: Boolean) = resource match
		{
			case null => 0
			case _ if !this.canAccept(resource) => 0
			case _ =>
			{
				val newAmount = Math.min(resource.amount, this.getCapacity - this.getFluidAmount)
				if(!this.canAccept(resource)) 0
				else
				{
					if(perform && newAmount > 0)
					{
						this.stack.amount += newAmount
						tankHolder.updateTileInfo()
					}
					newAmount
				}
			}
		}
		// Called when attempting to drain fluids
		override def drain(maxDrain: Int, perform: Boolean) =
		{
			val drained = Math.min(maxDrain, this.stack.amount)
			if(perform && drained > 0)
			{
				this.stack.amount -= drained
				tankHolder.updateTileInfo()
			}
			new FluidStack(this.stack, drained)
		}
		private[TEEnergyInjector] def synchronizeData =
		{
			val tag = new NBTTagCompound
			this.stack.writeToNBT(tag)
			tag
		}
		private[TEEnergyInjector] def synchronizeDataFrom(tag: NBTTagCompound) =
		{
			val newFluid = FluidStack.loadFluidStackFromNBT(tag)
			if(!this.canAccept(newFluid)) throw new RuntimeException("Restriction Error")
			this.stack.amount = newFluid.amount
		}
		private[TEEnergyInjector] def canAccept(fluid: Fluid) = fluid == this.stack.getFluid
		private[TEEnergyInjector] def canAccept(fluid: FluidStack): Boolean = this.canAccept(fluid.getFluid)
	}
	private lazy val waterTank = new RestrictedFluidTank(FluidRegistry.WATER)
	private lazy val energeticTank = new RestrictedFluidTank(Fluids.energeticFluid)
	private def getTank(from: ForgeDirection) = from match
	{
		case ForgeDirection.EAST if dir == ForgeDirection.NORTH => Some(this.waterTank)
		case ForgeDirection.EAST if dir == ForgeDirection.SOUTH => Some(this.energeticTank)
		case ForgeDirection.WEST if dir == ForgeDirection.NORTH => Some(this.energeticTank)
		case ForgeDirection.WEST if dir == ForgeDirection.SOUTH => Some(this.waterTank)
		case ForgeDirection.NORTH if dir == ForgeDirection.WEST => Some(this.waterTank)
		case ForgeDirection.NORTH if dir == ForgeDirection.EAST => Some(this.energeticTank)
		case ForgeDirection.SOUTH if dir == ForgeDirection.WEST => Some(this.energeticTank)
		case ForgeDirection.SOUTH if dir == ForgeDirection.EAST => Some(this.waterTank)
		case _ => None
	}
	override def getTankInfo(from: ForgeDirection) = Array((this.getTank(from) map { _.getInfo }).orNull)
	override def canDrain(from: ForgeDirection, fluid: Fluid) = this.getTank(from).isDefined
	override def canFill(from: ForgeDirection, fluid: Fluid) = this.getTank(from) map { _ canAccept fluid } getOrElse false
	override def fill(from: ForgeDirection, resource: FluidStack, perform: Boolean) = this.getTank(from) map
	{
		x => if(x canAccept resource) x.fill(resource, perform) else 0
	} getOrElse 0
	override def drain(from: ForgeDirection, resource: FluidStack, perform: Boolean) = (this.getTank(from) map
	{
		x => if(x.getFluid == resource.getFluid) x.drain(resource.amount, perform) else null
	}).orNull
	override def drain(from: ForgeDirection, maxDrain: Int, perform: Boolean) = (this.getTank(from) map { _.drain(maxDrain, perform) }).orNull

	// Data Synchronizations //
	private def writeSynchronizeDataToNBT(tag: NBTTagCompound) =
	{
		tag.setTag(WaterKey, this.waterTank.synchronizeData)
		tag.setTag(EnergeticKey, this.energeticTank.synchronizeData)
	}
	private def readSynchronizeDataFromNBT(tag: NBTTagCompound) =
	{
		this.waterTank synchronizeDataFrom tag.getTag(WaterKey).asInstanceOf[NBTTagCompound]
		this.energeticTank synchronizeDataFrom tag.getTag(EnergeticKey).asInstanceOf[NBTTagCompound]
	}
	override def writeToNBT(tag: NBTTagCompound) =
	{
		super.writeToNBT(tag)
		tag.setInteger(BlockDirectionKey, this.dir.ordinal)
		tag.setInteger(MaxAmountKey, this.maxFluidAmount)
		this.writeSynchronizeDataToNBT(tag)
	}
	override def readFromNBT(tag: NBTTagCompound) =
	{
		super.readFromNBT(tag)
		this.dir = ForgeDirection.values()(tag.getInteger(BlockDirectionKey))
		this.maxFluidAmount = tag.getInteger(MaxAmountKey)
		this.readSynchronizeDataFromNBT(tag)
	}
	override def getDescriptionPacket() =
	{
		val tag = new NBTTagCompound
		this.writeSynchronizeDataToNBT(tag)
		new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, tag)
	}
	override def onDataPacket(net: NetworkManager, packet: S35PacketUpdateTileEntity) = this.readSynchronizeDataFromNBT(packet.func_148857_g)
	private def updateTileInfo() =
	{
		this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord)
		this.markDirty()
	}

	// TileEntity Interacts //
	override val canUpdate = false
	// Called when power transmitted(Unit: RF/t)(Returns used energies)
	def injectFluids(amount: Int) =
	{
		// 1:1 = energy:water => energeticFluid
		val newEnergeticFluid = Fluids.newEnergeticFluidStack(amount)
		val drainable = this.waterTank.drain(amount, false).amount
		val acceptable = this.energeticTank.fill(newEnergeticFluid, false)
		val converted = Math.min(drainable, acceptable)
		if(converted > 0)
		{
			newEnergeticFluid.amount = converted
			this.waterTank.drain(converted, true)
			this.energeticTank.fill(newEnergeticFluid, true)
			converted
		}
		else 0
	}

	// Energy Acceptor Interacts //
	override def transferEnergyToAcceptor(side: ForgeDirection, amount: Double) = side match
	{
		case ForgeDirection.UP => this.injectFluids(amount.asInstanceOf[Int]).toDouble
		case _ => 0.0d
	}
	override def canReceiveEnergy(side: ForgeDirection) = side == ForgeDirection.UP

	// Energy Storage Exports(Note: EnergyInjector does not store any energies) //
	override val getEnergy = 0.0d
	override def setEnergy(newValue: Double) =
	{
		System.out.println(s"Storage Set: $newValue")
	}
	override val getMaxEnergy = 4000.0d			// provides max acceptable energies in EnergyAcceptor

	// Information Provider //
	def provideInformation(list: java.util.List[String]) =
	{
		list add s"Facing on ${this.dir}"
		list add s"Input(Water) Tank amount: ${this.waterTank.getFluidAmount} mb"
		list add s"Output(EnergeticFluid) Tank amount: ${this.energeticTank.getFluidAmount} mb"
	}
}
