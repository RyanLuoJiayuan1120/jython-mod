package net.luojiayuan.jython.mod;

import net.luojiayuan.jython.mod.platform.NeoForgePlatformHooks;
import net.luojiayuan.jython.mod.platform.PlatformHooks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * NeoForge entrypoint.
 *
 * <p>Unlike Fabric, NeoForge freezes its registries during mod construction, so
 * Python mods that register items/blocks cannot run in the constructor. The
 * shared {@code main} environment is therefore deferred to the
 * {@link RegisterEvent} phase, while client/server-only scripts run on the
 * corresponding lifecycle events.</p>
 */
@Mod("jythonmod")
public class JythonModNeoForge {
	private static final Logger LOGGER = LoggerFactory.getLogger(JythonModNeoForge.class);
	private static final AtomicBoolean MAIN_RAN = new AtomicBoolean(false);
	private static final AtomicBoolean CLIENT_RAN = new AtomicBoolean(false);
	private static final AtomicBoolean SERVER_RAN = new AtomicBoolean(false);

	public JythonModNeoForge(IEventBus modBus) {
		PlatformHooks.set(new NeoForgePlatformHooks());
		// Initialize the engine (mappings, GraalPy env) now; do not run Python
		// mods yet because NeoForge registries are still mutable at this point
		// but the main Loader would attempt registration too early.
		modBus.addListener(this::onRegister);
		modBus.addListener(this::onClientSetup);
		modBus.addListener(this::onServerSetup);
	}

	@SubscribeEvent
	public void onRegister(RegisterEvent event) {
		// Any registry event works as the "registries are mutable now" marker.
		// Run the shared Python environment exactly once.
		if (MAIN_RAN.compareAndSet(false, true)) {
			ModBootstrap.start(LOGGER, "main");
		}
	}

	@SubscribeEvent
	public void onClientSetup(FMLClientSetupEvent event) {
		if (FMLEnvironment.getDist().isClient() && CLIENT_RAN.compareAndSet(false, true)) {
			ModBootstrap.start(LOGGER, "client");
		}
	}

	@SubscribeEvent
	public void onServerSetup(FMLDedicatedServerSetupEvent event) {
		if (FMLEnvironment.getDist().isDedicatedServer() && SERVER_RAN.compareAndSet(false, true)) {
			ModBootstrap.start(LOGGER, "server");
		}
	}
}
