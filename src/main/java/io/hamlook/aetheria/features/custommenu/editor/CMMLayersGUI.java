package io.hamlook.aetheria.features.custommenu.editor;

import io.hamlook.aetheria.features.custommenu.CustomMMConfig;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.features.custommenu.util.CMMHelper;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

public class CMMLayersGUI extends AetheriaBaseScreen {
    private final CustomMMConfig config; private final GuiScreen parent; private GuiTextField search;
    public CMMLayersGUI(CustomMMConfig config, GuiScreen parent) { this.config = config; this.parent = parent; }
    @Override protected void onInitGui() { search = new GuiTextField(0, MinecraftCompat.getFontRenderer(), width/2-180, 42, 360, 20); search.setMaxStringLength(128); }
    @Override public void onResize(net.minecraft.client.Minecraft mc, int w, int h) { super.onResize(mc, w, h); ScreenHelper.updateScreenDimensions(w, h); if(search!=null){search.xPosition=w/2-ScreenHelper.getStaticWidth(180);search.yPosition=ScreenHelper.getStaticHeight(42);search.width=ScreenHelper.getStaticWidth(360);search.height=ScreenHelper.getStaticHeight(20);} }
    @Override protected void onDrawScreen(int mx, int my, float pt) {
        drawRect(0,0,width,height,0xF0121218); TextRenderUtils.drawCenteredStringScaleAware("CMM Layers",width/2f,25,0xFFFFFFFF,2f,true); search.drawTextBox();
        int row=0; String filter=search.getText().toLowerCase();
        for (int i=config.elements.size()-1;i>=0;i--) { CMMElement e=config.elements.get(i); String n=e.displayName==null||e.displayName.isEmpty()?e.getClass().getSimpleName():e.displayName; if(!filter.isEmpty()&&!n.toLowerCase().contains(filter))continue; int y=78+row++*25; boolean h=mx>width/2-180&&mx<width/2+180&&my>y-4&&my<y+18; drawRect(width/2-180,y-4,width/2+180,y+18,h?0xFF3B6982:0xFF25252D); TextRenderUtils.drawStringScaleAware((i+1)+"  "+n,width/2-165,y+3,e.visible?0xFFFFFFFF:0xFF777777,1f,false); TextRenderUtils.drawStringScaleAware(e.visible?"Visible":"Hidden",width/2+80,y+3,0xFFB8B8C8,1f,false); }
        TextRenderUtils.drawCenteredStringScaleAware("Left click: bring to front | Right click: toggle visibility | Escape: return",width/2f,height-30,0xFFB8B8C8,1f,false);
    }
    @Override protected void onMouseClicked(int mx,int my,int button) { search.mouseClicked(mx,my,button); if(my<78)return; int row=(my-74)/25; int visible=0; String filter=search.getText().toLowerCase(); for(int i=config.elements.size()-1;i>=0;i--){String n=config.elements.get(i).displayName==null||config.elements.get(i).displayName.isEmpty()?config.elements.get(i).getClass().getSimpleName():config.elements.get(i).displayName;if(filter.isEmpty()||n.toLowerCase().contains(filter)){if(visible++==row){CMMElement e=config.elements.get(i);if(button==1)e.visible=!e.visible;else{config.elements.remove(i);config.elements.add(e);}CMMHelper.savePreset(config);return;}}} }
    @Override protected void onKeyTyped(char c,int key){if(search.textboxKeyTyped(c,key))return;if(key==Keyboard.KEY_ESCAPE)MinecraftCompat.getMinecraft().displayGuiScreen(parent);}
}
