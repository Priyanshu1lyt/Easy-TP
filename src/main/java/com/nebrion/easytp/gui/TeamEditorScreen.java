package com.nebrion.easytp.gui;

import com.nebrion.easytp.config.ConfigManager;
import com.nebrion.easytp.player.PlayerData;
import com.nebrion.easytp.player.PlayerManager;
import com.nebrion.easytp.team.TeamData;
import com.nebrion.easytp.team.TeamManager;
import com.nebrion.easytp.util.ColourUtil;
import com.nebrion.easytp.util.Toasts;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Create and edit screen for a team: name, colour (palette or hex) and the member list.
 */
public final class TeamEditorScreen extends EasyTpScreen {
	private final TeamData existing;

	private String name;
	private int color;
	private final Set<UUID> members = new LinkedHashSet<>();

	private TextFieldWidget nameField;
	private TextFieldWidget hexField;

	private String error;

	private int leftX;
	private int leftW;
	private int rightX;
	private int rightW;
	private int membersY;
	private int membersH;
	private int memberScroll;

	private int paletteX;
	private int paletteY;

	public TeamEditorScreen(Screen parent, TeamData existing) {
		super(Text.literal(existing == null ? "Create team" : "Edit team"), parent);
		this.existing = existing;
		this.name = existing == null ? "" : existing.name();
		this.color = existing == null ? TeamManager.get().nextPaletteColor() : existing.color();

		if (existing != null) {
			this.members.addAll(existing.members());
		}
	}

	@Override
	protected void onFirstInit() {
		PlayerManager.get().refresh(client);
		TeamManager.get().ensureServer(client);
	}

	@Override
	protected void buildWidgets() {
		leftW = Math.max(150, Math.min(268, (int) (contentW * 0.46)));
		leftX = contentX;
		rightX = contentX + leftW + GAP;
		rightW = Math.max(120, contentW - leftW - GAP);

		int fieldY = contentY + 46;
		nameField = textField(leftX + 8, fieldY + 4, leftW - 16, 12, name, 24, value -> name = value);

		int hexY = fieldY + 34;
		paletteY = hexY + 60;
		paletteX = leftX + 8;
		hexField = textField(leftX + 8, hexY + 4, 96, 12, ColourUtil.toHex(color), 9, value -> {
			Integer parsed = ColourUtil.parseHex(value);

			if (parsed != null) {
				color = parsed;
				error = null;
			} else if (!value.isBlank()) {
				error = "Hex colour must look like #22C55E.";
			}
		});

		membersY = contentY + 46;
		membersH = Math.max(40, contentY + contentH - membersY - 30);
	}

	@Override
	protected int navIndex() {
		return -1;
	}

	@Override
	protected void drawContent(DrawContext context) {
		boolean editing = existing != null;

		Theme.text(context, textRenderer, editing ? "EDIT TEAM" : "CREATE TEAM", contentX, contentY + 2, Theme.TEXT);
		Theme.fill(context, contentX, contentY + 14, 22, 2, Theme.ACCENT);
		Theme.textRight(context, textRenderer, editing ? existing.name() : "new team", contentX + contentW,
				contentY + 4, Theme.TEXT_MUTED);

		Theme.panel(context, leftX, contentY + 24, leftW, contentY + contentH - contentY - 24 - 26, panelAlpha());

		Theme.text(context, textRenderer, "TEAM NAME", leftX + 8, contentY + 32, Theme.TEXT_MUTED);
		drawFieldBox(context, leftX + 8, contentY + 46, leftW - 16, 20, nameField != null && nameField.isFocused());

		int hexY = contentY + 80;
		Theme.text(context, textRenderer, "HEX COLOUR", leftX + 8, hexY, Theme.TEXT_MUTED);
		drawFieldBox(context, leftX + 8, hexY + 12, 96, 20, hexField != null && hexField.isFocused());
		Theme.fill(context, leftX + 110, hexY + 12, 30, 20, color);
		Theme.border(context, leftX + 110, hexY + 12, 30, 20, ColourUtil.brighten(color, 0.3F));
		Theme.text(context, textRenderer, ColourUtil.toHex(color), leftX + 146, hexY + 18, Theme.TEXT_DIM);

		Theme.text(context, textRenderer, "PALETTE", leftX + 8, hexY + 40, Theme.TEXT_MUTED);
		int swatch = 18;
		int perRow = Math.max(3, (leftW - 16) / (swatch + 3));

		for (int index = 0; index < ColourUtil.PALETTE.size(); index++) {
			int column = index % perRow;
			int row = index / perRow;
			int x = paletteX + column * (swatch + 3);
			int y = hexY + 52 + row * (swatch + 3);

			if (y + swatch > contentY + contentH - 60) {
				break;
			}

			int value = ColourUtil.PALETTE.get(index);
			boolean hovered = contains(mouseX, mouseY, x, y, swatch, swatch);
			Theme.fill(context, x, y, swatch, swatch, value);
			Theme.border(context, x, y, swatch, swatch, value == color
					? 0xFFFFFFFF
					: hovered ? ColourUtil.brighten(value, 0.4F) : Theme.BORDER);
			region(x, y, swatch, swatch, () -> {
				color = value;
				error = null;

				if (hexField != null) {
					hexField.setText(ColourUtil.toHex(value));
				}
			});
		}

		drawPreview(context, leftX + 8, contentY + contentH - 54, leftW - 16);

		if (error != null) {
			Theme.text(context, textRenderer, trim(error, contentW - 16), contentX, contentY + contentH - 36, Theme.RED);
		}

		// Member panel
		Theme.panel(context, rightX, membersY - 22, rightW, contentY + contentH - membersY + 22 - 26, panelAlpha());
		cardHeading(context, rightX, membersY - 22, rightW, "MEMBERS", members.size() + " in team");

		int viewportY = membersY;
		int viewportH = Math.max(20, membersH - 8);
		List<MemberEntry> entries = memberEntries();
		int rowH = rowHeight();
		int contentHeight = entries.size() * rowH;
		memberScroll = clampScroll(memberScroll, contentHeight, viewportH);

		if (entries.isEmpty()) {
			emptyState(context, rightX, viewportY, rightW, viewportH, "No players available yet.");
		} else {
			context.enableScissor(rightX + 1, viewportY, rightX + rightW - 1, viewportY + viewportH);

			for (int index = 0; index < entries.size(); index++) {
				int rowY = viewportY + index * rowH - memberScroll;

				if (rowY + rowH < viewportY || rowY > viewportY + viewportH) {
					continue;
				}

				drawMemberRow(context, entries.get(index), rightX + 1, rowY, rightW - 2, rowH, index);
			}

			context.disableScissor();
			scrollbar(context, rightX + rightW - 4, viewportY, viewportH, contentHeight, memberScroll);
		}

		// Footer actions
		int footerY = contentY + contentH - 18;
		int buttonW = 110;
		button(context, editing ? "SAVE CHANGES" : "CREATE TEAM", contentX, footerY, buttonW, 18, ButtonKind.PRIMARY, this::save);

		if (editing) {
			button(context, "DELETE TEAM", contentX + buttonW + GAP, footerY, buttonW, 18, ButtonKind.DANGER, () -> {
				TeamManager.get().delete(existing.id());
				Toasts.success("Team deleted.");
				close();
			});
		}

		button(context, "CANCEL", contentX + (buttonW + GAP) * 2, footerY, 80, 18, ButtonKind.GHOST, this::close);
	}

	private void drawPreview(DrawContext context, int x, int y, int w) {
		Theme.fill(context, x, y, w, 34, ColourUtil.withAlpha(color, 30));
		Theme.fill(context, x, y, 3, 34, color);
		String preview = name.isBlank() ? "TEAM NAME" : name.toUpperCase();
		Theme.text(context, textRenderer, trim(preview, w - 20), x + 10, y + 6, ColourUtil.brighten(color, 0.35F));
		Theme.text(context, textRenderer, members.size() + " members  ·  " + ColourUtil.toHex(color), x + 10, y + 19,
				Theme.TEXT_MUTED);
	}

	private void drawMemberRow(DrawContext context, MemberEntry entry, int x, int y, int w, int h, int index) {
		boolean selected = members.contains(entry.uuid);
		boolean hovered = contains(mouseX, mouseY, x, y, w, h);

		int background = index % 2 == 0 ? ColourUtil.withAlpha(0xFF16161B, 170) : ColourUtil.withAlpha(0xFF121216, 170);

		if (hovered) {
			background = ColourUtil.withAlpha(Theme.PANEL_HOVER, 220);
		}

		Theme.fill(context, x, y, w, h, background);
		Theme.checkbox(context, x + 6, y + (h - 9) / 2, 9, selected, color);

		int textX = x + 20;

		if (!entry.online) {
			Theme.fill(context, x, y, 2, h, Theme.TEXT_MUTED);
		}

		Theme.text(context, textRenderer, trim(entry.name, Math.max(30, w - 110)), textX, y + (h - 8) / 2,
				entry.online ? Theme.TEXT : Theme.TEXT_MUTED);

		if (!entry.online) {
			Theme.textRight(context, textRenderer, "OFFLINE", x + w - 8, y + (h - 8) / 2, Theme.TEXT_MUTED);
		} else if (entry.teamLabel != null) {
			Theme.textRight(context, textRenderer, trim(entry.teamLabel, 70), x + w - 8, y + (h - 8) / 2, Theme.TEXT_MUTED);
		}

		region(x, y, w, h, () -> {
			if (!members.remove(entry.uuid)) {
				members.add(entry.uuid);
			}
		});
	}

	private List<MemberEntry> memberEntries() {
		List<MemberEntry> list = new ArrayList<>();
		Set<UUID> seen = new LinkedHashSet<>();

		for (PlayerData data : PlayerManager.get().sortedByName()) {
			TeamData other = TeamManager.get().primaryTeamOf(data.uuid());
			String label = other != null && !other.id().equals(existing == null ? "" : existing.id())
					? other.name()
					: null;
			list.add(new MemberEntry(data.uuid(), data.name(), true, label));
			seen.add(data.uuid());
		}

		for (UUID member : members) {
			if (seen.contains(member)) {
				continue;
			}

			String stored = existing != null ? existing.displayName(member) : ConfigManager.get().knownName(member.toString());
			list.add(new MemberEntry(member, stored == null ? member.toString().substring(0, 8) : stored, false, null));
		}

		list.sort(Comparator
				.comparing((MemberEntry entry) -> !entry.online)
				.thenComparing(entry -> entry.name, String.CASE_INSENSITIVE_ORDER));
		return list;
	}

	private void save() {
		String trimmed = name == null ? "" : name.trim();

		if (trimmed.isEmpty()) {
			error = "Enter a team name.";
			return;
		}

		if (trimmed.length() > 24) {
			error = "Team names are limited to 24 characters.";
			return;
		}

		String ignoreId = existing == null ? null : existing.id();

		if (TeamManager.get().nameTaken(trimmed, ignoreId)) {
			error = "A team with that name already exists.";
			return;
		}

		error = null;
		TeamManager manager = TeamManager.get();

		if (existing == null) {
			TeamData team = manager.create(trimmed, color);

			for (UUID member : members) {
				PlayerData data = PlayerManager.get().get(member);
				manager.addMember(team.id(), member, data == null ? null : data.name());
			}

			Toasts.success("Team \"" + trimmed + "\" created.");
		} else {
			existing.setName(trimmed);
			existing.setColor(color);
			manager.setColor(existing.id(), color);

			for (UUID member : new ArrayList<>(existing.members())) {
				if (!members.contains(member)) {
					existing.removeMember(member);
				}
			}

			for (UUID member : members) {
				if (!existing.hasMember(member)) {
					PlayerData data = PlayerManager.get().get(member);
					existing.addMember(member, data == null ? null : data.name());
				}
			}

			manager.persist();
			Toasts.success("Team \"" + trimmed + "\" updated.");
		}

		close();
	}

	@Override
	protected boolean handleScroll(int mouseX, int mouseY, double amount) {
		if (!contains(mouseX, mouseY, rightX, membersY, rightW, membersH)) {
			return false;
		}

		int viewportH = Math.max(20, membersH - 8);
		memberScroll = clampScroll(memberScroll - (int) (amount * rowHeight()), memberEntries().size() * rowHeight(), viewportH);
		return true;
	}

	private record MemberEntry(UUID uuid, String name, boolean online, String teamLabel) {
	}
}
