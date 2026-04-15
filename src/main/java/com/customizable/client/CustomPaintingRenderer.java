package com.customizable.client;

import com.customizable.CustomPaintingBlock;
import com.customizable.CustomPaintingBlockEntity;
import com.customizable.customizable;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class CustomPaintingRenderer implements BlockEntityRenderer<CustomPaintingBlockEntity> {
    private static final Map<String, ResourceLocation> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<ResourceLocation>> LOADING = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation LOADING_TEX = new ResourceLocation("minecraft", "textures/item/painting.png");

    public CustomPaintingRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(CustomPaintingBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        BlockState state = be.getBlockState();
        if (!state.hasProperty(CustomPaintingBlock.HAS_PAINTING) || !state.getValue(CustomPaintingBlock.HAS_PAINTING)) {
            // #region agent log
            com.customizable.debug.DebugNdjsonLog.logOnce(
                    "paintEarly:" + be.getBlockPos().asLong(),
                    "P4",
                    "CustomPaintingRenderer.render",
                    "skip !HAS_PAINTING",
                    "{}");
            // #endregion
            return;
        }

        String path = be.getFilePath();
        if (path == null || path.isEmpty()) {
            // #region agent log
            com.customizable.debug.DebugNdjsonLog.logOnce(
                    "paintNoPath:" + be.getBlockPos().asLong(),
                    "P4",
                    "CustomPaintingRenderer.render",
                    "skip empty path",
                    "{}");
            // #endregion
            return;
        }

        ResourceLocation tex = CACHE.get(path);
        if (tex == null) {
            loadTexture(path);
            tex = LOADING_TEX;
        }

        Direction facing = Direction.NORTH;
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            facing = state.getValue(HorizontalDirectionalBlock.FACING);
        }

        int wBlocks = be.getWidth();
        int hBlocks = be.getHeight();

        poseStack.pushPose();
        
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        // Align to the front face of the block (0.5 is the exact face)
        poseStack.translate(0, 0, 0.501); 
        
        // Slightly scale down to avoid edge artifacts
        poseStack.scale(0.999f, 0.999f, 1f);

        float width = (float)wBlocks;
        float height = (float)hBlocks;
        
        // Center the painting relative to the anchor block
        poseStack.translate((width - 1) * 0.5f, (height - 1) * 0.5f, 0);

        renderPainting(poseStack, bufferSource, tex, width, height, combinedLight, combinedOverlay);

        poseStack.popPose();
    }

    private void renderPainting(PoseStack poseStack, MultiBufferSource buffer, ResourceLocation tex, float width, float height, int light, int overlay) {
        PoseStack.Pose last = poseStack.last();
        Matrix4f matrix4f = last.pose();
        Matrix3f matrix3f = last.normal();
        
        // Use EntityCutout for consistent rendering
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(tex));

        float hw = width / 2.0f;
        float hh = height / 2.0f;

        // Front face
        // We use standard Minecraft UVs: (0,0) is top-left, (1,1) is bottom-right.
        // Vertex order: bottom-left, bottom-right, top-right, top-left (counter-clockwise from front)
        
        // Bottom-left
        vertex(matrix4f, matrix3f, vertexConsumer, -hw, -hh, 0.0f, 0.0f, 1.0f, 0, 0, 1, light, overlay);
        // Bottom-right
        vertex(matrix4f, matrix3f, vertexConsumer,  hw, -hh, 0.0f, 1.0f, 1.0f, 0, 0, 1, light, overlay);
        // Top-right
        vertex(matrix4f, matrix3f, vertexConsumer,  hw,  hh, 0.0f, 1.0f, 0.0f, 0, 0, 1, light, overlay);
        // Top-left
        vertex(matrix4f, matrix3f, vertexConsumer, -hw,  hh, 0.0f, 0.0f, 0.0f, 0, 0, 1, light, overlay);

        // Back face (so it's visible from behind)
        vertex(matrix4f, matrix3f, vertexConsumer, -hw,  hh, -0.01f, 0.0f, 0.0f, 0, 0, -1, light, overlay);
        vertex(matrix4f, matrix3f, vertexConsumer,  hw,  hh, -0.01f, 1.0f, 0.0f, 0, 0, -1, light, overlay);
        vertex(matrix4f, matrix3f, vertexConsumer,  hw, -hh, -0.01f, 1.0f, 1.0f, 0, 0, -1, light, overlay);
        vertex(matrix4f, matrix3f, vertexConsumer, -hw, -hh, -0.01f, 0.0f, 1.0f, 0, 0, -1, light, overlay);
    }

    private void vertex(Matrix4f matrix4f, Matrix3f matrix3f, VertexConsumer vertexConsumer, float x, float y, float z, float u, float v, int nx, int ny, int nz, int light, int overlay) {
        vertexConsumer.vertex(matrix4f, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(matrix3f, (float)nx, (float)ny, (float)nz)
                .endVertex();
    }

    private void loadTexture(String path) {
        if (LOADING.containsKey(path) || CACHE.containsKey(path)) return;

        LOGGER.info("Starting to load custom painting: {}", path);
        CompletableFuture<ResourceLocation> future = CompletableFuture.supplyAsync(() -> {
            try {
                NativeImage image;
                if (path.startsWith("http")) {
                    try (InputStream is = new java.net.URL(path).openStream()) {
                        image = NativeImage.read(is);
                    }
                } else {
                    File file = new File(path);
                    if (!file.exists()) {
                        LOGGER.error("Custom painting file does not exist: {}", path);
                        // #region agent log
                        com.customizable.debug.DebugNdjsonLog.log(
                                "P5",
                                "CustomPaintingRenderer.loadTexture",
                                "file missing",
                                com.customizable.debug.DebugNdjsonLog.pathFieldsJson(path));
                        // #endregion
                        return null;
                    }
                    // Read all bytes first for more robust stream handling
                    byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                    try (InputStream is = new java.io.ByteArrayInputStream(bytes)) {
                        image = NativeImage.read(is);
                    }
                }

                if (image != null) {
                    final NativeImage finalImage = image;
                    String id = "p" + Math.abs(path.hashCode());
                    ResourceLocation rl = new ResourceLocation(customizable.MODID, "dynamic/" + id);
                    
                    // Register on the main thread
                    Minecraft.getInstance().execute(() -> {
                        try {
                            TextureManager tm = Minecraft.getInstance().getTextureManager();
                            tm.register(rl, new DynamicTexture(finalImage));
                            CACHE.put(path, rl);
                            LOGGER.info("Successfully registered custom texture: {} ({}x{}) for path {}", rl, finalImage.getWidth(), finalImage.getHeight(), path);
                            // #region agent log
                            com.customizable.debug.DebugNdjsonLog.log(
                                    "P5",
                                    "CustomPaintingRenderer.loadTexture",
                                    "texture registered",
                                    com.customizable.debug.DebugNdjsonLog.mergeObjects(
                                            "{\"iw\":" + finalImage.getWidth() + ",\"ih\":" + finalImage.getHeight() + "}",
                                            com.customizable.debug.DebugNdjsonLog.pathFieldsJson(path)));
                            // #endregion
                        } catch (Exception ex) {
                            LOGGER.error("Error registering custom texture in main thread: " + rl, ex);
                            // #region agent log
                            com.customizable.debug.DebugNdjsonLog.log(
                                    "P5",
                                    "CustomPaintingRenderer.loadTexture",
                                    "register failed",
                                    com.customizable.debug.DebugNdjsonLog.mergeObjects(
                                            com.customizable.debug.DebugNdjsonLog.pathFieldsJson(path),
                                            com.customizable.debug.DebugNdjsonLog.throwableFields(ex)));
                            // #endregion
                        }
                    });
                    return rl;
                } else {
                    LOGGER.error("NativeImage.read returned null for custom painting: {}", path);
                    // #region agent log
                    com.customizable.debug.DebugNdjsonLog.log(
                            "P5",
                            "CustomPaintingRenderer.loadTexture",
                            "nativeimage null",
                            com.customizable.debug.DebugNdjsonLog.pathFieldsJson(path));
                    // #endregion
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load custom painting from path: " + path, e);
                // #region agent log
                com.customizable.debug.DebugNdjsonLog.log(
                        "P5",
                        "CustomPaintingRenderer.loadTexture",
                        "load exception",
                        com.customizable.debug.DebugNdjsonLog.mergeObjects(
                                com.customizable.debug.DebugNdjsonLog.pathFieldsJson(path),
                                com.customizable.debug.DebugNdjsonLog.throwableFields(e)));
                // #endregion
            }
            return null;
        });

        future.thenAccept(rl -> {
            if (rl == null) {
                LOADING.remove(path);
            }
        });

        LOADING.put(path, future);
    }
}

