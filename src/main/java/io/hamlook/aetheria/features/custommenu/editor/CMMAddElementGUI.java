package io.hamlook.aetheria.features.custommenu.editor;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.custommenu.CustomMMConfig;
import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.features.custommenu.ui.buttons.impl.ActionButton;
import io.hamlook.aetheria.features.custommenu.ui.buttons.impl.GuiButton;
import io.hamlook.aetheria.features.custommenu.ui.dropdown.CMMDropdown;
import io.hamlook.aetheria.features.custommenu.ui.sprites.Sprite;
import io.hamlook.aetheria.features.custommenu.ui.text.Text;
import io.hamlook.aetheria.features.custommenu.util.CMMHelper;
import io.hamlook.aetheria.utils.compat.AetheriaBaseScreen;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import java.util.Arrays;

public class CMMAddElementGUI extends AetheriaBaseScreen {
    private final CustomMMConfig config; private final GuiScreen parent;
    private final String[] types = {"GuiButton", "Action Button", "Text", "Sprite", "Dropdown"};
    private final String[] presets = {"Singleplayer Button", "Multiplayer Button", "Aetheria Options Menu Button", "Aetheria's Skyblock Mod Title Text", "Aetheria's Skyblock Mod Logo"};
    private boolean presetsTab;
    public CMMAddElementGUI(CustomMMConfig config, GuiScreen parent) { this.config=config; this.parent=parent; }
    @Override protected void onDrawScreen(int mx,int my,float pt) { drawRect(0,0,width,height,0xF0121218); TextRenderUtils.drawCenteredStringScaleAware("Add CMM Element",width/2f,35,0xFFFFFFFF,2f,true); drawRect(width/2-155,55,width/2-5,78,presetsTab?0xFF292932:0xFF3B6982); drawRect(width/2+5,55,width/2+155,78,presetsTab?0xFF3B6982:0xFF292932); TextRenderUtils.drawCenteredStringScaleAware("Elements",width/2-80,67,0xFFFFFFFF,.9f,false); TextRenderUtils.drawCenteredStringScaleAware("Presets",width/2+80,67,0xFFFFFFFF,.9f,false); String[] list=presetsTab?presets:types; for(int i=0;i<list.length;i++){int y=90+i*32;boolean h=mx>width/2-150&&mx<width/2+150&&my>y&&my<y+24;drawRect(width/2-150,y,width/2+150,y+24,h?0xFF3B6982:0xFF292932);TextRenderUtils.drawCenteredStringScaleAware(list[i],width/2f,y+12,0xFFFFFFFF,.9f,false);} TextRenderUtils.drawCenteredStringScaleAware("Escape: return",width/2f,height-30,0xFFB8B8C8,1f,false); }
    @Override protected void onMouseClicked(int mx,int my,int button) { if(button!=0)return; if(my>=55&&my<78){presetsTab=mx>=width/2;return;} int i=(my-90)/32;String[] list=presetsTab?presets:types;if(i<0||i>=list.length)return;CMMElement e=create(presetsTab?(i==0?0:i==1?1:i==2?1:i==3?2:3):i);if(e!=null){config.addElement(e);CMMHelper.savePreset(config);MinecraftCompat.getMinecraft().displayGuiScreen(parent);} }
    private CMMElement create(int i){int x=width/2-100,y=height/2-10;Position p=Position.absolute(x,y);CMMElement element;switch(i){case 0:element=new GuiButton(p,200,20,"New Button","CMM Editor");break;case 1:element=new ActionButton(p,200,20,"Close",ActionButton.Action.CLOSE_MENU);break;case 2:element=new Text(p,true,"New Text",0xFFFFFFFF,1f);break;case 3:element=new Sprite(p,100,100,null,Resources.ASM_LOGO);break;case 4:element=new CMMDropdown(p,200,20,Arrays.<CMMDropdown.Item>asList(new CMMDropdown.NameItem("Option 1"),new CMMDropdown.NameItem("Option 2")));break;default:return null;} element.xPos=x; element.yPos=y; element.position=Position.absolute(x,y); return element;}
    @Override protected void onKeyTyped(char c,int key){if(key==Keyboard.KEY_ESCAPE)MinecraftCompat.getMinecraft().displayGuiScreen(parent);}
}
