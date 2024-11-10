package mchorse.bbs_mod.settings.values.base;

import mchorse.bbs_mod.utils.DataPath;

import java.util.List;

public abstract class BaseValueGroup extends BaseValue
{
    public BaseValueGroup(String id)
    {
        super(id);
    }

    public abstract List<BaseValue> getAll();

    public abstract BaseValue get(String key);

    public BaseValue getRecursively(DataPath path)
    {
        BaseValue value = this.get(path.size() <= 0 ? "" : path.strings.get(0));

        if (value == null && !path.strings.isEmpty())
        {
            value = this.searchRecursively(path);
        }

        if (value == null)
        {
            throw new IllegalStateException("Property by path " + path + " can't be found!");
        }

        return value;
    }

    private BaseValue searchRecursively(DataPath splits)
    {
        int i = 0;
        BaseValue current = this;

        while (current != null && i < splits.size() - 1)
        {
            if (current instanceof BaseValueGroup)
            {
                i += 1;
                current = ((BaseValueGroup) current).get(splits.strings.get(i));
            }
            else
            {
                current = null;
            }
        }

        if (current == null)
        {
            return null;
        }

        return current;
    }

    public abstract void copy(BaseValueGroup group);
}