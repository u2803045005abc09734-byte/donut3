package com.donutclient.render;

import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import com.donutclient.module.FinderModule;
import com.donutclient.module.ModuleManager;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws translucent boxes in the world. Outlines are built from thin boxes so their thickness
 * is fully adjustable. Two pipelines are used: one ignoring depth (through walls), one respecting it.
 */
public final class BoxRenderer {
	public static final BoxRenderer INSTANCE = new BoxRenderer();

	private static final RenderPipeline WALLS_PIPELINE = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath("donutclient", "pipeline/boxes_through_walls"))
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withBlend(BlendFunction.TRANSLUCENT)
			.build());

	private static final RenderPipeline DEPTH_PIPELINE = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath("donutclient", "pipeline/boxes_depth_tested"))
			.withBlend(BlendFunction.TRANSLUCENT)
			.build());

	private final Batch walls = new Batch(WALLS_PIPELINE, "donutclient walls");
	private final Batch depth = new Batch(DEPTH_PIPELINE, "donutclient depth");
	private Matrix4fc pose;

	private BoxRenderer() {}

	/** Called every frame from the world render event. */
	public void render(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) {
			return;
		}

		boolean any = false;
		for (FinderModule m : ModuleManager.finders()) {
			if (m.hasHighlights()) {
				any = true;
				break;
			}
		}
		if (!any) {
			return;
		}

		PoseStack matrices = context.matrices();
		Vec3 camera = context.worldState().cameraRenderState.pos;

		matrices.pushPose();
		matrices.translate(-camera.x, -camera.y, -camera.z);
		this.pose = matrices.last().pose();
		for (FinderModule m : ModuleManager.finders()) {
			if (m.hasHighlights()) {
				m.emit(this, camera);
			}
		}
		this.pose = null;
		matrices.popPose();

		walls.flush(mc);
		depth.flush(mc);
	}

	public void fill(AABB b, float r, float g, float bl, float a, boolean throughWalls) {
		if (a <= 0.004f || pose == null) {
			return;
		}
		(throughWalls ? walls : depth).box(pose, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, r, g, bl, a);
	}

	/** Twelve thin prisms that do not overlap, so translucent corners are not drawn twice. */
	public void outline(AABB b, double thickness, float r, float g, float bl, float a, boolean throughWalls) {
		if (a <= 0.004f || pose == null) {
			return;
		}
		Batch batch = throughWalls ? walls : depth;
		float h = (float) (thickness / 2.0);
		float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
		float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;

		float[] ys = {y0, y1};
		float[] zs = {z0, z1};
		float[] xs = {x0, x1};

		// Edges along X (full length including corners)
		for (float y : ys) {
			for (float z : zs) {
				batch.box(pose, x0 - h, y - h, z - h, x1 + h, y + h, z + h, r, g, bl, a);
			}
		}
		// Edges along Y (between the corner pieces)
		for (float x : xs) {
			for (float z : zs) {
				batch.box(pose, x - h, y0 + h, z - h, x + h, y1 - h, z + h, r, g, bl, a);
			}
		}
		// Edges along Z (between the corner pieces)
		for (float x : xs) {
			for (float y : ys) {
				batch.box(pose, x - h, y - h, z0 + h, x + h, y + h, z1 - h, r, g, bl, a);
			}
		}
	}

	public void close() {
		walls.close();
		depth.close();
	}

	private static final class Batch {
		private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
		private static final Vector3f MODEL_OFFSET = new Vector3f();
		private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

		private final RenderPipeline pipeline;
		private final String label;
		private final ByteBufferBuilder allocator = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
		private BufferBuilder buffer;
		private MappableRingBuffer vertexBuffer;

		Batch(RenderPipeline pipeline, String label) {
			this.pipeline = pipeline;
			this.label = label;
		}

		void box(Matrix4fc m, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a) {
			if (buffer == null) {
				buffer = new BufferBuilder(allocator, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
			}
			// Front
			buffer.addVertex(m, minX, minY, maxZ).setColor(r, g, b, a);
			buffer.addVertex(m, maxX, minY, maxZ).setColor(r, g, b, a);
			buffer.addVertex(m, maxX, maxY, maxZ).setColor(r, g, b, a);
			buffer.addVertex(m, minX, maxY, maxZ).setColor(r, g, b, a);
			// Back
			buffer.addVertex(m, maxX, minY, minZ).setColor(r, g, b, a);
			buffer.addVertex(m, minX, minY, minZ).setColor(r, g, b, a);
			buffer.addVertex(m, minX, maxY, minZ).setColor(r, g, b, a);
			buffer.addVertex(m, maxX, maxY, minZ).setColor(r, g, b, a);
			// Left
			buffer.addVertex(m, minX, minY, minZ).setColor(r, g, b, a);
			buffer.addVertex(m, minX, minY, maxZ).setColor(r, g, b, a);
			buffer.addVertex(m, minX, maxY, maxZ).setColor(r, g, b, a);
			buffer.addVertex(m, minX, maxY, minZ).setColor(r, g, b, a);
			// Right
			buffer.addVertex(m, maxX, minY, maxZ).setColor(r, g, b, a);
			buffer.addVertex(m, maxX, minY, minZ).setColor(r, g, b, a);
			buffer.addVertex(m, maxX, maxY, minZ).setColor(r, g, b, a);
			buffer.addVertex(m, maxX, maxY, maxZ).setColor(r, g, b, a);
			// Top
			buffer.addVertex(m, minX, maxY, maxZ).setColor(r, g, b, a);
			buffer.addVertex(m, maxX, maxY, maxZ).setColor(r, g, b, a);
			buffer.addVertex(m, maxX, maxY, minZ).setColor(r, g, b, a);
			buffer.addVertex(m, minX, maxY, minZ).setColor(r, g, b, a);
			// Bottom
			buffer.addVertex(m, minX, minY, minZ).setColor(r, g, b, a);
			buffer.addVertex(m, maxX, minY, minZ).setColor(r, g, b, a);
			buffer.addVertex(m, maxX, minY, maxZ).setColor(r, g, b, a);
			buffer.addVertex(m, minX, minY, maxZ).setColor(r, g, b, a);
		}

		void flush(Minecraft client) {
			if (buffer == null) {
				return;
			}
			MeshData built = buffer.buildOrThrow();
			buffer = null;
			MeshData.DrawState drawState = built.drawState();
			VertexFormat format = drawState.format();

			GpuBuffer vertices = upload(drawState, format, built);
			draw(client, built, drawState, vertices, format);

			vertexBuffer.rotate();
		}

		private GpuBuffer upload(MeshData.DrawState drawState, VertexFormat format, MeshData built) {
			int size = drawState.vertexCount() * format.getVertexSize();

			if (vertexBuffer == null || vertexBuffer.size() < size) {
				if (vertexBuffer != null) {
					vertexBuffer.close();
				}
				vertexBuffer = new MappableRingBuffer(() -> label + " vertices", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, size);
			}

			CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
			try (GpuBuffer.MappedView view = encoder.mapBuffer(vertexBuffer.currentBuffer().slice(0, built.vertexBuffer().remaining()), false, true)) {
				MemoryUtil.memCopy(built.vertexBuffer(), view.data());
			}
			return vertexBuffer.currentBuffer();
		}

		private void draw(Minecraft client, MeshData built, MeshData.DrawState drawState, GpuBuffer vertices, VertexFormat format) {
			GpuBuffer indices;
			VertexFormat.IndexType indexType;

			if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
				built.sortQuads(allocator, RenderSystem.getProjectionType().vertexSorting());
				indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(built.indexBuffer());
				indexType = built.drawState().indexType();
			} else {
				RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
				indices = shapeIndexBuffer.getBuffer(drawState.indexCount());
				indexType = shapeIndexBuffer.type();
			}

			GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
				.writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

			try (RenderPass pass = RenderSystem.getDevice()
				.createCommandEncoder()
				.createRenderPass(() -> label + " rendering",
					client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
					client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
				pass.setPipeline(pipeline);
				RenderSystem.bindDefaultUniforms(pass);
				pass.setUniform("DynamicTransforms", dynamicTransforms);
				pass.setVertexBuffer(0, vertices);
				pass.setIndexBuffer(indices, indexType);
				pass.drawIndexed(0, 0, drawState.indexCount(), 1);
			}

			built.close();
		}

		void close() {
			allocator.close();
			if (vertexBuffer != null) {
				vertexBuffer.close();
				vertexBuffer = null;
			}
		}
	}
}
