#version 330 core

// Second pass: rotating chromatic aberration + vignette pulse.
// Intensity builds at the end of the charge, spikes on release and hums
// while the beam is sustained; it attenuates with camera distance so far
// spectators get a subtler shake.

uniform sampler2D DiffuseSampler;

uniform vec3 CameraPosition;
uniform vec3 StartPosition;
uniform vec3 BeamDirection;
uniform float BeamRange;
uniform float iTime;
uniform float ChargeTime;
uniform float FireTime;
uniform float CasterView;
uniform float ReducedFlash;
uniform vec2 OutSize;

in vec2 texCoord;
out vec4 fragColor;

vec2 rotate2(vec2 p, float r) {
    float c = cos(r);
    float s = sin(r);
    return mat2(c, -s, s, c) * p;
}

void main() {
    vec3 original = texture(DiffuseSampler, texCoord).rgb;

    float chargeP = clamp(iTime / max(ChargeTime, 0.001), 0.0, 1.0);
    float localFire = iTime - ChargeTime;
    float fireP = clamp(localFire / max(FireTime, 0.001), 0.0, 1.0);
    // Matches the beam afterglow in csrc.fsh; the aberration/vignette die
    // with the beam while the sphere and surface waves keep going clean.
    float aftP = clamp((localFire - FireTime) / 2.8, 0.0, 1.0);

    float intensity = 7.0 * pow(chargeP, 6.0);
    // Heartbeat thumps (synced with csrc_charge.ogg, anchored to charge end)
    // kick the aberration for a single blink each.
    float hb = exp(-55.0 * abs(iTime - (ChargeTime - 2.65)))
             + exp(-55.0 * abs(iTime - (ChargeTime - 2.35)));
    intensity += 14.0 * clamp(hb, 0.0, 1.0) * step(iTime, ChargeTime);
    if (localFire > 0.0) {
        intensity = 52.0 * exp(-localFire * 3.1)
                + 6.0 * (0.7 + 0.3 * sin(iTime * 34.0)) * (1.0 - fireP * 0.5) * (1.0 - aftP);
        // The caster keeps a crisper image of their own beam.
        intensity *= mix(1.0, 0.6, CasterView);
        intensity *= mix(1.0, 0.55, ReducedFlash);
    }

    // Attenuate with distance from the camera to the beam segment.
    vec3 axis = normalize(BeamDirection);
    vec3 rel = CameraPosition - StartPosition;
    float proj = clamp(dot(rel, axis), 0.0, BeamRange);
    float dist = length(rel - axis * proj);
    intensity *= 30.0 / (dist + 30.0);

    vec2 onePixel = vec2(1.0 / max(OutSize.x, 1.0), 1.0 / max(OutSize.y, 1.0));
    vec2 rotatedPixel = rotate2(onePixel, -iTime * 1.7);

    float caRed = texture(DiffuseSampler, texCoord + rotatedPixel * intensity).r;
    rotatedPixel = rotate2(rotatedPixel, 2.09439510239);
    float caGreen = texture(DiffuseSampler, texCoord + (rotatedPixel - onePixel) * intensity).g;
    rotatedPixel = rotate2(rotatedPixel, 2.09439510239);
    float caBlue = texture(DiffuseSampler, texCoord + (rotatedPixel - onePixel) * intensity).b;

    float edge = 2.0 * length(texCoord - vec2(0.5));
    vec3 col = mix(original, vec3(caRed, caGreen, caBlue), clamp(edge, 0.0, 1.0));

    // Breathing vignette while the effect is alive.
    float alive = max(pow(chargeP, 3.0), (1.0 - aftP));
    float vig = 1.0 - 0.28 * alive * pow(edge, 2.2);
    col *= vig;

    // ------------------------------------------------------------------
    // Anime impact frames, matched frame-by-frame to OPM S1E12 (CSRC cut):
    //  1) rim-lit silhouette on black alternating with black silhouette on
    //     a pink/cyan gradient;
    //  2) then the long "ink on white" run: black sumi-e strokes on white
    //     with amber accents, broken by full-gold and pure-white frames.
    // Cuts flip at ~12 fps like the show (drawn on twos).
    // ------------------------------------------------------------------
    if (localFire > 0.0) {
        // Photosensitivity mode: cuts drop from ~12 to ~3 per second and land
        // softer, staying under the strobing danger zone.
        float frame = floor(iTime * mix(12.0, 3.0, ReducedFlash));
        float lum = dot(col, vec3(0.299, 0.587, 0.114));
        float tone = smoothstep(0.28, 0.60, lum);
        vec3 cutCol = col;
        float w = 0.0;

        if (localFire < 0.30) {
            // Opening pops: magenta rim on black <-> black on pink/cyan.
            if (mod(frame, 2.0) < 1.0) {
                cutCol = mix(vec3(0.0), vec3(1.0, 0.30, 0.75), tone);
            } else {
                vec3 grad = mix(vec3(0.25, 0.85, 1.0), vec3(1.0, 0.35, 0.80), texCoord.y);
                cutCol = mix(vec3(0.02), grad, tone);
            }
            w = 1.0;
        } else if (localFire < 1.6) {
            // The "eye" run: ink on white, with gold/white one-off frames.
            float m = mod(frame, 6.0);
            if (m < 1.0) {
                cutCol = vec3(1.0);
            } else if (m < 2.0) {
                cutCol = mix(vec3(0.45, 0.20, 0.02), vec3(1.0, 0.74, 0.16), tone);
            } else {
                cutCol = mix(vec3(0.05, 0.02, 0.02), vec3(1.0, 0.99, 0.95), tone);
            }
            w = 1.0 - smoothstep(0.9, 1.6, localFire);
        }

        // Landing burst: ink splatter on white again.
        float arr = localFire - FireTime * 0.70;
        if (arr > 0.0 && arr < 0.40) {
            if (mod(frame, 4.0) < 1.0) {
                cutCol = vec3(1.0);
            } else {
                cutCol = mix(vec3(0.04, 0.02, 0.02), vec3(1.0), tone);
            }
            w = max(w, 1.0 - arr / 0.40);
        }

        // Caster view: instead of removing the impact frames, they become
        // rhythmic bursts (drawn-on-twos montage) with clean windows between
        // them — the anime cuts still hit, and in every gap the caster sees
        // the golden beam and the impact sphere raw.
        if (CasterView > 0.5) {
            float cutGate = step(fract(localFire * 2.2), 0.45);
            w *= cutGate;
        }

        if (w > 0.0) {
            // Full punch up close (first person), softer for far spectators.
            float strength = w * clamp(140.0 / (dist + 60.0), 0.0, 1.0);
            strength *= mix(1.0, 0.5, ReducedFlash);
            col = mix(col, cutCol, strength);
        }
    }

    fragColor = vec4(col, 1.0);
}
