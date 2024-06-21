package mchorse.bbs_mod.camera.controller;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.utils.MathUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CameraController implements ICameraController
{
    public Camera camera = new Camera();
    private ICameraController current;
    private List<ICameraController> controllers = new ArrayList<>();

    public Vector3d getPosition()
    {
        return this.camera.position;
    }

    public float getYaw()
    {
        return MathUtils.toDeg(this.camera.rotation.y - MathUtils.PI);
    }

    public float getPitch()
    {
        return MathUtils.toDeg(this.camera.rotation.x);
    }

    public float getRoll()
    {
        return MathUtils.toDeg(this.camera.rotation.z);
    }

    public double getFOV()
    {
        return MathUtils.toDeg(this.camera.fov);
    }

    public void updateCurrent()
    {
        ICameraController current = null;

        for (ICameraController controller : this.controllers)
        {
            if (current == null)
            {
                current = controller;
            }
            else if (controller.getPriority() > current.getPriority())
            {
                current = controller;
            }
        }

        this.current = current;
    }

    public ICameraController getCurrent()
    {
        return this.current;
    }

    public void add(ICameraController controller)
    {
        this.controllers.add(controller);
        this.updateCurrent();
    }

    public void remove(Class clazz)
    {
        Iterator<ICameraController> it = this.controllers.iterator();

        while (it.hasNext())
        {
            if (it.next().getClass() == clazz)
            {
                it.remove();
            }
        }

        this.updateCurrent();
    }

    public void remove(ICameraController controller)
    {
        Iterator<ICameraController> it = this.controllers.iterator();

        while (it.hasNext())
        {
            if (it.next() == controller)
            {
                it.remove();
            }
        }

        this.updateCurrent();
    }

    @Override
    public void update()
    {
        if (this.current != null)
        {
            this.current.update();
        }
    }

    @Override
    public void setup(Camera camera, float transition)
    {
        if (this.current != null)
        {
            this.current.setup(camera, transition);
        }
    }

    public boolean has(ICameraController controller)
    {
        return this.controllers.contains(controller);
    }

    public boolean has(Class clazz)
    {
        for (ICameraController controller : this.controllers)
        {
            if (controller.getClass() == clazz)
            {
                return true;
            }
        }

        return false;
    }

    public void copy(Entity cameraEntity)
    {
        if (cameraEntity == null)
        {
            return;
        }

        Vec3d eyePos = cameraEntity.getEyePos();

        this.camera.position.set(eyePos.x, eyePos.y, eyePos.z);
        this.camera.rotation.set(MathUtils.toRad(cameraEntity.getPitch()), MathUtils.toRad(cameraEntity.getHeadYaw()), 0);
        this.camera.fov = MathUtils.toRad(MinecraftClient.getInstance().options.getFov().getValue());
    }
}