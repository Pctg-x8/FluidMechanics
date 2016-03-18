package com.cterm2.mcfm1710.blocks

import com.cterm2.mcfm1710.{FMEntry, Blocks}
import java.util.Random
import net.minecraft.block.{Block, BlockContainer}, net.minecraft.item.Item
import net.minecraft.block.material.Material
import net.minecraft.entity.player.EntityPlayer
import com.cterm2.mcfm1710.utils.{BlockPlacementInfo, Vector3i}
import com.cterm2.mcfm1710.utils.ForgeDirectionExtensions._
import com.cterm2.mcfm1710.utils.WorldExtensions._
import net.minecraftforge.common.util.ForgeDirection
import net.minecraft.item.ItemStack, net.minecraft.world.{World, IBlockAccess}
import com.cterm2.mcfm1710.Items, com.cterm2.mcfm1710.tiles.TEAssemblyTable
import cpw.mods.fml.relauncher.{SideOnly, Side}

// companion
object BlockAssemblyTablePart
{
	// MetaBits //
	final val SlaveBit = 0x08

	// Utilities //
	final def canPlaceBlock(placeinfo: BlockPlacementInfo, side: ForgeDirection, stack: ItemStack) =
		placeinfo.player.canPlayerEdit(placeinfo.pos.x, placeinfo.pos.y, placeinfo.pos.z, side.ordinal(), stack) &&
		placeinfo.player.canPlayerEdit(placeinfo.pos.x, placeinfo.pos.y + 1, placeinfo.pos.z, side.ordinal(), stack) &&
		placeinfo.world.isAirBlock(placeinfo.pos.x, placeinfo.pos.y, placeinfo.pos.z) &&
		placeinfo.world.isAirBlock(placeinfo.pos.x, placeinfo.pos.y + 1, placeinfo.pos.z) &&
		World.doesBlockHaveSolidTopSurface(placeinfo.world, placeinfo.pos.x, placeinfo.pos.y - 1, placeinfo.pos.z)
	// Determine this block is master(primary) block
	final def isMaster(meta: Int) = (meta & 0x08) == 0
	// Get direction from metadata
	final def getDirectionOffset(meta: Int) = ForgeDirection.values()(meta & 0x07).offset
}
final class BlockAssemblyTablePart extends BlockContainer(Material.rock)
{
	import BlockAssemblyTablePart._

	setHardness(1.0f)

	// Render Settings //
	// render as normal(solid cube) block flag
	@SideOnly(Side.CLIENT)
	override val renderAsNormalBlock = false
	// render as opaque(no transparency) block flag
	override val isOpaqueCube = false
	override val isNormalCube = false		// set false if block rendered by custom renderer

	// Block Actions //
	// Called when block is right-clicked(activated)
	override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, xo: Float, yo: Float, zo: Float) =
		if(world.isRemote) true else
		{
			val meta = world.getBlockMetadata(x, y, z)
			if(!isMaster(meta))
			{
				// Slave: Delegate process to master
				val masterPos = Vector3i(x, y, z) withOffset -getDirectionOffset(meta)
				val masterMeta = world.getBlockMetadata(masterPos.x, masterPos.y, masterPos.z)
				this.onBlockActivated(world, masterPos.x, masterPos.y, masterPos.z, player, masterMeta, xo, yo, zo)
			}
			else
			{
				// Master: Open Table's GUI
				// System.out.println(s"Open Table's GUI on Coordinate (${x}, ${y}, ${z})")
				val entity = world.getTileEntity(x, y, z).asInstanceOf[TEAssemblyTable]
				// System.out.println(s"TileEntity associated coordinate: ${entity}")
				if(entity != null) player.openGui(FMEntry, 0, world, x, y, z)
				true
			}
		}
	// Called when neighbor block changed(broken, placed new or pushed by pistons, passed coordinate is mine)
	override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, neighbor: Block) =
	{
		val meta = world.getBlockMetadata(x, y, z)
		val dir = getDirectionOffset(meta)

		// flip direction if slave block
		if(isMaster(meta)) this.breakOnIllegalAdjacents(world, Vector3i(x, y, z), dir)
		else this.breakOnIllegalAdjacents(world, Vector3i(x, y, z), -dir, true)
	}
	// Called when the block is attempted to be harvested
	override def onBlockHarvested(world: World, x: Int, y: Int, z: Int, meta: Int, player: EntityPlayer) =
		if(player.capabilities.isCreativeMode && isMaster(meta))
		{
			// on master
			val dir = getDirectionOffset(meta)
			val slavePos = Vector3i(x, y, z) withOffset dir
			// break slaves too
			world.replaceBlockToAir(slavePos, this)
			world.replaceBlockToAir(Vector3i(x, y + 1, z), Blocks.asmTableTop)
			world.replaceBlockToAir(slavePos + Vector3i(0, 1, 0), Blocks.asmTableTop)
		}
	// Called when the block is broken
	override def breakBlock(world: World, x: Int, y: Int, z: Int, block: Block, meta: Int) =
	{
		val entity = world.getTileEntity(x, y, z).asInstanceOf[TEAssemblyTable]
		if(entity != null) entity.dropItemsAtRandom(world, x, y, z)
	}

	// Configures //
	// Configure dropped item
	override def getItemDropped(meta: Int, randomizer: Random, quantity: Int) =
		if(isMaster(meta)) Item.getItemById(0) else Items.asmTable
	// Drops the block items with a specified chance of dropping the specified items(only from slave block)
	override def dropBlockAsItemWithChance(world: World, x: Int, y: Int, z: Int, meta: Int, f1: Float, i4: Int) =
		if(!isMaster(meta)) super.dropBlockAsItemWithChance(world, x, y, z, meta, f1, 0)
	// Set as immobility
	override val getMobilityFlag = 2
	// Gets an item for the block being called on
	@SideOnly(Side.CLIENT)
	override def getItem(world: World, x: Int, y: Int, z: Int) = Items.asmTable
	// TileEntity associated to block(have only in master)
	override def createNewTileEntity(world: World, meta: Int) = if(isMaster(meta)) new TEAssemblyTable else null

	// Miscs //
	// Break on illegal adjacents found
	private final def breakOnIllegalAdjacents(world: World, thisPos: Vector3i, pairDir: Vector3i, dropItem: Boolean = false) =
		if(world.getBlock(thisPos.x + pairDir.x, thisPos.y, thisPos.z + pairDir.z) != this ||
			world.getBlock(thisPos.x, thisPos.y + 1, thisPos.z) != Blocks.asmTableTop ||
			world.getBlock(thisPos.x + pairDir.x, thisPos.y + 1, thisPos.z + pairDir.z) != Blocks.asmTableTop)
		{
			// illegal adjacents -> break
			world.setBlockToAir(thisPos.x, thisPos.y, thisPos.z)
			if(!world.isRemote && dropItem) this.dropBlockAsItem(world, thisPos.x, thisPos.y, thisPos.z, SlaveBit, 0)
		}
}

// Additional Block
final class BlockAssemblyTableTop extends Block(Material.rock)
{
	import BlockAssemblyTablePart._

	this.setHardness(1.0f)
	this.applyBlockBounds()

	// Rendering Configurations //
	override val isOpaqueCube = false

	// Block Interacts //
	// Called when block is right-clicked(activated, delegate process to lower blocks)
	override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, xo: Float, yo: Float, zo: Float) =
		world.getBlock(x, y - 1, z).asInstanceOf[BlockAssemblyTablePart].onBlockActivated(world, x, y - 1, z, player, side, xo, yo, zo)
	// Called when neighbor block changed(broken, placed new or pushed by pistons, passed coordinate is mine)
	override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, neighbor: Block) =
	{
		val meta = world.getBlockMetadata(x, y, z)
		val dir = getDirectionOffset(meta)

		// flip direction if slave block
		if(isMaster(meta)) this.breakOnIllegalAdjacents(world, Vector3i(x, y, z), dir)
		else this.breakOnIllegalAdjacents(world, Vector3i(x, y, z), -dir)
	}
	// Called when the block is attempted to be harvested(creative item dropping supression)
	override def onBlockHarvested(world: World, x: Int, y: Int, z: Int, meta: Int, player: EntityPlayer) =
		if(player.capabilities.isCreativeMode)
		{
			// on master
			val dir = getDirectionOffset(meta)
			val slavePos = if(isMaster(meta)) Vector3i(x, y, z) withOffset dir else Vector3i(x, y, z) withOffset -dir
			// break slaves too
			world.replaceBlockToAir(slavePos, this)
			world.replaceBlockToAir(Vector3i(x, y - 1, z), Blocks.asmTable)
			world.replaceBlockToAir(slavePos + Vector3i(0, -1, 0), Blocks.asmTable)
		}

	// Block Configurations //
	override val getMobilityFlag = 2
	override def getItemDropped(meta: Int, randomizer: Random, quantity: Int) = null

	private def applyBlockBounds() = this.setBlockBounds(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f)
	// illegal adjacents -> break
	private final def breakOnIllegalAdjacents(world: World, thisPos: Vector3i, pairDir: Vector3i) =
		if(world.getBlock(thisPos.x + pairDir.x, thisPos.y, thisPos.z + pairDir.z) != this ||
			world.getBlock(thisPos.x, thisPos.y - 1, thisPos.z) != Blocks.asmTable ||
			world.getBlock(thisPos.x + pairDir.x, thisPos.y - 1, thisPos.z + pairDir.z) != Blocks.asmTable)
				world.setBlockToAir(thisPos.x, thisPos.y, thisPos.z)
}
