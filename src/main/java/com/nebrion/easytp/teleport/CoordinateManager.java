package com.nebrion.easytp.teleport;

import com.nebrion.easytp.config.ConfigManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsing, validation and formatting of teleport coordinates, plus the list of selectable
 * dimensions.
 */
public final class CoordinateManager {
	public static final Identifier OVERWORLD = Identifier.of("minecraft", "overworld");
	public static final Identifier NETHER = Identifier.of("minecraft", "the_nether");
	public static final Identifier END = Identifier.of("minecraft", "the_end");

	public static final double MAX_HORIZONTAL = 30_000_000.0;
	public static final double MIN_Y = -2048.0;
	public static final double MAX_Y = 2048.0;

	private CoordinateManager() {
	}

	public record DimensionOption(String id, String label, Identifier dimension) {
	}

	/** Every dimension the operator can pick: the server's default plus the three vanilla ones. */
	public static List<DimensionOption> dimensions() {
		List<DimensionOption> options = new ArrayList<>();
		options.add(new DimensionOption("CURRENT", "Current dimension", null));
		options.add(new DimensionOption("OVERWORLD", "Overworld", OVERWORLD));
		options.add(new DimensionOption("NETHER", "Nether", NETHER));
		options.add(new DimensionOption("END", "The End", END));
		return options;
	}

	public static String dimensionLabel(Identifier dimension) {
		if (dimension == null) {
			return "Current dimension";
		}

		if (dimension.equals(OVERWORLD)) {
			return "Overworld";
		}

		if (dimension.equals(NETHER)) {
			return "Nether";
		}

		if (dimension.equals(END)) {
			return "The End";
		}

		return dimension.toString();
	}

	public static Identifier currentDimension(MinecraftClient client) {
		if (client == null || client.world == null) {
			return null;
		}

		return client.world.getRegistryKey().getValue();
	}

	public static String currentDimensionLabel(MinecraftClient client) {
		Identifier dimension = currentDimension(client);
		return dimension == null ? "Unknown" : dimensionLabel(dimension);
	}

	/** The operator's own position, or {@code null} when not in a world. */
	public static Vec3d myLocation(MinecraftClient client) {
		if (client == null || client.player == null) {
			return null;
		}

		return new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
	}

	public static Validation parse(String x, String y, String z) {
		if (x == null || x.isBlank() || y == null || y.isBlank() || z == null || z.isBlank()) {
			return new Validation(null, null, null, "Enter a value for X, Y and Z.", false);
		}

		Double px = parseNumber(x);
		Double py = parseNumber(y);
		Double pz = parseNumber(z);

		if (px == null || py == null || pz == null) {
			return new Validation(null, null, null, "Coordinates must be numbers (decimals allowed).", false);
		}

		if (Math.abs(px) > MAX_HORIZONTAL || Math.abs(pz) > MAX_HORIZONTAL) {
			return new Validation(null, null, null,
					"X and Z must stay within " + (long) MAX_HORIZONTAL + " blocks of the origin.", false);
		}

		if (py < MIN_Y || py > MAX_Y) {
			return new Validation(null, null, null, "Y must be between " + (long) MIN_Y + " and " + (long) MAX_Y + ".", false);
		}

		String warning = null;

		if (py < -70.0 || py > 330.0) {
			warning = "Y is outside the usual world height; the server may clamp or reject it.";
		}

		return new Validation(px, py, pz, warning, true);
	}

	private static Double parseNumber(String raw) {
		String value = raw.trim().replace(',', '.');

		try {
			double parsed = Double.parseDouble(value);

			if (!Double.isFinite(parsed)) {
				return null;
			}

			return parsed;
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	public static void remember(Destination destination) {
		if (destination == null || !destination.isCoordinates()) {
			return;
		}

		ConfigManager config = ConfigManager.get();
		List<Double> coords = config.root().lastCoordinates;

		if (coords == null) {
			coords = new ArrayList<>();
			config.root().lastCoordinates = coords;
		}

		coords.clear();
		coords.add(destination.x());
		coords.add(destination.y());
		coords.add(destination.z());
		config.root().lastDimension = destination.dimension() == null
				? "minecraft:overworld"
				: destination.dimension().toString();
		config.requestSave();
	}

	public static List<Double> savedCoordinates() {
		return ConfigManager.get().root().lastCoordinates;
	}

	public static Identifier savedDimension() {
		return Identifier.tryParse(ConfigManager.get().root().lastDimension);
	}

	/** Result of {@link #parse(String, String, String)}. */
	public static final class Validation {
		private final Double x;
		private final Double y;
		private final Double z;
		private final String message;
		private final boolean valid;

		private Validation(Double x, Double y, Double z, String message, boolean valid) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.message = message;
			this.valid = valid;
		}

		public boolean valid() {
			return valid;
		}

		public boolean hasWarning() {
			return valid && message != null;
		}

		public String message() {
			return message == null ? "" : message;
		}

		public double x() {
			return x == null ? 0.0 : x;
		}

		public double y() {
			return y == null ? 0.0 : y;
		}

		public double z() {
			return z == null ? 0.0 : z;
		}
	}
}
