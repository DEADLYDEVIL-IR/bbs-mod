package mchorse.bbs_mod.cubic.model.loaders;

import mchorse.bbs_mod.cubic.CubicLoader;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.animation.Animation;
import mchorse.bbs_mod.cubic.data.animation.Animations;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelData;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.data.model.ModelMesh;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.obj.MeshOBJ;
import mchorse.bbs_mod.obj.MeshesOBJ;
import mchorse.bbs_mod.obj.OBJMaterial;
import mchorse.bbs_mod.obj.OBJParser;
import mchorse.bbs_mod.resources.AssetProvider;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.PNGEncoder;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.resources.Pixels;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CubicModelLoader implements IModelLoader
{
    @Override
    public ModelInstance load(String id, ModelManager models, Link model, Collection<Link> links, MapType config)
    {
        Link modelBBS = IModelLoader.getLink(model.combine("model.bbs.json"), links, ".bbs.json");
        Link modelTexture = IModelLoader.getLink(model.combine("model.png"), links, ".png");
        ModelInstance newModel = new ModelInstance(id, null, new Animations(models.parser), modelTexture);
        Map<String, MeshesOBJ> compile = this.tryLoadingOBJMeshes(models, model, IModelLoader.getLinks(links, ".obj"));
        Model theModel = null;

        try (InputStream stream = models.provider.getAsset(modelBBS))
        {
            CubicLoader loader = new CubicLoader();
            CubicLoader.LoadingInfo info = loader.load(models.parser, stream, modelBBS.path);

            if (info.model != null)
            {
                theModel = info.model;
            }

            if (info.animations != null)
            {
                for (Animation animation : info.animations.getAll())
                {
                    newModel.animations.add(animation);
                }
            }
        }
        catch (Exception e)
        {
            System.err.println("Failed to load BBS file: " + modelBBS);
        }

        /* Construct the model from compiled data */
        if (!compile.isEmpty())
        {
            HashSet<String> declined = new HashSet<>();

            if (theModel == null)
            {
                theModel = new Model(models.parser);
                theModel.textureWidth = 1;
                theModel.textureHeight = 1;

                this.tryMakingFlatMaterials(models.provider, model, newModel, compile);
            }

            for (Map.Entry<String, MeshesOBJ> entry : compile.entrySet())
            {
                MeshesOBJ value = entry.getValue();
                ModelGroup group = theModel.getGroup(entry.getKey());

                if (group == null)
                {
                    group = new ModelGroup(entry.getKey());

                    theModel.topGroups.add(group);
                }

                for (MeshOBJ mesh : value.meshes)
                {
                    ModelMesh modelMesh = new ModelMesh();

                    modelMesh.baseData.fill(mesh, theModel.textureWidth, theModel.textureHeight);
                    group.meshes.add(modelMesh);
                }

                this.fillShapes(declined, value.shapes, group, theModel.textureWidth, theModel.textureHeight);
            }

            theModel.initialize();

            for (String s : declined)
            {
                System.out.println("Model \"" + model + "\" has shape keys \"" + s + "\" that have invalid shape keys (triangle count doesn't match)!");
            }
        }

        if (theModel == null || theModel.topGroups.isEmpty())
        {
            return null;
        }

        newModel.model = theModel;

        for (Animation animation : this.tryLoadingExternalAnimations(models, config).getAll())
        {
            newModel.animations.add(animation);
        }

        newModel.applyConfig(config);

        return newModel;
    }

    private void tryMakingFlatMaterials(AssetProvider provider, Link model, ModelInstance newModel, Map<String, MeshesOBJ> compile)
    {
        List<OBJMaterial> materials = new ArrayList<>();

        for (MeshesOBJ value : compile.values())
        {
            for (MeshOBJ mesh : value.meshes)
            {
                if (mesh.material == null)
                {
                    continue;
                }

                if (!materials.contains(mesh.material))
                {
                    materials.add(mesh.material);
                }

                if (mesh.material.useTexture)
                {
                    return;
                }
            }
        }

        Link paletteLink = model.combine("palette.png");
        File paletteFile = provider.getFile(paletteLink);
        Pixels pixels = Pixels.fromSize(materials.size(), 1);

        for (int x = 0; x < materials.size(); x++)
        {
            OBJMaterial objMaterial = materials.get(x);
            Color set = new Color().set(objMaterial.r, objMaterial.g, objMaterial.b, 1F);
            
            pixels.setColor(x, 0, set);
        }

        try
        {
            PNGEncoder.writeToFile(pixels, paletteFile);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        pixels.delete();

        for (MeshesOBJ value : compile.values())
        {
            for (MeshOBJ mesh : value.meshes)
            {
                int index = materials.indexOf(mesh.material);

                for (int i = 0, c = mesh.triangles; i < c; i++)
                {
                    mesh.texData[i * 2] = (index + 0.5F) / materials.size();
                    mesh.texData[i * 2 + 1] = 0.5F;
                }
            }
        }

        newModel.texture = paletteLink;
    }

    /**
     * Load OBJ meshes from multiple files
     */
    private Map<String, MeshesOBJ> tryLoadingOBJMeshes(ModelManager models, Link model, List<Link> modelsOBJ)
    {
        Map<String, MeshesOBJ> compile = new HashMap<>();

        /* Load the base OBJ file */
        for (Link link : modelsOBJ)
        {
            String path = link.path.substring(model.path.length() + 1);

            if (path.contains("/"))
            {
                continue;
            }

            Link mtl = new Link(link.source, StringUtils.removeExtension(link.path) + ".mtl");

            try (InputStream stream = models.provider.getAsset(link))
            {
                InputStream mtlStream = null;

                try
                {
                    mtlStream = models.provider.getAsset(mtl);
                }
                catch (Exception e)
                {}

                OBJParser parser = new OBJParser(stream, mtlStream);

                parser.read();
                compile.putAll(parser.compile());

                if (mtlStream != null)
                {
                    mtlStream.close();
                }

                break;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        /* Load shapes from shapes folder */
        for (Link link : modelsOBJ)
        {
            if (!link.path.contains("/shapes/"))
            {
                continue;
            }

            try (InputStream stream = models.provider.getAsset(link))
            {
                OBJParser parser = new OBJParser(stream, null);
                String name = StringUtils.fileName(StringUtils.removeExtension(link.path));

                parser.read();

                Map<String, MeshesOBJ> compiled = parser.compile();

                for (Map.Entry<String, MeshesOBJ> entry : compiled.entrySet())
                {
                    MeshesOBJ meshesOBJ = compile.get(entry.getKey());

                    if (meshesOBJ == null)
                    {
                        compile.put(entry.getKey(), entry.getValue());
                    }
                    else
                    {
                        meshesOBJ.mergeShape(name, entry.getValue());
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return compile;
    }

    private void fillShapes(Set<String> declined, Map<String, List<MeshOBJ>> shapes, ModelGroup group, int tw, int th)
    {
        if (shapes == null)
        {
            return;
        }

        for (Map.Entry<String, List<MeshOBJ>> shapeEntry : shapes.entrySet())
        {
            int i = 0;

            for (MeshOBJ mesh : shapeEntry.getValue())
            {
                ModelMesh modelMesh = CollectionUtils.getSafe(group.meshes, i);
                ModelData data = new ModelData();

                data.fill(mesh, tw, th);

                if (
                    data.vertices.size() == modelMesh.baseData.vertices.size() &&
                    data.normals.size() == modelMesh.baseData.normals.size() &&
                    data.uvs.size() == modelMesh.baseData.uvs.size()
                ) {
                    modelMesh.data.put(shapeEntry.getKey(), data);
                }
                else
                {
                    declined.add(shapeEntry.getKey());
                }

                i += 1;
            }
        }
    }

    /**
     * Loading external animations mentioned in the config
     */
    private Animations tryLoadingExternalAnimations(ModelManager models, MapType config)
    {
        Animations animations = new Animations(models.parser);

        if (config == null)
        {
            return animations;
        }

        for (BaseType type : config.getList("animations"))
        {
            if (type.isString())
            {
                Link animationFile = Link.create(type.asString());

                try (InputStream asset = models.provider.getAsset(animationFile))
                {
                    CubicLoader loader = new CubicLoader();
                    CubicLoader.LoadingInfo info = loader.load(models.parser, asset, type.asString());

                    if (info.animations != null)
                    {
                        for (Animation animation : info.animations.getAll())
                        {
                            animations.add(animation);
                        }
                    }
                }
                catch (FileNotFoundException e)
                {
                    return new Animations(models.parser);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }

        return animations;
    }
}