package mchorse.bbs_mod.utils.keyframes.factories;

import java.util.HashMap;
import java.util.Map;

public class KeyframeFactories
{
    public static final Map<String, IKeyframeFactory> FACTORIES = new HashMap<>();

    public static final ColorKeyframeFactory COLOR = new ColorKeyframeFactory();
    public static final TransformKeyframeFactory TRANSFORM = new TransformKeyframeFactory();
    public static final PoseKeyframeFactory POSE = new PoseKeyframeFactory();
    public static final BooleanKeyframeFactory BOOLEAN = new BooleanKeyframeFactory();
    public static final StringKeyframeFactory STRING = new StringKeyframeFactory();
    public static final FloatKeyframeFactory FLOAT = new FloatKeyframeFactory();
    public static final DoubleKeyframeFactory DOUBLE = new DoubleKeyframeFactory();
    public static final IntegerKeyframeFactory INTEGER = new IntegerKeyframeFactory();
    public static final LinkKeyframeFactory LINK = new LinkKeyframeFactory();
    public static final Vector4fKeyframeFactory VECTOR4F = new Vector4fKeyframeFactory();
    public static final AnchorKeyframeFactory ANCHOR = new AnchorKeyframeFactory();
    public static final BlockStateKeyframeFactory BLOCK_STATE = new BlockStateKeyframeFactory();
    public static final ItemStackKeyframeFactory ITEM_STACK = new ItemStackKeyframeFactory();
    public static final ActionsConfigKeyframeFactory ACTIONS_CONFIG = new ActionsConfigKeyframeFactory();

    static
    {
        FACTORIES.put("color", COLOR);
        FACTORIES.put("transform", TRANSFORM);
        FACTORIES.put("pose", POSE);
        FACTORIES.put("boolean", BOOLEAN);
        FACTORIES.put("string", STRING);
        FACTORIES.put("float", FLOAT);
        FACTORIES.put("double", DOUBLE);
        FACTORIES.put("integer", INTEGER);
        FACTORIES.put("link", LINK);
        FACTORIES.put("vector4f", VECTOR4F);
        FACTORIES.put("anchor", ANCHOR);
        FACTORIES.put("block_state", BLOCK_STATE);
        FACTORIES.put("item_stack", ITEM_STACK);
        FACTORIES.put("actions_config", ACTIONS_CONFIG);
    }
}