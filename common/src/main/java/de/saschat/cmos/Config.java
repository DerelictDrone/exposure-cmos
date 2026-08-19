package de.saschat.cmos;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
	public static class Server {
		public static final ModConfigSpec SPEC;
		public static final ModConfigSpec.DoubleValue CMOS_CAMERA_COOLDOWN;
		public static final ModConfigSpec.IntValue COOLDOWN_RESOLUTION;
		public static final ModConfigSpec.DoubleValue COOLDOWN_BW;
		public static final ModConfigSpec.DoubleValue COOLDOWN_SILENT;
		public static final ModConfigSpec.BooleanValue CMOS_CAMERA_DO_COOLDOWN_MODIFIERS;
		static {
			ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
			{
				builder.push("cmos_camera");
				CMOS_CAMERA_COOLDOWN = builder.comment("Default CMOS Camera cooldown in ticks","Default: 100")
				.defineInRange("cmos_camera_cooldown",100,0,Double.MAX_VALUE);
				CMOS_CAMERA_DO_COOLDOWN_MODIFIERS = builder.comment("Enables or disables scaling the above cooldown.","Default: true")
				.define("cmos_camera_do_cooldown_modifiers",true);
				COOLDOWN_RESOLUTION = builder.comment("Scale cooldown by (resolution^2)/(cooldown_resolution^2), set to 0 to disable","Default: 128")
				.defineInRange("cooldown_resolution",128,0,4096);
				COOLDOWN_BW = builder.comment("Scale cooldown by this scalar if image is in black and white, set to 1 to disable","Default:0.5")
				.defineInRange("cooldown_bw",0.5,0,Double.MAX_VALUE);
				COOLDOWN_SILENT = builder.comment("Scale cooldown by this scalar if camera/camera stand is set as silent, set to 1 to disable","Default:0.125")
				.defineInRange("cooldown_silent",0.125,0,Double.MAX_VALUE);
				builder.pop();
			}
			SPEC = builder.build();
		}
	}
}