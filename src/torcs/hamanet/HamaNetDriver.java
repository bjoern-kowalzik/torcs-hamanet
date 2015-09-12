//Gruppe: Björn Kowalzik, Adrian Hernaiz Garcia, Oliver Wroblewski

package torcs.hamanet;

import java.util.Arrays;
import torcs.scr.Driver;
import torcs.scr.Action;
import torcs.scr.SensorModel;

/**
 * Simple controller as a starting point to develop your own one - accelerates
 * slowly - tries to maintain a constant speed (only accelerating, no braking) -
 * stays in first gear - steering follows the track and avoids to come too close
 * to the edges
 */
public class HamaNetDriver extends Driver {
	
	private double lane_angle = Math.PI / 9;
	private double m_width = 0.7;
	private double minSpeed = 100;
	private int deadticks = 0;
	private int myGear = 1;
	
	public Action control(SensorModel sm) {

		Action action = new Action();

		if (Math.abs(sm.getTrackPosition()) > 1 && Math.abs(sm.getAngleToTrackAxis()) > ((5.0/9.0)*Math.PI))
		{
			if (sm.getSpeed() > 10)
			{
				action.brake = 1;
				return action;
			}
			else
			{
				action.gear = (int)Math.signum(sm.getAngleToTrackAxis()*sm.getTrackPosition());
				action.steering = action.gear*targetAngle(sm,0);
			}
		}
		
		//Curve Management
		int curve = curveAhead(sm);
		action.steering = targetLane(sm, curve*0.7);
		if (curve == 0 && sm.getTrackEdgeSensors()[9] >= 100 || sm.getSpeed() < minSpeed) action.accelerate = 1;
		
		//System.out.println(sm.getTrackEdgeSensors()[9]);

		//check speed
		speedAdaption(sm, action);
		
		//gear
		if (sm.getRPM() >= 8600)
		{
			myGear = sm.getGear() + 1;
		} else if(sm.getRPM() <= 3000)
		{
			myGear = sm.getGear() - 1;
		}
		if (myGear == 7) myGear = 6;
		if (myGear == 0) myGear = 1;
		action.gear = myGear;
		
		//unstuck
        if (Math.abs(sm.getTrackPosition())>1)
        {
                //action.steering = targetLane(sm,0);
                action.brake = 0;
                action.accelerate = 1;
        }
       
        if (sm.getRPM()>100 && sm.getSpeed() < 5) deadticks++;
        else deadticks = 0;

        if (deadticks > 150)
        {
                action.gear = (int)-Math.signum(sm.getGear());
                action.steering = Math.signum(action.gear*targetLane(sm, 0));
                action.accelerate = 0.3;
                deadticks = 0;
        }
        else if (sm.getGear() == -1)
        {
                if ( Math.abs(sm.getTrackPosition()) > 0.5){
                        action.gear = -1;
                        action.steering = Math.signum(action.gear*targetLane(sm, 0));
                } else if (Math.abs(sm.getSpeed()) > 0.1)
                {
                        action.accelerate = 0;
                        action.brake = 1;
                        action.steering = Math.signum(action.gear*targetLane(sm, 0));
                }
                else action.gear = 1;
        }
		
		return action;
	}
	
	private int curveAhead(SensorModel sm)
	{
		double[] te = sm.getTrackEdgeSensors();
		int c = 0;
		for (int i = 0; i<te.length; i++)
		{
			if (te[i] == 200) c++;
		}
		double[] temp = new double[te.length-c];
		c = 0;
		for(int i = 0; i<temp.length; i++)
		{
			if (te[i] == 200){c++; continue;}
			temp[i] = te[i+c];
		}
		te = temp;
		
		boolean[] angles = new boolean[te.length-2];
		//int i=7;
		//return getDirection(te[i], te[i+1], te[i+2], Math.PI/18.0);
		for (int i = 0; i<te.length-2; i++)
		{
			angles[i] = angleSubPie(te[i], te[i+1], te[i+2], Math.PI/18.0);
		}
		
		int u = angles.length - 1;
		int l = 0;
		int u2 = angles.length - 1;
		int l2 = 0;
		
		c = 0;

		for (int i = 0; i<angles.length-1; i++)
		{
			if (angles[i] != angles[i+1]) c++;
			if (angles[u]) u--;
			if (!angles[l]) l++;
			if (!angles[u2]) u2--;
			if (angles[l2]) l2++;
		}
		//System.out.println(Arrays.toString(angles));
		//System.out.println(u + " " + l+ " " + u2 +" " + l2);
		
		if (c>4) return 0;
		if (Math.min(u, l) >= 2) return -1;
		if (Math.min(u2, l2) >= 2) return 1;
		return 0;
	}
	
	private void speedAdaption(SensorModel sm, Action action){
		double dircetionAng = getMax(sm.getTrackEdgeSensors());
		double maxSpeed = getMaxSpeed(dircetionAng);
		
		if(dircetionAng >= 90) maxSpeed = 300;
		if(maxSpeed > sm.getSpeed()) {
			action.accelerate = 1;
			action.brake = 0;
		}else
		if(maxSpeed < sm.getSpeed()) {
			if(sm.getSpeed() > 100 && dircetionAng > 150)
				System.out.println("if1" + " direc" + dircetionAng +"macspeed" + maxSpeed);
				action.accelerate = 0;
				action.brake = 1;
			if(sm.getSpeed() > 80 && dircetionAng < 80)
				action.accelerate = 0;
				System.out.println("if2" + " direc " + dircetionAng +" macspeed " + maxSpeed);
				action.brake = 1;
			if(sm.getSpeed() > 60 && dircetionAng < 60)
				action.accelerate = 0;
				System.out.println("if4" + " direc" + dircetionAng +"macspeed" + maxSpeed);
				action.brake = 1;
			if(sm.getSpeed() > 40 && dircetionAng < 40)
				action.accelerate = 0;
				System.out.println("if5" + " direc" + dircetionAng +"macspeed" + maxSpeed);
				action.brake = 1;
			if(sm.getSpeed() > 20 && dircetionAng < 25)
				action.accelerate = 0;
				System.out.println("if6" + " direc" + dircetionAng +"macspeed" + maxSpeed);
				action.brake = 1;
		}

		System.out.println(maxSpeed + " " + dircetionAng);
		
	}
	
	private double getMaxSpeed(double focus){
		return 3.6*(Math.sqrt(9.80665*1.4*focus));
	}
	
	private double getMax(double [] array){
		double max = 0;
		for(int i = 0; i < array.length-1;i++)
			if(array[i] > max) max = array[i];
		return max;
	}
	
	private boolean angleSubPie(double a, double b, double c, double alpha)
	{
		double x = Math.sqrt(a*a+c*c-2*a*c*Math.cos(2*alpha));
		double delta = Math.acos(((x*x)+(c*c)-(a*a))/(2*x*c));
		return b < (Math.sin(delta)*c)/Math.sin(Math.PI-alpha-delta);
		
	}
	private double targetAngle(SensorModel sm, double ang)
	{
		return sm.getAngleToTrackAxis()-ang;
	}
	private double targetLane(SensorModel sm, double lane)
	{
		if (inrange(lane - m_width / 2, lane + m_width / 2, sm.getTrackPosition()))
		{
			return targetAngle(sm, 0);
		}
		return targetAngle(sm, -lane_angle*Math.signum(lane-sm.getTrackPosition()));
	}
	private boolean inrange(double l, double u, double t)
	{return u>=t && t>=l;}

	public void shutdown() {
		System.out.println("Bye bye!");
	}
}