#version 330 core

// Boros — Collapsing Star Roaring Cannon.
// Fullscreen raymarched post effect inspired by Mishkis' Orbital Railgun:
// world position is reconstructed from the depth buffer and an SDF scene
// (white-hot core beam + twin golden spiral streams + collapsing star at the
// muzzle + impact dome) is marched on top of the rendered frame.

#define STEPS 170
#define MIN_DIST 0.0025
#define MAX_DIST 1400.0

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform mat4 InvProjMat;
uniform mat4 InvViewMat;
uniform mat4 WorldToClip;

uniform vec3 CameraPosition;
uniform vec3 StartPosition;
uniform vec3 BeamDirection;
uniform vec3 ImpactPosition;

uniform float BeamRange;
uniform float ImpactRadius;
uniform float iTime;
uniform float ChargeTime;
uniform float FireTime;
uniform vec2 OutSize;

in vec2 texCoord;
out vec4 fragColor;

const vec3 CORE_WHITE = vec3(1.00, 0.97, 0.90);
const vec3 BOROS_BLUE = vec3(0.45, 0.80, 1.00);
const vec3 BOROS_GOLD = vec3(1.00, 0.70, 0.26);
const vec3 DEEP_ORANGE = vec3(1.00, 0.45, 0.12);
const float AFTERGLOW_TIME = 2.4;

// Beam frame, filled in main().
vec3 beamX = vec3(1.0, 0.0, 0.0);
vec3 beamY = vec3(0.0, 1.0, 0.0);
vec3 beamZ = vec3(0.0, 0.0, 1.0);
float fireP = 0.0;
float aftP = 0.0;
float envelope = 0.0;
float front = 0.0;
vec3 impactRel = vec3(0.0);

float smoothMin(float a, float b, float k) {
    float diff = a - b;
    return 0.5 * (a + b - sqrt(diff * diff + k * k * k));
}

vec2 rotate2(vec2 p, float r) {
    float c = cos(r);
    float s = sin(r);
    return mat2(c, -s, s, c) * p;
}

float hash31(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453);
}

float coreRadius(float x) {
    float grow = mix(2.3, 7.2, smoothstep(0.0, 1.0, x / max(BeamRange, 1.0)));
    float pulse = 0.93 + 0.07 * sin(iTime * 21.0 - x * 0.11);
    return grow * envelope * pulse;
}

float sdCore(vec3 q) {
    if (envelope <= 0.002) return 1.0e5;
    float x = clamp(q.x, 0.0, front);
    float d = length(q.yz) - coreRadius(x);
    d = max(d, q.x - front);
    d = max(d, -q.x - 3.0);
    return d;
}

float sdSpiral(vec3 q) {
    if (envelope <= 0.002) return 1.0e5;
    float x = clamp(q.x, 0.0, front);
    float rr = coreRadius(x);

    // Primary golden pair, twisting forward.
    vec2 yz = rotate2(q.yz, q.x * 0.34 - iTime * 9.0);
    float rs = rr * 1.75 + 1.0;
    float ribbon = rr * 0.30 + 0.42;
    float d1 = min(length(yz - vec2(rs, 0.0)), length(yz + vec2(rs, 0.0))) - ribbon;

    // Counter-rotating deeper-orange pair, wider and thinner.
    vec2 yz2 = rotate2(q.yz, -q.x * 0.22 + iTime * 6.5 + 1.57);
    float rs2 = rr * 2.25 + 1.6;
    float d2 = min(length(yz2 - vec2(rs2, 0.0)), length(yz2 + vec2(rs2, 0.0))) - ribbon * 0.62;

    float d = min(d1, d2);
    d = max(d, q.x - front);
    d = max(d, -q.x - 2.0);
    return d;
}

float sdStar(vec3 p) {
    float chargeP = clamp(iTime / max(ChargeTime, 0.001), 0.0, 1.0);
    float starR;
    if (iTime < ChargeTime) {
        starR = 0.5 + 2.9 * pow(chargeP, 1.6);
    } else {
        starR = (3.4 - fireP * 1.6) * (1.0 - aftP);
    }
    if (starR <= 0.05) return 1.0e5;
    return length(p) - starR;
}

float sdImpact(vec3 p) {
    float arrive = clamp((fireP - 0.70) / 0.30, 0.0, 1.0);
    if (arrive <= 0.001) return 1.0e5;
    float impR = ImpactRadius * 0.52 * arrive * (1.0 - aftP * 0.9);
    impR *= 0.95 + 0.05 * sin(iTime * 17.0);
    return length(p - impactRel) - impR;
}

float sDist(vec3 p) {
    vec3 q = vec3(dot(p, beamX), dot(p, beamY), dot(p, beamZ));
    float d = smoothMin(sdCore(q), sdSpiral(q), 2.2);
    d = smoothMin(d, sdStar(p), 3.0);
    d = smoothMin(d, sdImpact(p), 5.0);
    return d;
}

// Returns (distance traveled, near-surface step count, closest approach).
vec3 raycast(vec3 point, vec3 dir) {
    float traveled = 0.0;
    float closeSteps = 0.0;
    float minSafe = 1.0e5;
    for (int i = 0; i < STEPS; i++) {
        float safe = sDist(point);
        minSafe = min(minSafe, safe);
        if (safe <= MIN_DIST || traveled >= MAX_DIST) {
            break;
        }
        traveled += safe;
        point += dir * safe;
        if (safe <= 0.35) {
            closeSteps += 1.0;
        }
    }
    return vec3(traveled, closeSteps, minSafe);
}

vec3 worldPos(vec3 point) {
    vec3 ndc = point * 2.0 - 1.0;
    vec4 homPos = InvProjMat * vec4(ndc, 1.0);
    vec3 viewPos = homPos.xyz / homPos.w;
    return (InvViewMat * vec4(viewPos, 1.0)).xyz + CameraPosition;
}

void main() {
    float depth = texture(DepthSampler, texCoord).r;
    float chargeP = clamp(iTime / max(ChargeTime, 0.001), 0.0, 1.0);
    float localFire = iTime - ChargeTime;
    fireP = clamp(localFire / max(FireTime, 0.001), 0.0, 1.0);
    aftP = clamp((localFire - FireTime) / AFTERGLOW_TIME, 0.0, 1.0);
    envelope = smoothstep(0.0, 0.10, fireP) * pow(1.0 - aftP, 1.7);
    front = BeamRange * clamp(fireP * 1.30, 0.0, 1.0);

    beamX = normalize(BeamDirection);
    vec3 ref = abs(beamX.y) > 0.98 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 0.0);
    beamY = normalize(cross(ref, beamX));
    beamZ = cross(beamX, beamY);
    impactRel = ImpactPosition - StartPosition;

    // Everything below works relative to the muzzle to keep numbers small.
    vec3 startPoint = CameraPosition - StartPosition;
    vec3 endPoint = worldPos(vec3(texCoord, depth)) - StartPosition;
    vec3 dir = normalize(endPoint - startPoint);
    float sceneDist = length(endPoint - startPoint);

    vec2 aspect = vec2(OutSize.x / max(OutSize.y, 1.0), 1.0);

    // ------------------------------------------------------------------
    // CHARGE: the star collapses at the muzzle, bending light toward it.
    // ------------------------------------------------------------------
    if (iTime < ChargeTime) {
        vec2 uv = texCoord;
        vec4 clip = WorldToClip * vec4(StartPosition - CameraPosition, 1.0);
        float muzzleVisible = 0.0;
        vec2 uvm = vec2(0.5);
        if (clip.w > 0.0) {
            uvm = (clip.xyz / clip.w).xy * 0.5 + 0.5;
            muzzleVisible = 1.0;
            vec2 delta = (texCoord - uvm) * aspect;
            float len = length(delta) + 1.0e-4;
            float lens = 0.05 * pow(chargeP, 2.3) * exp(-len * 2.4);
            uv = texCoord - (delta / aspect) / len * lens;
        }
        vec3 original = texture(DiffuseSampler, uv).rgb;

        // The world dims and cools while the star drinks the light in.
        float dim = 1.0 - 0.72 * pow(chargeP, 1.7);
        vec3 col = original * mix(vec3(1.0), vec3(0.58, 0.66, 0.92), chargeP * 0.55) * dim;

        // Analytic star: distance from the view ray to the muzzle point.
        float tProj = max(dot(-startPoint, dir), 0.0);
        float dRay = length(startPoint + dir * tProj);
        float occluded = step(sceneDist, tProj);
        float starR = 0.5 + 2.9 * pow(chargeP, 1.6);
        float flicker = 0.9 + 0.1 * sin(iTime * 27.0) * sin(iTime * 13.7);

        float halo = starR * (0.10 + 0.55 * chargeP) / pow(max(dRay - starR * 0.45, 0.05), 1.35);
        float disc = smoothstep(starR, starR * 0.5, dRay);
        vec3 starCol = mix(BOROS_BLUE, CORE_WHITE, 0.35 + 0.5 * chargeP);
        col += (1.0 - occluded) * flicker * (starCol * halo * 0.16 + CORE_WHITE * disc * (0.8 + 1.6 * chargeP));

        // Golden motes streaming inward.
        if (muzzleVisible > 0.5) {
            vec2 delta = (texCoord - uvm) * aspect;
            float len = length(delta);
            float ang = atan(delta.y, delta.x);
            float dash = pow(max(0.0, sin(ang * 9.0 + len * 14.0)), 26.0);
            dash *= pow(fract(-len * 3.0 + iTime * 2.1), 3.0);
            col += BOROS_GOLD * dash * smoothstep(0.7, 0.06, len) * chargeP * 0.55;
        }

        // Energy orbs materializing around Boros and sinking into him,
        // staggered so new ones keep appearing through the whole charge.
        for (int i = 0; i < 8; i++) {
            float fi = float(i);
            float orbP = clamp(chargeP * 1.55 - fi * 0.11, 0.0, 1.0);
            if (orbP <= 0.001 || orbP >= 0.985) continue;

            float orbAng = fi * 2.39996 + iTime * (0.9 + 0.13 * fi);
            float orbRad = (7.5 + 2.0 * sin(fi * 1.7)) * pow(1.0 - orbP, 1.4);
            vec3 orbPos = (beamY * cos(orbAng) + beamZ * sin(orbAng)) * orbRad
                    + beamX * (0.6 - 1.8 * (1.0 - orbP))
                    + vec3(0.0, sin(fi * 2.1 + iTime * 1.3) * 1.2 * (1.0 - orbP), 0.0);

            float tOrb = max(dot(orbPos - startPoint, dir), 0.0);
            float dOrb = length(startPoint + dir * tOrb - orbPos);
            float orbR = 0.35 + 0.55 * orbP;
            float orbGlow = orbR * 0.35 / (pow(max(dOrb - orbR * 0.3, 0.02), 1.6) + 0.01);
            vec3 orbCol = mod(fi, 2.0) < 1.0 ? BOROS_GOLD : BOROS_BLUE;
            col += (1.0 - step(sceneDist, tOrb)) * orbCol * min(orbGlow, 4.0) * (0.25 + 0.75 * orbP);
        }

        // Held breath right before release: the world goes black.
        float blackout = pow(clamp((chargeP - 0.86) / 0.14, 0.0, 1.0), 2.0);
        col = mix(col, vec3(0.0), blackout * 0.92);
        col += CORE_WHITE * disc * blackout * (1.0 - occluded) * 2.5;

        fragColor = vec4(col, 1.0);
        return;
    }

    // ------------------------------------------------------------------
    // FIRE + AFTERGLOW: raymarch the beam over the scene.
    // ------------------------------------------------------------------
    vec3 original = texture(DiffuseSampler, texCoord).rgb;

    vec3 hit = raycast(startPoint, dir);
    vec3 hitPoint = startPoint + dir * hit.x;

    float threshold = step(sDist(hitPoint), MIN_DIST * 3.0);
    threshold *= step(hit.x, sceneDist);
    threshold *= 1.0 - aftP;

    // Which part of the scene did we hit?
    vec3 hq = vec3(dot(hitPoint, beamX), dot(hitPoint, beamY), dot(hitPoint, beamZ));
    float dCore = sdCore(hq);
    float dSpiral = sdSpiral(hq);
    float dOther = min(sdStar(hitPoint), sdImpact(hitPoint));
    float rim = smoothstep(4.0, 16.0, hit.y);

    vec3 beamCol;
    if (dCore <= dSpiral && dCore <= dOther) {
        beamCol = CORE_WHITE * (2.0 + 1.1 * exp(-hq.x * 0.012));
        beamCol = mix(beamCol, BOROS_BLUE * 1.6, rim * 0.75);
    } else if (dSpiral <= dOther) {
        beamCol = mix(BOROS_GOLD, DEEP_ORANGE, 0.5 + 0.5 * sin(hq.x * 0.2 - iTime * 5.0)) * 1.7;
        beamCol = mix(beamCol, CORE_WHITE * 1.8, rim * 0.5);
        // Glitter crawling along the golden streams.
        float sparkle = pow(hash31(floor(hq * 1.5) + vec3(floor(iTime * 9.0))), 8.0);
        beamCol *= 1.0 + sparkle * 1.4;
    } else {
        beamCol = mix(CORE_WHITE, BOROS_GOLD, 0.35) * 2.1;
        beamCol = mix(beamCol, BOROS_BLUE * 1.5, rim * 0.4);
    }

    // World shading: the landscape is lit by the beam passing above it.
    float proj = clamp(dot(endPoint, beamX), 0.0, max(front, 0.001));
    float dLine = length(endPoint - beamX * proj);
    vec3 lineLight = (BOROS_GOLD * 0.6 + BOROS_BLUE * 0.4) * envelope * 16.0 / (pow(dLine, 1.35) + 7.0);

    float dim = mix(1.0, 0.34, envelope * (1.0 - aftP * 0.7));
    vec3 worldCol = original * (vec3(dim) + lineLight);

    // Additive halo: rays that pass close to the beam without hitting it
    // pick up a soft golden-white corona, giving the beam real volume.
    float halo = exp(-max(hit.z, 0.0) * 0.16) * envelope;
    worldCol += (BOROS_GOLD * 0.55 + CORE_WHITE * 0.45) * halo * 1.15;

    // Shockwaves once the beam reaches the far end.
    float arriveT = localFire - FireTime * 0.70;
    if (arriveT > 0.0) {
        float dImp = distance(endPoint, impactRel);
        float fadeImpact = clamp(1.0 - aftP, 0.0, 1.0);
        float wave1 = 16.0 / max(abs(dImp - 95.0 * arriveT), 2.0);
        float wave2 = 6.0 / max(abs(dImp - 48.0 * arriveT), 1.5);
        float crater = 90.0 / (pow(dImp, 1.45) + 14.0);
        worldCol += original * (wave1 * 0.9 + wave2 * 0.7) * fadeImpact * (BOROS_GOLD * 0.7 + CORE_WHITE * 0.3);
        worldCol += (DEEP_ORANGE * 0.75 + BOROS_GOLD * 0.25) * crater * fadeImpact * 0.35;
        worldCol = mix(worldCol, CORE_WHITE, clamp(smoothstep(dImp - 14.0, dImp, 120.0 * arriveT) * fadeImpact * 0.4, 0.0, 1.0));
    }

    vec3 col = mix(worldCol, beamCol, threshold);

    // Release flash: one blinding frame-filling burst as the star fires.
    float flash = exp(-max(localFire, 0.0) * 6.5);
    col = mix(col, CORE_WHITE * 1.25, clamp(flash, 0.0, 1.0) * 0.9);

    fragColor = vec4(col, 1.0);
}
