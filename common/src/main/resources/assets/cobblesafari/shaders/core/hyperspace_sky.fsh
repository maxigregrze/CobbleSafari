#version 150

// Sky of Hyperspace Lumiose (action plan 197, section 4.2). Layers, back to front: dark background,
// slow RGB nebula waves, coloured stars, the overhead disc showing the overworld map in fish-eye with a
// rainbow sheen turning along the inside of its edge, its iridescent rim, rainbow waves leaving the rim
// outwards and the sparkles running along it.

uniform sampler2D Sampler0; // overworld map, 256x256, alpha 0 = no data (MapMode 1)
uniform sampler2D Sampler1; // overlay only: depth just before translucent terrain
uniform sampler2D Sampler2; // overlay only: final scene depth

uniform mat4 InvViewProj;
uniform float CsTime;   // seconds, wraps every 3600: every animation below is periodic over it
uniform int MapMode;    // 0 = no map (haze only), 1 = map in Sampler0
uniform int OverlayMode; // 1 = drawn after the Iris pipeline, sky pixels picked from the depth copies

in vec2 texCoord;
in vec2 ndc;

out vec4 fragColor;

const float PI = 3.14159265;
const float TAU = 6.28318531;

const vec3 BACKGROUND = vec3(0.01, 0.01, 0.03);
const vec3 HAZE = vec3(0.86, 0.88, 0.95);
const float DISC_RADIUS = radians(40.0);
const float RIM_WIDTH = radians(0.6);
const float RIM_SPREAD = radians(0.25);
const int SPARKLES = 10;
const float SHEEN_WIDTH = radians(7.0);
const float SHEEN_TURN_SECONDS = 40.0; // must divide 3600 so the loop has no jump
const float WAVE_SPACING = radians(3.0);
const float WAVE_SECONDS = 4.0;        // time for a wave to travel one spacing; must divide 3600
const float WAVES_PER_LOOP = 3600.0 / WAVE_SECONDS;
const float WAVE_REACH = radians(12.0);
const float WAVE_SPLIT = 0.07;         // per-channel offset of the waves, in spacings
const float BEHIND_TRANSLUCENT_ALPHA = 0.75;

// --- noise -----------------------------------------------------------------------------------

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}

vec3 hash33(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.xxy + p.yxx) * p.zyx);
}

float valueNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash13(i);
    float n100 = hash13(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash13(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash13(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash13(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash13(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash13(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash13(i + vec3(1.0, 1.0, 1.0));
    return mix(mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y),
               mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y), f.z);
}

float fbm(vec3 p) {
    float sum = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        sum += amplitude * valueNoise(p);
        p = p * 2.03 + vec3(1.7, 9.2, 4.1);
        amplitude *= 0.5;
    }
    return sum / 0.9375;
}

/** Hue in [0, 1) to a saturated colour. */
vec3 spectrum(float hue) {
    return clamp(abs(fract(hue + vec3(0.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0) - 1.0, 0.0, 1.0);
}

// --- layers ----------------------------------------------------------------------------------

vec3 nebula(vec3 dir, float phase) {
    // Circular drift through the noise. The multipliers of phase must stay whole numbers so the
    // drift loops without a jump; doubling them doubles the speed.
    vec3 drift = vec3(cos(phase * 12.0), sin(phase * 8.0), sin(phase * 12.0)) * 4.0;
    vec3 p = dir * 2.2 + drift;
    float warp = fbm(p * 0.8);
    float a = fbm(p + warp * 1.5);
    float b = fbm(p * 1.3 - drift * 0.5 + 7.0);
    float hue = a * 1.6 + b * 0.7 + phase / TAU; // one whole hue turn per loop: no jump at the wrap
    float intensity = smoothstep(0.45, 0.85, a) * 0.20 + smoothstep(0.55, 0.9, b) * 0.10;
    return spectrum(fract(hue)) * intensity;
}

vec3 stars(vec3 dir, float phase) {
    vec3 p = dir * 80.0;
    vec3 cell = floor(p);
    vec3 h = hash33(cell);
    if (h.x < 0.965) {
        return vec3(0.0);
    }
    vec3 starDir = normalize(cell + 0.2 + 0.6 * hash33(cell + 17.0));
    float angle = acos(clamp(dot(dir, starDir), -1.0, 1.0));
    float size = mix(0.0015, 0.004, h.y); // radians: one to three pixels at a usual FOV
    float glow = 1.0 - smoothstep(0.0, size, angle);
    float twinkle = 0.6 + 0.4 * sin(phase * 60.0 + h.z * TAU);
    vec3 tint = h.z < 0.55 ? vec3(1.0) : spectrum(h.z * 3.0) * 0.8 + 0.2;
    return tint * glow * twinkle;
}

/** One channel of the outer waves: arcs of a ring, the ring id picking which arcs are lit. */
float wave(float u, float azimuth) {
    // A ring keeps its id while it travels; mod keeps the ids identical across the loop wrap.
    float id = mod(floor(u), WAVES_PER_LOOP);
    float f = fract(u);
    float profile = smoothstep(0.0, 0.35, f) * (1.0 - smoothstep(0.35, 1.0, f));
    float arcs = smoothstep(0.42, 0.72, fbm(vec3(cos(azimuth) * 2.2, sin(azimuth) * 2.2, id * 3.7)));
    return profile * arcs;
}

/** Rainbow waves leaving the rim outwards, fading as they go. The channels are offset: chromatic aberration. */
vec3 outerWaves(float theta, float azimuth) {
    float below = theta - DISC_RADIUS;
    if (below <= 0.0 || below >= WAVE_REACH) {
        return vec3(0.0);
    }
    float u = below / WAVE_SPACING - CsTime / WAVE_SECONDS;
    vec3 colour = vec3(wave(u - WAVE_SPLIT, azimuth), wave(u, azimuth), wave(u + WAVE_SPLIT, azimuth));
    float fade = exp(-below / radians(4.0)) * (1.0 - smoothstep(WAVE_REACH * 0.7, WAVE_REACH, below));
    return colour * fade * 0.55;
}

/**
 * Rainbow sheen along the inside of the rim, over the map, turning around the disc. Returns the
 * colour and its weight; it whitens towards the rim so it blends into the white border.
 */
vec4 innerSheen(float theta, float azimuth) {
    float inside = DISC_RADIUS - theta;
    if (inside < 0.0 || inside >= SHEEN_WIDTH) {
        return vec4(0.0);
    }
    float depth = inside / SHEEN_WIDTH; // 0 at the rim, 1 at the inner edge of the sheen
    float spin = azimuth / TAU - CsTime / SHEEN_TURN_SECONDS;
    // Patches sampled on a circle: no seam where azimuth wraps.
    vec2 around = vec2(cos(spin * TAU), sin(spin * TAU)) * 1.8;
    float patches = smoothstep(0.3, 0.75, fbm(vec3(around, depth * 1.5)));
    vec3 colour = 0.45 + 0.55 * spectrum(fract(spin * 2.0 + depth * 0.8));
    colour = mix(colour, vec3(1.0), 1.0 - smoothstep(0.0, 0.25, depth));
    float weight = pow(1.0 - depth, 1.5) * (0.35 + 0.65 * patches) * 0.85;
    return vec4(colour, weight);
}

/** Map seen in fish-eye, as if glued on a sphere seen from below (section 4.2.1). */
vec3 discContent(float theta, float azimuth, float phase) {
    float rho = theta / DISC_RADIUS;
    vec3 colour = HAZE * 0.9;
    if (MapMode == 1) {
        float t = asin(clamp(rho, 0.0, 1.0)) / (PI * 0.5);
        vec2 uv = 0.5 + 0.5 * t * vec2(cos(azimuth), sin(azimuth));
        vec4 map = texture(Sampler0, uv);
        if (map.a > 0.5) {
            vec3 c = map.rgb;
            float luminance = dot(c, vec3(0.299, 0.587, 0.114));
            c = mix(c, vec3(luminance), 0.5);
            c = mix(c, vec3(1.0), 0.25);
            vec3 sweep = 0.6 + 0.6 * spectrum(fract(azimuth / TAU + phase / TAU * 2.0 + rho * 0.3));
            c *= mix(vec3(1.0), sweep, 0.35);
            colour = c;
        }
    }
    float haze = mix(0.15, 0.6, rho * rho);
    return mix(colour, HAZE, haze);
}

float ring(float theta, float radius) {
    float d = (theta - radius) / RIM_WIDTH;
    return exp(-d * d);
}

vec3 rim(float theta) {
    vec3 fringes = vec3(ring(theta, DISC_RADIUS + RIM_SPREAD), ring(theta, DISC_RADIUS), ring(theta, DISC_RADIUS - RIM_SPREAD));
    float core = ring(theta, DISC_RADIUS);
    float halo = exp(-abs(theta - DISC_RADIUS) / radians(3.0)) * 0.25;
    return fringes * 1.2 + vec3(core * 0.8) + vec3(halo);
}

vec3 sparkles(vec3 dir, float phase) {
    vec3 sum = vec3(0.0);
    for (int i = 0; i < SPARKLES; i++) {
        float fi = float(i);
        float speed = mod(fi, 3.0) + 1.0; // whole turns per loop: periodic
        float azimuth = TAU * fi / float(SPARKLES) + phase * speed + hash13(vec3(fi, 3.1, 7.7)) * 1.5;
        vec3 point = vec3(sin(DISC_RADIUS) * cos(azimuth), cos(DISC_RADIUS), sin(DISC_RADIUS) * sin(azimuth));
        float angle = acos(clamp(dot(dir, point), -1.0, 1.0));
        sum += vec3(1.0 - smoothstep(0.0, radians(0.5), angle));
    }
    return sum;
}

void main() {
    float alpha = 1.0;
    if (OverlayMode == 1) {
        if (texture(Sampler1, texCoord).r < 1.0) {
            discard;
        }
        if (texture(Sampler2, texCoord).r < 1.0) {
            alpha = BEHIND_TRANSLUCENT_ALPHA;
        }
    }

    vec4 world = InvViewProj * vec4(ndc, 1.0, 1.0);
    vec3 dir = normalize(world.xyz / world.w);
    float phase = CsTime / 3600.0 * TAU;

    float theta = acos(clamp(dir.y, -1.0, 1.0));
    float azimuth = atan(dir.z, dir.x);

    vec3 colour = BACKGROUND + nebula(dir, phase) + stars(dir, phase) + outerWaves(theta, azimuth);

    float discMask = 1.0 - smoothstep(DISC_RADIUS - radians(0.3), DISC_RADIUS, theta);
    if (discMask > 0.0) {
        colour = mix(colour, discContent(theta, azimuth, phase), discMask);
        vec4 sheen = innerSheen(theta, azimuth);
        colour = mix(colour, sheen.rgb, sheen.a * discMask);
    }
    colour += rim(theta) + sparkles(dir, phase);

    fragColor = vec4(colour, alpha);
}
