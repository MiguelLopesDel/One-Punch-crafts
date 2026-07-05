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
uniform float CasterView;
uniform float ReducedFlash;
uniform vec2 OutSize;

in vec2 texCoord;
out vec4 fragColor;

const vec3 CORE_WHITE = vec3(1.00, 0.97, 0.90);
const vec3 BOROS_BLUE = vec3(0.45, 0.80, 1.00);
const vec3 BOROS_GOLD = vec3(1.00, 0.70, 0.26);
const vec3 DEEP_ORANGE = vec3(1.00, 0.45, 0.12);
// Staggered afterglows: the beam dies first, the impact sphere lingers
// longer, and the shockwaves crawling over the landscape outlive both.
const float BEAM_AFTERGLOW = 2.8;
const float SPHERE_AFTERGLOW = 4.6;
const float ARCS_AFTERGLOW = 6.5;

// Beam frame, filled in main().
vec3 beamX = vec3(1.0, 0.0, 0.0);
vec3 beamY = vec3(0.0, 1.0, 0.0);
vec3 beamZ = vec3(0.0, 0.0, 1.0);
float fireP = 0.0;
float aftP = 0.0;
float aftSphere = 0.0;
float envelope = 0.0;
float front = 0.0;
// Caster view: hollow out the first blocks of the beam volumes so the
// first-person camera (which sits inside them) sees a tunnel, not a wall.
float nearCut = 0.0;
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
    d = max(d, nearCut - q.x);
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
    d = max(d, nearCut - q.x);
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
    float d = length(p) - starR;
    if (nearCut > 0.0) d = max(d, nearCut - dot(p, beamX));
    return d;
}

float sdImpact(vec3 p) {
    float arrive = clamp((fireP - 0.70) / 0.30, 0.0, 1.0);
    if (arrive <= 0.001) return 1.0e5;
    // The golden energy sphere outlives the beam (SPHERE_AFTERGLOW).
    float impR = ImpactRadius * 0.52 * arrive * (1.0 - aftSphere * 0.92);
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
    aftP = clamp((localFire - FireTime) / BEAM_AFTERGLOW, 0.0, 1.0);
    aftSphere = clamp((localFire - FireTime) / SPHERE_AFTERGLOW, 0.0, 1.0);
    float aftArcs = clamp((localFire - FireTime) / ARCS_AFTERGLOW, 0.0, 1.0);
    envelope = smoothstep(0.0, 0.10, fireP) * pow(1.0 - aftP, 1.7);
    front = BeamRange * clamp(fireP * 1.30, 0.0, 1.0);

    beamX = normalize(BeamDirection);
    vec3 ref = abs(beamX.y) > 0.98 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 0.0);
    beamY = normalize(cross(ref, beamX));
    beamZ = cross(beamX, beamY);
    nearCut = CasterView * 14.0;
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

        // Caster view: when the camera sits at the muzzle (first person) the
        // star would flood the whole screen — tone it down with proximity.
        float fpvStar = mix(1.0, smoothstep(0.8, 5.0, length(startPoint)), CasterView);

        float halo = starR * (0.10 + 0.55 * chargeP) / pow(max(dRay - starR * 0.45, 0.05), 1.35);
        float disc = smoothstep(starR, starR * 0.5, dRay);
        vec3 starCol = mix(BOROS_BLUE, CORE_WHITE, 0.35 + 0.5 * chargeP);
        col += (1.0 - occluded) * flicker * (starCol * halo * 0.16 + CORE_WHITE * disc * (0.8 + 1.6 * chargeP)) * fpvStar;

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

        // Blue and red energy bolts crossing Boros vertically while he
        // gathers power (the cut right before the heartbeat), jittering at
        // ~12 fps like hand-drawn lightning.
        float tEndB = max(ChargeTime, 0.001);
        float boltPhase = smoothstep(tEndB - 3.7, tEndB - 3.2, iTime)
                * (1.0 - smoothstep(tEndB - 2.5, tEndB - 2.2, iTime));
        if (boltPhase > 0.001 && muzzleVisible > 0.5) {
            float boltFrame = floor(iTime * mix(12.0, 3.0, ReducedFlash));
            float vFadeB = smoothstep(1.2, 0.3, abs(texCoord.y - uvm.y));
            for (int i = 0; i < 4; i++) {
                float bi = float(i);
                float jitter = (hash31(vec3(boltFrame, bi, 7.0)) - 0.5) * 0.26;
                float bx = (texCoord.x - uvm.x) * aspect.x - jitter;
                float bw = 0.007 + 0.007 * hash31(vec3(boltFrame, bi, 3.0));
                float bolt = pow(max(0.0, 1.0 - abs(bx) / bw), 2.0);
                float gateB = step(0.35, hash31(vec3(boltFrame, bi, 11.0)));
                vec3 boltCol = mod(bi, 2.0) < 1.0 ? vec3(0.30, 0.62, 1.00) : vec3(1.00, 0.20, 0.10);
                col += boltCol * bolt * gateB * boltPhase * vFadeB * 1.25;
            }
        }

        // ------------------------------------------------------------------
        // Anime charge finale (matched to the OPM cut and csrc_charge.ogg,
        // both anchored to the end of the charge): heartbeat double-thump ->
        // the world drops to black with only golden lines radiating from the
        // star while red/gold beams cross Boros vertically -> held breath.
        // ------------------------------------------------------------------
        float tEnd = max(ChargeTime, 0.001);

        // Heartbeat: two low thumps that squeeze the screen dark for a blink,
        // with a red pressure ring creeping in from the edges.
        float hb = exp(-55.0 * abs(iTime - (tEnd - 2.65)))
                 + exp(-55.0 * abs(iTime - (tEnd - 2.35)));
        hb = clamp(hb, 0.0, 1.0);
        float edgeCh = 2.0 * length(texCoord - vec2(0.5));
        col *= 1.0 - 0.5 * hb * mix(1.0, 0.6, ReducedFlash);
        col += vec3(0.42, 0.03, 0.02) * hb * pow(clamp(edgeCh, 0.0, 1.0), 1.6);

        // Blackout with yellow lines: Boros drawn in black, only his glowing
        // outlines remain, cut at ~12 fps like the show.
        float blackPhase = smoothstep(tEnd - 2.25, tEnd - 1.85, iTime);
        if (blackPhase > 0.001 && muzzleVisible > 0.5) {
            float frame12 = floor(iTime * mix(12.0, 3.0, ReducedFlash));
            float flick = 0.55 + 0.45 * step(0.5, fract(frame12 * 0.618));
            flick = mix(flick, 0.8, ReducedFlash);

            vec2 deltaB = (texCoord - uvm) * aspect;
            float lenB = length(deltaB) + 1.0e-4;
            float angB = atan(deltaB.y, deltaB.x);

            // Golden cracks radiating from the star, like the "eye" frames.
            float lines = pow(abs(sin(angB * 9.0 + frame12 * 1.7)), 42.0);
            lines *= smoothstep(1.05, 0.10, lenB);

            // Red and gold beams crossing Boros vertically.
            float vx = (texCoord.x - uvm.x) * aspect.x;
            float beamGold = pow(max(0.0, 1.0 - abs(vx) / 0.020), 3.0);
            float beamRedL = pow(max(0.0, 1.0 - abs(vx + 0.065) / 0.014), 3.0);
            float beamRedR = pow(max(0.0, 1.0 - abs(vx - 0.055) / 0.014), 3.0);
            float vFade = smoothstep(1.15, 0.25, abs(texCoord.y - uvm.y));

            vec3 dark = col * 0.08;
            dark += BOROS_GOLD * lines * 1.6 * flick;
            dark += BOROS_GOLD * beamGold * 1.35 * flick * vFade;
            dark += vec3(1.0, 0.16, 0.07) * (beamRedL + beamRedR) * 1.1 * flick * vFade;
            dark += CORE_WHITE * disc * (1.0 - occluded) * 1.8 * fpvStar;
            col = mix(col, dark, blackPhase * 0.94);
        }

        // Held breath right before release: the world goes black.
        float blackout = pow(clamp((chargeP - 0.86) / 0.14, 0.0, 1.0), 2.0);
        col = mix(col, vec3(0.0), blackout * 0.92);
        col += CORE_WHITE * disc * blackout * (1.0 - occluded) * 2.5 * fpvStar;

        fragColor = vec4(col, 1.0);
        return;
    }

    // ------------------------------------------------------------------
    // FIRE + AFTERGLOW: raymarch the beam over the scene.
    // ------------------------------------------------------------------
    vec3 original = texture(DiffuseSampler, texCoord).rgb;

    // Caster view: the star and beam volumes wrap the first-person camera,
    // so start the march a few blocks out and fade anything closer than
    // ~16 blocks — the caster sees the golden spirals corkscrewing away and
    // the impact sphere instead of a screen-filling wall of white.
    float nearOffset = CasterView * 6.0;
    vec3 marchStart = startPoint + dir * nearOffset;
    vec3 hit = raycast(marchStart, dir);
    float hitDist = hit.x + nearOffset;
    vec3 hitPoint = marchStart + dir * hit.x;

    float threshold = step(sDist(hitPoint), MIN_DIST * 3.0);
    threshold *= step(hitDist, sceneDist);
    threshold *= mix(1.0, smoothstep(6.0, 13.0, hitDist), CasterView);

    // Which part of the scene did we hit?
    vec3 hq = vec3(dot(hitPoint, beamX), dot(hitPoint, beamY), dot(hitPoint, beamZ));
    float dCore = sdCore(hq);
    float dSpiral = sdSpiral(hq);
    float dStar = sdStar(hitPoint);
    float dImp = sdImpact(hitPoint);
    float dOther = min(dStar, dImp);
    // Beam parts fade on the beam clock; the impact sphere on its own.
    threshold *= dImp < min(dCore, min(dSpiral, dStar)) ? (1.0 - aftSphere) : (1.0 - aftP);
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
    // pick up a soft golden-white corona, giving the beam real volume. In
    // caster view it is capped, otherwise it floods the first-person screen
    // (every forward ray hugs the beam) and hides everything in white-gold.
    float haloAmt = exp(-max(hit.z, 0.0) * 0.16) * envelope * 1.15;
    haloAmt = mix(haloAmt, min(haloAmt, 0.45), CasterView);
    worldCol += (BOROS_GOLD * 0.55 + CORE_WHITE * 0.45) * haloAmt;

    // Shockwaves once the beam reaches the far end: golden rings that crawl
    // over the landscape, slow enough to still be walking when the beam and
    // the sphere are already gone (ARCS_AFTERGLOW).
    float arriveT = localFire - FireTime * 0.70;
    if (arriveT > 0.0) {
        float dGround = distance(endPoint, impactRel);
        float fadeImpact = clamp(1.0 - aftArcs, 0.0, 1.0);
        float wave1 = 16.0 / max(abs(dGround - 48.0 * arriveT), 2.0);
        float wave2 = 6.0 / max(abs(dGround - 26.0 * arriveT), 1.5);
        float wave3 = 9.0 / max(abs(dGround - 14.0 * arriveT), 1.8);
        float crater = 90.0 / (pow(dGround, 1.45) + 14.0);
        worldCol += original * (wave1 * 0.9 + wave2 * 0.7 + wave3 * 0.8) * fadeImpact * (BOROS_GOLD * 0.7 + CORE_WHITE * 0.3);
        worldCol += (DEEP_ORANGE * 0.75 + BOROS_GOLD * 0.25) * crater * fadeImpact * 0.35;
        worldCol = mix(worldCol, CORE_WHITE, clamp(smoothstep(dGround - 14.0, dGround, 60.0 * arriveT) * clamp(1.0 - aftSphere, 0.0, 1.0) * 0.4, 0.0, 1.0));
    }

    vec3 col = mix(worldCol, beamCol, threshold);

    // Release flash: one blinding frame-filling burst as the star fires.
    // In caster view the flash decays faster and caps lower so the caster's
    // own beam punches through it almost immediately; in reduced-flash mode
    // it is softened for photosensitive players.
    float flash = exp(-max(localFire, 0.0) * mix(6.5, 11.0, CasterView));
    float flashAmt = mix(0.9, 0.55, CasterView) * mix(1.0, 0.45, ReducedFlash);
    col = mix(col, CORE_WHITE * 1.25, clamp(flash, 0.0, 1.0) * flashAmt);

    fragColor = vec4(col, 1.0);
}
