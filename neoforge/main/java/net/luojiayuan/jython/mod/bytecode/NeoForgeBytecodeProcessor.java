package net.luojiayuan.jython.mod.bytecode;

import java.util.Arrays;
import java.util.Set;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoForge {@link ClassProcessor} that runs the shared {@link BytecodeRegistry}
 * transformer chain after Mixin has processed each class.
 *
 * <p>Registered via {@code META-INF/services/net.neoforged.neoforgespi.transformation.ClassProcessor}.
 * The transformation is byte-array based (mirroring the Fabric hook), so the
 * {@link ClassNode} is serialized to bytes, run through the registry, and
 * deserialized back when any transformer produced a change.</p>
 */
public class NeoForgeBytecodeProcessor implements ClassProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger("jython-mod");
    private static final ProcessorName NAME = new ProcessorName("jythonmod", "python_bytecode");

    @Override
    public ProcessorName name() {
        return NAME;
    }

    @Override
    public Set<ProcessorName> runsAfter() {
        // Run after Mixin (like Fabric's post-Mixin hook) and after frame computation.
        return Set.of(ClassProcessorIds.MIXIN, ClassProcessorIds.COMPUTING_FRAMES);
    }

    @Override
    public boolean handlesClass(SelectionContext context) {
        // Only take part once a transformer is registered; otherwise stay out of the way.
        return BytecodeRegistry.transformerCount() > 0;
    }

    @Override
    public ComputeFlags processClass(TransformationContext context) {
        if (BytecodeRegistry.transformerCount() == 0) {
            return ComputeFlags.NO_REWRITE;
        }

        ClassNode node = context.node();
        String className = context.type().getClassName();

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        byte[] original = writer.toByteArray();
        byte[] transformed = BytecodeRegistry.transform(className, original);

        if (transformed == original || Arrays.equals(transformed, original)) {
            return ComputeFlags.NO_REWRITE;
        }

        try {
            ClassNode replacement = new ClassNode();
            new ClassReader(transformed).accept(replacement, ClassReader.EXPAND_FRAMES);
            copyNode(replacement, node);
            return ComputeFlags.COMPUTE_MAXS;
        } catch (Exception e) {
            LOGGER.error("[NeoForgeBytecodeProcessor] Failed to re-parse transformed class {}: {}", className, e.getMessage(), e);
            return ComputeFlags.NO_REWRITE;
        }
    }

    private static void copyNode(ClassNode from, ClassNode to) {
        to.version = from.version;
        to.access = from.access;
        to.name = from.name;
        to.signature = from.signature;
        to.superName = from.superName;
        to.interfaces = from.interfaces;
        to.sourceFile = from.sourceFile;
        to.sourceDebug = from.sourceDebug;
        to.module = from.module;
        to.outerClass = from.outerClass;
        to.outerMethod = from.outerMethod;
        to.outerMethodDesc = from.outerMethodDesc;
        to.visibleAnnotations = from.visibleAnnotations;
        to.invisibleAnnotations = from.invisibleAnnotations;
        to.visibleTypeAnnotations = from.visibleTypeAnnotations;
        to.invisibleTypeAnnotations = from.invisibleTypeAnnotations;
        to.attrs = from.attrs;
        to.innerClasses = from.innerClasses;
        to.nestHostClass = from.nestHostClass;
        to.nestMembers = from.nestMembers;
        to.permittedSubclasses = from.permittedSubclasses;
        to.recordComponents = from.recordComponents;
        to.fields = from.fields;
        to.methods = from.methods;
    }
}
