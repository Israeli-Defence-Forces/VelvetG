package me.movi.violet;

/** Short vanilla-style estimates. The caller bounds collision queries and supplies observed water. */
public final class ProjectilePrediction {
    public interface Environment {
        boolean loaded(double x, double y, double z);
        boolean water(double x, double y, double z);
        boolean collision(double x, double y, double z, double nx, double ny, double nz, double[] impact);
    }
    private ProjectilePrediction() {}
    public static int predict(double[] out, double x, double y, double z, double vx, double vy, double vz,
                              int kind, double ax, double ay, double az, int steps, Environment world) {
        if (!Double.isFinite(x+y+z+vx+vy+vz+ax+ay+az)) return 0;
        int count = 1; out[0]=x;out[1]=y;out[2]=z;
        double[] impact = new double[3];
        for (int i=0; i<Math.min(16, Math.min(steps,out.length/3-1)); i++) {
            double nx=x+vx, ny=y+vy, nz=z+vz;
            if (!world.loaded(nx,ny,nz)) break;
            boolean water=world.water(x,y,z);
            if (world.collision(x,y,z,nx,ny,nz,impact)) { int at=count++*3;out[at]=impact[0];out[at+1]=impact[1];out[at+2]=impact[2];break; }
            int at=count++*3;out[at]=nx;out[at+1]=ny;out[at+2]=nz;
            double drag=water?(kind==2?.6:.8):(kind==0?.95:.99);
            if(kind==0){vx=(vx+ax)*drag;vy=(vy+ay)*drag;vz=(vz+az)*drag;}
            else {vx*=drag;vy=vy*drag-(kind==2?.05:.03);vz*=drag;}
            x=nx;y=ny;z=nz;
        }
        return count;
    }
    public static double segmentDistanceSquared(double px,double py,double pz,double x,double y,double z,double nx,double ny,double nz) {
        double dx=nx-x,dy=ny-y,dz=nz-z,length=dx*dx+dy*dy+dz*dz;
        double t=length==0?0:Math.max(0,Math.min(1,((px-x)*dx+(py-y)*dy+(pz-z)*dz)/length));
        double a=x+t*dx-px,b=y+t*dy-py,c=z+t*dz-pz;return a*a+b*b+c*c;
    }
}
