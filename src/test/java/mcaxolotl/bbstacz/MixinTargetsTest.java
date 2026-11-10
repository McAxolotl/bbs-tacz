package mcaxolotl.bbstacz;

import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import static org.junit.jupiter.api.Assertions.*;

/** Audits every registered injection/accessor against the exact pinned dependency class files. */
class MixinTargetsTest {
    @Test void standaloneFormDoesNotInheritAnotherFormType() throws Exception {
        assertEquals("mchorse/bbs_mod/forms/forms/Form", read("mcaxolotl/bbstacz/forms/TaczForm").superName);
        var metadata = JsonParser.parseString(Files.readString(Path.of("src/main/resources/fabric.mod.json"))).getAsJsonObject();
        assertEquals("BBS TACZ", metadata.get("name").getAsString());
        assertEquals("McAxolotl", metadata.getAsJsonArray("authors").get(0).getAsString());
        assertEquals("1.0.2-rc1+1.20", metadata.getAsJsonObject("depends").get("player-animator").getAsString());
    }
    @Test void allMixinTargetsAndDescriptorsExist() throws Exception {
        int checked = 0;
        for (String config : List.of("bbs_tacz.mixins.json", "bbs_tacz.client.mixins.json")) {
            var json = JsonParser.parseString(Files.readString(Path.of("src/main/resources", config))).getAsJsonObject();
            String root = json.get("package").getAsString().replace('.', '/');
            var names = json.has("mixins") ? json.getAsJsonArray("mixins") : json.getAsJsonArray("client");
            for (var name : names) {
                ClassNode mixin = read(root + "/" + name.getAsString());
                AnnotationNode annotation = mixin.invisibleAnnotations.stream().filter(a -> a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
                List<Type> types = value(annotation, "value");
                List<String> targetNames = value(annotation, "targets");
                assertTrue(types != null && !types.isEmpty() || targetNames != null && !targetNames.isEmpty(), "Mixin has no target");
                ClassNode target = read(types != null && !types.isEmpty() ? types.get(0).getInternalName() : targetNames.get(0).replace('.', '/'));
                for (var method : mixin.methods) {
                    List<AnnotationNode> annotations = new ArrayList<>();
                    if (method.visibleAnnotations != null) annotations.addAll(method.visibleAnnotations);
                    if (method.invisibleAnnotations != null) annotations.addAll(method.invisibleAnnotations);
                    for (var injection : annotations) {
                        if (injection.desc.endsWith("/WrapMethod;")) {
                            List<String> selectors=value(injection,"method");
                            for(String selector:selectors) {
                                var candidates=findMethods(target,selector,new HashSet<>());
                                Type[] args=Type.getArgumentTypes(method.desc);
                                assertTrue(candidates.stream().anyMatch(candidate -> {
                                    Type[] nativeArgs=Type.getArgumentTypes(candidate.desc);
                                    return args.length==nativeArgs.length+1
                                        && java.util.Arrays.equals(java.util.Arrays.copyOf(args,nativeArgs.length),nativeArgs)
                                        && Type.getReturnType(method.desc).equals(Type.getReturnType(candidate.desc));
                                }),"Invalid method wrapper "+selector);
                                assertEquals("com.llamalad7.mixinextras.injector.wrapoperation.Operation",args[args.length-1].getClassName()); checked++;
                            }
                        } else if (injection.desc.endsWith("/Inject;")) {
                            List<String> selectors = value(injection, "method");
                            for (String selector : selectors) {
                                assertTrue(findMethod(target, selector, new HashSet<>()), root + "/" + name.getAsString() + " -> " + selector);
                                List<org.objectweb.asm.tree.MethodNode> targets = findMethods(target, selector, new HashSet<>());
                                Type[] callbackArgs = Type.getArgumentTypes(method.desc);
                                int count = callbackArgs.length - 1;
                                assertTrue(targets.stream().anyMatch(candidate -> {
                                    Type[] args = Type.getArgumentTypes(candidate.desc);
                                    if (count == 0) return true;
                                    if (count != args.length) return false;
                                    for (int i = 0; i < count; i++) if (!callbackArgs[i].equals(args[i])) return false;
                                    return true;
                                }), name.getAsString() + " callback descriptor disagrees with " + selector);
                                checked++;
                            }
                        } else if (injection.desc.endsWith("/Redirect;") || injection.desc.endsWith("/ModifyArg;")) {
                            List<String> selectors = value(injection, "method");
                            AnnotationNode at = value(injection, "at"); String call = value(at, "target");
                            int semicolon = call.indexOf(';'), paren = call.indexOf('(');
                            String owner = call.substring(1, semicolon), invoked = call.substring(semicolon + 1);
                            assertTrue(findMethod(read(owner), invoked, new HashSet<>()), "Missing redirected call " + call);
                            String invokedName = call.substring(semicolon + 1, paren), descriptor = call.substring(paren);
                            if (injection.desc.endsWith("/ModifyArg;")) {
                                Integer index = value(injection, "index");
                                assertNotNull(index); var argument = Type.getArgumentTypes(descriptor)[index];
                                assertEquals(argument, Type.getReturnType(method.desc), "Modified argument return type disagrees with " + call);
                                assertArrayEquals(new Type[]{argument}, Type.getArgumentTypes(method.desc), "Modified argument input disagrees with " + call);
                            } else assertEquals(Type.getReturnType(descriptor), Type.getReturnType(method.desc), "Redirect return type disagrees with " + call);
                            for (String selector : selectors) {
                                var candidates = findMethods(target, selector, new HashSet<>());
                                assertTrue(candidates.stream().anyMatch(candidate -> {
                                    for (var instruction : candidate.instructions) if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode invoke
                                        && invoke.owner.equals(owner) && invoke.name.equals(invokedName) && invoke.desc.equals(descriptor)) return true;
                                    return false;
                                }), "Redirect callsite missing in " + selector + " -> " + call); checked++;
                            }
                        } else if (injection.desc.endsWith("/Invoker;")) {
                            String invoked = value(injection, "value");
                            assertTrue(findMethod(target, invoked + method.desc, new HashSet<>()), "Missing invoker target " + invoked); checked++;
                        } else if (injection.desc.endsWith("/Accessor;")) {
                            String field = value(injection, "value");
                            assertTrue(target.fields.stream().anyMatch(f -> f.name.equals(field)), "Missing accessor field " + field); checked++;
                        }
                    }
                }
            }
        }
        assertTrue(checked >= 45, "Audit must cover the real registry, not an empty fixture");
    }
    @SuppressWarnings("unchecked") static <T> T value(AnnotationNode annotation, String name) {
        if (annotation.values == null) return null;
        for (int i = 0; i < annotation.values.size(); i += 2) if (annotation.values.get(i).equals(name)) return (T) annotation.values.get(i + 1);
        return null;
    }
    static ClassNode read(String name) throws Exception {
        try (InputStream in = MixinTargetsTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(in, "Class missing: " + name); ClassNode node = new ClassNode(); new ClassReader(in).accept(node, 0); return node;
        }
    }
    static List<org.objectweb.asm.tree.MethodNode> findMethods(ClassNode type, String selector, Set<String> visited) throws Exception {
        if (!visited.add(type.name)) return List.of();
        int paren = selector.indexOf('('); String name = paren < 0 ? selector : selector.substring(0, paren);
        String descriptor = paren < 0 ? null : selector.substring(paren);
        var found = new ArrayList<org.objectweb.asm.tree.MethodNode>();
        type.methods.stream().filter(m -> m.name.equals(name) && (descriptor == null || m.desc.equals(descriptor))).forEach(found::add);
        if (type.superName != null) found.addAll(findMethods(read(type.superName), selector, visited));
        for (String implemented : type.interfaces) found.addAll(findMethods(read(implemented), selector, visited));
        return found;
    }
    static boolean findMethod(ClassNode type, String selector, Set<String> visited) throws Exception {
        if (!visited.add(type.name)) return false;
        int paren = selector.indexOf('('); String name = paren < 0 ? selector : selector.substring(0, paren);
        String descriptor = paren < 0 ? null : selector.substring(paren);
        if (type.methods.stream().anyMatch(m -> m.name.equals(name) && (descriptor == null || m.desc.equals(descriptor)))) return true;
        if (type.superName != null && findMethod(read(type.superName), selector, visited)) return true;
        for (String implemented : type.interfaces) if (findMethod(read(implemented), selector, visited)) return true;
        return false;
    }
}
