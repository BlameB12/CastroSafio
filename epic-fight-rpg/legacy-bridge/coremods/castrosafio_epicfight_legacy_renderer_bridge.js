var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var FieldNode = Java.type('org.objectweb.asm.tree.FieldNode');
var MethodNode = Java.type('org.objectweb.asm.tree.MethodNode');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var TypeInsnNode = Java.type('org.objectweb.asm.tree.TypeInsnNode');
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var LdcInsnNode = Java.type('org.objectweb.asm.tree.LdcInsnNode');

var RENDER_BASE = 'yesman/epicfight/client/renderer/patched/item/RenderItemBase';
var MATRIX = 'yesman/epicfight/api/utils/math/OpenMatrix4f';
var LIVING_PATCH = 'yesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch';
var ARMATURE = 'yesman/epicfight/api/model/Armature';
var HUMANOID_ARMATURE = 'yesman/epicfight/model/armature/HumanoidArmature';
var POSE_STACK = 'com/mojang/blaze3d/vertex/PoseStack';
var ITEM = 'net/minecraft/world/item/Item';
var CLIENT_ENGINE = 'yesman/epicfight/client/ClientEngine';
var RENDER_ENGINE = 'yesman/epicfight/client/events/engine/RenderEngine';

var MATRIX_DESC = 'L' + MATRIX + ';';
var RENDER_BASE_DESC = 'L' + RENDER_BASE + ';';
var ITEM_DESC = 'L' + ITEM + ';';
var MODERN_RENDER_DESC = '(Lnet/minecraft/world/item/ItemStack;L' + LIVING_PATCH + ';Lnet/minecraft/world/InteractionHand;[' + MATRIX_DESC + 'Lnet/minecraft/client/renderer/MultiBufferSource;L' + POSE_STACK + ';IF)V';
var LEGACY_RENDER_DESC = '(Lnet/minecraft/world/item/ItemStack;L' + LIVING_PATCH + ';Lnet/minecraft/world/InteractionHand;L' + HUMANOID_ARMATURE + ';[' + MATRIX_DESC + 'Lnet/minecraft/client/renderer/MultiBufferSource;L' + POSE_STACK + ';IF)V';
var LEGACY_ADD_ITEM_RENDERER_DESC = '(' + ITEM_DESC + RENDER_BASE_DESC + ')V';

function hasField(classNode, name, desc) {
    for (var i = 0; i < classNode.fields.size(); i++) {
        var field = classNode.fields.get(i);
        if (field.name === name && field.desc === desc) return true;
    }
    return false;
}

function hasMethod(classNode, name, desc) {
    for (var i = 0; i < classNode.methods.size(); i++) {
        var method = classNode.methods.get(i);
        if (method.name === name && method.desc === desc) return true;
    }
    return false;
}

function addLegacyMatrixFields(classNode) {
    if (!hasField(classNode, 'mainhandcorrectionMatrix', MATRIX_DESC)) {
        classNode.fields.add(new FieldNode(Opcodes.ACC_PROTECTED, 'mainhandcorrectionMatrix', MATRIX_DESC, null, null));
    }
    if (!hasField(classNode, 'offhandCorrectionMatrix', MATRIX_DESC)) {
        classNode.fields.add(new FieldNode(Opcodes.ACC_PROTECTED, 'offhandCorrectionMatrix', MATRIX_DESC, null, null));
    }
}

function addLegacyNoArgConstructor(classNode) {
    if (hasMethod(classNode, '<init>', '()V')) return;

    var m = new MethodNode(Opcodes.ACC_PUBLIC, '<init>', '()V', null, null);
    var insn = m.instructions;

    // Delegate to the modern JSON constructor with an empty object.
    insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
    insn.add(new TypeInsnNode(Opcodes.NEW, 'com/google/gson/JsonObject'));
    insn.add(new InsnNode(Opcodes.DUP));
    insn.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, 'com/google/gson/JsonObject', '<init>', '()V', false));
    insn.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, RENDER_BASE, '<init>', '(Lcom/google/gson/JsonElement;)V', false));

    // Legacy main-hand matrix = mutable copy of the modern Tool_R default.
    insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
    insn.add(new TypeInsnNode(Opcodes.NEW, MATRIX));
    insn.add(new InsnNode(Opcodes.DUP));
    insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
    insn.add(new FieldInsnNode(Opcodes.GETFIELD, RENDER_BASE, 'mainhandCorrectionTransforms', 'Ljava/util/Map;'));
    insn.add(new LdcInsnNode('Tool_R'));
    insn.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, 'java/util/Map', 'get', '(Ljava/lang/Object;)Ljava/lang/Object;', true));
    insn.add(new TypeInsnNode(Opcodes.CHECKCAST, MATRIX));
    insn.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, MATRIX, '<init>', '(' + MATRIX_DESC + ')V', false));
    insn.add(new FieldInsnNode(Opcodes.PUTFIELD, RENDER_BASE, 'mainhandcorrectionMatrix', MATRIX_DESC));

    // Legacy off-hand matrix = mutable copy of the modern Tool_L default.
    insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
    insn.add(new TypeInsnNode(Opcodes.NEW, MATRIX));
    insn.add(new InsnNode(Opcodes.DUP));
    insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
    insn.add(new FieldInsnNode(Opcodes.GETFIELD, RENDER_BASE, 'offhandCorrectionTransforms', 'Ljava/util/Map;'));
    insn.add(new LdcInsnNode('Tool_L'));
    insn.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, 'java/util/Map', 'get', '(Ljava/lang/Object;)Ljava/lang/Object;', true));
    insn.add(new TypeInsnNode(Opcodes.CHECKCAST, MATRIX));
    insn.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, MATRIX, '<init>', '(' + MATRIX_DESC + ')V', false));
    insn.add(new FieldInsnNode(Opcodes.PUTFIELD, RENDER_BASE, 'offhandCorrectionMatrix', MATRIX_DESC));

    insn.add(new InsnNode(Opcodes.RETURN));
    m.maxStack = 6;
    m.maxLocals = 1;
    classNode.methods.add(m);
}

function addLegacyMulPoseStack(classNode) {
    var desc = '(L' + POSE_STACK + ';' + MATRIX_DESC + ')V';
    if (hasMethod(classNode, 'mulPoseStack', desc)) return;

    var m = new MethodNode(Opcodes.ACC_PROTECTED, 'mulPoseStack', desc, null, null);
    m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
    m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
    m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, 'yesman/epicfight/api/utils/math/MathUtils', 'mulStack', desc, false));
    m.instructions.add(new InsnNode(Opcodes.RETURN));
    m.maxStack = 2;
    m.maxLocals = 3;
    classNode.methods.add(m);
}

function addModernRendererBridge(classNode) {
    if (hasMethod(classNode, 'renderItemInHand', MODERN_RENDER_DESC)) return;
    if (!hasMethod(classNode, 'renderItemInHand', LEGACY_RENDER_DESC)) return;

    var m = new MethodNode(Opcodes.ACC_PUBLIC, 'renderItemInHand', MODERN_RENDER_DESC, null, null);
    var insn = m.instructions;

    // this.renderItemInHand(stack, patch, hand, (HumanoidArmature) patch.getArmature(), poses, buffer, poseStack, light, partialTicks)
    insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
    insn.add(new VarInsnNode(Opcodes.ALOAD, 1));
    insn.add(new VarInsnNode(Opcodes.ALOAD, 2));
    insn.add(new VarInsnNode(Opcodes.ALOAD, 3));
    insn.add(new VarInsnNode(Opcodes.ALOAD, 2));
    insn.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, LIVING_PATCH, 'getArmature', '()L' + ARMATURE + ';', false));
    insn.add(new TypeInsnNode(Opcodes.CHECKCAST, HUMANOID_ARMATURE));
    insn.add(new VarInsnNode(Opcodes.ALOAD, 4));
    insn.add(new VarInsnNode(Opcodes.ALOAD, 5));
    insn.add(new VarInsnNode(Opcodes.ALOAD, 6));
    insn.add(new VarInsnNode(Opcodes.ILOAD, 7));
    insn.add(new VarInsnNode(Opcodes.FLOAD, 8));
    insn.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, classNode.name, 'renderItemInHand', LEGACY_RENDER_DESC, false));
    insn.add(new InsnNode(Opcodes.RETURN));

    m.maxStack = 10;
    m.maxLocals = 9;
    classNode.methods.add(m);
}

function addRenderEngineLegacyItemInstaller(classNode) {
    var methodName = 'castrosafio$legacyAddItemRenderer';
    if (hasMethod(classNode, methodName, LEGACY_ADD_ITEM_RENDERER_DESC)) return;

    var m = new MethodNode(Opcodes.ACC_PUBLIC, methodName, LEGACY_ADD_ITEM_RENDERER_DESC, null, null);
    var insn = m.instructions;

    // this.itemRendererMapByInstance.put(item, renderer)
    insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
    insn.add(new FieldInsnNode(Opcodes.GETFIELD, RENDER_ENGINE, 'itemRendererMapByInstance', 'Ljava/util/Map;'));
    insn.add(new VarInsnNode(Opcodes.ALOAD, 1));
    insn.add(new VarInsnNode(Opcodes.ALOAD, 2));
    insn.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, 'java/util/Map', 'put', '(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;', true));
    insn.add(new InsnNode(Opcodes.POP));
    insn.add(new InsnNode(Opcodes.RETURN));

    m.maxStack = 3;
    m.maxLocals = 3;
    classNode.methods.add(m);
}

function addLegacyPatchedRenderersAddItemRenderer(classNode) {
    if (hasMethod(classNode, 'addItemRenderer', LEGACY_ADD_ITEM_RENDERER_DESC)) return;

    var m = new MethodNode(Opcodes.ACC_PUBLIC, 'addItemRenderer', LEGACY_ADD_ITEM_RENDERER_DESC, null, null);
    var insn = m.instructions;

    // ClientEngine.getInstance().renderEngine.castrosafio$legacyAddItemRenderer(item, renderer)
    insn.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CLIENT_ENGINE, 'getInstance', '()L' + CLIENT_ENGINE + ';', false));
    insn.add(new FieldInsnNode(Opcodes.GETFIELD, CLIENT_ENGINE, 'renderEngine', 'L' + RENDER_ENGINE + ';'));
    insn.add(new VarInsnNode(Opcodes.ALOAD, 1));
    insn.add(new VarInsnNode(Opcodes.ALOAD, 2));
    insn.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, RENDER_ENGINE, 'castrosafio$legacyAddItemRenderer', LEGACY_ADD_ITEM_RENDERER_DESC, false));
    insn.add(new InsnNode(Opcodes.RETURN));

    m.maxStack = 3;
    m.maxLocals = 3;
    classNode.methods.add(m);
}

function initializeCoreMod() {
    return {
        'CastroSafio RenderItemBase legacy API': {
            'target': {
                'type': 'CLASS',
                'name': 'yesman.epicfight.client.renderer.patched.item.RenderItemBase'
            },
            'transformer': function(classNode) {
                addLegacyMatrixFields(classNode);
                addLegacyNoArgConstructor(classNode);
                addLegacyMulPoseStack(classNode);
                return classNode;
            }
        },
        'CastroSafio RenderEngine legacy item installer': {
            'target': {
                'type': 'CLASS',
                'name': 'yesman.epicfight.client.events.engine.RenderEngine'
            },
            'transformer': function(classNode) {
                addRenderEngineLegacyItemInstaller(classNode);
                return classNode;
            }
        },
        'CastroSafio PatchedRenderersEvent Add legacy item registration': {
            'target': {
                'type': 'CLASS',
                'name': 'yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent$Add'
            },
            'transformer': function(classNode) {
                addLegacyPatchedRenderersAddItemRenderer(classNode);
                return classNode;
            }
        },
        'CastroSafio Warriors Of World Espada Oscura renderer bridge': {
            'target': {
                'type': 'CLASS',
                'name': 'warriorsofworldcaballeros.EspadaOscuraRender'
            },
            'transformer': function(classNode) {
                addModernRendererBridge(classNode);
                return classNode;
            }
        },
        'CastroSafio Warriors Of World Claymore renderer bridge': {
            'target': {
                'type': 'CLASS',
                'name': 'warriorsofworldcaballeros.ClaymoreRender'
            },
            'transformer': function(classNode) {
                addModernRendererBridge(classNode);
                return classNode;
            }
        }
    };
}
