package com.example.abilitytoolkit.payload;

import com.example.abilitytoolkit.AbilityLimits;
import com.example.abilitytoolkit.AbilityToolkit;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> Client: which abilities this player is authorized to use. Also used as the server-side permission object. */
public record PermissionPayload(boolean noKnockback, boolean flight, boolean speed, int maxSpeedLevel)
		implements CustomPacketPayload {
	/** Nothing is allowed. This is also what the client assumes when the server never sends a handshake. */
	public static final PermissionPayload NONE = new PermissionPayload(false, false, false, AbilityLimits.MIN_SPEED_LEVEL);

	public static final CustomPacketPayload.Type<PermissionPayload> TYPE =
			new CustomPacketPayload.Type<>(AbilityToolkit.id("permissions"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PermissionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, PermissionPayload::noKnockback,
			ByteBufCodecs.BOOL, PermissionPayload::flight,
			ByteBufCodecs.BOOL, PermissionPayload::speed,
			ByteBufCodecs.VAR_INT, PermissionPayload::maxSpeedLevel,
			PermissionPayload::new);

	public boolean any() {
		return noKnockback || flight || speed;
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
