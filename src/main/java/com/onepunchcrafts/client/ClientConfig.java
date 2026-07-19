package com.onepunchcrafts.client;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client-only options (config/onepunchcrafts-client.toml). Each player picks
 * the language Boros shouts his ultimate in; the sound is played locally, so
 * different players on the same server can hear different languages.
 */
public final class ClientConfig {

    public enum CsrcVoice {
        JAPANESE,
        ENGLISH,
        PORTUGUESE
    }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<CsrcVoice> CSRC_VOICE;
    public static final ForgeConfigSpec.BooleanValue CSRC_CASTER_BEAM_VIEW;
    public static final ForgeConfigSpec.BooleanValue CSRC_CINEMATIC_CAMERA;
    public static final ForgeConfigSpec.BooleanValue CSRC_REDUCED_FLASHES;
    public static final ForgeConfigSpec.DoubleValue CSRC_VOICE_VOLUME;
    public static final ForgeConfigSpec.DoubleValue CSRC_MUSIC_VOLUME;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("boros");
        CSRC_VOICE = builder
                .comment("Language of Boros' shout when firing the Collapsing Star Roaring Cannon.")
                .defineEnum("csrcVoiceLanguage", CsrcVoice.JAPANESE);
        CSRC_CASTER_BEAM_VIEW = builder
                .comment("When true and YOU are the one firing the CSRC, the anime impact",
                        "frames turn into rhythmic cuts with clean windows in between, so the",
                        "caster also gets to see the golden beam and the impact sphere.",
                        "Togglable in-game through the CSRC options screen (default key: H).")
                .define("csrcCasterBeamView", false);
        CSRC_CINEMATIC_CAMERA = builder
                .comment("Cinematic camera for the caster: third person during the charge (so",
                        "you see Boros charging up) and back to first person on the release.")
                .define("csrcCinematicCamera", true);
        CSRC_REDUCED_FLASHES = builder
                .comment("Photosensitivity accessibility: slows the anime impact-frame cuts",
                        "from ~12 to ~3 per second and softens the white flashes and strobing",
                        "while keeping the overall look of the effect.")
                .define("csrcReducedFlashes", false);
        CSRC_VOICE_VOLUME = builder
                .comment("Volume multiplier for Boros' shout (client-side).")
                .defineInRange("csrcVoiceVolume", 1.0, 0.0, 2.0);
        CSRC_MUSIC_VOLUME = builder
                .comment("Volume multiplier for the CSRC charge soundtrack/heartbeat (client-side).")
                .defineInRange("csrcMusicVolume", 1.0, 0.0, 2.0);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {}
}
