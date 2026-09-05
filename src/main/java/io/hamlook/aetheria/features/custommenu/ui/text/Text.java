package io.hamlook.aetheria.features.custommenu.ui.text;

import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.placeholders.PlaceholderManager;
import io.hamlook.aetheria.utils.render.MiniMessageDetector;
import io.hamlook.aetheria.utils.render.ResolutionUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import net.minecraft.client.gui.FontRenderer;

import java.util.List;

public class Text extends CMMElement {

    public boolean centered;
    public String text;
    public float scale;
    public int color;
    public boolean placeholders = true;

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
        String toDraw = placeholders ? PlaceholderManager.replace(this.text) : this.text;
        List<MiniMessageDetector.Segment> segments = MiniMessageDetector.parse(toDraw);

        FontRenderer fr = MinecraftCompat.getFontRenderer();
        boolean displayScale = false;
        float scaleDisplay = displayScale ? ResolutionUtils.getXStatic(1) : 1f;
        float finalScale = Math.max(0.25f, this.scale * scaleDisplay);

        // total unscaled width
        int totalWidth = 0;
        for (MiniMessageDetector.Segment seg : segments) {
            totalWidth += fr.getStringWidth(seg.text);
        }

        float x = centered ? (position.getX() - totalWidth * finalScale / 2f) : position.getX();
        float y = position.getY();

        for (MiniMessageDetector.Segment seg : segments) {
            if (seg.gradientStart != -1 && seg.gradientEnd != -1) {
                TextRenderUtils.drawStringGradientScaleAware(seg.text, x, y,
                        seg.gradientStart, seg.gradientEnd, this.scale, displayScale);
            } else {
                int col;
                if (seg.color != -1) {
                    col = seg.color;
                } else if (seg.text.contains("§")) {
                    col = -1; // let Minecraft handle § codes
                } else {
                    col = this.color;
                }
                TextRenderUtils.drawStringScaleAware(seg.text, x, y, col, this.scale, displayScale);
            }
            x += fr.getStringWidth(seg.text) * finalScale;
        }
    }

    @Override
    public int[] getCorners() {
        FontRenderer fr = MinecraftCompat.getFontRenderer();
        int[] corners =  new int[4];
        int width = fr.getStringWidth(placeholders ? PlaceholderManager.replace(this.text) : this.text);
        int height = (int)(fr.FONT_HEIGHT * scale);
        corners[0] = centered ? (this.xPos - width/2) : this.xPos;
        corners[1] = centered ? (this.yPos - height/2) : this.yPos;
        corners[2] = centered ? (this.xPos + width/2) : this.xPos + width;
        corners[3] = centered ? (this.yPos + height/2) : this.yPos + height;
        return corners;
    }
}
