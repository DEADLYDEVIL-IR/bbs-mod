package mchorse.bbs_mod.settings.values;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.LongType;
import mchorse.bbs_mod.settings.values.base.BaseValueNumber;
import mchorse.bbs_mod.utils.MathUtils;

public class ValueLong extends BaseValueNumber<Long>
{
    public ValueLong(String id, Long defaultValue)
    {
        this(id, defaultValue, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public ValueLong(String id, Long defaultValue, Long min, Long max)
    {
        super(id, defaultValue, min, max);
    }

    @Override
    protected Long clamp(Long value)
    {
        return MathUtils.clamp(value, this.min, this.max);
    }

    @Override
    public BaseType toData()
    {
        return new LongType(this.value);
    }

    @Override
    public void fromData(BaseType data)
    {
        if (data.isNumeric())
        {
            this.value = data.asNumeric().longValue();
        }
    }

    @Override
    public String toString()
    {
        return Long.toString(this.value);
    }
}