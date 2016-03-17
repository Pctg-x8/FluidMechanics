package com.cterm2.mcfm1710.utils

object LocalTranslationUtils
{
	// "t" interpolator
	implicit class TranslatedStringHelper(val s: StringContext) extends AnyVal
	{
		import net.minecraft.util.StatCollector

		def t(args: Any*) = StatCollector.translateToLocal(s.parts.iterator.next)
	}
}
