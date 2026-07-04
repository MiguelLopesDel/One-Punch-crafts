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

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("boros");
        CSRC_VOICE = builder
                .comment("Language of Boros' shout when firing the Collapsing Star Roaring Cannon.")
                .defineEnum("csrcVoiceLanguage", CsrcVoice.JAPANESE);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {}
}
