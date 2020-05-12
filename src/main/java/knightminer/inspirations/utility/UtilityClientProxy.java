package knightminer.inspirations.utility;

import knightminer.inspirations.common.ClientProxy;
import knightminer.inspirations.utility.client.CollectorScreen;
import knightminer.inspirations.utility.client.PipeScreen;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class UtilityClientProxy extends ClientProxy {

	@SubscribeEvent
	public void clientSetup(FMLClientSetupEvent event) {
		RenderType cutout = RenderType.getCutout();
		RenderTypeLookup.setRenderLayer(InspirationsUtility.torchLeverFloor, cutout);
		RenderTypeLookup.setRenderLayer(InspirationsUtility.torchLeverWall, cutout);
	}

	@SubscribeEvent
	public void commonSetup(FMLCommonSetupEvent event) {

		// Register GUIs.
		ScreenManager.registerFactory(InspirationsUtility.contCollector, CollectorScreen::new);
		ScreenManager.registerFactory(InspirationsUtility.contPipe, PipeScreen::new);
	}
}
