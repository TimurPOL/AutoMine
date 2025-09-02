package org.example.e.untitled5.Module.AutoShaXTa.Utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;

public class Modules {
    public int key;
    public String name;
    public boolean toggle;
    public Minecraft mc  = Minecraft.getInstance();
    public Modules(int key,String name){
        this.name = name;
        this.key = key;
    }
    public int getKey(){
        return key;
    }   public boolean getToggle(){
        return toggle;
    }
    public void isToggle(){
        toggle = !toggle;
         if(toggle){
             onEnable();
         }else {
             onDisable();
         }
    }
    public void onDisable(){
        MinecraftForge.EVENT_BUS.unregister(this);
        mc.player.sendMessage(new StringTextComponent("disable"),mc.player.getUUID());
    }
    public void onEnable(){
        MinecraftForge.EVENT_BUS.register(this);
        mc.player.sendMessage(new StringTextComponent("enable"),mc.player.getUUID());

    }
}
