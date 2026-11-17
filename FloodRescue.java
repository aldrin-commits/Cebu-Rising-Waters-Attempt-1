import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

/** Cebu: Rising Waters — a small, dependency-free Java Swing rescue game. */
public class FloodRescue {
    static final int W=1000,H=680, MAP_X=40,MAP_Y=95,MAP_W=920,MAP_H=520;
    static final Color INK=new Color(24,42,55), NAVY=new Color(19,43,61), WATER=new Color(175,218,235), DEEP=new Color(107,174,204), PALE=new Color(239,247,248), RED=new Color(238,100,94), GREEN=new Color(72,174,130), GOLD=new Color(246,190,85);
    enum Screen { MENU, PROLOGUE, PLAY, SHOP, END, GAME_OVER }
    static class Rect { double x,y,w,h; Rect(double x,double y,double w,double h){this.x=x;this.y=y;this.w=w;this.h=h;} Rectangle2D shape(){return new Rectangle2D.Double(x,y,w,h);} }
    static class Level {
        String name,place,brief; int people,goal; boolean landslide,flash; ArrayList<Rect> walls=new ArrayList<>(); ArrayList<Point2D.Double> peoplePts=new ArrayList<>();
        Level(String n,String p,String b,int persons,int g,boolean l,boolean f){name=n;place=p;brief=b;people=persons;goal=g;landslide=l;flash=f;}
    }
    static class Game extends JPanel implements ActionListener,KeyListener {
        Screen screen=Screen.MENU; final javax.swing.Timer timer; final Random rng=new Random();
        Level[] levels; int level=0, coins=0, rescuedTotal=0; double bx=90,by=545,vx=0,vy=0; int health=3,capacity=3,aboard=0,speedLevel=0; boolean boost=false,cleaner=false,repair=false,paused=false;
        long boostUntil=0,hitInvulnerableUntil=0,effectUntil=0; double effectX,effectY; boolean[] rescued; boolean[] warnings; String toast=""; long toastUntil=0; int tick=0,tutorialStep=0; boolean inSafeZone=false;
        Game(){setPreferredSize(new Dimension(W,H));setFocusable(true);addKeyListener(this);levels=makeLevels();timer=new javax.swing.Timer(16,this);timer.start();}
        Level[] makeLevels(){
            Level a=new Level("The First Warning","Talamban, Cebu City","Rainfall and early flooding",7,5,false,false);
            a.walls.add(new Rect(455,MAP_Y+MAP_H-145,55,145));
            Level b=new Level("When the Roads Disappear","Banilad / Colon","Heavy rainfall and flooded roads",8,6,true,false);
            b.walls.addAll(Arrays.asList(new Rect(230,140,38,265),new Rect(390,275,250,32),new Rect(600,  170,40,200),new Rect(730,380,190,34),new Rect(350,440,250,34),new Rect(790,140,36,155)));
            Level c=new Level("Rising Pressure","A.S. Fortuna","Severe flooding and limited resources",10,7,true,true);
            c.walls.addAll(Arrays.asList(new Rect(200,155,260,32),new Rect(490,155,35,170),new Rect(580,275,300,32),new Rect(330,365,35,185),new Rect(420,470,230,32),new Rect(740,380,35,160)));
            Level d=new Level("The Storm","Guadalupe","Typhoon, flooding, landslides, multiple emergencies",12,9,true,true);
            d.walls.addAll(Arrays.asList(new Rect(170,150,300,32),new Rect(500,150,36,220),new Rect(600,265,300,32),new Rect(230,330,220,32),new Rect(600,415,280,32),new Rect(410,445,35,115),new Rect(760,145,35,85)));
            return new Level[]{a,b,c,d};
        }
        void beginLevel(){Level l=levels[level];bx=MAP_X+55;by=MAP_Y+MAP_H-55;vx=vy=0;aboard=0;boost=false;cleaner=false;repair=false;paused=false;tutorialStep=0;inSafeZone=false;hitInvulnerableUntil=0;effectUntil=0;rescued=new boolean[l.people];warnings=new boolean[l.people];l.peoplePts.clear();
            if(level==0){
                l.peoplePts.add(new Point2D.Double(MAP_X+115,MAP_Y+MAP_H-90));
                l.peoplePts.add(new Point2D.Double(MAP_X+600,MAP_Y+MAP_H-125));
                l.peoplePts.add(new Point2D.Double(MAP_X+665,MAP_Y+MAP_H-165));
                l.peoplePts.add(new Point2D.Double(MAP_X+720,MAP_Y+MAP_H-115));
                l.peoplePts.add(new Point2D.Double(MAP_X+770,MAP_Y+MAP_H-170));
                l.peoplePts.add(new Point2D.Double(MAP_X+815,MAP_Y+MAP_H-115));
                l.peoplePts.add(new Point2D.Double(MAP_X+850,MAP_Y+MAP_H-185));
            } else for(int i=0;i<l.people;i++){double px=MAP_X+130+(i*173%(MAP_W-220)),py=MAP_Y+65+(i*113%(MAP_H-150)); if(i==l.people-1){px=MAP_X+MAP_W-60;py=MAP_Y+MAP_H-75;} l.peoplePts.add(new Point2D.Double(px,py));}
            l.peoplePts=new ArrayList<>(l.peoplePts.subList(0,l.people));screen=Screen.PLAY;toast("Reach the green safe zone. Rescue at least "+l.goal+" people.");
        }
        void toast(String s){toast=s;toastUntil=System.currentTimeMillis()+2400;}
        public void actionPerformed(ActionEvent e){tick++;if(screen==Screen.PLAY&&!paused)updateGame();repaint();}
        void updateGame(){long now=System.currentTimeMillis(); double accel=boost&&now<boostUntil?.24:.18, max=(boost&&now<boostUntil?4.6:3.15)+speedLevel*.35;
            if(boost&&now>=boostUntil)boost=false;
            vx*=.90;vy*=.90;
            if(keys.contains(KeyEvent.VK_LEFT)||keys.contains(KeyEvent.VK_A))vx-=accel;if(keys.contains(KeyEvent.VK_RIGHT)||keys.contains(KeyEvent.VK_D))vx+=accel;
            if(keys.contains(KeyEvent.VK_UP)||keys.contains(KeyEvent.VK_W))vy-=accel;if(keys.contains(KeyEvent.VK_DOWN)||keys.contains(KeyEvent.VK_S))vy+=accel;
            // Currents exist only inside arrow-marked horizontal water lanes.
            // Outside those bands, movement is entirely controlled by the player.
            double current=levels[level].flash?.11:.065; if(cleaner)current*=.38; if(levels[level].landslide&&bx>MAP_X+MAP_W*.62)current*=1.3;
            for(int lane=0;lane<3;lane++){
                if(level==0&&lane!=1)continue;
                double laneTop=MAP_Y+105+lane*145;
                if(by>=laneTop&&by<=laneTop+72){vx+=(level==0||lane%2==0?1:-1)*current;break;}
            }
            double sp=Math.hypot(vx,vy);if(sp>max){vx=vx/sp*max;vy=vy/sp*max;}
            double oldBx=bx,oldBy=by;double nx=bx+vx,ny=by+vy; if(nx<MAP_X+18||nx>MAP_X+MAP_W-18){vx*=-.35;nx=bx;} if(ny<MAP_Y+18||ny>MAP_Y+MAP_H-18){vy*=-.35;ny=by;}
            bx=nx;by=ny;
            Rectangle2D player=new Rectangle2D.Double(bx-12,by-12,24,24);
            for(Rect r:levels[level].walls)if(player.intersects(r.shape())){bx=oldBx;by=oldBy;vx*=-.35;vy*=-.35;if(now>hitInvulnerableUntil){health--;hitInvulnerableUntil=now+1000;toast("Ouch! Watch out for obstacles.");if(health<=0)screen=Screen.GAME_OVER;}break;}
            for(int i=0;i<levels[level].people;i++)if(!rescued[i]){Point2D p=levels[level].peoplePts.get(i);if(p.distance(bx,by)<29&&aboard<capacity){rescued[i]=true;aboard++;effectX=p.getX();effectY=p.getY();effectUntil=now+500;toast("Survivor rescued!  "+aboard+" / "+capacity+" aboard");}}
            if(level==0){double laneTop=MAP_Y+250;if(tutorialStep==0&&rescuedCount()>0)tutorialStep=1;else if(tutorialStep==1&&by>=laneTop&&by<=laneTop+72)tutorialStep=2;else if(tutorialStep==2&&bx>MAP_X+280&&(by<laneTop||by>laneTop+72))tutorialStep=3;else if(tutorialStep==3&&bx>MAP_X+520)tutorialStep=4;else if(tutorialStep==4&&rescuedCount()>=levels[0].goal)tutorialStep=5;}
            boolean atSafeZone=bx>MAP_X+MAP_W-60&&by>MAP_Y+MAP_H-115;
            if(atSafeZone&&!inSafeZone){if(aboard>0){rescuedTotal+=aboard;coins+=aboard*12;aboard=0;effectX=bx;effectY=by;effectUntil=now+700;toast("Delivered safely!  Supplies earned.");} if(rescuedCount()>=levels[level].goal){screen=Screen.SHOP;toast("Mission complete!");}else toast("Safe zone reached. Rescue "+(levels[level].goal-rescuedCount())+" more.");}
            inSafeZone=atSafeZone;
            if(levels[level].landslide&&tick%850==0){toast("LANDSLIDE WARNING — keep clear of the marked area!");warnings[rng.nextInt(warnings.length)]=true;}
        }
        int rescuedCount(){int n=0;for(boolean v:rescued)if(v)n++;return n;}
        Point2D tutorialTarget(){
            switch(tutorialStep){
                case 0:return levels[0].peoplePts.get(0);
                case 1:return new Point2D.Double(MAP_X+430,MAP_Y+285);
                case 2:return new Point2D.Double(MAP_X+570,MAP_Y+285);
                case 3:return new Point2D.Double(MAP_X+535,MAP_Y+355);
                case 4:return new Point2D.Double(MAP_X+700,MAP_Y+MAP_H-145);
                default:return new Point2D.Double(MAP_X+MAP_W-55,MAP_Y+MAP_H-55);
            }
        }
        static Set<Integer> keys=new HashSet<>();
        public void keyPressed(KeyEvent e){keys.add(e.getKeyCode());if(e.getKeyCode()==KeyEvent.VK_ESCAPE){if(screen==Screen.PLAY)paused=!paused;else if(screen==Screen.MENU){}else screen=Screen.MENU;}
            if(screen==Screen.MENU&&(e.getKeyCode()==KeyEvent.VK_ENTER||e.getKeyCode()==KeyEvent.VK_SPACE))screen=Screen.PROLOGUE;
            else if(screen==Screen.PROLOGUE&&(e.getKeyCode()==KeyEvent.VK_ENTER||e.getKeyCode()==KeyEvent.VK_SPACE))beginLevel();
            else if(screen==Screen.PLAY){if(e.getKeyCode()==KeyEvent.VK_SHIFT&&!boost){boost=true;boostUntil=System.currentTimeMillis()+3500;} if(e.getKeyCode()==KeyEvent.VK_G&&level>=0&&!cleaner){cleaner=true;toast("Garbage cleared: currents ease for this mission.");}if(e.getKeyCode()==KeyEvent.VK_R&&!repair&&aboard==0){repair=true;health=Math.min(3,health+1);toast("Health restored.");}}
            else if(screen==Screen.SHOP){if(e.getKeyCode()==KeyEvent.VK_1&&coins>=35){coins-=35;health=3;toast("Health restored.");}if(e.getKeyCode()==KeyEvent.VK_2&&coins>=45){coins-=45;capacity++;toast("Rescue capacity upgraded.");}if(e.getKeyCode()==KeyEvent.VK_3&&coins>=40){coins-=40;speedLevel++;toast("Movement speed upgraded.");}if(e.getKeyCode()==KeyEvent.VK_ENTER){if(level==levels.length-1)screen=Screen.END;else{level++;beginLevel();}}}
            else if((screen==Screen.END||screen==Screen.GAME_OVER)&&(e.getKeyCode()==KeyEvent.VK_ENTER||e.getKeyCode()==KeyEvent.VK_SPACE)){screen=Screen.MENU;level=0;health=3;capacity=3;coins=0;rescuedTotal=0;}
        }
        public void keyReleased(KeyEvent e){keys.remove(e.getKeyCode());}public void keyTyped(KeyEvent e){}
        protected void paintComponent(Graphics gg){super.paintComponent(gg);Graphics2D g=(Graphics2D)gg.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(PALE);g.fillRect(0,0,W,H);
            if(screen==Screen.MENU)drawMenu(g);else if(screen==Screen.PROLOGUE)drawPrologue(g);else if(screen==Screen.PLAY)drawPlay(g);else if(screen==Screen.SHOP)drawShop(g);else drawEnd(g);g.dispose();}
        void text(Graphics2D g,String s,int x,int y,int size,Color c){g.setFont(new Font("SansSerif",Font.PLAIN,size));g.setColor(c);g.drawString(s,x,y);} void bold(Graphics2D g,String s,int x,int y,int size,Color c){g.setFont(new Font("SansSerif",Font.BOLD,size));g.setColor(c);g.drawString(s,x,y);}
        void drawMenu(Graphics2D g){g.setColor(new Color(203,233,242));g.fillRect(0,340,W,340);g.setColor(new Color(153,208,225));for(int i=0;i<5;i++)g.fill(new Ellipse2D.Double(i*250-70,395+(i%2)*20,330,95));bold(g,"CEBU: RISING WATERS",95,165,48,NAVY);text(g,"A neighborhood rescue campaign",101,212,25,INK);text(g,"Guide a person through rising water and bring neighbors to safety.",101,270,18,INK);button(g,"START CAMPAIGN  ·  ENTER",101,320,300,54,GREEN);text(g,"WASD / ARROWS to move   ·   SHIFT boost   ·   G clear garbage   ·   R restore health",101,600,16,NAVY);player(g,760,490);}
        void drawPrologue(Graphics2D g){header(g,"MISSION BRIEFING");Level l=levels[level];bold(g,"MISSION 1  —  "+l.name,90,190,32,NAVY);text(g,l.place+"  ·  Cebu City",90,230,21,GREEN);text(g,l.brief,90,278,19,INK);text(g,"Rainwater is rising through the neighborhood.",90,345,21,INK);text(g,"Move around solid obstacles, pick up survivors, and reach the safe zone.",90,380,18,INK);text(g,"You can carry "+capacity+" people at a time. Deliver them to refill capacity.",90,413,18,INK);text(g,"You have 3 health points. Shop between missions to restore health and upgrade.",90,446,18,INK);button(g,"LAUNCH MISSION  ·  ENTER",90,505,300,55,GREEN);}
        void header(Graphics2D g,String title){bold(g,title,38,50,15,GREEN);g.setColor(new Color(220,231,234));g.drawLine(38,66,962,66);}
        void drawPlay(Graphics2D g){Level l=levels[level];header(g,"MISSION "+(level+1)+" / 4     ·     "+l.name.toUpperCase());g.setColor(WATER);g.fillRoundRect(MAP_X,MAP_Y,MAP_W,MAP_H,12,12);
            // Soft moving ripples make the flood feel alive without obscuring the playfield.
            g.setColor(new Color(255,255,255,65));for(int i=0;i<24;i++){int rx=MAP_X+22+Math.floorMod(i*139+tick/2,MAP_W-75);int ry=MAP_Y+32+(i*71%(MAP_H-60));g.drawArc(rx,ry,30,9,10,150);}
            // Current lanes
            for(int j=0;j<3;j++){if(level==0&&j!=1)continue;int y=MAP_Y+105+j*145;g.setColor(level==0?new Color(160,211,232):((level>=2&&j==1)?new Color(115,183,211):new Color(195,230,241)));g.fillRoundRect(MAP_X+8,y,MAP_W-16,72,16,16);if(level==0){g.setColor(new Color(240,250,255,150));g.drawRoundRect(MAP_X+8,y,MAP_W-16,72,16,16);}g.setColor((level>=2&&j==1)?RED:DEEP);for(int x=MAP_X+28;x<MAP_X+MAP_W-50;x+=125){int dir=(level==0||j%2==0)?1:-1;int ax=x+(tick/3%100)*dir;g.drawLine(ax,y+36,ax+34*dir,y+36);g.drawLine(ax+34*dir,y+36,ax+26*dir,y+30);g.drawLine(ax+34*dir,y+36,ax+26*dir,y+42);}}
            // Safe zone
            int safePulse=(int)(Math.sin(tick*.08)*3);g.setColor(new Color(144,219,181));g.fillRoundRect(MAP_X+MAP_W-105-safePulse,MAP_Y+MAP_H-100-safePulse,92+safePulse*2,87+safePulse*2,16,16);bold(g,"SAFE",MAP_X+MAP_W-91,MAP_Y+MAP_H-55,16,new Color(27,105,69));
            for(Rect r:l.walls){g.setColor(new Color(119,130,131));g.fillRoundRect((int)r.x,(int)r.y,(int)r.w,(int)r.h,7,7);g.setColor(new Color(88,99,101));g.drawRoundRect((int)r.x,(int)r.y,(int)r.w,(int)r.h,7,7);}
            if(l.landslide){int sx=MAP_X+MAP_W*.63>0?(int)(MAP_X+MAP_W*.63):0;g.setColor(new Color(240,110,98,100));g.fillRoundRect(sx,MAP_Y+80,68,62,12,12);bold(g,"!",sx+26,MAP_Y+120,32,RED);}
            for(int i=0;i<l.people;i++)if(!rescued[i]){Point2D p=l.peoplePts.get(i);double bob=Math.sin((tick+i*19)*.08)*3,ring=14+Math.sin((tick+i*17)*.07)*2;g.setColor(new Color(238,100,94,90));g.draw(new Ellipse2D.Double(p.getX()-ring,p.getY()-ring,ring*2,ring*2));g.setColor(RED);g.fill(new Ellipse2D.Double(p.getX()-8,p.getY()-8+bob,16,16));g.setColor(Color.WHITE);g.fill(new Ellipse2D.Double(p.getX()-3,p.getY()-3+bob,6,6));}
            if(level==0){Point2D target=tutorialTarget();double pulse=23+Math.sin(tick*.11)*4;g.setColor(new Color(246,190,85,120));g.setStroke(new BasicStroke(3));g.draw(new Ellipse2D.Double(target.getX()-pulse,target.getY()-pulse,pulse*2,pulse*2));g.setColor(GOLD);g.fill(new Ellipse2D.Double(target.getX()-4,target.getY()-4,8,8));g.setStroke(new BasicStroke(1));}
            if(System.currentTimeMillis()<effectUntil){float alpha=Math.max(0f,Math.min(1f,(effectUntil-System.currentTimeMillis())/700f));int radius=(int)((1-alpha)*28)+8;g.setColor(new Color(255,255,255,(int)(alpha*210)));g.setStroke(new BasicStroke(3));g.draw(new Ellipse2D.Double(effectX-radius,effectY-radius,radius*2,radius*2));g.setStroke(new BasicStroke(1));}
            player(g,(int)bx,(int)by);
            g.setColor(Color.WHITE);g.fillRoundRect(40,625,920,38,10,10);bold(g,"HEALTH",55,650,14,INK);for(int i=0;i<3;i++){g.setColor(i<health?RED:new Color(220,225,225));g.fillRoundRect(107+i*22,638,16,13,4,4);}text(g,"PASSENGERS  "+aboard+" / "+capacity,190,650,14,INK);text(g,"RESCUED  "+rescuedCount()+" / "+l.goal,365,650,14,INK);text(g,"SUPPLIES  "+coins,525,650,14,INK);text(g,"WASD move · SHIFT boost · G clear · R restore · ESC pause",638,650,12,NAVY);
            if(level==0){String[] tips={"Move with WASD or arrow keys. Pick up the nearby person.","Head upward to the blue arrow row to learn about currents.","The arrows push right here. Leave the row to stop the current.","Steer around the single gray obstacle to reach the group.","Move close to survivors to pick them up. Rescue at least 5.","Head to the green safe zone at lower right to deliver survivors."};g.setColor(new Color(15,39,57,220));g.fillRoundRect(185,MAP_Y+10,630,38,10,10);text(g,tips[Math.min(tutorialStep,tips.length-1)],205,MAP_Y+35,15,Color.WHITE);}
            if(paused){g.setColor(new Color(15,34,49,190));g.fillRect(0,0,W,H);bold(g,"PAUSED",420,315,44,Color.WHITE);text(g,"Press ESC to resume",410,350,18,Color.WHITE);}if(System.currentTimeMillis()<toastUntil){g.setColor(new Color(15,39,57,220));g.fillRoundRect(280,78,440,38,10,10);text(g,toast,300,103,15,Color.WHITE);}}
        void player(Graphics2D g,int x,int y){if(boost&&System.currentTimeMillis()<boostUntil){g.setColor(new Color(246,190,85,55));g.fillRoundRect(x-20,y-20,40,40,12,12);}if(System.currentTimeMillis()<hitInvulnerableUntil&&(tick/5)%2==0)return;g.setColor(new Color(10,54,48,65));g.fillRoundRect(x-13,y-10,28,28,5,5);g.setColor(new Color(35,111,83));g.fillRoundRect(x-12,y-14,24,24,4,4);g.setColor(new Color(124,222,164));g.fillRect(x-8,y-10,16,15);g.setColor(Color.WHITE);g.fillRect(x+2,y-6,3,3);}
        void button(Graphics2D g,String s,int x,int y,int w,int h,Color c){g.setColor(c);g.fillRoundRect(x,y,w,h,14,14);bold(g,s,x+18,y+h/2+6,17,Color.WHITE);}
        void drawShop(Graphics2D g){header(g,"POST-MISSION SHOP     ·     SUPPLIES: "+coins);bold(g,"Neighbors made it to safety.",75,145,34,NAVY);text(g,"Use supplies to prepare for the next stretch of the flood.",75,183,18,INK);String[] items={"[1] Health restore      Restore health to 3      35 supplies","[2] Rescue capacity     +1 person capacity      45 supplies","[3] Faster movement     Improve move speed       40 supplies"};for(int i=0;i<3;i++){g.setColor(Color.WHITE);g.fillRoundRect(75,225+i*85,850,65,12,12);bold(g,items[i],100,264+i*85,19,INK);}text(g,"Next mission unlocks new hazards and tools. Enter to continue.",75,510,18,GREEN);button(g,level==levels.length-1?"FINISH CAMPAIGN  ·  ENTER":"NEXT MISSION  ·  ENTER",75,545,330,55,GREEN);}
        void drawEnd(Graphics2D g){header(g,"CAMPAIGN COMPLETE");boolean over=screen==Screen.GAME_OVER;bold(g,over?"The storm overwhelmed you.":"Cebu stood together.",90,190,42,NAVY);text(g,over?"You rescued "+rescuedTotal+" neighbors before running out of health.":"You guided your crew through all four flood emergencies.",90,242,20,INK);text(g,"Neighbors brought to safety: "+rescuedTotal,90,305,23,GREEN);text(g,"Press ENTER to return to the title screen.",90,390,18,INK);}
    }
    public static void main(String[] args){SwingUtilities.invokeLater(()->{JFrame f=new JFrame("Cebu: Rising Waters");Game game=new Game();f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);f.setContentPane(game);f.pack();f.setLocationRelativeTo(null);f.setResizable(false);f.setVisible(true);game.requestFocusInWindow();});}
}

