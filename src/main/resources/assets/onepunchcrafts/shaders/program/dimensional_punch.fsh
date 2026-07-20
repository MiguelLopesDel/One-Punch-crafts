#version 330 core

// Dimensional Punch screen pass. All timeline math happens Java-side; this
// shader shatters the frame around the impact point: jagged radial cracks that
// refract and chromatically split the world, a glowing violet/cyan seam along
// the fractures, and the other dimension bleeding through the open rift.

uniform sampler2D DiffuseSampler;

uniform vec2 OutSize;
uniform vec2 FocusPoint;
uniform float FocusValid;
uniform float CrackAmount;
uniform float RiftOpen;
uniform float Distort;
uniform float Aberration;
uniform float Flash;

in vec2 texCoord;
out vec4 fragColor;

float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

// Jagged radial crack field emanating from the focus point.
float cracks(vec2 relA) {
    float ang = atan(relA.y, relA.x);
    float r = length(relA);
    float field = 0.0;
    for (int i = 0; i < 6; i++) {
        float a = hash(float(i) * 1.7) * 6.2831853;
        float d = abs(atan(sin(ang - a), cos(ang - a)));           // angular distance 0..pi
        float jag = 0.04 + 0.05 * hash(float(i) * 3.1 + floor(ang * 4.0));
        float line = smoothstep(jag, 0.0, d);
        float reach = smoothstep(0.0, 0.02, r) * (1.0 - smoothstep(0.12, 0.55, r));
        field = max(field, line * reach);
    }
    return field;
}

void main() {
    float aspect = OutSize.x / max(OutSize.y, 1.0);
    vec2 focus = mix(vec2(0.5), FocusPoint, FocusValid);

    vec2 rel = texCoord - focus;
    vec2 relA = vec2(rel.x * aspect, rel.y);
    float dist = length(relA);
    vec2 dirN = dist > 1.0e-5 ? relA / dist : vec2(0.0);

    float crack = cracks(relA) * clamp(CrackAmount, 0.0, 1.0) * FocusValid;

    // Refraction: displace along the cracks and radially near the rift.
    float disp = Distort * (crack * 0.5 + exp(-dist * 3.0) * 0.4) * FocusValid;
    vec2 sampleUv = texCoord + vec2(dirN.x / max(aspect, 1.0e-3), dirN.y) * disp;
    sampleUv = clamp(sampleUv, vec2(0.001), vec2(0.999));

    // Chromatic split along the radial direction, strongest on the cracks.
    vec2 ca = dirN * (Aberration * (0.4 + crack)) / OutSize;
    vec3 color;
    color.r = texture(DiffuseSampler, clamp(sampleUv + ca, vec2(0.001), vec2(0.999))).r;
    color.g = texture(DiffuseSampler, sampleUv).g;
    color.b = texture(DiffuseSampler, clamp(sampleUv - ca, vec2(0.001), vec2(0.999))).b;

    // Glowing seam along the fractures — violet fading to cyan.
    vec3 seam = mix(vec3(0.60, 0.30, 1.0), vec3(0.40, 0.90, 1.0),
                    hash(floor(atan(relA.y, relA.x) * 3.0)));
    color += seam * crack * 0.8;

    // The other dimension bleeding through the open rift.
    float voidMask = clamp(RiftOpen, 0.0, 1.0) * FocusValid * (1.0 - smoothstep(0.0, 0.18, dist));
    color = mix(color, vec3(0.25, 0.10, 0.45), voidMask * 0.75);
    float rim = clamp(RiftOpen, 0.0, 1.0) * FocusValid * exp(-pow((dist - 0.14) * 16.0, 2.0));
    color += vec3(0.70, 0.50, 1.0) * rim;

    // Impact flash, a hair over 1.0.
    color = mix(color, vec3(1.04), clamp(Flash, 0.0, 1.0));

    fragColor = vec4(color, 1.0);
}
