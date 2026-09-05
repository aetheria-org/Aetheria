package io.hamlook.aetheria.utils.render;

import io.hamlook.aetheria.core.moulconfig.editors.ChromaColour;
import java.util.ArrayList;
import java.util.List;

/** Defensive parser for CMM markup; malformed tags stay literal instead of crashing rendering. */
public final class MiniMessageDetector {
    private MiniMessageDetector() { }
    public static class Segment {
        public final String text; public final int color, gradientStart, gradientEnd; public final String chromaStyle;
        public Segment(String text,int color){this.text=text;this.color=color;gradientStart=-1;gradientEnd=-1;chromaStyle=null;}
        public Segment(String text,int start,int end){this.text=text;color=-1;gradientStart=start;gradientEnd=end;chromaStyle=null;}
        public Segment(String text,String chroma){this.text=text;color=-1;gradientStart=-1;gradientEnd=-1;chromaStyle=chroma;}
    }
    public static List<Segment> parse(String input){
        List<Segment> out=new ArrayList<>(); if(input==null||input.isEmpty())return out;
        StringBuilder b=new StringBuilder(); int color=-1,gradient=-1; boolean inGradient=false; String chroma=null; boolean inChroma=false;
        int i=0; while(i<input.length()){
            if(input.startsWith("<color:",i)){int e=input.indexOf('>',i);if(e<0){b.append(input.charAt(i++));continue;}flush(out,b,color,gradient,inGradient,chroma,inChroma);color=parseHex(input.substring(i+7,e));gradient=-1;inGradient=false;chroma=null;inChroma=false;i=e+1;continue;}
            if(input.startsWith("</color",i)){int e=input.indexOf('>',i);if(e<0){b.append(input.charAt(i++));continue;}flush(out,b,color,gradient,inGradient,chroma,inChroma);color=-1;i=e+1;continue;}
            if(input.startsWith("<gradient:",i)){int e=input.indexOf('>',i);if(e<0){b.append(input.charAt(i++));continue;}flush(out,b,color,gradient,inGradient,chroma,inChroma);gradient=parseHex(input.substring(i+10,e));color=-1;inGradient=true;chroma=null;inChroma=false;i=e+1;continue;}
            if(input.startsWith("</gradient:",i)){int e=input.indexOf('>',i);if(e<0){b.append(input.charAt(i++));continue;}int end=parseHex(input.substring(i+11,e));if(inGradient&&b.length()>0)out.add(new Segment(b.toString(),gradient,end));b.setLength(0);gradient=-1;inGradient=false;i=e+1;continue;}
            if(input.startsWith("<chroma:",i)){int e=input.indexOf('>',i);if(e<0){b.append(input.charAt(i++));continue;}String style=createChromaStyle(input.substring(i+8,e));if(style==null){b.append(input,i,e+1);i=e+1;continue;}flush(out,b,color,gradient,inGradient,chroma,inChroma);chroma=style;inChroma=true;color=-1;gradient=-1;inGradient=false;i=e+1;continue;}
            if(input.startsWith("</chroma",i)){int e=input.indexOf('>',i);if(e<0){b.append(input.charAt(i++));continue;}if(inChroma&&b.length()>0)out.add(new Segment(b.toString(),chroma));b.setLength(0);chroma=null;inChroma=false;i=e+1;continue;}
            b.append(input.charAt(i++));
        }
        flush(out,b,color,gradient,inGradient,chroma,inChroma);return out;
    }
    private static void flush(List<Segment> out,StringBuilder b,int color,int gradient,boolean inGradient,String chroma,boolean inChroma){if(b.length()==0)return;if(inChroma&&chroma!=null)out.add(new Segment(b.toString(),chroma));else if(inGradient&&gradient!=-1)out.add(new Segment(b.toString(),gradient,gradient));else out.add(new Segment(b.toString(),color));b.setLength(0);}
    private static String createChromaStyle(String value){try{String[] p=value.split(":");float seconds=Math.max(1f,Math.min(60f,Float.parseFloat(p[0])));int speed=Math.max(1,Math.min(255,Math.round(255f-((seconds-1f)/59f)*254f)));int rgb=p.length>1&&!p[1].isEmpty()?parseHex(p[1])&0xFFFFFF:0xFFFFFF;return ChromaColour.special(speed,255,rgb);}catch(Exception ignored){return null;}}
    private static int parseHex(String value){try{String h=value.replace("#","");if(h.length()==6)h="FF"+h;return (int)Long.parseLong(h,16);}catch(Exception ignored){return -1;}}
}
