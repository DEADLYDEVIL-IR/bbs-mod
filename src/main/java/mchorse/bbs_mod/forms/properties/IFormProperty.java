package mchorse.bbs_mod.forms.properties;

import mchorse.bbs_mod.data.IDataSerializable;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.interps.IInterp;

public interface IFormProperty <T> extends IDataSerializable<BaseType>
{
    /**
     * Get this property's key (which is used for serialization).
     */
    public String getKey();

    /**
     * Get this property's form owner.
     */
    public Form getForm();

    /**
     * Overwite the value of this property (this also stops tweening).
     */
    public void set(T value);

    /**
     * Get current value of the property (which isn't affected by tweening).
     */
    public T get();

    /**
     * Get tweened value (if this property's tween is in progress) or the current value.
     */
    public T get(float transition);

    /**
     * Get previous value of the property (which isn't affected by tweening).
     * It could be null.
     */
    public T getLast();

    /**
     * Update current property (needed for tweening).
     */
    public void update();

    /**
     * Tween this property to new value. Some properties may not fully support tweening!
     */
    public void tween(T preValue, T oldValue, T newValue, T postValue, float duration, IInterp interpolation, float offset, boolean playing);

    /**
     * Check whether this property is in progress of tweening.
     */
    public boolean isTweening();

    /**
     * Get tween factor (0 - started tweening, 1 - finished tweening).
     */
    public float getTweenFactor(float transition);

    /**
     * Get tween factor with interpolation applied (0 - started tweening, 1 - finished tweening).
     */
    public float getTweenFactorInterpolated(float transition);

    /**
     * Checks whether this property can create a generic keyframe channel
     */
    public boolean canCreateChannel();

    /**
     * Create a generic keyframe channel that can be used with this property
     */
    public KeyframeChannel createChannel(String key);
}