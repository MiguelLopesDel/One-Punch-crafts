#version 330 core

// Serious Punch screen pass. All timeline math happens Java-side; this shader
// just applies the requested amounts:
//   - windup: desaturation + space compressed toward the fist + vignette
//   - impact: few-frame white flash, lens ripple racing outward from the
//     fist, chromatic aberration spike

uniform sampler2D DiffuseSampler;

uniform vec2 OutSize;
uniform vec2 FocusPoint;
uniform float FocusValid;
uniform float Desaturate;
uniform float CompressAmount;
uniform float FlashAmount;
uniform float RippleProgress;
uniform float RippleAmp;
uniform float Aberration;
uniform float VignetteAmount;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    float aspect = OutSize.x / max(OutSize.y, 1.0);
    vec2 focus = mix(vec2(0.5), FocusPoint, FocusValid);

    vec2 rel = texCoord - focus;
    vec2 relA = vec2(rel.x * aspect, rel.y);
    float dist = length(relA);
    vec2 dirN = dist > 1.0e-5 ? relA / dist : vec2(0.0);

    // Windup: pixels pulled toward the fist, strongest close to it.
    float displacement = -CompressAmount * exp(-dist * 2.4) * FocusValid;

    // Impact: a single shock ring pushing the image outward as it passes.
    float ring = dist - RippleProgress * 1.35;
    displacement += RippleAmp * exp(-ring * ring * 260.0) * FocusValid;

    vec2 sampleUv = texCoord + vec2(dirN.x / max(aspect, 1.0e-3), dirN.y) * displacement;
    sampleUv = clamp(sampleUv, vec2(0.001), vec2(0.999));

    // Chromatic aberration along the radial direction (pixels).
    vec2 ca = dirN * Aberration / OutSize;
    vec3 color;
    color.r = texture(DiffuseSampler, clamp(sampleUv + ca, vec2(0.001), vec2(0.999))).r;
    color.g = texture(DiffuseSampler, sampleUv).g;
    color.b = texture(DiffuseSampler, clamp(sampleUv - ca, vec2(0.001), vec2(0.999))).b;

    // The world loses its colors while the punch is being held back.
    float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(color, vec3(luma) * 0.92, clamp(Desaturate, 0.0, 1.0));

    // Vignette pressing in from the edges.
    vec2 centered = texCoord - vec2(0.5);
    float edge = dot(centered, centered) * 2.0;
    color *= 1.0 - VignetteAmount * edge;

    // The white flash: only a few frames, slightly over 1.0 for punch.
    color = mix(color, vec3(1.04), clamp(FlashAmount, 0.0, 1.0));

    fragColor = vec4(color, 1.0);
}
