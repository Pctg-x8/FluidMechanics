package com.cterm2.mcfm1710.utils

import net.minecraft.entity.player.EntityPlayer, net.minecraft.world.World

final case class BlockPlacementInfo(val player: EntityPlayer, val world: World, val pos: Vector3i)
