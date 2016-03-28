package com.cterm2.mcfm1710.utils

object DeobfuscatorSupport
{
	import net.minecraft.item.ItemBlock

	implicit class ForItemBlock(val ib: ItemBlock) extends AnyVal
	{
		def block = ib.field_150939_a
	}
}
