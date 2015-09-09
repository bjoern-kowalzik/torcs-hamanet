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
	
	double lane_angle = Math.PI / 18;
	double m_width = 0.4;
	double minSpeed = 100;
	int stuck = 0;

	private int tickcounter = 0;
	private int myGear = 1;
	
	public Action control(SensorModel sm) {

		// adjust tick counter
		tickcounter++;

		// create new action object to send our commands to the server
		Action action = new Action();
		System.out.println(sm.getTrackEdgeSensors()[9]);
		System.out.println(sm.getTrackPosition());
		System.out.println(sm.getAngleToTrackAxis());
		System.out.println(stuck);
		System.out.println("----------------");

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
		action.steering = targetLane(sm, curve*0.9);
		if (curve == 0 && sm.getTrackEdgeSensors()[9] >= 100 || sm.getSpeed() < minSpeed) action.accelerate = 1;
		
		//System.out.println(sm.getTrackEdgeSensors()[9]);

		//gear
		if (sm.getRPM() >= 8000)
		{
			myGear = sm.getGear() + 1;
		} else if(sm.getRPM() <= 2000)
		{
			myGear = sm.getGear() - 1;
		}
		if (myGear == 7) myGear = 6;
		if (myGear == 0) myGear = 1;
		action.gear = myGear;
		
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