package net.luojiayuan.jython.mod.mixin.client;

import java.util.stream.Stream;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hide jython-mod's generated packs from the resource pack / data pack
 * selection screens.
 *
 * <p>The packs are registered as required built-in packs, so hiding the list
 * entries does not disable them: {@code PackRepository.rebuildSelected}
 * always keeps required packs. The model's internal selected list is left
 * untouched, which also keeps them enabled if the user presses "Done".</p>
 */
@Mixin(PackSelectionModel.class)
public abstract class PackSelectionModelMixin {
    private static final String JYTHON_PACK_PREFIX = "jython-mod:";

    @Inject(method = "getUnselected", at = @At("RETURN"), cancellable = true)
    private void jython$hideGeneratedPacksFromAvailable(CallbackInfoReturnable<Stream<PackSelectionModel.Entry>> cir) {
        cir.setReturnValue(cir.getReturnValue().filter(PackSelectionModelMixin::jython$isVisible));
    }

    @Inject(method = "getSelected", at = @At("RETURN"), cancellable = true)
    private void jython$hideGeneratedPacksFromSelected(CallbackInfoReturnable<Stream<PackSelectionModel.Entry>> cir) {
        cir.setReturnValue(cir.getReturnValue().filter(PackSelectionModelMixin::jython$isVisible));
    }

    private static boolean jython$isVisible(PackSelectionModel.Entry entry) {
        String id = entry.getId();
        return id == null || !id.startsWith(JYTHON_PACK_PREFIX);
    }
}
