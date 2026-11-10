package com.nebrion.easytp.teleport;

import net.minecraft.util.Identifier;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Builds the ordinary vanilla teleport commands Easy TP sends on the operator's behalf.
 *
 * <p>The mod never invents packet level teleports: every operation is a normal, permission checked
 * server command (`/tp`).</p>
 */
public final class CommandBuilder {
	private CommandBuilder() {
	}

	public static String playerTarget(String targetName, String destinationName) {
		return "tp " + targetName + " " + destinationName;
	}

	public static String coordinateTarget(String targetName, Destination destination, Identifier currentDimension) {
		String base = "tp " + targetName + " "
				+ formatCoordinate(destination.x()) + " "
				+ formatCoordinate(destination.y()) + " "
				+ formatCoordinate(destination.z());

		Identifier dimension = destination.dimension();

		if (dimension != null && currentDimension != null && !dimension.equals(currentDimension)) {
			return "execute in " + dimension + " run " + base;
		}

		return base;
	}

	/** Formats a coordinate without trailing zeros, the way the vanilla command parser expects it. */
	public static String formatCoordinate(double value) {
		if (!Double.isFinite(value)) {
			return "0";
		}

		BigDecimal decimal = BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros();
		return decimal.toPlainString();
	}

	public static String dimensionName(Identifier dimension) {
		if (dimension == null) {
			return "current";
		}

		return dimension.toString();
	}
}
