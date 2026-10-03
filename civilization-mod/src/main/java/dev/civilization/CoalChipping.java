package dev.civilization;

/** Pure fixed-grid extraction rule, shared by server contact and fast checks. */
public final class CoalChipping {
    public static int index(int x,int y,int z){return x|(y<<2)|(z<<4);}
    public static boolean occupied(long mask,int x,int y,int z){return x>=0&&x<4&&y>=0&&y<4&&z>=0&&z<4&&(mask&(1L<<index(x,y,z)))!=0;}
    public static long chip(long mask,double px,double py,double pz,int axis,int sign){
        double[] p={px,py,pz};p[axis]-=sign*.0001;int[] cell=new int[3];for(int i=0;i<3;i++)cell[i]=Math.clamp((int)Math.floor(p[i]*4),0,3);
        int u=axis==0?1:0,v=axis==2?1:2;long removed=0;
        for(int a=0;a<2;a++)for(int b=0;b<2;b++){
            int[] c=cell.clone();c[u]=cell[u]/2*2+a;c[v]=cell[v]/2*2+b;
            int[] next=c.clone();next[axis]+=sign;
            if(occupied(mask,c[0],c[1],c[2])&&!occupied(mask,next[0],next[1],next[2]))removed|=1L<<index(c[0],c[1],c[2]);
        }
        return removed;
    }
    private CoalChipping(){}
}
