package io.hamlook.aetheria.features.custommenu.editor;

import io.hamlook.aetheria.features.custommenu.CustomMMConfig;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.features.custommenu.util.CMMHelper;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import io.hamlook.aetheria.features.custommenu.util.ScreenHelper;
import io.hamlook.aetheria.features.custommenu.util.PercentageUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

public class CMMLayersGUI extends AetheriaBaseScreen {
    private final CustomMMConfig config; private final GuiScreen parent; private GuiTextField search;
    public CMMLayersGUI(CustomMMConfig config, GuiScreen parent) { this.config = config; this.parent = parent; }
    private int left() { return PercentageUtils.centeredX(42f); }
    private int panelWidth() { return PercentageUtils.width(42f); }
    private int rowY(int row) { return PercentageUtils.y(16.25f) + row * PercentageUtils.height(5.2f); }
    @Override protected void onInitGui() { search = new GuiTextField(0, MinecraftCompat.getFontRenderer(), left(), PercentageUtils.y(8.75f), panelWidth(), PercentageUtils.height(4.2f)); search.setMaxStringLength(128); }
    @Override public void onResize(net.minecraft.client.Minecraft mc, int w, int h) { super.onResize(mc, w, h); ScreenHelper.updateScreenDimensions(w, h); if(search!=null){search.xPosition=left();search.yPosition=PercentageUtils.y(8.75f);search.width=panelWidth();search.height=PercentageUtils.height(4.2f);} }
    @Override protected void onDrawScreen(int mx, int my, float pt) {
        drawRect(0,0,width,height,0xF0121218); TextRenderUtils.drawCenteredStringScaleAware("CMM Layers",PercentageUtils.centerX(),PercentageUtils.y(5f),0xFFFFFFFF,2f,true); search.drawTextBox();
        int row=0; String filter=search.getText().toLowerCase();
        for (int i=config.elements.size()-1;i>=0;i--) { CMMElement e=config.elements.get(i); String n=e.displayName==null||e.displayName.isEmpty()?e.getClass().getSimpleName():e.displayName; if(!filter.isEmpty()&&!n.toLowerCase().contains(filter))continue; int y=rowY(row++); boolean h=mx>left()&&mx<left()+panelWidth()&&my>y-PercentageUtils.height(0.8f)&&my<y+PercentageUtils.height(4.6f); drawRect(left(),y-PercentageUtils.height(0.8f),left()+panelWidth(),y+PercentageUtils.height(4.6f),h?0xFF3B6982:0xFF25252D); TextRenderUtils.drawStringScaleAware((i+1)+"  "+n,left()+PercentageUtils.width(1.8f),y+PercentageUtils.height(0.6f),e.visible?0xFFFFFFFF:0xFF777777,1f,false); TextRenderUtils.drawStringScaleAware(e.visible?"Visible":"Hidden",left()+PercentageUtils.width(31f),y+PercentageUtils.height(0.6f),0xFFB8B8C8,1f,false); }
        TextRenderUtils.drawCenteredStringScaleAware("Left click: bring to front | Right click: toggle visibility | Escape: return",PercentageUtils.centerX(),PercentageUtils.y(94f),0xFFB8B8C8,1f,false);
    }
    @Override protected void onMouseClicked(int mx,int my,int button) { search.mouseClicked(mx,my,button); if(my<PercentageUtils.y(16.25f))return; int row=(my-PercentageUtils.y(15.4f))/PercentageUtils.height(5.2f); int visible=0; String filter=search.getText().toLowerCase(); for(int i=config.elements.size()-1;i>=0;i--){String n=config.elements.get(i).displayName==null||config.elements.get(i).displayName.isEmpty()?config.elements.get(i).getClass().getSimpleName():config.elements.get(i).displayName;if(filter.isEmpty()||n.toLowerCase().contains(filter)){if(visible++==row){CMMElement e=config.elements.get(i);if(button==1)e.visible=!e.visible;else{config.elements.remove(i);config.elements.add(e);}CMMHelper.savePreset(config);return;}}} }
    @Override protected void onKeyTyped(char c,int key){if(search.textboxKeyTyped(c,key))return;if(key==Keyboard.KEY_ESCAPE)MinecraftCompat.getMinecraft().displayGuiScreen(parent);}
}
