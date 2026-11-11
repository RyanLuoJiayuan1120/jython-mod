package net.luojiayuan.jython.mod.platform;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.ModOrigin;

/**
 * A tiny {@link ModContainer} view whose root points at the generated pack
 * directory instead of the mod JAR.
 *
 * <p>Fabric's built-in pack API only accepts packs located under a mod
 * container root. The generated packs live in the game directory, so this
 * wrapper delegates metadata/origin to the real jython-mod container but
 * exposes {@code <gamedir>/jython-mod/packs} as the only root.</p>
 */
final class GeneratedPackContainer implements ModContainer {
    private final ModContainer delegate;
    private final Path root;

    GeneratedPackContainer(ModContainer delegate, Path root) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
    }

    @Override
    public ModMetadata getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public List<Path> getRootPaths() {
        return List.of(root);
    }

    @Override
    public Optional<Path> findPath(String path) {
        Path resolved = root.resolve(path).normalize();
        if (!resolved.startsWith(root) || !Files.exists(resolved)) {
            return Optional.empty();
        }
        return Optional.of(resolved);
    }

    @Override
    public ModOrigin getOrigin() {
        return delegate.getOrigin();
    }

    @Override
    public Optional<ModContainer> getContainingMod() {
        return delegate.getContainingMod();
    }

    @Override
    public Collection<ModContainer> getContainedMods() {
        return delegate.getContainedMods();
    }

    @Override
    public Path getRootPath() {
        return root;
    }

    @Override
    public Path getPath(String path) {
        return root.resolve(path).normalize();
    }
}
