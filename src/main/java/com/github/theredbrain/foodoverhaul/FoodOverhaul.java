package com.github.theredbrain.foodoverhaul;

import com.github.theredbrain.foodoverhaul.config.ServerConfig;
import com.github.theredbrain.foodoverhaul.config.ServerConfigWrapper;
import com.github.theredbrain.foodoverhaul.item.TemporaryItemModifications;
import com.github.theredbrain.foodoverhaul.registry.StatusEffectsRegistry;
import com.google.gson.Gson;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FoodOverhaul implements ModInitializer {
	public static final String MOD_ID = "foodoverhaul";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static ServerConfig serverConfig;
	private static PacketByteBuf serverConfigSerialized = PacketByteBufs.create();

	public static EntityAttribute MAX_FOOD_EFFECTS;

	@Override
	public void onInitialize() {
		LOGGER.info("Enjoy your overhauled food!");

		// Config
		AutoConfig.register(ServerConfigWrapper.class, PartitioningSerializer.wrap(JanksonConfigSerializer::new));
		serverConfig = ((ServerConfigWrapper)AutoConfig.getConfigHolder(ServerConfigWrapper.class).getConfig()).server;

		// TODO 1.20.6
////		PayloadTypeRegistry.playS2C().register(ConfigSyncPacket.PACKET_ID, ConfigSyncPacket.PACKET_CODEC); // TODO might be necessary, works for now
//		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
//			sender.sendPacket(new ConfigSyncPacket(serverConfig));
//		});

		// Events
		serverConfigSerialized = ServerConfigSync.write(serverConfig);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			sender.sendPacket(ServerConfigSync.ID, serverConfigSerialized);
		});

		StatusEffectsRegistry.registerEffects();

		TemporaryItemModifications.applyFoodComponentModifications();
	}

	public static class ServerConfigSync { // TODO 1.20.6 port to packet
		public static Identifier ID = identifier("server_config_sync");

		public static PacketByteBuf write(ServerConfig serverConfig) {
			var gson = new Gson();
			var json = gson.toJson(serverConfig);
			var buffer = PacketByteBufs.create();
			buffer.writeString(json);
			return buffer;
		}

		public static ServerConfig read(PacketByteBuf buffer) {
			var gson = new Gson();
			var json = buffer.readString();
			return gson.fromJson(json, ServerConfig.class);
		}
	}

	public static Identifier identifier(String path) {
		return new Identifier(MOD_ID, path);
	}
}