package com.customizable.client;

import com.customizable.CustomPaintedBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class CustomPaintedRenderer implements BlockEntityRenderer<CustomPaintedBlockEntity> {
    public CustomPaintedRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(CustomPaintedBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        String baseId = be.getBaseBlockId();
        ResourceLocation texture = getTexture(baseId);

        // Render solid block
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutout(texture));
        
        int color = be.getColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        renderCube(poseStack, vertexConsumer, r, g, b, combinedLight, combinedOverlay);
    }

    private ResourceLocation getTexture(String baseId) {
        if (baseId.contains("wool")) return new ResourceLocation("minecraft", "textures/block/white_wool.png");
        if (baseId.contains("concrete_powder")) return new ResourceLocation("minecraft", "textures/block/white_concrete_powder.png");
        if (baseId.contains("concrete")) return new ResourceLocation("minecraft", "textures/block/white_concrete.png");
        if (baseId.contains("terracotta")) return new ResourceLocation("minecraft", "textures/block/white_terracotta.png");
        if (baseId.contains("carpet")) return new ResourceLocation("minecraft", "textures/block/white_wool.png");
        if (baseId.contains("shulker")) return new ResourceLocation("minecraft", "textures/entity/shulker/shulker_white.png");
        if (baseId.contains("glass")) return new ResourceLocation("minecraft", "textures/block/white_stained_glass.png");
        
        return new ResourceLocation("minecraft", "textures/block/white_wool.png");
    }

    private void renderCube(PoseStack poseStack, VertexConsumer vb, float r, float g, float b, int light, int overlay) {
        Matrix4f mat = poseStack.last().pose();
        Matrix3f norm = poseStack.last().normal();

        // Use slightly smaller cube to avoid z-fighting with adjacent blocks
        float s = 0.0001f;
        float e = 1.0f - s;

        // Front (South)
        vertex(mat, norm, vb, s, s, e, 0, 1, r, g, b, 0, 0, 1, light, overlay);
        vertex(mat, norm, vb, e, s, e, 1, 1, r, g, b, 0, 0, 1, light, overlay);
        vertex(mat, norm, vb, e, e, e, 1, 0, r, g, b, 0, 0, 1, light, overlay);
        vertex(mat, norm, vb, s, e, e, 0, 0, r, g, b, 0, 0, 1, light, overlay);

        // Back (North)
        vertex(mat, norm, vb, e, s, s, 0, 1, r, g, b, 0, 0, -1, light, overlay);
        vertex(mat, norm, vb, s, s, s, 1, 1, r, g, b, 0, 0, -1, light, overlay);
        vertex(mat, norm, vb, s, e, s, 1, 0, r, g, b, 0, 0, -1, light, overlay);
        vertex(mat, norm, vb, e, e, s, 0, 0, r, g, b, 0, 0, -1, light, overlay);

        // Up
        vertex(mat, norm, vb, s, e, e, 0, 1, r, g, b, 0, 1, 0, light, overlay);
        vertex(mat, norm, vb, e, e, e, 1, 1, r, g, b, 0, 1, 0, light, overlay);
        vertex(mat, norm, vb, e, e, s, 1, 0, r, g, b, 0, 1, 0, light, overlay);
        vertex(mat, norm, vb, s, e, s, 0, 0, r, g, b, 0, 1, 0, light, overlay);

        // Down
        vertex(mat, norm, vb, s, s, s, 0, 1, r, g, b, 0, -1, 0, light, overlay);
        vertex(mat, norm, vb, e, s, s, 1, 1, r, g, b, 0, -1, 0, light, overlay);
        vertex(mat, norm, vb, e, s, e, 1, 0, r, g, b, 0, -1, 0, light, overlay);
        vertex(mat, norm, vb, s, s, e, 0, 0, r, g, b, 0, -1, 0, light, overlay);

        // Right (East)
        vertex(mat, norm, vb, e, s, e, 0, 1, r, g, b, 1, 0, 0, light, overlay);
        vertex(mat, norm, vb, e, s, s, 1, 1, r, g, b, 1, 0, 0, light, overlay);
        vertex(mat, norm, vb, e, e, s, 1, 0, r, g, b, 1, 0, 0, light, overlay);
        vertex(mat, norm, vb, e, e, e, 0, 0, r, g, b, 1, 0, 0, light, overlay);

        // Left (West)
        vertex(mat, norm, vb, s, s, s, 0, 1, r, g, b, -1, 0, 0, light, overlay);
        vertex(mat, norm, vb, s, s, e, 1, 1, r, g, b, -1, 0, 0, light, overlay);
        vertex(mat, norm, vb, s, e, e, 1, 0, r, g, b, -1, 0, 0, light, overlay);
        vertex(mat, norm, vb, s, e, s, 0, 0, r, g, b, -1, 0, 0, light, overlay);
    }

    private void vertex(Matrix4f mat, Matrix3f norm, VertexConsumer vb, float x, float y, float z, float u, float v, float r, float g, float b, int nx, int ny, int nz, int light, int overlay) {
        vb.vertex(mat, x, y, z).color(r, g, b, 1.0f).uv(u, v).overlayCoords(overlay).uv2(light).normal(norm, (float)nx, (float)ny, (float)nz).endVertex();
    }
}
