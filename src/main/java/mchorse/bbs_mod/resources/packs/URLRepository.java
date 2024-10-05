package mchorse.bbs_mod.resources.packs;

import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.FFMpegUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class URLRepository
{
    private File folder;
    private File cacheFile;
    private Map<String, File> cache = new HashMap<>();

    public URLRepository(File folder)
    {
        this.folder = folder;
        this.folder.mkdirs();

        this.cacheFile = new File(this.folder, "cache.json");

        this.readCache();
    }

    private void readCache()
    {
        try
        {
            MapType type = (MapType) DataToString.read(this.cacheFile);

            this.cache.clear();

            for (String key : type.keys())
            {
                this.cache.put(key, new File(this.folder, type.getString(key)));
            }
        }
        catch (Exception e)
        {}
    }

    private void saveCache()
    {
        try
        {
            MapType type = new MapType();

            for (Map.Entry<String, File> entry : this.cache.entrySet())
            {
                type.putString(entry.getKey(), entry.getValue().getName());
            }

            DataToString.writeSilently(this.cacheFile, type, true);
        }
        catch (Exception e)
        {}
    }

    public File getFile(String url)
    {
        return this.cache.get(url);
    }

    public File convertInputStream(String url, InputStream stream) throws Exception
    {
        String uuid = UUID.randomUUID() + ".png";
        File buffer = new File(this.folder, "buffer");
        File value = new File(this.folder, uuid);

        IOUtils.copy(stream, new FileOutputStream(buffer));

        if (FFMpegUtils.execute(this.folder, "-i", "buffer", uuid))
        {
            this.cache.put(url, value);
            this.saveCache();

            return value;
        }
        else
        {
            URLTextureErrorCallback.EVENT.invoker().onError(url, URLError.FFMPEG);
        }

        return null;
    }
}