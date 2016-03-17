import std.stdio;
import imageformats;
import std.algorithm, std.range, std.typecons, std.conv;
import std.experimental.ndslice;
import noise;

void main(string[] args)
{
	if(args.length <= 1)
	{
		writeln("makeContainer for Fluid Mechanics");
		writeln("Container backimage factory.");
	}

	auto pixels = new float[256 * 256 * 3].sliced(256, 256, 3);
	int currentOffsetX = 0, currentOffsetY = 0;

	void placeSlot(int x, int y, int w, int h)
	{
		pixels[y .. y + h, x .. x + w, 0 .. $] = 0.75f * 0.875f;
		pixels[y - 1, x - 1 .. x + w + 1, 0 .. $] = 0.75f * 0.6875f;
		pixels[y .. y + h + 1, x - 1, 0 .. $] = 0.75f * 0.6875f;
		pixels[y + h, x - 1 .. x + w + 1, 0 .. $] = 0.75f * 1.125f;
		pixels[y - 1 .. y + h, x + w, 0 .. $] = 0.75f * 1.125f;
	}
	immutable void delegate(const string[])[string] instructionProcessors =
	[
		"surface": (const string[] args)
		{
			immutable w = args[0].to!int, h = args[1].to!int;

			writeln("initializing surface with size ", w, "x", h);
			pixels[] = cast(ubyte)0xff;
			pixels[0 .. h, 0 .. w, 0 .. $] = 0.75f;
			// horizontal lighting
			pixels[0, 0 .. w, 0 .. $] *= 1.125f;
			pixels[1, 0 .. w - 1, 0 .. $] *= 1.125f;
			// vertical lighting
			pixels[2 .. h, 0, 0 .. $] *= 1.125f;
			pixels[2 .. h - 1, 1, 0 .. $] *= 1.125f;
			// horizontal shadowing
			pixels[h - 1, 0 .. w, 0 .. $] *= 0.6875f;
			pixels[h - 2, 1 .. w, 0 .. $] *= 0.6875f;
			// vertical shadowing
			pixels[0 .. h - 2, w - 1, 0 .. $] *= 0.6875f;
			pixels[1 .. h - 2, w - 2, 0 .. $] *= 0.6875f;
			// effect
			foreach(x; 0 .. w) foreach(y; 0 .. h)
			{
				pixels[y, x, 0 .. $] *= noise.turbulence(x, y, 4) * 0.09375 + 0.90625;
			}
		},
		"slot": (const string[] args)
		{
			immutable x = currentOffsetX + args[0].to!int, y = currentOffsetY + args[1].to!int,
				w = args[2].to!int, h = args[3].to!int;
			placeSlot(x, y, w, h);
		},
		"slotMatrix": (const string[] args)
		{
			// slotMatrix startX, startY, w, h, countX, countY, stepX, stepY
			immutable startX = currentOffsetX + args[0].to!int, startY = currentOffsetY + args[1].to!int;
			immutable w = args[2].to!int, h = args[3].to!int, countX = args[4].to!int, countY = args[5].to!int;
			immutable stepX = args[6].to!int, stepY = args[7].to!int;

			for(int y = 0; y < countY; y++)
			{
				for(int x = 0; x < countX; x++)
				{
					placeSlot(startX + x * stepX, startY + y * stepY, w, h);
				}
			}
		},
		"arrow": (const string[] args)
		{
			// arrow x, y
			immutable x = currentOffsetX + args[0].to!int, y = currentOffsetY + args[1].to!int;

			pixels[y + 6 .. y + 10, x .. x + 10, 0 .. $] *= 0.875f;
			pixels[y + 2 .. y + 14, x + 10 .. x + 13, 0 .. $] *= 0.875f;
			for(int i = 0; i < 5; i++)
			{
				pixels[y + 3 + i .. y + 13 - i, x + 13 + i, 0 .. $] *= 0.875f;
			}
		},
		"offset": (const string[] args)
		{
			currentOffsetX = args[0].to!int;
			currentOffsetY = args[1].to!int;
		},
		"output": (const string[] args)
		{
			writeln("writing to ", args[0], "...");
			auto write_data = new ubyte[256 * 256 * 3];
			write_data[] = (&pixels[0, 0, 0])[0 .. pixels.elementsCount].map!(x => cast(ubyte)(x.clamp(0.0f, 1.0f) * 255.0f)).array;
			/*for(i, pixel_f; (&pixels[0, 0, 0])[0 .. pixels.elementsCount])
			{
				write_data[i] = cast(ubyte)(pixel_f.clamp(0.0f, 1.0f) * 255.0f);
			}*/
			write_image(args[0], 256, 256, write_data, ColFmt.RGB);
		}
	];

	writeln("processing ", args, "...");
	foreach(i, line; File(args[1]).byLine.enumerate(1).filter!(x => !x.value.empty && x.value.front != '#')
		.map!(x => tuple(x.index, x.value.idup)))
	{
		immutable instructionLength = line.countUntilSpace();
		const instruction = line[0 .. instructionLength];
		const arguments = line[instructionLength .. $].dropSpaces().split(",").map!dropSpaces.array;
		auto iter = instruction in instructionProcessors;
		if(iter !is null) (*iter)(arguments);
		else writeln("[?]unknown instruction: ", instruction, " on line ", i);
	}
}

/// Returns number of characters until space
size_t countUntilSpace(const string input)
{
	return input.front != ' ' ? input[1 .. $].countUntilSpace + 1 : 0;
}
/// Drop spaces and return dropped
const(string) dropSpaces(const string input)
{
	return input.front == ' ' ? input[1 .. $].dropSpaces : input;
}
