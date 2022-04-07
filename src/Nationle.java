import javax.imageio.ImageIO;
import javax.swing.JFrame;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JPanel;


public class Nationle extends JFrame {
	public static final int ACTUAL_WIDTH = 1050;
	public static final int ACTUAL_HEIGHT = 1050;
	
	public static DrawingCanvas canvas; 
	
	@SuppressWarnings("unchecked")
	public Nationle() throws IOException {
		super("Nationle");
		setSize(ACTUAL_WIDTH, ACTUAL_HEIGHT);
		setLocation(350,0);
		
		canvas = new DrawingCanvas(ACTUAL_WIDTH, ACTUAL_HEIGHT, this);
		((Component)canvas).setFocusable(true);
		
		getContentPane().add(canvas);
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
	}
	
	public static void main(String[] args) throws IOException {
		Nationle go = new Nationle();
	}
}