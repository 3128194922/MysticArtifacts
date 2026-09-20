package com.uniye.mysticartifacts.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

// Made with Blockbench 5.2.0
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Native Blockbench geometry; package, imports and resource identifiers adapted for MysticArtifacts.


public class FlagSpearModel<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("mysticartifacts", "flag_spear"), "main");
	public static final ResourceLocation TEXTURE = new ResourceLocation("mysticartifacts", "textures/item/flag_spear_model.png");
	private final ModelPart flag_spear;
	private final ModelPart shaft;
	private final ModelPart spearhead;
	private final ModelPart banner;
	private final ModelPart bindings;
	private final ModelPart goldHemBottom1;
	private final ModelPart goldHemBottom2;
	private final ModelPart goldHemBottom3;
	private final ModelPart goldHemBottom4;
	private final ModelPart tasselOne;
	private final ModelPart tasselTwo;

	public FlagSpearModel(ModelPart root) {
		this.flag_spear = root.getChild("flag_spear");
		this.shaft = this.flag_spear.getChild("shaft");
		this.spearhead = this.flag_spear.getChild("spearhead");
		this.banner = this.flag_spear.getChild("banner");
		this.bindings = this.flag_spear.getChild("bindings");
		this.goldHemBottom1 = this.banner.getChild("gold_hem_bottom_1_r1");
		this.goldHemBottom2 = this.banner.getChild("gold_hem_bottom_2_r1");
		this.goldHemBottom3 = this.banner.getChild("gold_hem_bottom_3_r1");
		this.goldHemBottom4 = this.banner.getChild("gold_hem_bottom_4_r1");
		this.tasselOne = this.banner.getChild("tassel_one_r1");
		this.tasselTwo = this.banner.getChild("tassel_two_r1");
	}

	/** Applies a small phase-shifted client-only breeze animation to the flag. */
	public void animateBanner(float time) {
		banner.zRot = Mth.sin(time * 0.055F) * 0.018F;
		banner.yRot = Mth.sin(time * 0.073F + 0.6F) * 0.028F;
		animateSegment(goldHemBottom1, 0.2094F, time, 0.0F, 0.045F);
		animateSegment(goldHemBottom2, -0.1396F, time, 0.7F, 0.065F);
		animateSegment(goldHemBottom3, -0.2793F, time, 1.4F, 0.085F);
		animateSegment(goldHemBottom4, 0.1396F, time, 2.1F, 0.105F);
		tasselOne.zRot = -0.2094F + Mth.sin(time * 0.11F + 0.8F) * 0.16F;
		tasselTwo.zRot = 0.2094F + Mth.sin(time * 0.105F + 2.0F) * 0.16F;
	}

	private static void animateSegment(ModelPart segment, float baseYRot, float time,
								   float phase, float amplitude) {
		segment.yRot = baseYRot + Mth.sin(time * 0.073F + phase) * amplitude;
		segment.xRot = Mth.sin(time * 0.091F + phase + 0.4F) * amplitude * 0.45F;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition flag_spear = partdefinition.addOrReplaceChild("flag_spear", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition shaft = flag_spear.addOrReplaceChild("shaft", CubeListBuilder.create().texOffs(0, 0).addBox(-0.65F, -27.0F, -0.65F, 1.3F, 47.0F, 1.3F, new CubeDeformation(0.0F))
		.texOffs(16, 24).addBox(-0.83F, -4.0F, -0.83F, 1.66F, 9.0F, 1.66F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition spearhead = flag_spear.addOrReplaceChild("spearhead", CubeListBuilder.create().texOffs(48, 16).addBox(-2.4F, -27.3F, -0.7F, 4.8F, 0.8F, 1.4F, new CubeDeformation(0.0F))
		.texOffs(48, 0).addBox(-1.55F, -31.0F, -0.5F, 3.1F, 3.7F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(48, 0).addBox(-1.05F, -33.3F, -0.4F, 2.1F, 2.3F, 0.8F, new CubeDeformation(0.0F))
		.texOffs(48, 0).addBox(-0.5F, -35.5F, -0.3F, 1.0F, 2.2F, 0.6F, new CubeDeformation(0.0F))
		.texOffs(50, 0).addBox(-0.28F, -33.5F, -0.65F, 0.56F, 5.8F, 1.3F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition terminal_point_r1 = spearhead.addOrReplaceChild("terminal_point_r1", CubeListBuilder.create().texOffs(50, 0).addBox(-0.5F, -0.5F, -0.3F, 1.0F, 1.0F, 0.6F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -35.5F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition banner = flag_spear.addOrReplaceChild("banner", CubeListBuilder.create().texOffs(16, 0).addBox(-4.0F, -24.0F, -0.15F, 3.0F, 10.2F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(32, 40).addBox(-4.0F, -24.0F, -0.19F, 3.0F, 0.55F, 0.38F, new CubeDeformation(0.0F))
		.texOffs(32, 40).addBox(-4.0F, -14.25F, -0.19F, 3.0F, 0.45F, 0.38F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition tassel_two_r1 = banner.addOrReplaceChild("tassel_two_r1", CubeListBuilder.create().texOffs(16, 30).addBox(-0.3F, 0.0F, -0.2F, 0.6F, 3.5F, 0.6F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.3F, -13.9F, 0.0F, 0.0F, 0.0F, 0.2094F));

		PartDefinition tassel_one_r1 = banner.addOrReplaceChild("tassel_one_r1", CubeListBuilder.create().texOffs(16, 30).addBox(-0.3F, 0.0F, -0.4F, 0.6F, 4.4F, 0.6F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.3F, -13.9F, 0.0F, 0.0F, 0.0F, -0.2094F));

		PartDefinition sigil_inset_r1 = banner.addOrReplaceChild("sigil_inset_r1", CubeListBuilder.create().texOffs(48, 24).addBox(-0.65F, -0.65F, -0.95F, 1.3F, 1.3F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(32, 40).addBox(-1.2F, -1.2F, -0.9F, 2.4F, 2.4F, 1.9F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.5F, -18.7F, 0.2F, 0.0F, 0.0F, 0.7854F));

		PartDefinition gold_hem_bottom_4_r1 = banner.addOrReplaceChild("gold_hem_bottom_4_r1", CubeListBuilder.create().texOffs(32, 40).addBox(-2.0F, 6.55F, -0.19F, 2.0F, 0.45F, 0.38F, new CubeDeformation(0.0F))
		.texOffs(32, 40).addBox(-2.0F, 0.0F, -0.19F, 2.0F, 0.55F, 0.38F, new CubeDeformation(0.0F))
		.texOffs(32, 0).addBox(-2.0F, 0.0F, -0.15F, 2.0F, 7.0F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-13.0F, -24.0F, -0.62F, 0.0F, 0.1396F, 0.0F));

		PartDefinition gold_hem_bottom_3_r1 = banner.addOrReplaceChild("gold_hem_bottom_3_r1", CubeListBuilder.create().texOffs(32, 40).addBox(-3.0F, 9.35F, -0.19F, 3.0F, 0.45F, 0.38F, new CubeDeformation(0.0F))
		.texOffs(32, 40).addBox(-3.0F, 0.0F, -0.19F, 3.0F, 0.55F, 0.38F, new CubeDeformation(0.0F))
		.texOffs(28, 0).addBox(-3.0F, 0.0F, -0.15F, 3.0F, 9.8F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.0F, -24.0F, 0.21F, 0.0F, -0.2793F, 0.0F));

		PartDefinition gold_hem_bottom_2_r1 = banner.addOrReplaceChild("gold_hem_bottom_2_r1", CubeListBuilder.create().texOffs(32, 40).addBox(-3.0F, 10.15F, -0.19F, 3.0F, 0.45F, 0.38F, new CubeDeformation(0.0F))
		.texOffs(32, 40).addBox(-3.0F, 0.0F, -0.19F, 3.0F, 0.55F, 0.38F, new CubeDeformation(0.0F))
		.texOffs(24, 0).addBox(-3.0F, 0.0F, -0.15F, 3.0F, 10.6F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.0F, -24.0F, 0.63F, 0.0F, -0.1396F, 0.0F));

		PartDefinition gold_hem_bottom_1_r1 = banner.addOrReplaceChild("gold_hem_bottom_1_r1", CubeListBuilder.create().texOffs(32, 40).addBox(-3.0F, 10.55F, -0.19F, 3.0F, 0.45F, 0.38F, new CubeDeformation(0.0F))
		.texOffs(32, 40).addBox(-3.0F, 0.0F, -0.19F, 3.0F, 0.55F, 0.38F, new CubeDeformation(0.0F))
		.texOffs(20, 0).addBox(-3.0F, 0.0F, -0.15F, 3.0F, 11.0F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -24.0F, 0.0F, 0.0F, 0.2094F, 0.0F));

		PartDefinition bindings = flag_spear.addOrReplaceChild("bindings", CubeListBuilder.create().texOffs(32, 40).addBox(-0.9F, 19.0F, -0.9F, 1.8F, 2.0F, 1.8F, new CubeDeformation(0.0F))
		.texOffs(32, 40).addBox(-1.0F, -28.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(32, 40).addBox(-0.95F, 4.35F, -0.95F, 1.9F, 0.65F, 1.9F, new CubeDeformation(0.0F))
		.texOffs(32, 40).addBox(-0.95F, -4.25F, -0.95F, 1.9F, 0.65F, 1.9F, new CubeDeformation(0.0F))
		.texOffs(32, 40).addBox(-0.95F, -14.65F, -0.95F, 1.9F, 0.65F, 1.9F, new CubeDeformation(0.0F))
		.texOffs(32, 40).addBox(-0.95F, -24.65F, -0.95F, 1.9F, 0.65F, 1.9F, new CubeDeformation(0.0F))
		.texOffs(32, 40).addBox(-5.0F, -24.5F, -0.45F, 4.5F, 0.7F, 0.9F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		flag_spear.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
