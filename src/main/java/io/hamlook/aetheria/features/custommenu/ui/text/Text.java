package io.hamlook.aetheria.features.custommenu.ui.text;

import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.utils.render.TextRenderUtils;

public class Text extends CMMElement {

    public boolean centered;
    public String text;
    public float scale;
    public int color;

    public Text(Position pos,boolean centered,String text,int color){
        this(pos,centered,text,color,1f);
    }
    public Text(Position pos,boolean centered,String text,int color,float scale){
        super(pos,-1,-1);
        this.centered = centered;
        this.text = text;
        this.color = color;
        this.scale = scale;
    }

    @Override
    public void draw(int mouseX,int mouseY,float partialTicks) {
        if(centered){
            TextRenderUtils.drawCenteredStringScaleAware(text,position.getX(),position.getY(),color,scale,false);
        }else{
            TextRenderUtils.drawStringScaleAware(text,position.getX(),position.getY(),color,scale,false);
        }
    }
}
