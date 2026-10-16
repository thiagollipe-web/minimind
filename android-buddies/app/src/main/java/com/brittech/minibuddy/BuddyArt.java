package com.brittech.minibuddy;

/** Original, platform-independent vector characters. Android and previews use this same geometry. */
public final class BuddyArt {
    public static final String[] IDS={"matematica","ingles","portugues","espanhol","ciencias","historia","geografia","sociologia","filosofia","fisica","quimica","biologia"};
    public interface Painter {
        void ellipse(float x,float y,float rx,float ry,int color);
        void rect(float x,float y,float w,float h,float radius,int color);
        void line(float x1,float y1,float x2,float y2,float width,int color);
        void polygon(int color,float... points);
        void push(float x,float y);void pop();
    }
    private static final int INK=0xff26344c,WHITE=0xfffffbef,BLUSH=0xfff3a8b5,GOLD=0xffffd56c;
    private BuddyArt(){}
    public static int index(String id){for(int i=0;i<IDS.length;i++)if(IDS[i].equals(id))return i;return 0;}
    private static float wave(double t,double speed){return (float)Math.sin(t*speed);}
    private static void eye(Painter p,float x,float y,float radius,boolean happy,boolean closed,float direction){
        if(happy){p.line(x-radius,y+2,x,y-radius*.5f,3,INK);p.line(x,y-radius*.5f,x+radius,y+2,3,INK);}
        else if(closed)p.line(x-radius,y+2,x+radius,y+2,3,INK);
        else{p.ellipse(x+direction,y,radius,radius*1.25f,INK);p.ellipse(x+direction-2,y-3,2,2.5f,WHITE);}
    }
    private static void face(Painter p,float x,float y,float spacing,float radius,boolean happy,boolean closed,float direction){
        eye(p,x-spacing,y,radius,happy,closed,direction);eye(p,x+spacing,y,radius,happy,closed,direction);
        p.ellipse(x-spacing-9,y+13,6,3,BLUSH);p.ellipse(x+spacing+9,y+13,6,3,BLUSH);
        p.line(x-4,y+17,x,y+20,2,INK);p.line(x,y+20,x+4,y+17,2,INK);
    }
    private static void star(Painter p,float x,float y,float radius,int color){float[] xy=new float[20];for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float r=i%2==0?radius:radius*.43f;xy[2*i]=x+(float)Math.cos(a)*r;xy[2*i+1]=y+(float)Math.sin(a)*r;}p.polygon(color,xy);}
    public static void draw(Painter p,String id,double time,boolean happy,boolean resting,float direction,int level){
        int k=index(id);direction=Math.max(-3,Math.min(3,direction));
        boolean closed=resting||((int)(time*10)%53)<2;
        p.ellipse(80,148,43,5,0x24344361);
        float bob=resting?0:wave(time,1.3+k*.13)*(happy?3:1.4f);
        p.push(0,bob);
        double motion=resting?0:time;
        switch(k){
            case 0: geometry(p,motion,happy,closed,direction);break;
            case 1: owl(p,motion,happy,closed,direction);break;
            case 2: book(p,motion,happy,closed,direction);break;
            case 3: sun(p,motion,happy,closed,direction);break;
            case 4: axolotl(p,motion,happy,closed,direction);break;
            case 5: turtle(p,motion,happy,closed,direction);break;
            case 6: globe(p,motion,happy,closed,direction);break;
            case 7: friends(p,motion,happy,closed,direction);break;
            case 8: moth(p,motion,happy,closed,direction);break;
            case 9: rocket(p,motion,happy,closed,direction);break;
            case 10: flask(p,motion,happy,closed,direction);break;
            default: sprout(p,motion,happy,closed,direction);
        }
        if(level>=2)star(p,124,126,8,GOLD);
        if(level>=3){p.polygon(GOLD,63,22,59,8,73,15,80,3,87,15,101,8,97,22);p.rect(63,20,34,5,2,0xffd99b44);}
        if(happy){star(p,24,27-wave(time,4)*4,6,BLUSH);star(p,139,37+wave(time,4)*4,7,BLUSH);}
        p.pop();
    }
    // Mati: angular cube automaton, ruler ears and a moving triangle antenna.
    private static void geometry(Painter p,double t,boolean h,boolean c,float d){
        p.rect(42,130,23,16,4,0xff489f9d);p.rect(96,130,23,16,4,0xff489f9d);
        p.rect(18,66,17,47,3,GOLD);p.rect(127,66,17,47,3,GOLD);
        for(int y=73;y<110;y+=9){p.line(19,y,26,y,2,INK);p.line(136,y,143,y,2,INK);}
        p.polygon(0xff3b8c92,31,49,46,34,129,34,141,49,141,125,127,138,31,138);
        p.rect(30,47,99,85,13,0xff8adbd0);p.rect(40,57,79,58,8,WHITE);
        p.line(80,34,80,22,4,INK);float x=wave(t,2)*4;p.polygon(GOLD,80+x,6,69+x,23,91+x,23);
        face(p,80,78,21,6,h,c,d);p.line(65,123,75,123,2,INK);p.line(85,120,85,127,2,INK);p.line(81,124,89,124,2,INK);
    }
    // Pip: round owl, headphones, feather wings and an open pocket dictionary.
    private static void owl(Painter p,double t,boolean h,boolean c,float d){
        float flap=wave(t,3)*5;p.ellipse(33,101+flap,13,27,0xff74a7d3);p.ellipse(127,101-flap,13,27,0xff74a7d3);
        p.ellipse(80,94,46,49,0xff9cc9ef);p.polygon(0xff9cc9ef,39,60,42,26,68,49);p.polygon(0xff9cc9ef,92,49,118,26,121,60);
        p.ellipse(59,76,23,29,WHITE);p.ellipse(101,76,23,29,WHITE);
        p.line(32,69,32,47,8,INK);p.line(32,47,52,29,8,INK);p.line(52,29,108,29,8,INK);p.line(108,29,128,47,8,INK);p.line(128,47,128,69,8,INK);
        p.rect(22,58,15,30,7,GOLD);p.rect(123,58,15,30,7,GOLD);
        eye(p,59,76,7,h,c,d);eye(p,101,76,7,h,c,d);p.polygon(0xfff5b65a,73,91,87,91,80,101);
        p.polygon(0xff4c79a6,50,111,79,116,80,141,50,135);p.polygon(WHITE,80,116,110,111,110,135,80,141);p.line(80,116,80,141,2,INK);
        p.ellipse(59,144,12,4,0xfff5b65a);p.ellipse(101,144,12,4,0xfff5b65a);
    }
    // Lumi: living book with a visible page block, ribbon and a feather pen.
    private static void book(Painter p,double t,boolean h,boolean c,float d){
        p.rect(39,132,19,14,5,0xff8f6aa5);p.rect(99,132,19,14,5,0xff8f6aa5);
        p.line(31,96,19,110+wave(t,2)*4,7,0xffb898d0);p.line(125,95,139,83,7,0xffb898d0);
        p.rect(37,32,90,102,8,0xff8f6aa5);p.rect(30,28,90,102,8,0xffd8bde8);p.rect(43,34,70,88,3,WHITE);p.rect(30,28,13,102,4,0xffa27cba);
        for(int y=43;y<61;y+=7)p.line(51,y,104,y,2,0xffd8c9dd);
        face(p,79,78,18,6,h,c,d);p.polygon(0xffe58fae,87,114,101,114,101,140,94,134,87,140);
        p.line(137,87,145,45,3,INK);p.polygon(0xff8bd3bc,142,70,134,45,143,20,154,41,149,62);p.line(141,65,145,32,2,0xff4c9d87);
        p.line(49,116,78,116,2,0xffbfa8cf);
    }
    // Sol: sun-shaped companion with animated rays, expressive cheeks and a scarf.
    private static void sun(Painter p,double t,boolean h,boolean c,float d){
        for(int i=0;i<12;i++){double a=i*Math.PI/6;float r=53+wave(t+i,.9)*3;float x=80+(float)Math.cos(a)*r,y=76+(float)Math.sin(a)*r;p.line(80+(float)Math.cos(a)*44,76+(float)Math.sin(a)*44,x,y,6,0xffefab4d);}
        p.ellipse(80,76,44,44,0xffffd67d);p.ellipse(67,61,22,20,0xffffe9b7);face(p,80,75,18,6,h,c,d);
        p.rect(45,113,70,16,7,0xffe87d86);p.polygon(0xffcf5e75,92,121,111,124,122,144,104,140);p.rect(55,128,12,17,5,0xffe7a44b);p.rect(94,128,12,17,5,0xffe7a44b);
        p.rect(16,33,5,13,2,0xffe87d86);p.ellipse(18.5f,53,2.5f,2.5f,0xffe87d86);
    }
    // Nino: axolotl scientist with external gills, laboratory goggles and a vial.
    private static void axolotl(Painter p,double t,boolean h,boolean c,float d){
        p.ellipse(80,115,33,27,0xffa8d8ba);p.ellipse(57,141,14,5,0xff6ba38b);p.ellipse(103,141,14,5,0xff6ba38b);
        for(int i=0;i<3;i++){float y=53+i*17;p.line(37,y+8,19,y-4+wave(t+i,2)*3,7,0xffedb7be);p.line(123,y+8,141,y-4-wave(t+i,2)*3,7,0xffedb7be);}
        p.rect(32,43,96,65,27,0xffb8e7c9);p.rect(45,61,30,27,10,0xffedfaff);p.rect(85,61,30,27,10,0xffedfaff);p.line(75,72,85,72,4,INK);p.line(34,70,45,70,4,INK);p.line(115,70,126,70,4,INK);
        eye(p,60,74,5,h,c,d);eye(p,100,74,5,h,c,d);p.line(75,97,85,97,2,INK);
        p.rect(62,112,36,21,4,WHITE);p.line(80,113,80,133,2,0xff7db59a);p.ellipse(89,120,3,3,GOLD);
        p.rect(125,113,13,24,5,0xffd6f2f4);p.rect(126,123,11,12,4,0xff8cb7e6);p.line(124,111,140,111,3,INK);
    }
    // Clio: low turtle silhouette carrying a terracotta archive shell and a scroll.
    private static void turtle(Painter p,double t,boolean h,boolean c,float d){
        p.ellipse(75,107,49,31,0xffb88467);p.polygon(0xffd9a783,40,108,57,80,88,80,110,107,91,128,60,128);p.line(57,80,60,128,3,0xff94684f);p.line(88,80,91,128,3,0xff94684f);
        p.ellipse(44,139,16,7,0xff92b9a0);p.ellipse(102,139,16,7,0xff92b9a0);p.ellipse(80,63+wave(t,1)*1.5f,34,31,0xffc1debc);
        face(p,80,63,14,5,h,c,d);p.rect(48,30,64,10,4,0xffa57c5b);p.rect(62,15,35,20,6,0xffc69b72);
        p.rect(15,94,15,39,5,0xfff7dfad);p.ellipse(22,94,10,5,0xffe2bf85);p.line(18,104,25,104,2,0xffae8b59);p.line(18,112,25,112,2,0xffae8b59);
        p.line(73,103,87,103,3,GOLD);p.line(73,119,87,119,3,GOLD);p.line(74,104,86,118,2,GOLD);p.line(86,104,74,118,2,GOLD);
    }
    // Geo: globe explorer, continent patches, latitude lines and a compass satchel.
    private static void globe(Painter p,double t,boolean h,boolean c,float d){
        p.rect(53,130,16,15,5,0xff4d8f9b);p.rect(92,130,16,15,5,0xff4d8f9b);p.line(30,87,17,104+wave(t,2)*3,7,0xff83c8e4);p.line(130,87,144,101,7,0xff83c8e4);
        p.ellipse(80,82,49,51,0xff83c8e4);p.polygon(0xff8bbf8b,40,53,65,39,70,56,58,67,63,82,44,80);p.polygon(0xff8bbf8b,112,50,124,66,116,77,126,92,114,111,99,102,104,83,96,70);p.polygon(0xff8bbf8b,53,112,65,105,75,117,69,129,58,125);
        p.line(37,57,123,57,1,0xffc8e5e5);p.line(34,107,127,107,1,0xffc8e5e5);
        p.rect(30,32,99,12,6,0xffd8be82);p.rect(48,14,64,26,10,0xffead69f);p.rect(48,30,64,6,2,0xff9a8662);
        face(p,80,78,18,6,h,c,d);p.line(115,114,136,128,4,0xffa48563);p.ellipse(132,132,13,13,GOLD);p.polygon(INK,132,121,128,136,138,132);
    }
    // Soci: central companion joined to two tiny friends; connections move gently.
    private static void friends(Painter p,double t,boolean h,boolean c,float d){
        float a=wave(t,2)*5;p.line(48,92,22,58+a,4,0xffbd7f9d);p.line(112,92,138,58-a,4,0xffbd7f9d);
        p.ellipse(22,52+a,16,17,0xffedb3ca);p.ellipse(138,52-a,16,17,0xffb7cdec);eye(p,18,51+a,2.5f,h,c,0);eye(p,27,51+a,2.5f,h,c,0);eye(p,133,51-a,2.5f,h,c,0);eye(p,142,51-a,2.5f,h,c,0);
        p.polygon(0xffbf7d9d,47,94,37,129,55,142,107,142,124,128,113,94);p.rect(49,48,62,62,23,0xffedb3ca);p.ellipse(67,41,13,14,0xffedb3ca);p.ellipse(92,40,13,14,0xffedb3ca);face(p,80,76,15,5,h,c,d);
        p.ellipse(58,140,14,6,0xffa86688);p.ellipse(104,140,14,6,0xffa86688);p.ellipse(80,122,9,9,WHITE);p.ellipse(67,124,7,7,WHITE);p.ellipse(93,124,7,7,WHITE);
    }
    // Sofia: moth with large wings, thoughtful hood and a gently orbiting light.
    private static void moth(Painter p,double t,boolean h,boolean c,float d){
        float flap=wave(t,2)*4;p.ellipse(38-flap,88,24,38,0xffbaa8db);p.ellipse(122+flap,88,24,38,0xffbaa8db);p.ellipse(42-flap,111,20,24,0xff8f83be);p.ellipse(118+flap,111,20,24,0xff8f83be);
        star(p,36-flap,77,8,GOLD);star(p,124+flap,77,8,GOLD);p.ellipse(80,103,22,39,0xff756b9e);
        p.polygon(0xffa997cf,47,78,53,46,79,21,106,46,114,78,102,108,58,108);p.ellipse(80,74,27,29,WHITE);face(p,80,73,12,5,h,c,d);
        p.line(69,39,65,25,3,0xff8e7daf);p.line(91,39,95,25,3,0xff8e7daf);p.ellipse(64,22,4,4,GOLD);p.ellipse(96,22,4,4,GOLD);
        p.ellipse(80,123,7,7,GOLD);p.ellipse(133+wave(t,1.5)*4,27+wave(t,2)*4,7,7,0xfff5e6a4);
    }
    // Volt: pointed rocket, fins, electric badge and pulsing exhaust.
    private static void rocket(Painter p,double t,boolean h,boolean c,float d){
        float flame=4+Math.abs(wave(t,6))*10;p.polygon(0xffffba58,67,130,80,137+flame,93,130);p.polygon(0xffffe6a5,73,130,80,143,87,130);
        p.polygon(0xff5582a8,51,83,29,113,28,139,56,124);p.polygon(0xff5582a8,109,83,131,113,132,139,104,124);
        p.rect(50,51,60,82,20,0xffb6d5df);p.polygon(0xffe98188,50,56,60,32,80,8,100,32,110,56);p.rect(57,126,46,10,4,0xff476b8b);
        p.ellipse(80,78,25,25,0xff527d9e);p.ellipse(80,78,20,20,WHITE);eye(p,72,76,4,h,c,d*.5f);eye(p,88,76,4,h,c,d*.5f);p.line(77,88,83,88,2,INK);
        p.polygon(GOLD,81,105,69,120,79,120,75,130,92,113,82,113);p.ellipse(29+wave(t,2)*3,39,4,4,0xffa4c6e6);p.line(15,70,25,70,2,0xffa4c6e6);
    }
    // Atom: glass flask, liquid body, cork and independent rising bubbles.
    private static void flask(Painter p,double t,boolean h,boolean c,float d){
        p.rect(51,132,19,14,5,0xffbf8858);p.rect(91,132,19,14,5,0xffbf8858);p.line(37,106,21,112,7,0xffaad5dc);p.line(123,106,139,99,7,0xffaad5dc);
        p.polygon(0xffbbdce1,61,30,99,30,99,62,126,117,123,132,37,132,34,117,61,62);p.polygon(0xfff0b66f,50,91,110,91,123,118,119,128,41,128,37,118);p.rect(56,24,48,12,4,0xffd7eff0);p.rect(63,14,34,12,4,0xffb68b63);
        p.line(47,107,50,99,3,WHITE);face(p,80,101,17,5,h,c,d);
        for(int i=0;i<3;i++){float y=78-(float)((t*11+i*17)%38);p.ellipse(73+i*8,y,3+i%2,3+i%2,0xffe5f8ed);}
        p.ellipse(121,42-wave(t,2)*4,6,6,0xff9cd7d7);p.ellipse(133,24-wave(t,1.5)*3,4,4,0xffb7e4dc);
    }
    // Bia: seedling with leaf ears, stem, tiny root feet and a ladybird friend.
    private static void sprout(Painter p,double t,boolean h,boolean c,float d){
        float sway=wave(t,2)*4;p.line(80,53,80+sway,26,5,0xff669456);p.polygon(0xff8dbb70,80+sway,30,56+sway,15,43+sway,24,63+sway,39);p.polygon(0xffb0d98a,80+sway,25,96+sway,9,118+sway,12,106+sway,31);
        p.ellipse(80,106,34,37,0xff9cc47b);p.polygon(0xff709f62,44,84,19,66,22,96,49,107);p.polygon(0xff709f62,116,84,141,66,138,96,111,107);
        p.ellipse(80,79,37,33,0xffc5dda3);face(p,80,77,16,6,h,c,d);p.line(62,136,51,147,5,0xff76965e);p.line(98,136,109,147,5,0xff76965e);p.line(80,137,80,148,4,0xff76965e);
        p.ellipse(116,120,8,7,0xffe58b85);p.line(116,114,116,126,2,INK);p.ellipse(112,119,1.5f,1.5f,INK);p.ellipse(120,122,1.5f,1.5f,INK);p.ellipse(116,113,3,3,INK);
        p.line(80,114,80,131,2,0xff6b9459);p.line(80,121,73,116,2,0xff6b9459);p.line(80,126,87,121,2,0xff6b9459);
    }
}
