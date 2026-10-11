package com.example.abilitytoolkit.payload;

import com.example.abilitytoolkit.AbilityToolkit;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> Server: "this is what I would like enabled". The server decides what is actually granted. */
public record AbilityRequestPayload(boolean noKnockback, boolean flight, boolean speed, int speedLevel)
		implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<AbilityRequestPayload> TYPE =
			new CustomPacketPayload.Type<>(AbilityToolkit.id("ability_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, AbilityRequestPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, AbilityRequestPayload::noKnockback,
			ByteBufCodecs.BOOL, AbilityRequestPayload::flight,
			ByteBufCodecs.BOOL, AbilityRequestPayload::speed,
			ByteBufCodecs.VAR_INT, AbilityRequestPayload::speedLevel,
			AbilityRequestPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
