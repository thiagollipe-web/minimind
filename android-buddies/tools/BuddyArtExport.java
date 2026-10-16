import com.brittech.minibuddy.BuddyArt;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Exports the actual Canvas drawing geometry to SVG previews and Android vector launcher icons. */
public final class BuddyArtExport implements BuddyArt.Painter {
    final StringBuilder svg=new StringBuilder(),vector=new StringBuilder();
    int depth;
    private static String f(float n){return String.format(Locale.ROOT,"%.3f",n);}
    private static String rgb(int c){return String.format(Locale.ROOT,"#%06X",c&0xffffff);}
    private static String argb(int c){return String.format(Locale.ROOT,"#%08X",c);}
    private void shape(String path,int color,boolean stroke,float width){
        String attr=stroke?"stroke":"fill";
        svg.append("<path d=\"").append(path).append("\" ").append(attr).append("=\"").append(rgb(color)).append("\" opacity=\"").append(f(((color>>>24)&255)/255f)).append("\"");
        if(stroke)svg.append(" fill=\"none\" stroke-width=\"").append(f(width)).append("\" stroke-linecap=\"round\"");svg.append("/>\n");
        vector.append("<path android:pathData=\"").append(path).append("\" ");
        if(stroke)vector.append("android:fillColor=\"#00000000\" android:strokeColor=\"").append(argb(color)).append("\" android:strokeWidth=\"").append(f(width)).append("\" android:strokeLineCap=\"round\"");
        else vector.append("android:fillColor=\"").append(argb(color)).append("\"");vector.append("/>\n");
    }
    public void ellipse(float x,float y,float rx,float ry,int color){
        shape("M"+f(x-rx)+","+f(y)+" A"+f(rx)+","+f(ry)+" 0 1,0 "+f(x+rx)+","+f(y)+" A"+f(rx)+","+f(ry)+" 0 1,0 "+f(x-rx)+","+f(y)+" Z",color,false,0);
    }
    public void rect(float x,float y,float w,float h,float radius,int color){
        float r=Math.min(radius,Math.min(w,h)/2);String path="M"+f(x+r)+","+f(y)+" H"+f(x+w-r)+" Q"+f(x+w)+","+f(y)+" "+f(x+w)+","+f(y+r)+" V"+f(y+h-r)+" Q"+f(x+w)+","+f(y+h)+" "+f(x+w-r)+","+f(y+h)+" H"+f(x+r)+" Q"+f(x)+","+f(y+h)+" "+f(x)+","+f(y+h-r)+" V"+f(y+r)+" Q"+f(x)+","+f(y)+" "+f(x+r)+","+f(y)+" Z";shape(path,color,false,0);
    }
    public void line(float x,float y,float xx,float yy,float width,int color){shape("M"+f(x)+","+f(y)+" L"+f(xx)+","+f(yy),color,true,width);}
    public void polygon(int color,float... xy){StringBuilder path=new StringBuilder();for(int i=0;i<xy.length;i+=2)path.append(i==0?"M":" L").append(f(xy[i])).append(',').append(f(xy[i+1]));shape(path+" Z",color,false,0);}
    public void push(float x,float y){depth++;svg.append("<g transform=\"translate(").append(f(x)).append(' ').append(f(y)).append(")\">\n");vector.append("<group android:translateX=\"").append(f(x)).append("\" android:translateY=\"").append(f(y)).append("\">\n");}
    public void pop(){depth--;if(depth<0)throw new IllegalStateException("Unbalanced transforms");svg.append("</g>\n");vector.append("</group>\n");}
    public static void main(String[] args)throws Exception{
        Path out=Paths.get(args[0]);Files.createDirectories(out);double t=args.length>1?Double.parseDouble(args[1]):.7;
        String[] names={"Mati","Pip","Lumi","Sol","Nino","Clio","Geo","Soci","Sofia","Volt","Atom","Bia"};
        String[] subjects={"Matemática","Inglês","Português","Espanhol","Ciências","História","Geografia","Sociologia","Filosofia","Física","Química","Biologia"};
        StringBuilder sheet=new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1200\" height=\"1050\" viewBox=\"0 0 1200 1050\"><rect width=\"1200\" height=\"1050\" fill=\"#11182A\"/><text x=\"44\" y=\"54\" font-family=\"sans-serif\" font-size=\"30\" fill=\"#FFFFFF\" font-weight=\"bold\">MiniBuddies • 12 companheiros diferentes</text><text x=\"44\" y=\"84\" font-family=\"sans-serif\" font-size=\"17\" fill=\"#BCC7DF\">Personagens vetoriais originais • formas, acessórios e movimentos próprios</text>");
        for(int i=0;i<BuddyArt.IDS.length;i++){
            String id=BuddyArt.IDS[i];BuddyArtExport art=new BuddyArtExport();BuddyArt.draw(art,id,t,false,false,0,1);if(art.depth!=0)throw new IllegalStateException();
            String svg="<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"160\" height=\"178\" viewBox=\"0 0 160 178\">"+art.svg+"</svg>";
            Files.write(out.resolve(id+".svg"),svg.getBytes(StandardCharsets.UTF_8));
            String vector="<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" android:width=\"48dp\" android:height=\"48dp\" android:viewportWidth=\"178\" android:viewportHeight=\"178\"><group android:translateX=\"9\">"+art.vector+"</group></vector>";
            Files.write(out.resolve(id+".xml"),vector.getBytes(StandardCharsets.UTF_8));
            float x=30+(i%4)*295,y=112+(i/4)*304;sheet.append("<g transform=\"translate(").append(x).append(' ').append(y).append(")\"><rect width=\"275\" height=\"286\" rx=\"22\" fill=\"#202B42\"/><g transform=\"translate(49 4) scale(1.1)\">").append(art.svg).append("</g><text x=\"137.5\" y=\"218\" text-anchor=\"middle\" font-family=\"sans-serif\" font-weight=\"bold\" font-size=\"22\" fill=\"#FFF9EB\">").append(names[i]).append("</text><text x=\"137.5\" y=\"246\" text-anchor=\"middle\" font-family=\"sans-serif\" font-size=\"17\" fill=\"#BBC9DD\">").append(subjects[i]).append("</text></g>");
        }
        sheet.append("</svg>");Files.write(out.resolve("familia.svg"),sheet.toString().getBytes(StandardCharsets.UTF_8));
    }
}
