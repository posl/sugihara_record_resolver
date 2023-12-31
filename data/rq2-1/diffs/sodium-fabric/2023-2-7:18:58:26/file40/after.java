package me.jellysquid.mods.sodium.mixin.core.pipeline.vertex;


import me.jellysquid.mods.sodium.client.render.vertex.buffer.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.render.vertex.VertexTransformers;
import net.minecraft.client.render.VertexConsumer;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

public class MixinVertexConsumers {
    @Mixin(targets = "net/minecraft/client/render/VertexConsumers$Dual")
    public static class MixinDual implements VertexBufferWriter {
        @Shadow
        @Final
        private VertexConsumer first;

        @Shadow
        @Final
        private VertexConsumer second;

        @Override
        public void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format) {
            VertexTransformers.copyPush(this.first, stack, ptr, count, format);
            VertexTransformers.copyPush(this.second, stack, ptr, count, format);
        }
    }

    @Mixin(targets = "net/minecraft/client/render/VertexConsumers$Union")
    public static class MixinUnion implements VertexBufferWriter {
        @Shadow
        @Final
        private VertexConsumer[] delegates;

        @Override
        public void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format) {
            for (var delegate : this.delegates) {
                VertexTransformers.copyPush(delegate, stack, ptr, count, format);
            }
        }
    }
}
