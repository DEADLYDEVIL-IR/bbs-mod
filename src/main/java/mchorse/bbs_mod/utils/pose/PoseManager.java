package mchorse.bbs_mod.utils.pose;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.MapType;

import java.io.File;
import java.io.FileNotFoundException;

public class PoseManager
{
    private static MapType poses = new MapType();

    public static MapType getPoses(String group)
    {
        MapType newPoses;

        if (poses.has(group))
        {
            return poses.getMap(group);
        }

        newPoses = new MapType();

        try
        {
            newPoses = (MapType) DataToString.read(getPosesFile(group));
        }
        catch (FileNotFoundException e)
        {}
        catch (Exception e)
        {
            e.printStackTrace();
        }

        poses.put(group, newPoses);

        return newPoses;
    }

    public static void savePose(String group, String key, MapType pose)
    {
        if (group.isEmpty())
        {
            System.err.println("Can't save empty pose group!");

            return;
        }

        MapType newPoses = poses.getMap(group);

        newPoses.put(key, pose);

        DataToString.writeSilently(getPosesFile(group), newPoses, true);
    }

    private static File getPosesFile(String group)
    {
        File poses = BBSMod.getAssetsPath(ModelManager.MODELS_PREFIX + group + "/");

        poses.mkdirs();

        return new File(poses, "poses.json");
    }
}