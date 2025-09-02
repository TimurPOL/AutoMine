package org.example.e.untitled5.Module.AutoShaXTa.Utils;

import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.example.e.untitled5.Module.Modules.AutoMine;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RegisterModule {
    public static CopyOnWriteArrayList<Modules> m = new CopyOnWriteArrayList<>();
    public static void register(){
        m.add(new AutoMine(GLFW.GLFW_KEY_R,"n"));
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event){
     if (!(event.getAction() == GLFW.GLFW_PRESS))return;
        for (Modules module : m){
            if(event.getKey() == module.getKey()){
                module.isToggle();
            }
        }

    }
}
