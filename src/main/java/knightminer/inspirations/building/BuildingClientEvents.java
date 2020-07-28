package knightminer.inspirations.building;

import knightminer.inspirations.Inspirations;
import knightminer.inspirations.building.block.BookshelfBlock;
import knightminer.inspirations.building.block.type.BushType;
import knightminer.inspirations.building.block.type.ShelfType;
import knightminer.inspirations.building.client.BookshelfModel;
import knightminer.inspirations.building.tileentity.BookshelfTileEntity;
import knightminer.inspirations.common.ClientEvents;
import knightminer.inspirations.library.Util;
import knightminer.inspirations.library.client.ClientUtil;
import knightminer.inspirations.library.util.TextureBlockUtil;
import knightminer.inspirations.shared.client.BackgroundContainerScreen;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.world.FoliageColors;
import net.minecraft.world.biome.BiomeColors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = Inspirations.modID, value = Dist.CLIENT, bus = Bus.MOD)
public class BuildingClientEvents extends ClientEvents {
	private static final Minecraft mc = Minecraft.getInstance();

	@SubscribeEvent
	static void clientSetup(FMLClientSetupEvent event) {
		// set render types
		RenderType cutout = RenderType.getCutout();
		Consumer<Block> setCutout = (block) -> RenderTypeLookup.setRenderLayer(block, cutout);
		RenderType cutoutMipped = RenderType.getCutoutMipped();
		Consumer<Block> setCutoutMipped = (block) -> RenderTypeLookup.setRenderLayer(block, cutoutMipped);

		// general
		InspirationsBuilding.bookshelf.forEach(setCutout);
		InspirationsBuilding.enlightenedBush.forEach(setCutoutMipped);

		// ropes
		setRenderLayer(InspirationsBuilding.rope, cutout);
		setRenderLayer(InspirationsBuilding.vine, cutout);
		setRenderLayer(InspirationsBuilding.ironBars, cutoutMipped);

		// doors
		setRenderLayer(InspirationsBuilding.glassDoor, cutoutMipped);
		setRenderLayer(InspirationsBuilding.glassTrapdoor, cutoutMipped);

		// flower
		InspirationsBuilding.flower.forEach(setCutout);
		InspirationsBuilding.flowerPot.forEach(setCutout);
	}

	@SubscribeEvent
	static void commonSetup(FMLCommonSetupEvent event) {
		// listener to clear bookshelf model cache as its shared by all bookshelf model files
		IResourceManager manager = Minecraft.getInstance().getResourceManager();
		// should always be true, but just in case
		if(manager instanceof IReloadableResourceManager) {
			((IReloadableResourceManager) manager).addReloadListener(
					(stage, resMan, prepProp, reloadProf, bgExec, gameExec) -> CompletableFuture
							.runAsync(BookshelfModel.BOOK_CACHE::invalidateAll, gameExec)
							.thenCompose(stage::markCompleteAwaitingOthers)
			);
		} else {
			Inspirations.log.error("Failed to register resource reload listener, expected instance of IReloadableResourceManager but got {}", manager.getClass());
		}

		// Register GUIs.
		registerScreenFactory(InspirationsBuilding.contBookshelf, new BackgroundContainerScreen.Factory<>("bookshelf"));
	}


	@SubscribeEvent
	static void registerBlockColors(ColorHandlerEvent.Block event) {
		BlockColors blockColors = event.getBlockColors();

		// coloring of books for normal bookshelf
		registerBlockColors(blockColors, (state, world, pos, tintIndex) -> {
			if(tintIndex > 0 && tintIndex <= 14 && world != null && pos != null) {
				TileEntity te = world.getTileEntity(pos);
				if(te instanceof BookshelfTileEntity) {
					ItemStack stack = ((BookshelfTileEntity) te).getStackInSlot(tintIndex - 1);
					if(!stack.isEmpty()) {
						int color = ClientUtil.getItemColor(stack.getItem());
						int itemColors = mc.getItemColors().getColor(stack, 0);
						if(itemColors > -1) {
							// combine twice to make sure the item colors result is dominant
							color = Util.combineColors(color, itemColors, 3);
						}
						return color;
					}
				}
			}

			return -1;
		}, InspirationsBuilding.bookshelf.getOrNull(ShelfType.NORMAL));

		// rope vine coloring
		registerBlockColors(blockColors, (state, world, pos, tintIndex) -> {
			if(world != null && pos != null) {
				return BiomeColors.getFoliageColor(world, pos);
			}
			return FoliageColors.getDefault();
		}, InspirationsBuilding.vine);

		// bush block coloring
		// First the three which never change tint.
		InspirationsBuilding.enlightenedBush.forEach((type, bush) -> {
			if (type != BushType.WHITE) {
				int color = type.getColor(); // Make closure capture just the int.
				blockColors.register((state, world, pos, tintIndex) -> tintIndex == 0 ? color : -1, bush);
			}
		});

		// white copies the default leaf colors
		registerBlockColors(blockColors, (state, world, pos, tintIndex) -> {
			if(tintIndex != 0 || world == null || pos == null) {
				return -1;
			}
			TileEntity te = world.getTileEntity(pos);
			if(te != null) {
				Block block = TextureBlockUtil.getTextureBlock(te);
				if (block != Blocks.AIR) {
					return ClientUtil.getStackBlockColorsSafe(new ItemStack(block), world, pos, 0);
				}
			}
			return FoliageColors.getDefault();
		}, InspirationsBuilding.enlightenedBush.getOrNull(BushType.WHITE));
	}

	@SubscribeEvent
	static void registerItemColors(ColorHandlerEvent.Item event) {
		ItemColors itemColors = event.getItemColors();

		// coloring of books for normal bookshelf
		registerItemColors(itemColors, (stack, tintIndex) -> {
			if(tintIndex > 0 && tintIndex <= 14) {
				return 0x654B17;
			}
			return -1;
		}, InspirationsBuilding.bookshelf.getOrNull(ShelfType.NORMAL));

		// book covers, too lazy to make 16 cover textures
		InspirationsBuilding.coloredBooks.forEach((color, book) -> {
			int hexColor = color.colorValue;
			itemColors.register((stack, tintIndex) -> (tintIndex == 0) ? hexColor : -1, book);
		});

		// bush block colors
		// First the three blocks which never change tint.
		InspirationsBuilding.enlightenedBush.forEach((type, bush) -> {
			if (type != BushType.WHITE) {
				int color = type.getColor();
				itemColors.register((stack, tintIndex) -> tintIndex == 0 ? color : -1, bush);
			}
		});

		// The main one uses the tint of the textured stack
		registerItemColors(itemColors, (stack, tintIndex) -> {
			if(tintIndex != 0) {
				return -1;
			}
			// redirect to block for colors
			Block block = TextureBlockUtil.getTextureBlock(stack);
			if(block != Blocks.AIR) {
				return itemColors.getColor(new ItemStack(block), 0);
			} else {
				return FoliageColors.getDefault();
			}
		}, InspirationsBuilding.enlightenedBush.getOrNull(BushType.WHITE));

		// We can't get the world position of the item, so use the default tint.
		registerItemColors(itemColors, (stack, tintIndex) -> FoliageColors.getDefault(), InspirationsBuilding.vine);
	}

	/**
	 * Replaces the bookshelf models with the dynamic texture model, which also handles books
	 */
	@SubscribeEvent
	static void onModelBake(ModelBakeEvent event) {
		for (BookshelfBlock block : InspirationsBuilding.bookshelf.values()) {
			replaceBookshelfModel(event, block);
		}
		for (Block block : InspirationsBuilding.enlightenedBush.values()) {
			replaceBothTexturedModels(event, block.getRegistryName(), "leaves");
		}
	}

	@Deprecated
	private static void replaceBookshelfModel(ModelBakeEvent event, BookshelfBlock shelf) {
		if (shelf.getRegistryName() == null) {
			throw new AssertionError("Null registry name");
		}
		for(Direction facing : Direction.Plane.HORIZONTAL){
			ModelResourceLocation location = new ModelResourceLocation(shelf.getRegistryName(), String.format("facing=%s", facing.getString()));
			replaceModel(event, location, (loader, model) -> new BookshelfModel(location, loader, model));
		}
		replaceTexturedModel(event, new ModelResourceLocation(shelf.getRegistryName(), "inventory"), "texture",true);
	}
}