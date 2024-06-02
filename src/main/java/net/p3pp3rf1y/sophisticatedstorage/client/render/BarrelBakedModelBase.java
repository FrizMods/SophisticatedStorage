package net.p3pp3rf1y.sophisticatedstorage.client.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Quaternion;
import com.mojang.math.Transformation;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.client.model.QuadTransformer;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.common.model.TransformationHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.BarrelMaterial;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.BlockSide;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;
import net.p3pp3rf1y.sophisticatedstorage.item.BarrelBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.p3pp3rf1y.sophisticatedstorage.client.render.DisplayItemRenderer.*;

public abstract class BarrelBakedModelBase implements IDynamicBakedModel {
	static {
		ImmutableMap.Builder<ItemTransforms.TransformType, Transformation> builder = ImmutableMap.builder();
		builder.put(ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND, new Transformation(
				new Vector3f(0, 2.5f / 16f, 0),
				new Quaternion(75, 45, 0, true),
				new Vector3f(0.375f, 0.375f, 0.375f), null
		));
		builder.put(ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, new Transformation(
				new Vector3f(0, 2.5f / 16f, 0),
				new Quaternion(75, 45, 0, true),
				new Vector3f(0.375f, 0.375f, 0.375f), null
		));
		builder.put(ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND, new Transformation(
				new Vector3f(0, 0, 0),
				new Quaternion(0, 225, 0, true),
				new Vector3f(0.4f, 0.4f, 0.4f), null
		));
		builder.put(ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, new Transformation(
				new Vector3f(0, 0, 0),
				new Quaternion(0, 45, 0, true),
				new Vector3f(0.4f, 0.4f, 0.4f), null
		));
		builder.put(ItemTransforms.TransformType.HEAD, new Transformation(
				new Vector3f(0, 14.25f / 16f, 0),
				new Quaternion(0, 0, 0, true),
				new Vector3f(1, 1, 1), null
		));
		builder.put(ItemTransforms.TransformType.GUI, new Transformation(
				new Vector3f(0, 0, 0),
				new Quaternion(30, 45, 0, true),
				new Vector3f(0.625f, 0.625f, 0.625f), null
		));
		builder.put(ItemTransforms.TransformType.GROUND, new Transformation(
				new Vector3f(0, 3 / 16f, 0),
				new Quaternion(0, 0, 0, true),
				new Vector3f(0.25f, 0.25f, 0.25f), null
		));
		builder.put(ItemTransforms.TransformType.FIXED, new Transformation(
				new Vector3f(0, 0, 0),
				new Quaternion(0, 0, 0, true),
				new Vector3f(0.5f, 0.5f, 0.5f), null
		));
		TRANSFORMS = builder.build();

		ITEM_TRANSFORMS = createItemTransforms();
	}

	@SuppressWarnings("deprecation")
	private static ItemTransforms createItemTransforms() {
		return new ItemTransforms(fromTransformation(TRANSFORMS.get(ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND)), fromTransformation(TRANSFORMS.get(ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND)),
				fromTransformation(TRANSFORMS.get(ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND)), fromTransformation(TRANSFORMS.get(ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND)),
				fromTransformation(TRANSFORMS.get(ItemTransforms.TransformType.HEAD)), fromTransformation(TRANSFORMS.get(ItemTransforms.TransformType.GUI)),
				fromTransformation(TRANSFORMS.get(ItemTransforms.TransformType.GROUND)), fromTransformation(TRANSFORMS.get(ItemTransforms.TransformType.FIXED)));
	}

	@SuppressWarnings("deprecation")
	private static ItemTransform fromTransformation(Transformation transformation) {
		return new ItemTransform(transformation.getLeftRotation().toXYZ(), transformation.getTranslation(), transformation.getScale());
	}

	private static final Map<ItemTransforms.TransformType, Transformation> TRANSFORMS;
	private static final ItemTransforms ITEM_TRANSFORMS;
	private static final QuadTransformer MOVE_TO_CORNER = new QuadTransformer(new Transformation(new Vector3f(-.5f, -.5f, -.5f), null, null, null));
	public static final Map<Direction, QuadTransformer> DIRECTION_ROTATES = Map.of(
			Direction.UP, BarrelBakedModelBase.getDirectionRotationTransform(Direction.UP),
			Direction.DOWN, BarrelBakedModelBase.getDirectionRotationTransform(Direction.DOWN),
			Direction.NORTH, BarrelBakedModelBase.getDirectionRotationTransform(Direction.NORTH),
			Direction.SOUTH, BarrelBakedModelBase.getDirectionRotationTransform(Direction.SOUTH),
			Direction.WEST, BarrelBakedModelBase.getDirectionRotationTransform(Direction.WEST),
			Direction.EAST, BarrelBakedModelBase.getDirectionRotationTransform(Direction.EAST)
	);
	private static final LoadingCache<Direction, Cache<Integer, QuadTransformer>> DIRECTION_MOVES_3D_ITEMS = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build(new CacheLoader<>() {
		@Override
		public Cache<Integer, QuadTransformer> load(Direction key) {
			return CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build();
		}
	});
	private static final QuadTransformer SCALE_BIG_2D_ITEM = new QuadTransformer(new Transformation(null, null, new Vector3f(BIG_2D_ITEM_SCALE, BIG_2D_ITEM_SCALE, BIG_2D_ITEM_SCALE), null));
	private static final QuadTransformer SCALE_SMALL_3D_ITEM = new QuadTransformer(new Transformation(null, null, new Vector3f(SMALL_3D_ITEM_SCALE, SMALL_3D_ITEM_SCALE, SMALL_3D_ITEM_SCALE), null));
	private static final QuadTransformer SCALE_SMALL_2D_ITEM = new QuadTransformer(new Transformation(null, null, new Vector3f(SMALL_2D_ITEM_SCALE, SMALL_2D_ITEM_SCALE, SMALL_2D_ITEM_SCALE), null));
	private static final Cache<Integer, QuadTransformer> DIRECTION_MOVE_BACK_TO_SIDE = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build();
	private static final ModelProperty<String> WOOD_NAME = new ModelProperty<>();
	private static final ModelProperty<Boolean> IS_PACKED = new ModelProperty<>();
	private static final ModelProperty<Boolean> SHOWS_LOCK = new ModelProperty<>();
	private static final ModelProperty<Boolean> SHOWS_TIER = new ModelProperty<>();
	private static final ModelProperty<Boolean> HAS_MAIN_COLOR = new ModelProperty<>();
	private static final ModelProperty<Boolean> HAS_ACCENT_COLOR = new ModelProperty<>();
	private static final ModelProperty<List<RenderInfo.DisplayItem>> DISPLAY_ITEMS = new ModelProperty<>();
	private static final ModelProperty<List<Integer>> INACCESSIBLE_SLOTS = new ModelProperty<>();
	private static final Cache<Integer, List<BakedQuad>> BAKED_QUADS_CACHE = CacheBuilder.newBuilder().expireAfterAccess(15L, TimeUnit.MINUTES).build();
	private static final Map<Integer, QuadTransformer> DISPLAY_ROTATIONS = new HashMap<>();
	private static final ModelProperty<Map<BarrelMaterial, ResourceLocation>> MATERIALS = new ModelProperty<>();
	private static final List<BarrelMaterial> PARTICLE_ICON_MATERIAL_PRIORITY = List.of(BarrelMaterial.ALL, BarrelMaterial.ALL_BUT_TRIM, BarrelMaterial.TOP_ALL, BarrelMaterial.TOP);

	public static void invalidateCache() {
		DIRECTION_MOVES_3D_ITEMS.invalidateAll();
		DIRECTION_MOVE_BACK_TO_SIDE.invalidateAll();
		BAKED_QUADS_CACHE.invalidateAll();
	}

	protected final Map<String, Map<BarrelModelPart, BakedModel>> woodModelParts;

	private final ItemOverrides barrelItemOverrides;
	private Item barrelItem = Items.AIR;
	@Nullable
	private String barrelWoodName = null;
	private boolean barrelHasMainColor = false;
	private boolean barrelHasAccentColor = false;
	private boolean barrelIsPacked = false;
	private boolean barrelShowsTier = true;
	private Map<BarrelMaterial, ResourceLocation> barrelMaterials = new EnumMap<>(BarrelMaterial.class);

	private boolean flatTop = false;
	private final Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData;
	private final Map<String, Map<BarrelModelPart, BakedModel>> woodPartitionedModelParts;
	private final Cache<Integer, BakedModel> dynamicBakedModelCache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();
	private final ModelBakery modelBakery;

	protected BarrelBakedModelBase(ModelBakery modelBakery, Map<String, Map<BarrelModelPart, BakedModel>> woodModelParts, @Nullable BakedModel flatTopModel, Map<String, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData>> woodDynamicBakingData, Map<String, Map<BarrelModelPart, BakedModel>> woodPartitionedModelParts) {
		this.modelBakery = modelBakery;
		this.woodModelParts = woodModelParts;
		barrelItemOverrides = new BarrelItemOverrides(this, flatTopModel);
		this.woodDynamicBakingData = woodDynamicBakingData;
		this.woodPartitionedModelParts = woodPartitionedModelParts;
	}

	private static QuadTransformer getDirectionRotationTransform(Direction dir) {
		return new QuadTransformer(new Transformation(null, DisplayItemRenderer.getNorthBasedRotation(dir), null, null));
	}

	private QuadTransformer getDirectionMoveBackToSide(BlockState state, Direction dir, float distFromCenter, int displayItemIndex, int displayItemCount) {
		int hash = calculateMoveBackToSideHash(state, dir, distFromCenter, displayItemIndex, displayItemCount);
		QuadTransformer transform = DIRECTION_MOVE_BACK_TO_SIDE.getIfPresent(hash);
		if (transform == null) {
			Vec3i normal = dir.getNormal();
			Vector3f offset = new Vector3f(distFromCenter, distFromCenter, distFromCenter);
			offset.mul(normal.getX(), normal.getY(), normal.getZ());
			Vector3f frontOffset = DisplayItemRenderer.getDisplayItemIndexFrontOffset(displayItemIndex, displayItemCount);
			frontOffset.add(-0.5f, -0.5f, -0.5f);
			rotateDisplayItemFrontOffset(state, dir, frontOffset);
			frontOffset.add(0.5f, 0.5f, 0.5f);
			offset.add(frontOffset);
			transform = new QuadTransformer(new Transformation(offset, null, null, null));

			DIRECTION_MOVE_BACK_TO_SIDE.put(hash, transform);
		}
		return transform;
	}

	@SuppressWarnings({"java:S1172", "unused"}) //state used in override
	protected void rotateDisplayItemFrontOffset(BlockState state, Direction dir, Vector3f frontOffset) {
		frontOffset.transform(getNorthBasedRotation(dir));
	}

	@SuppressWarnings({"java:S1172", "unused"}) //state used in override
	protected int calculateMoveBackToSideHash(BlockState state, Direction dir, float distFromCenter, int displayItemIndex, int displayItemCount) {
		int hash = Float.hashCode(distFromCenter);
		hash = 31 * hash + displayItemIndex;
		hash = 31 * hash + displayItemCount;
		return hash;
	}

	@Nonnull
	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand, IModelData extraData) {
		int hash = createHash(state, side, extraData);
		List<BakedQuad> quads = BAKED_QUADS_CACHE.getIfPresent(hash);
		if (quads != null) {
			return quads;
		}

		String woodName = null;
		boolean hasMainColor;
		boolean hasAccentColor;
		boolean isPacked;
		boolean showsTier;
		Map<BarrelMaterial, ResourceLocation> materials;
		if (state != null) {
			hasMainColor = Boolean.TRUE.equals(extraData.getData(HAS_MAIN_COLOR));
			hasAccentColor = Boolean.TRUE.equals(extraData.getData(HAS_ACCENT_COLOR));
			if (extraData.hasProperty(WOOD_NAME)) {
				woodName = extraData.getData(WOOD_NAME);
			}
			isPacked = isPacked(extraData);
			materials = getMaterials(extraData);
			showsTier = showsTier(extraData);
		} else {
			woodName = barrelWoodName;
			hasMainColor = barrelHasMainColor;
			hasAccentColor = barrelHasAccentColor;
			isPacked = barrelIsPacked;
			materials = barrelMaterials;
			showsTier = barrelShowsTier;
		}

		List<BakedQuad> ret = new ArrayList<>();

		boolean isBakedDynamically = !materials.isEmpty() && woodDynamicBakingData.containsKey(woodName);
		Set<BarrelMaterial.MaterialModelPart> materialModelParts = materials.keySet().stream().map(BarrelMaterial::getMaterialModelPart).collect(Collectors.toSet());
		boolean rendersUsingSplitModel = materialModelParts.contains(BarrelMaterial.MaterialModelPart.CORE) || materialModelParts.contains(BarrelMaterial.MaterialModelPart.TRIM);

		Map<BarrelModelPart, BakedModel> modelParts = getWoodModelParts(woodName, isBakedDynamically && rendersUsingSplitModel);
		if (modelParts.isEmpty()) {
			return Collections.emptyList();
		}

		if ((!hasMainColor || !hasAccentColor) && !isBakedDynamically) {
			addPartQuads(state, side, rand, ret, modelParts, getBasePart(state));
		}

		addTintableModelQuads(state, side, rand, ret, hasMainColor, hasAccentColor, modelParts);

		if (isBakedDynamically) {
			bakeAndAddDynamicQuads(getSpriteSide(state, side), rand, woodName, materials, rendersUsingSplitModel,
					!hasMainColor || materialModelParts.contains(BarrelMaterial.MaterialModelPart.CORE), !hasAccentColor || materialModelParts.contains(BarrelMaterial.MaterialModelPart.TRIM))
					.forEach(bakedModel -> ret.addAll(bakedModel.getQuads(state, side, rand, EmptyModelData.INSTANCE)));
		}

		if (showsTier) {
			addPartQuads(state, side, rand, ret, modelParts, BarrelModelPart.TIER);
		}

		if (isPacked) {
			addPartQuads(state, side, rand, ret, modelParts, BarrelModelPart.PACKED);
		} else {
			if (showsLocked(extraData)) {
				addPartQuads(state, side, rand, ret, modelParts, BarrelModelPart.LOCKED);
			}
			addDisplayItemQuads(state, side, rand, ret, extraData);
		}

		BAKED_QUADS_CACHE.put(hash, ret);

		return ret;
	}

	public List<BakedQuad> getTierQuads(BlockState state, Random rand, String woodName) {
		return getPartQuads(state, rand, woodName, BarrelModelPart.TIER);
	}

	public List<BakedQuad> getLockQuads(BlockState state, Random rand, String woodName) {
		return getPartQuads(state, rand, woodName, BarrelModelPart.LOCKED);
	}

	private List<BakedQuad> getPartQuads(BlockState state, Random rand, String woodName, BarrelModelPart part) {
		List<BakedQuad> ret = new ArrayList<>();

		Map<BarrelModelPart, BakedModel> modelParts = getWoodModelParts(woodName, false);

		for (Direction dir : Direction.values()) {
			addPartQuads(state, dir, rand, ret, modelParts, part);
		}

		return ret;
	}

	private static Direction getSpriteSide(@Nullable BlockState state, @Nullable Direction side) {
		if (side == null) {
			return Direction.NORTH;
		}
		Direction sideBeforeRotation;
		if (state != null && state.getBlock() instanceof BarrelBlock barrelBlock) {
			sideBeforeRotation = BlockSide.fromDirection(side, barrelBlock.getHorizontalDirection(state), barrelBlock.getVerticalFacing(state))
					.toDirection(Direction.NORTH, VerticalFacing.NO);
		} else {
			sideBeforeRotation = BlockSide.fromDirection(side, Direction.NORTH, VerticalFacing.UP).toDirection(Direction.NORTH, VerticalFacing.NO);
		}
		return sideBeforeRotation;
	}

	private List<BakedModel> bakeAndAddDynamicQuads(@Nullable Direction spriteSide, Random rand, @Nullable String woodName,
			Map<BarrelMaterial, ResourceLocation> barrelMaterials, boolean rendersUsingSplitModel, boolean renderCore, boolean renderTrim) {

		Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData> bakingData = woodDynamicBakingData.get(woodName);

		Map<String, Either<Material, String>> materials = new HashMap<>();
		for (Map.Entry<BarrelMaterial, ResourceLocation> entry : barrelMaterials.entrySet()) {
			BarrelMaterial barrelMaterial = entry.getKey();
			ResourceLocation blockName = entry.getValue();
			TextureAtlasSprite sprite = RenderHelper.getSprite(blockName, spriteSide, rand);
			Either<Material, String> material = Either.left(new Material(InventoryMenu.BLOCK_ATLAS, sprite.getName()));

			for (BarrelMaterial childMaterial : barrelMaterial.getChildren()) {
				materials.put(childMaterial.getSerializedName(), material);
			}
		}

		List<BakedModel> models = new ArrayList<>();
		if (rendersUsingSplitModel) {
			if (renderCore) {
				models.add(getDynamicModel(woodName, bakingData, materials, DynamicBarrelBakingData.DynamicPart.CORE));
			}
			if (renderTrim) {
				models.add(getDynamicModel(woodName, bakingData, materials, DynamicBarrelBakingData.DynamicPart.TRIM));
			}
		} else {
			models.add(getDynamicModel(woodName, bakingData, materials, DynamicBarrelBakingData.DynamicPart.WHOLE));
		}

		return models;
	}

	private BakedModel getDynamicModel(@Nullable String woodName, Map<DynamicBarrelBakingData.DynamicPart, DynamicBarrelBakingData> bakingData,
			Map<String, Either<Material, String>> materials, DynamicBarrelBakingData.DynamicPart dynamicPart) {
		int hash = Objects.hash(woodName, materials, dynamicPart.name());
		BakedModel bakedModel = dynamicBakedModelCache.getIfPresent(hash);
		if (bakedModel == null) {
			bakedModel = compileAndBakeModel(materials, bakingData.get(dynamicPart));
			dynamicBakedModelCache.put(hash, bakedModel);
		}
		return bakedModel;
	}

	private BlockState getDefaultBlockState(ResourceLocation blockName) {
		Block block = ForgeRegistries.BLOCKS.getValue(blockName);
		return block != null ? block.defaultBlockState() : Blocks.AIR.defaultBlockState();
	}

	private Map<BarrelMaterial, ResourceLocation> getMaterials(IModelData extraData) {
		return extraData.hasProperty(MATERIALS) ? Objects.requireNonNull(extraData.getData(MATERIALS)) : Collections.emptyMap();
	}

	private BakedModel compileAndBakeModel(Map<String, Either<Material, String>> materials, DynamicBarrelBakingData bakingData) {
		bakingData.modelPartDefinition().textures().forEach((textureName, texture) -> {
			if (!materials.containsKey(textureName)) {
				materials.put(textureName, Either.left(texture));
			}
		});

		BarrelDynamicModelBase.BarrelModelPartDefinition baseModelPartDefinition = bakingData.modelPartDefinition();
		return baseModelPartDefinition.modelLocation().map(modelLocation -> {
			BlockModel baseModel = new CompositeElementsModel(modelLocation, materials);
			baseModel.getMaterials(modelBakery::getModel, new HashSet<>()); //need to call getMaterials here to get parent models loaded which happens in that call
			return baseModel.bake(modelBakery, baseModel, modelBakery.getSpriteMap()::getSprite, bakingData.modelState(), bakingData.modelLocation(), false);
		}).orElse(Minecraft.getInstance().getModelManager().getMissingModel());
	}

	protected abstract BarrelModelPart getBasePart(@Nullable BlockState state);

	private boolean isPacked(IModelData extraData) {
		return extraData.hasProperty(IS_PACKED) && Boolean.TRUE.equals(extraData.getData(IS_PACKED));
	}

	private boolean showsLocked(IModelData extraData) {
		return extraData.hasProperty(SHOWS_LOCK) && Boolean.TRUE.equals(extraData.getData(SHOWS_LOCK));
	}

	private boolean showsTier(IModelData extraData) {
		return extraData.hasProperty(SHOWS_TIER) && Boolean.TRUE.equals(extraData.getData(SHOWS_TIER));
	}

	private int createHash(@Nullable BlockState state, @Nullable Direction side, IModelData data) {
		int hash;
		if (state != null) {
			hash = getInWorldBlockHash(state, data);
		} else {
			hash = getItemBlockHash();
		}
		hash = hash * 31 + (side == null ? 0 : side.get3DDataValue() + 1);
		hash = getDisplayItemsHash(data, hash);
		return hash;
	}

	private int getItemBlockHash() {
		int hash = barrelItem.hashCode();
		hash = hash * 31 + (barrelWoodName != null ? barrelWoodName.hashCode() + 1 : 0);
		hash = hash * 31 + (barrelHasMainColor ? 1 : 0);
		hash = hash * 31 + (barrelHasAccentColor ? 1 : 0);
		hash = hash * 31 + (barrelIsPacked ? 1 : 0);
		hash = hash * 31 + (barrelShowsTier ? 1 : 0);
		hash = hash * 31 + (flatTop ? 1 : 0);
		hash = hash * 31 + barrelMaterials.hashCode();
		return hash;
	}

	protected int getInWorldBlockHash(BlockState state, IModelData data) {
		int hash = state.getBlock().hashCode();

		//noinspection ConstantConditions
		hash = hash * 31 + (data.hasProperty(WOOD_NAME) ? data.getData(WOOD_NAME).hashCode() + 1 : 0);
		hash = hash * 31 + (data.hasProperty(HAS_MAIN_COLOR) && Boolean.TRUE.equals(data.getData(HAS_MAIN_COLOR)) ? 1 : 0);
		hash = hash * 31 + (data.hasProperty(HAS_ACCENT_COLOR) && Boolean.TRUE.equals(data.getData(HAS_ACCENT_COLOR)) ? 1 : 0);
		hash = hash * 31 + (isPacked(data) ? 1 : 0);
		hash = hash * 31 + (showsLocked(data) ? 1 : 0);
		hash = hash * 31 + (showsTier(data) ? 1 : 0);
		hash = hash * 31 + (Boolean.TRUE.equals(state.getValue(BarrelBlock.FLAT_TOP)) ? 1 : 0);
		//noinspection ConstantConditions
		hash = hash * 31 + (data.hasProperty(MATERIALS) ? data.getData(MATERIALS).hashCode() : 0);
		return hash;
	}

	private int getDisplayItemsHash(IModelData data, int hash) {
		if (data.hasProperty(DISPLAY_ITEMS)) {
			List<RenderInfo.DisplayItem> displayItems = data.getData(DISPLAY_ITEMS);
			//noinspection ConstantConditions
			for (RenderInfo.DisplayItem displayItem : displayItems) {
				hash = hash * 31 + getDisplayItemHash(displayItem);
			}
		}
		if (data.hasProperty(INACCESSIBLE_SLOTS)) {
			List<Integer> inaccessibleSlots = data.getData(INACCESSIBLE_SLOTS);
			//noinspection ConstantConditions
			for (Integer inaccessibleSlot : inaccessibleSlots) {
				hash = hash * 31 + inaccessibleSlot;
			}
		}
		return hash;
	}

	private int getDisplayItemHash(RenderInfo.DisplayItem displayItem) {
		int hash = displayItem.getRotation();
		hash = hash * 31 + ItemStackKey.getHashCode(displayItem.getItem());
		hash = hash * 31 + displayItem.getSlotIndex();
		return hash;
	}

	private void addDisplayItemQuads(@Nullable BlockState state, @Nullable Direction side, Random rand, List<BakedQuad> ret, IModelData data) {
		if (state == null || side != null || !(state.getBlock() instanceof BarrelBlock barrelBlock)) {
			return;
		}

		List<RenderInfo.DisplayItem> displayItems = data.getData(DISPLAY_ITEMS);

		Minecraft minecraft = Minecraft.getInstance();
		ItemRenderer itemRenderer = minecraft.getItemRenderer();
		if (displayItems != null && !displayItems.isEmpty()) {
			int index = 0;
			for (RenderInfo.DisplayItem displayItem : displayItems) {
				ItemStack item = displayItem.getItem();
				if (barrelBlock.hasFixedIndexDisplayItems()) {
					index = displayItem.getSlotIndex();
				}
				if (item.isEmpty()) {
					continue;
				}

				BakedModel model = itemRenderer.getModel(item, null, minecraft.player, 0);
				if (!model.isCustomRenderer()) {
					int rotation = displayItem.getRotation();
					for (Direction face : Direction.values()) {
						addRenderedItemSide(state, rand, ret, item, model, rotation, face, index, barrelBlock.getDisplayItemsCount(displayItems));
					}
					addRenderedItemSide(state, rand, ret, item, model, rotation, null, index, barrelBlock.getDisplayItemsCount(displayItems));
				}
				index++;
			}
		}

		addInaccessibleSlotsQuads(state, rand, ret, data, barrelBlock, displayItems, minecraft);
	}

	private void addInaccessibleSlotsQuads(BlockState state, Random rand, List<BakedQuad> ret, IModelData data, BarrelBlock barrelBlock,
			@Nullable List<RenderInfo.DisplayItem> displayItems, Minecraft minecraft) {
		List<Integer> inaccessibleSlots = data.getData(INACCESSIBLE_SLOTS);
		if (displayItems != null && inaccessibleSlots != null) {
			ItemStack inaccessibleSlotStack = new ItemStack(ModItems.INACCESSIBLE_SLOT.get());
			BakedModel model = minecraft.getItemRenderer().getModel(inaccessibleSlotStack, null, minecraft.player, 0);
			for (int inaccessibleSlot : inaccessibleSlots) {
				if (!model.isCustomRenderer()) {
					for (Direction face : Direction.values()) {
						addRenderedItemSide(state, rand, ret, inaccessibleSlotStack, model, 0, face, inaccessibleSlot, barrelBlock.getDisplayItemsCount(displayItems));
					}
					addRenderedItemSide(state, rand, ret, inaccessibleSlotStack, model, 0, null, inaccessibleSlot, barrelBlock.getDisplayItemsCount(displayItems));
				}
			}
		}
	}

	@SuppressWarnings({"deprecation", "java:S107"})
	private void addRenderedItemSide(BlockState state, Random rand, List<BakedQuad> ret, ItemStack displayItem, BakedModel model, int rotation,
			@Nullable Direction dir, int displayItemIndex, int displayItemCount) {
		List<BakedQuad> quads = model.getQuads(null, dir, rand);
		quads = MOVE_TO_CORNER.processMany(quads);
		quads = new QuadTransformer(TransformationHelper.toTransformation(model.getTransforms().getTransform(ItemTransforms.TransformType.FIXED))).processMany(quads);
		if (!model.isGui3d()) {
			if (displayItemCount == 1) {
				quads = SCALE_BIG_2D_ITEM.processMany(quads);
			} else {
				quads = SCALE_SMALL_2D_ITEM.processMany(quads);
			}
		} else if (displayItemCount > 1) {
			quads = SCALE_SMALL_3D_ITEM.processMany(quads);
		}

		if (rotation != 0) {
			quads = getDisplayRotation(rotation).processMany(quads);
		}

		Direction facing = state.getBlock() instanceof BarrelBlock barrelBlock ? barrelBlock.getFacing(state) : Direction.NORTH;
		quads = rotateDisplayItemQuads(quads, state);

		if (model.isGui3d()) {
			QuadTransformer transformer = getDirectionMove(displayItem, model, state, facing, displayItemIndex, displayItemCount, displayItemCount == 1 ? 1 : SMALL_3D_ITEM_SCALE);
			quads = transformer.processMany(quads);
			recalculateDirections(quads);
		} else {
			quads = getDirectionMove(displayItem, model, state, facing, displayItemIndex, displayItemCount, 1).processMany(quads);
			recalculateDirections(quads);
		}

		updateTintIndexes(quads, displayItemIndex);

		ret.addAll(quads);
	}

	protected abstract List<BakedQuad> rotateDisplayItemQuads(List<BakedQuad> quads, BlockState state);

	private void updateTintIndexes(List<BakedQuad> quads, int displayItemIndex) {
		int offset = (displayItemIndex + 1) * 10;
		quads.forEach(quad -> {
			if (quad.tintIndex >= 0) {
				quad.tintIndex = quad.tintIndex + offset;
			}
		});
	}

	private void recalculateDirections(List<BakedQuad> quads) {
		quads.forEach(quad -> quad.direction = FaceBakery.calculateFacing(quad.getVertices()));
	}

	private QuadTransformer getDirectionMove(ItemStack displayItem, BakedModel model, BlockState state, Direction direction, int displayItemIndex, int displayItemCount, float itemScale) {
		boolean isFlatTop = state.getValue(BarrelBlock.FLAT_TOP);
		int hash = calculateDirectionMoveHash(state, displayItem, displayItemIndex, displayItemCount, isFlatTop);
		Cache<Integer, QuadTransformer> directionCache = DIRECTION_MOVES_3D_ITEMS.getUnchecked(direction);
		QuadTransformer transformer = directionCache.getIfPresent(hash);

		if (transformer == null) {
			double offset = DisplayItemRenderer.getDisplayItemOffset(displayItem, model, itemScale);
			if (!isFlatTop) {
				offset -= 1 / 16D;
			}

			transformer = getDirectionMoveBackToSide(state, direction, (float) (0.5f + offset), displayItemIndex, displayItemCount);
			directionCache.put(hash, transformer);
		}

		return transformer;
	}

	@SuppressWarnings({"java:S1172", "unused"}) //state used in override
	protected int calculateDirectionMoveHash(BlockState state, ItemStack displayItem, int displayItemIndex, int displayItemCount, boolean isFlatTop) {
		int hashCode = ItemStackKey.getHashCode(displayItem);
		hashCode = hashCode * 31 + displayItemIndex;
		hashCode = hashCode * 31 + displayItemCount;
		hashCode = hashCode * 31 + (isFlatTop ? 1 : 0);
		return hashCode;
	}

	private QuadTransformer getDisplayRotation(int rotation) {
		return DISPLAY_ROTATIONS.computeIfAbsent(rotation, r -> new QuadTransformer(new Transformation(null, Vector3f.ZP.rotationDegrees(rotation), null, null)));
	}

	private void addTintableModelQuads(
			@Nullable BlockState state, @Nullable
	Direction side, Random rand, List<BakedQuad> ret, boolean hasMainColor, boolean hasAccentColor, Map<BarrelModelPart, BakedModel> modelParts) {
		if (hasAccentColor) {
			addPartQuads(state, side, rand, ret, modelParts, BarrelModelPart.TINTABLE_ACCENT);
		}

		if (hasMainColor) {
			addPartQuads(state, side, rand, ret, modelParts, getMainPart(state));
		}
	}

	private BarrelModelPart getMainPart(@Nullable BlockState state) {
		return rendersOpen() && state != null && Boolean.TRUE.equals(state.getValue(BarrelBlock.OPEN)) ? BarrelModelPart.TINTABLE_MAIN_OPEN : BarrelModelPart.TINTABLE_MAIN;
	}

	protected abstract boolean rendersOpen();

	private void addPartQuads(
			@Nullable BlockState state, @Nullable Direction side, Random rand, List<BakedQuad> ret, Map<BarrelModelPart, BakedModel> modelParts, BarrelModelPart part) {
		if (modelParts.containsKey(part)) {
			ret.addAll(modelParts.getOrDefault(part, Minecraft.getInstance().getModelManager().getMissingModel()).getQuads(state, side, rand, EmptyModelData.INSTANCE));
		}
	}

	private Map<BarrelModelPart, BakedModel> getWoodModelParts(@Nullable String barrelWoodName, boolean requiresPartitionedModel) {
		if (requiresPartitionedModel && woodPartitionedModelParts.containsKey(barrelWoodName)) {
			return woodPartitionedModelParts.get(barrelWoodName);
		} else {
			if (woodModelParts.isEmpty()) {
				return Collections.emptyMap();
			} else if (barrelWoodName == null || !woodModelParts.containsKey(barrelWoodName)) {
				return woodModelParts.values().iterator().next();
			} else {
				return woodModelParts.get(barrelWoodName);
			}
		}
	}

	@Override
	public boolean useAmbientOcclusion() {
		return false; //because occlusion calculation makes display item dark on faces that are exposed to light
	}

	@Override
	public boolean isGui3d() {
		return true;
	}

	@Override
	public boolean usesBlockLight() {
		return true;
	}

	@Override
	public boolean isCustomRenderer() {
		return false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public TextureAtlasSprite getParticleIcon() {
		return getWoodModelParts(null, false).getOrDefault(BarrelModelPart.BASE, Minecraft.getInstance().getModelManager().getMissingModel()).getParticleIcon();
	}

	@SuppressWarnings("deprecation")
	@Override
	public ItemTransforms getTransforms() {
		return ITEM_TRANSFORMS;
	}

	@Override
	public TextureAtlasSprite getParticleIcon(IModelData data) {
		if (data.hasProperty(HAS_MAIN_COLOR) && Boolean.TRUE.equals(data.getData(HAS_MAIN_COLOR))) {
			return getWoodModelParts(null, false).get(BarrelModelPart.TINTABLE_MAIN).getParticleIcon(data);
		}

		if (data.hasProperty(MATERIALS)) {
			Map<BarrelMaterial, ResourceLocation> materials = data.getData(MATERIALS);
			if (materials != null) {
				for (BarrelMaterial barrelMaterial : PARTICLE_ICON_MATERIAL_PRIORITY) {
					if (materials.containsKey(barrelMaterial)) {
						BlockState blockState = getDefaultBlockState(materials.get(barrelMaterial));
						return Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState).getParticleIcon(EmptyModelData.INSTANCE);
					}
				}
			}
		}

		if (data.hasProperty(WOOD_NAME)) {
			String name = data.getData(WOOD_NAME);
			if (!woodModelParts.containsKey(name)) {
				return getParticleIcon();
			}
			return getWoodModelParts(name, false).get(BarrelModelPart.BASE).getParticleIcon(data);
		}
		return getParticleIcon();
	}

	@Nonnull
	@Override
	public IModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state, IModelData tileData) {
		return WorldHelper.getBlockEntity(world, pos, BarrelBlockEntity.class)
				.map(be -> {
					ModelDataMap.Builder builder = new ModelDataMap.Builder();
					boolean hasMainColor = be.getStorageWrapper().hasMainColor();
					builder.withInitial(HAS_MAIN_COLOR, hasMainColor);
					boolean hasAccentColor = be.getStorageWrapper().hasAccentColor();
					builder.withInitial(HAS_ACCENT_COLOR, hasAccentColor);
					if (!be.hasFullyDynamicRenderer()) {
						builder.withInitial(DISPLAY_ITEMS, be.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getDisplayItems());
						builder.withInitial(INACCESSIBLE_SLOTS, be.getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getInaccessibleSlots());
					}
					builder.withInitial(IS_PACKED, be.isPacked());
					builder.withInitial(SHOWS_LOCK, be.isLocked() && be.shouldShowLock());
					builder.withInitial(SHOWS_TIER, be.shouldShowTier());
					Optional<WoodType> woodType = be.getWoodType();
					if (woodType.isPresent() || !(hasMainColor && hasAccentColor)) {
						builder.withInitial(WOOD_NAME, woodType.orElse(WoodType.ACACIA).name());
					}

					Map<BarrelMaterial, ResourceLocation> materials = be.getMaterials();
					if (!materials.isEmpty()) {
						builder.withInitial(MATERIALS, materials);
					}
					return (IModelData) builder.build();
				}).orElse(EmptyModelData.INSTANCE);
	}

	@Override
	public ItemOverrides getOverrides() {
		return barrelItemOverrides;
	}

	@Override
	public BakedModel handlePerspective(ItemTransforms.TransformType cameraTransformType, PoseStack matrixStack) {
		if (cameraTransformType == ItemTransforms.TransformType.NONE) {
			return this;
		}

		Transformation tr = TRANSFORMS.get(cameraTransformType);

		if (!tr.isIdentity()) {
			tr.push(matrixStack);
		}
		return this;
	}

	private static class BarrelItemOverrides extends ItemOverrides {
		private final BarrelBakedModelBase barrelBakedModel;
		@Nullable
		private final BakedModel flatTopModel;

		public BarrelItemOverrides(BarrelBakedModelBase barrelBakedModel, @Nullable BakedModel flatTopModel) {
			this.barrelBakedModel = barrelBakedModel;
			this.flatTopModel = flatTopModel;
		}

		@Nullable
		@Override
		public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
			boolean flatTop = BarrelBlockItem.isFlatTop(stack);
			if (flatTopModel != null && flatTop) {
				return flatTopModel.getOverrides().resolve(flatTopModel, stack, level, entity, seed);
			}

			barrelBakedModel.barrelHasMainColor = StorageBlockItem.getMainColorFromStack(stack).isPresent();
			barrelBakedModel.barrelHasAccentColor = StorageBlockItem.getAccentColorFromStack(stack).isPresent();
			barrelBakedModel.barrelWoodName = WoodStorageBlockItem.getWoodType(stack).map(WoodType::name)
					.orElse(barrelBakedModel.barrelHasAccentColor && barrelBakedModel.barrelHasMainColor ? null : WoodType.ACACIA.name());
			barrelBakedModel.barrelIsPacked = WoodStorageBlockItem.isPacked(stack);
			barrelBakedModel.barrelShowsTier = StorageBlockItem.showsTier(stack);
			barrelBakedModel.barrelItem = stack.getItem();
			barrelBakedModel.flatTop = flatTop;
			barrelBakedModel.barrelMaterials = BarrelBlockItem.getMaterials(stack);
			return barrelBakedModel;
		}
	}
}
