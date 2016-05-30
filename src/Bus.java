package avs.models;

import avs.utils.Animator;
import avs.utils.Textures;

import java.util.ArrayList;
import java.awt.image.*;
import java.awt.*;

import javax.swing.*;

public class Bus extends Entity{
	private BufferedImage[] animation;
	private int speed;
	Animator anim;

	public Bus(double x, double y, Textures texx){
		super(x,y,Textures.BUS_WIDTH,Textures.BUS_HEIGHT,texx);
		this.animation = texx.getBusArray();
		this.speed = 1;
		anim = new Animator(5,animation);
	}

	public void tick(){
		setX(getX()+speed);
		anim.runAnimation();
	}

	public void render(Graphics g){
		//g.drawImage(busImage, (int)this.getX(), (int)this.getY(), null);
		anim.drawAnimation(g,getX(),getY(),width,height,0);
	}
}
