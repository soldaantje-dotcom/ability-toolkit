package com.example.abilitytoolkit.client;

import com.example.abilitytoolkit.payload.PermissionPayload;
import net.minecraft.network.chat.Component;

/** Runtime (not saved) client state: what the server allows, plus a short HUD message and a pending "start flying". */
public final class ClientState {
	private static PermissionPayload permissions = PermissionPayload.NONE;
	private static boolean serverHandshake = false;

	private static Component message = Component.empty();
	private static long messageUntilMillis = 0;

	private static int pendingFlyTicks = 0;

	private ClientState() {
	}

	public static PermissionPayload permissions() {
		return permissions;
	}

	/** True once the server has told us its policy, i.e. the server runs this mod. */
	public static boolean hasServerHandshake() {
		return serverHandshake;
	}

	public static void setPermissions(PermissionPayload payload) {
		permissions = payload;
		serverHandshake = true;
	}

	/** Called on join/disconnect: until a server says otherwise, everything is locked. */
	public static void reset() {
		permissions = PermissionPayload.NONE;
		serverHandshake = false;
		pendingFlyTicks = 0;
	}

	public static void flash(Component text) {
		message = text;
		messageUntilMillis = System.currentTimeMillis() + 2500;
	}

	public static Component activeMessage() {
		return System.currentTimeMillis() < messageUntilMillis ? message : null;
	}

	/** After enabling flight we wait (up to 2s) for the server's abilities packet, then start flying. */
	public static void requestFlyStart() {
		pendingFlyTicks = 40;
	}

	public static int pendingFlyTicks() {
		return pendingFlyTicks;
	}

	public static void decrementPendingFly() {
		if (pendingFlyTicks > 0) {
			pendingFlyTicks--;
		}
	}

	public static void clearPendingFly() {
		pendingFlyTicks = 0;
	}
}
