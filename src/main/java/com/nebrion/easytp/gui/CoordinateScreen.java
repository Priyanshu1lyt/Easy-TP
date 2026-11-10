package com.nebrion.easytp.gui;

import com.nebrion.easytp.player.PlayerManager;
import com.nebrion.easytp.teleport.CoordinateManager;
import com.nebrion.easytp.teleport.Destination;
import com.nebrion.easytp.teleport.TeleportManager;
import com.nebrion.easytp.util.ColourUtil;
import com.nebrion.easytp.util.Toasts;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Destination mode B: teleport to custom XYZ coordinates, with an explicit destination dimension.
 */
public final class CoordinateScreen extends EasyTpScreen {
	private TextFieldWidget xField;
	private TextFieldWidget yField;
	private TextFieldWidget zField;

	private String xText = "";
	private String yText = "";
	private String zText = "";

	private Identifier dimension;
	private String message = "";
	private boolean messageOk;

	public CoordinateScreen(Screen parent) {
		super(Text.literal("Coordinate destination"), parent);

		Destination current = TeleportManager.get().destination();

		if (current != null && current.isCoordinates()) {
			xText = com.nebrion.easytp.teleport.CommandBuilder.formatCoordinate(current.x());
			yText = com.nebrion.easytp.teleport.CommandBuilder.formatCoordinate(current.y());
			zText = com.nebrion.easytp.teleport.CommandBuilder.formatCoordinate(current.z());
			dimension = current.dimension();
		} else {
			List<Double> saved = CoordinateManager.savedCoordinates();

			if (saved != null && saved.size() >= 3) {
				xText = com.nebrion.easytp.teleport.CommandBuilder.formatCoordinate(saved.get(0));
				yText = com.nebrion.easytp.teleport.CommandBuilder.formatCoordinate(saved.get(1));
				zText = com.nebrion.easytp.teleport.CommandBuilder.formatCoordinate(saved.get(2));
				dimension = CoordinateManager.savedDimension();
			}
		}
	}

	@Override
	protected void onFirstInit() {
		PlayerManager.get().refresh(client);
		validate(false);
	}

	@Override
	protected void buildWidgets() {
		int columnWidth = Math.max(70, (contentW - GAP * 2) / 3);
		int fieldY = contentY + 58;

		xField = textField(contentX + 8, fieldY + 4, columnWidth - 16, 12, xText, 18, value -> {
			xText = value;
			validate(false);
		});
		yField = textField(contentX + columnWidth + 8, fieldY + 4, columnWidth - 16, 12, yText, 12, value -> {
			yText = value;
			validate(false);
		});
		zField = textField(contentX + columnWidth * 2 + 8, fieldY + 4, columnWidth - 16, 12, zText, 18, value -> {
			zText = value;
			validate(false);
		});
	}

	@Override
	protected int navIndex() {
		return -1;
	}

	@Override
	protected void drawContent(DrawContext context) {
		int columnWidth = Math.max(70, (contentW - GAP * 2) / 3);
		int fieldY = contentY + 58;

		Theme.text(context, textRenderer, "COORDINATE DESTINATION", contentX, contentY + 2, Theme.TEXT);
		Theme.fill(context, contentX, contentY + 14, 22, 2, Theme.GOLD);
		Theme.textRight(context, textRenderer, "Your dimension: " + CoordinateManager.currentDimensionLabel(client),
				contentX + contentW, contentY + 4, Theme.TEXT_DIM);

		Theme.text(context, textRenderer, "X COORDINATE", contentX + 8, contentY + 44, Theme.TEXT_MUTED);
		drawFieldBox(context, contentX + 8, fieldY, columnWidth - 16, 20, xField != null && xField.isFocused());

		Theme.text(context, textRenderer, "Y COORDINATE", contentX + columnWidth + 8, contentY + 44, Theme.TEXT_MUTED);
		drawFieldBox(context, contentX + columnWidth + 8, fieldY, columnWidth - 16, 20,
				yField != null && yField.isFocused());

		Theme.text(context, textRenderer, "Z COORDINATE", contentX + columnWidth * 2 + 8, contentY + 44, Theme.TEXT_MUTED);
		drawFieldBox(context, contentX + columnWidth * 2 + 8, fieldY, columnWidth - 16, 20,
				zField != null && zField.isFocused());

		int buttonsY = fieldY + 28;
		int third = (contentW - 16 - GAP * 2) / 3;
		button(context, "USE MY LOCATION", contentX + 8, buttonsY, third, 17, ButtonKind.SECONDARY, this::useMyLocation);
		button(context, "CLEAR", contentX + 8 + third + GAP, buttonsY, third, 17, ButtonKind.GHOST, () -> {
			xText = "";
			yText = "";
			zText = "";

			if (xField != null) {
				xField.setText("");
			}

			if (yField != null) {
				yField.setText("");
			}

			if (zField != null) {
				zField.setText("");
			}

			message = "";
			messageOk = false;
		});
		button(context, "VALIDATE", contentX + 8 + (third + GAP) * 2, buttonsY, third, 17, ButtonKind.GHOST,
				() -> validate(true));

		// Dimension picker
		int dimensionY = buttonsY + 26;
		Theme.text(context, textRenderer, "DESTINATION DIMENSION", contentX + 8, dimensionY, Theme.TEXT_MUTED);

		List<CoordinateManager.DimensionOption> options = CoordinateManager.dimensions();
		int optionWidth = Math.max(56, (contentW - 16 - GAP * 3) / 4);
		int optionY = dimensionY + 12;

		for (int index = 0; index < options.size(); index++) {
			CoordinateManager.DimensionOption option = options.get(index);
			boolean selected = dimensionEquals(option.dimension());
			tab(context, option.label(), contentX + 8 + index * (optionWidth + GAP), optionY, optionWidth, 17,
					selected, () -> {
						dimension = option.dimension();
						validate(false);
					});
		}

		// Validation status
		int statusY = optionY + 26;
		boolean valid = validate(false).valid();
		int statusColor = message.isEmpty() ? Theme.TEXT_MUTED : (messageOk ? Theme.GREEN : Theme.RED);
		String statusLabel = message.isEmpty()
				? "Enter coordinates, then validate or confirm."
				: message;
		Theme.fill(context, contentX, statusY, contentW, 30, ColourUtil.withAlpha(statusColor, 26));
		Theme.fill(context, contentX, statusY, 2, 30, statusColor);
		Theme.text(context, textRenderer, trim(valid ? "VALID DESTINATION" : "INVALID DESTINATION", contentW - 20),
				contentX + 8, statusY + 5, statusColor);
		Theme.text(context, textRenderer, trim(statusLabel, contentW - 20), contentX + 8, statusY + 16, Theme.TEXT_DIM);

		// Preview card
		int previewY = statusY + 36;
		int previewH = Math.max(36, contentY + contentH - previewY - 26);

		if (previewH >= 36) {
			Theme.panel(context, contentX, previewY, contentW, previewH, panelAlpha());
			cardHeading(context, contentX, previewY, contentW, "COMMAND PREVIEW", null);

			String x = xText.isBlank() ? "0" : xText.trim();
			String y = yText.isBlank() ? "0" : yText.trim();
			String z = zText.isBlank() ? "0" : zText.trim();
			Identifier effective = effectiveDimension();
			String prefix = effective != null && !effective.equals(CoordinateManager.currentDimension(client))
					? "/execute in " + effective + " run "
					: "/";
			String preview = prefix + "tp <player> " + x + " " + y + " " + z;
			Theme.text(context, textRenderer, trim(preview, contentW - 16), contentX + 8, previewY + 24, Theme.TEXT_DIM);

			if (previewH >= 56) {
				Theme.text(context, textRenderer, "Dimension: " + CoordinateManager.dimensionLabel(effective),
						contentX + 8, previewY + 36, Theme.TEXT_MUTED);
			}
		}

		// Actions
		int footerY = contentY + contentH - 18;
		button(context, "CONFIRM DESTINATION", contentX, footerY, 150, 18, ButtonKind.PRIMARY, this::confirm);
		button(context, "CANCEL", contentX + 156, footerY, 80, 18, ButtonKind.GHOST, this::close);
	}

	private boolean dimensionEquals(Identifier other) {
		if (dimension == null || other == null) {
			return dimension == other;
		}

		return dimension.equals(other);
	}

	private Identifier effectiveDimension() {
		if (dimension != null) {
			return dimension;
		}

		return CoordinateManager.currentDimension(client);
	}

	private CoordinateManager.Validation validate(boolean announce) {
		CoordinateManager.Validation validation = CoordinateManager.parse(xText, yText, zText);

		if (validation.valid()) {
			message = validation.hasWarning() ? validation.message() : "Coordinates are valid.";
			messageOk = !validation.hasWarning();
		} else {
			message = validation.message();
			messageOk = false;
		}

		if (announce) {
			if (validation.valid()) {
				Toasts.success(validation.hasWarning() ? "Valid, but: " + validation.message() : "Coordinates are valid.");
			} else {
				Toasts.error(validation.message());
			}
		}

		return validation;
	}

	private void useMyLocation() {
		Vec3d location = CoordinateManager.myLocation(client);

		if (location == null) {
			Toasts.error("Your position is not available right now.");
			return;
		}

		xText = com.nebrion.easytp.teleport.CommandBuilder.formatCoordinate(location.x);
		yText = com.nebrion.easytp.teleport.CommandBuilder.formatCoordinate(location.y);
		zText = com.nebrion.easytp.teleport.CommandBuilder.formatCoordinate(location.z);
		dimension = null;

		if (xField != null) {
			xField.setText(xText);
		}

		if (yField != null) {
			yField.setText(yText);
		}

		if (zField != null) {
			zField.setText(zText);
		}

		validate(false);
		Toasts.info("Copied your current location (" + CoordinateManager.currentDimensionLabel(client) + ").");
	}

	private void confirm() {
		CoordinateManager.Validation validation = validate(false);

		if (!validation.valid()) {
			Toasts.error(validation.message());
			return;
		}

		Identifier effective = effectiveDimension();
		Destination destination = Destination.coordinates(validation.x(), validation.y(), validation.z(), effective);
		TeleportManager.get().setDestination(destination);
		CoordinateManager.remember(destination);
		Toasts.success("Destination set to " + destination.coordinateLabel()
				+ " (" + CoordinateManager.dimensionLabel(effective) + ").");

		if (parent != null && client != null) {
			client.setScreen(parent);
		} else {
			close();
		}
	}
}
