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
