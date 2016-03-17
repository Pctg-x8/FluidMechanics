module noise;

// Noise Generator
import std.experimental.ndslice, std.random, std.range;

// preconfig
immutable noiseWidth = 256;
immutable noiseHeight = 256;
immutable real[] noiseBuffer;
static this()
{
	noiseBuffer = new real[256 * 256];
	foreach(ref e; noiseBuffer) e = uniform01!real;
}

auto noise(real x, real y)
{
	auto noiseBase = noiseBuffer.sliced(256, 256);

	// get fractional part of x and y
	immutable fracX = x - cast(int)x;
	immutable fracY = y - cast(int)y;

	// wrap around
	immutable x1 = (cast(int)x + noiseWidth) % noiseWidth;
	immutable y1 = (cast(int)y + noiseHeight) % noiseHeight;

	// neighbor values
	immutable x2 = (x1 + noiseWidth - 1) % noiseWidth;
	immutable y2 = (y1 + noiseHeight - 1) % noiseHeight;

	// smooth the noise with bilinear interpolation
	return fracX * fracY * noiseBase[y1, x1]
		+ (1.0 - fracX) * fracY * noiseBase[y1, x2]
		+ fracX * (1.0 - fracY) * noiseBase[y2, x1]
		+ (1.0 - fracX) * (1.0 - fracY) * noiseBase[y2, x2];
}
auto turbulence(real x, real y, real initScale)
{
	real value = 0;

	for(auto scale = initScale; scale >= 1; scale /= 2.0)
	{
		value += noise(x / scale, y / scale) * scale;
	}
	return 0.5 * value / initScale;
}
