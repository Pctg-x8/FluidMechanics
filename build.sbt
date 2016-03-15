name := "mcfm1710"
version := "1.0"
organization := "com.cterm2"
scalaVersion := "2.11.8"
minecraftVersion := "1.7.10"
forgeVersion := "10.13.4.1614"
buildName := "FluidMechanics-mc1710-1341614"

scalaSource in Compile := baseDirectory.value / "src"
lazy val srcJarVersionSignature = Def.setting { Seq(minecraftVersion.value, forgeVersion.value, minecraftVersion.value).mkString("-") }
lazy val gradleCaches = file(System.getProperty("user.home")) / ".gradle" / "caches"
lazy val forgeSources = Def.setting { gradleCaches / "minecraft" / "net" / "minecraftforge" / "forge" / srcJarVersionSignature.value }
unmanagedJars in Compile ++= Seq(
	forgeSources.value / ("forgeSrc-" + srcJarVersionSignature.value + ".jar"),
	baseDirectory.value / "build" / "tmp" / "deobfuscateJar" / "deobfed.jar"
)
libraryDependencies ++= Seq(
	"org.apache.logging.log4j" % "log4j-api" % "2.0-beta9",
	"org.apache.logging.log4j" % "log4j-core" % "2.0-beta9",
	"com.google.code.gson" % "gson" % "2.2.4",
	"org.lwjgl.lwjgl" % "lwjgl" % "2.9.1",
	"org.lwjgl.lwjgl" % "lwjgl_util" % "2.9.1",
	"org.apache.commons" % "commons-compress" % "1.8.1",
	"org.apache.commons" % "commons-lang3" % "3.3.2",
	"com.google.guava" % "guava" % "17.0",
	"commons-logging" % "commons-logging" % "1.1.3",
	"commons-io" % "commons-io" % "2.4",
	"com.mojang" % "authlib" % "1.5.16",
	"tv.twitch" % "twitch" % "5.16",
	"com.ibm.icu" % "icu4j-core-mojang" % "51.2",
	"io.netty" % "netty-all" % "4.0.10.Final",
	"com.paulscode" % "soundsystem" % "20120107",
	"com.paulscode" % "libraryjavasound" % "20101123",
	"com.paulscode" % "librarylwjglopenal" % "20100824",
	"com.paulscode" % "codecjorbis" % "20101023",
	"com.paulscode" % "codecwav" % "20101023",
	"java3d" % "vecmath" % "1.3.1",
	"net.minecraft" % "launchwrapper" % "1.12",
	"net.sf.trove4j" % "trove4j" % "3.0.3",
	"net.minecraftforge.gradle" % "ForgeGradle" % "1.2-SNAPSHOT"
)
resolvers += "forge" at "http://files.minecraftforge.net/maven"
resolvers += "minecraft" at "https://libraries.minecraft.net/"

scalacOptions ++= Seq("-deprecation", "-encoding", "UTF8")

/*lazy val runClient = taskKey[Unit]("Run Minecraft as client")
runClient in Compile := {
	(compile in Compile).value
	System.out.println("Running Client...")
}*/

import java.io.File

fork := true
envVars := Map(
	"fml.ignoreInvalidMinecraftCertificates" -> "true",
	"net.minecraftforge.gradle.GradleStart.srgDir" -> (new File("@@SRGDIR@@")).getCanonicalPath()
)
mainClass in Compile := Some("net.minecraft.launchwrapper.Launch")
run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))

/// Custom Settings ///
lazy val minecraftVersion = settingKey[String]("Target Minecraft Version")
lazy val forgeVersion = settingKey[String]("Target Forge Version")
lazy val buildName = settingKey[String]("Build Filename")
