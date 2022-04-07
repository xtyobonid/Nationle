import java.awt.*;
import java.awt.List;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.event.KeyEvent;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.*;

public class DrawingCanvas extends Canvas implements KeyListener, Runnable, MouseListener {

	public static int ACTUAL_WIDTH = 1050;
	public static int ACTUAL_HEIGHT = 1050;
	
	private BufferedImage back;
	
	public static BufferedImage currentCountryImage;
	public static String currentCountryName;
	
	public static BufferedImage settingsIcon;
	public static Rectangle settingsBounds;
	public static int settingsIconWidth = 50;
	public static int settingsIconHeight = 50;
	public static int settingsX = ACTUAL_WIDTH - settingsIconWidth - 20;
	public static int settingsY = 0;
	
	public static Rectangle leaveSettingsBounds;
	public static int leaveSettingsX = ACTUAL_WIDTH - settingsIconWidth - 100;
	public static int leaveSettingsY = 120;
	public static int leaveSettingsFontSize = 40;
	
	public static BufferedImage optionOnIcon;
	public static BufferedImage optionOffIcon;
	
	public static boolean rotateImagesOption = false;
	public static int rotateImagesOptionX = 100;
	public static int rotateImagesOptionY = 250;
	public static Rectangle rotateImagesOptionBounds;
	
	public static boolean islandsOption = false;
	public static int islandsOptionX = 100;
	public static int islandsOptionY = 400;
	public static Rectangle islandsOptionBounds;
	
	public static ArrayList<String> countryNames;
	public static ArrayList<String> islandNames;
	
	public static ArrayList<ArrayList<String>> countryCoords;
	public static ArrayList<ArrayList<String>> mercatorPixelCoords;
	
	public static ArrayList<String[]> guessFeedback;
	
	public static String typeBuffer = "";
	public static String drawMode = "game";
	
	public boolean shift = false;
	public boolean updateText = false;
	public static boolean refreshScreen = false;
	
	public static JFrame frame; 
	
	public DrawingCanvas (int actualWidth, int actualHeight, JFrame frame_) throws IOException {
		setBackground(Color.BLACK);

		ACTUAL_WIDTH = actualWidth;
		ACTUAL_HEIGHT = actualHeight;
		
		frame = frame_;
		
		guessFeedback = new ArrayList<String[]>();
		
		loadCountryNames();
		loadIslandNames();
		
		loadMercatorPixelCoords();
		loadCountryCoords();
		
		loadIcons();
		
		loadButtonBounds();
		
		nextCountry();
		
		this.addKeyListener(this);
		this.addMouseListener(this);
		new Thread(this).start();
	}
	
	private void loadMercatorPixelCoords() throws IOException {
		mercatorPixelCoords = new ArrayList<>();
		try (BufferedReader in = new BufferedReader(new FileReader("mercatorcountrycoords.csv"))) {
		    String line;
		    while ((line = in.readLine()) != null) {
		        String[] values = line.split(",");
		        mercatorPixelCoords.add(new ArrayList<>(Arrays.asList(values)));
		    }
		    in.close();
		}
	}
	
	private double[][] getMercatorPixelCoords(String countryName) {
		for (int i = 0; i < mercatorPixelCoords.size(); i++) {
			if (mercatorPixelCoords.get(i).get(0).equalsIgnoreCase(countryName)) {
				return new double[][]{{Double.parseDouble(mercatorPixelCoords.get(i).get(1)), Double.parseDouble((mercatorPixelCoords.get(i).get(2)))},{Double.parseDouble(mercatorPixelCoords.get(i).get(3)), Double.parseDouble((mercatorPixelCoords.get(i).get(2)))}};
			}
		}
		
		return null;
	}

	private void loadIslandNames() throws FileNotFoundException {
		Scanner in = new Scanner(new File("islandslist.txt"));
		islandNames = new ArrayList<String>(25);
		while(in.hasNextLine()) {
			String line = in.nextLine();
			islandNames.add(line);
		}
		in.close();
	}
	
	private void loadCountryNames() throws FileNotFoundException {
		Scanner in = new Scanner(new File("countrynames.txt"));
		countryNames = new ArrayList<String>(192);
		while(in.hasNextLine()) {
			String line = in.nextLine();
			countryNames.add(line);
		}
		in.close();
	}

	private void loadButtonBounds() {
		settingsBounds = new Rectangle(settingsX, settingsY, settingsIconWidth, settingsIconHeight);
		leaveSettingsBounds = new Rectangle(leaveSettingsX, leaveSettingsY - leaveSettingsFontSize, 100, leaveSettingsFontSize);
		rotateImagesOptionBounds = new Rectangle(rotateImagesOptionX, rotateImagesOptionY, optionOnIcon.getWidth(), optionOnIcon.getHeight());
		islandsOptionBounds = new Rectangle(islandsOptionX, islandsOptionY, optionOnIcon.getWidth(), optionOnIcon.getHeight());
	}

	private void loadIcons() throws IOException {
		File settingsIconFileLoc = new File("settingsmenu.png");
		settingsIcon = ImageIO.read(settingsIconFileLoc);
		settingsIcon = resize(settingsIcon, settingsIconWidth, settingsIconHeight);
		
		File optionOnIconFileLoc = new File("optionOn.png");
		optionOnIcon = ImageIO.read(optionOnIconFileLoc);
		optionOnIcon = resize(optionOnIcon, 85, 50);
		
		File optionOffIconFileLoc = new File("optionOff.png");
		optionOffIcon = ImageIO.read(optionOffIconFileLoc);
		optionOffIcon = resize(optionOffIcon, 85, 50);
	}

	public void nextCountry() throws IOException {
		guessFeedback.clear();
		
		String countryName = "";
		if(islandsOption) {
			int random = (int) (Math.random() * countryNames.size());
			
			countryName = countryNames.get(random);
		} else {
			while(true) {
				int random = (int) (Math.random() * countryNames.size());
				
				countryName = countryNames.get(random);
				
				if(!isIsland(countryName)) {
					break;
				}
			}
		}
		
		setCountryName(countryName);
		
		for(int i = 0; i < countryName.length(); i++) {
			if(countryName.substring(i,i+1).equals(" ")) {
				countryName = countryName.substring(0,i) + "_" + countryName.substring(i+1);
			}
		}
		
		System.out.println(countryName);
		File countryImageLoc = new File("countrypngs\\" + countryName + ".png");
		BufferedImage countryImage = ImageIO.read(countryImageLoc);
		
		if (rotateImagesOption) {
			countryImage = rotateImageByDegrees(countryImage, (int) (Math.random()*360));
		}
		
		setCountryImage(countryImage);
		
		refreshScreen = true;
		
	}

	private static boolean isIsland(String countryName) {
		for (int i = 0; i < islandNames.size(); i++) {
			if (islandNames.get(i).equals(countryName)) {
				return true;
			}
		}
		return false;
	}

	public void update (Graphics window) {
		paint(window);
	}
	
	public void paint (Graphics window) {
		
		Graphics2D g = (Graphics2D) window;
		
		if (refreshScreen) {
			g.setColor(Color.BLACK);
			g.fillRect(0,0,1050,1050);
			
			refreshScreen = false;
		}
		
		if (drawMode.equals("game")) {
			
			if (currentCountryImage != null) 
				drawCountryImage(g);
				//System.out.println("No image loaded");
			
			if (updateText) {
				
				g.setColor(Color.BLACK);
				g.fillRect(0,550,1050,100);
				
				g.setColor(Color.WHITE);
				drawCenteredString(g, typeBuffer, new Rectangle(0,550,1050,100), new Font("Arial", Font.PLAIN, 40));
				
				updateText = false;
			}
			
			drawSettingsIcon(g);
			
			if(guessFeedback.size() > 0) {
				drawGuessFeedback(g);
			}
			
			//tdg.drawImage(back, null, 0, 0);
		} else if (drawMode.equals("settings")) {
			
			g.setColor(Color.WHITE);
			g.setFont(new Font("Arial", Font.PLAIN, 70));
			g.drawString("Settings Menu", 100, 150);
			
			g.setColor(Color.RED);
			g.setFont(new Font("Arial", Font.PLAIN, leaveSettingsFontSize));
			g.drawString("Exit", leaveSettingsX, leaveSettingsY);
			
			if(rotateImagesOption) {
				g.drawImage(optionOnIcon, rotateImagesOptionX, rotateImagesOptionY, null);
			} else {
				g.drawImage(optionOffIcon, rotateImagesOptionX, rotateImagesOptionY, null);
			}
			
			g.setColor(Color.WHITE);
			g.setFont(new Font("Arial", Font.PLAIN, 40));
			g.drawString("Rotate Images", rotateImagesOptionX + optionOnIcon.getWidth() + 40, rotateImagesOptionY+ 38);
			//drawCenteredString(g, "Rotate Images", new Rectangle(rotateImagesOptionX + optionOnIcon.getWidth() + 10, rotateImagesOptionY, 200, optionOnIcon.getHeight()), new Font("Arial", Font.PLAIN, 40));
			
			if(islandsOption) {
				g.drawImage(optionOnIcon, islandsOptionX, islandsOptionY, null);
			} else {
				g.drawImage(optionOffIcon, islandsOptionX, islandsOptionY, null);
			}
			
			g.setColor(Color.WHITE);
			g.setFont(new Font("Arial", Font.PLAIN, 40));
			g.drawString("Enable Islands", islandsOptionX + optionOnIcon.getWidth() + 40, islandsOptionY+ 38);
			
		}
		
	}
	
	private void drawGuessFeedback(Graphics2D g) {
		for (int i = 0; i < guessFeedback.size(); i++) {
			g.setStroke(new BasicStroke(5));
			Rectangle guessBounds = new Rectangle(100,650 + (i*60),400,40);
			g.draw(guessBounds);
			int fontSize = 20*(32/guessFeedback.get(i)[0].length());
			if (fontSize > 30) {
				fontSize = 30;
			}
			drawCenteredString(g, guessFeedback.get(i)[0], guessBounds, new Font("Arial", Font.PLAIN, fontSize));
			
			Rectangle distanceBounds = new Rectangle(520,650 + (i*60),200,40);
			g.draw(distanceBounds);
			fontSize = 20;
			drawCenteredString(g, guessFeedback.get(i)[1] + " km", distanceBounds, new Font("Arial", Font.PLAIN, fontSize));
			
			drawArrow(g, 760, 668 + (i*60), Integer.parseInt(guessFeedback.get(i)[2]));
		}
	}

	private void drawArrow(Graphics2D g, int x, int y, int angle) {
		
		g.setStroke(new BasicStroke(3));

		AffineTransform tx = new AffineTransform();
		//System.out.println();
		
		Line2D.Double line = new Line2D.Double(20 * Math.cos(Math.toRadians(180 + angle)) + x, 20 * Math.sin(Math.toRadians(180 + angle)) + y, 20 * Math.cos(Math.toRadians(angle)) + x, 20 * Math.sin(Math.toRadians(angle)) + y);
		g.draw(line);


		Polygon arrowHead = new Polygon();  
		arrowHead.addPoint( 0,5);
		arrowHead.addPoint( -5, -5);
		arrowHead.addPoint( 5,-5);

		tx.setToIdentity();
		double ang = Math.atan2(line.y2-line.y1, line.x2-line.x1);
		//System.out.println(line.x2 + " " + line.y2);
		tx.translate(line.x2, line.y2);
		tx.rotate((ang-Math.PI/2d));  
		
		g.setTransform(tx);
		g.fill(arrowHead);
		g.setTransform(new AffineTransform());
	}

	private void drawSettingsIcon(Graphics2D g) {
		g.drawImage(settingsIcon, ACTUAL_WIDTH - settingsIcon.getWidth()- 20, 0, null);
	}

	private void drawCountryImage(Graphics2D g) {
		int width = currentCountryImage.getWidth();
		int height = currentCountryImage.getHeight();
		if (width > 850) {
			currentCountryImage = resize(currentCountryImage, 850, (int) ((double)height/width * 850));
			width = currentCountryImage.getWidth();
			height = currentCountryImage.getHeight();
		}
		if (height > 500) {
			currentCountryImage = resize(currentCountryImage, (int) ((double)width/height * 500), 500);
			width = currentCountryImage.getWidth();
			height = currentCountryImage.getHeight();
		}
		
		//System.out.println(width + " " + height);
		g.drawImage(currentCountryImage, (1050-width)/2, (600-height)/2, null);
		
		g.setStroke(new BasicStroke(5));
		g.setColor(new Color(255,255,255));
		g.drawRect(((1050-width)/2)-30, ((600-height)/2)-10, width + 60, height + 20);
	}
	
	public void drawCenteredString(Graphics g, String text, Rectangle rect, Font font) {
	    // Get the FontMetrics
	    FontMetrics metrics = g.getFontMetrics(font);
	    // Determine the X coordinate for the text
	    int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
	    // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
	    int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
	    // Set the font
	    g.setFont(font);
	    // Draw the String
	    g.drawString(text, x, y);
	}
	
	public static BufferedImage resize(BufferedImage img, int newW, int newH) { 
	    Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
	    BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

	    Graphics2D g2d = dimg.createGraphics();
	    g2d.drawImage(tmp, 0, 0, null);
	    g2d.dispose();

	    return dimg;
	} 
	
	public static BufferedImage rotateImageByDegrees(BufferedImage img, double angle) {
	    double rads = Math.toRadians(angle);
	    double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
	    int w = img.getWidth();
	    int h = img.getHeight();
	    int newWidth = (int) Math.floor(w * cos + h * sin);
	    int newHeight = (int) Math.floor(h * cos + w * sin);

	    BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g2d = rotated.createGraphics();
	    AffineTransform at = new AffineTransform();
	    at.translate((newWidth - w) / 2, (newHeight - h) / 2);

	    int x = w / 2;
	    int y = h / 2;

	    at.rotate(rads, x, y);
	    g2d.setTransform(at);
	    g2d.drawImage(img, 0, 0, null);
	    //g2d.setColor(Color.RED);
	    //g2d.drawRect(0, 0, newWidth - 1, newHeight - 1);
	    g2d.dispose();

	    return rotated;
	}

	public static void setCountryImage(BufferedImage image) {
		currentCountryImage = image;
	}

	public void run() {
   		try {
	   		while(true) {
	   		   Thread.currentThread().sleep(5);
	           repaint();
	        }
      	}
      	catch(Exception e) {
      	}
  	}
	
	public void keyTyped(KeyEvent e) {
		
	}

	public void keyPressed(KeyEvent e) {
		String key = "" + e.getKeyChar();
		if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
			if (typeBuffer.length() > 0) {
				typeBuffer = typeBuffer.substring(0, typeBuffer.length()-1);
				
				updateText = true;
				
				//System.out.println(typeBuffer);
			}
		} else if (key.matches("^[a-zA-Z0-9]")) {
			typeBuffer += key;
			
			updateText = true;
			
			//System.out.println(typeBuffer);
		} else if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
			shift = true;
		} else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			typeBuffer += " ";
		} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			try {
				checkGuess();
			} catch (Exception exception) {
				
			}
		}
			
	}

	private void checkGuess() throws InterruptedException, IOException {
		Graphics2D g = (Graphics2D) this.getGraphics();
		
		if (typeBuffer.equalsIgnoreCase(currentCountryName)) {
			g.setColor(Color.BLACK);
			g.fillRect(0,580,1050,50);
			
			g.setColor(Color.GREEN);
			drawCenteredString(g, "CORRECT!", new Rectangle(0,580,1050,50), new Font("Arial", Font.PLAIN, 40));
			
			Thread.sleep(1500);
			
			g.setColor(Color.BLACK);
			g.fillRect(0,550,1050,50);
			
			typeBuffer = "";
			
			nextCountry();
		} else {
			if(!verifyGuess(typeBuffer)) {
				g.setColor(Color.BLACK);
				g.fillRect(0,580,1050,50);
				
				g.setColor(Color.RED);
				drawCenteredString(g, "INVALID GUESS", new Rectangle(0,580,1050,50), new Font("Arial", Font.PLAIN, 40));
				
				Thread.sleep(1500);
				
				g.setColor(Color.BLACK);
				g.fillRect(0,550,1050,50);
				
				typeBuffer = "";
			} else {
				g.setColor(Color.BLACK);
				g.fillRect(0,580,1050,50);
				
				g.setColor(Color.RED);
				drawCenteredString(g, "WRONG", new Rectangle(0,580,1050,50), new Font("Arial", Font.PLAIN, 40));
				
				double[] guessCoords = getCountryCoords(typeBuffer);
				double[] correctCoords = getCountryCoords(currentCountryName);
				
				int[] smallestDistCoords = getSmallestDistCoords(typeBuffer, currentCountryName);
				//System.out.println("");
				
				int dist = distance(guessCoords[0], guessCoords[1], correctCoords[0], correctCoords[1], 'K');
				int angle = angleBetweenCoords(smallestDistCoords[0], smallestDistCoords[1], smallestDistCoords[2], smallestDistCoords[3]);
				
				String[] temp = new String[]{typeBuffer, "" + dist, "" + angle};
				System.out.println(Arrays.toString(temp));
				guessFeedback.add(temp);
				
				Thread.sleep(1500);
				
				g.setColor(Color.BLACK);
				g.fillRect(0,550,1050,100);
				
				typeBuffer = "";
			}
		}
	}
	
//	private int angleBetweenCoords(double latitude1, double longitude1, double latitude2, double longitude2) {
//		double y = Math.sin(longitude2-longitude1) * Math.cos(latitude2);
//		double x = Math.cos(latitude1) * Math.sin(latitude2) -
//				   Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longitude2-longitude1);
//		double theta = Math.atan2(y, x);
//		double bearing = (theta * 180/Math.PI + 360) % 360;
//		
//		return (int) bearing;
//	}
//	
//	private int angleBetweenCoords(double lat1, double long1, double lat2, double long2) {
//
//	    double dLon = (long2 - long1);
//
//	    double y = Math.sin(dLon) * Math.cos(lat2);
//	    double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
//	            * Math.cos(lat2) * Math.cos(dLon);
//
//	    double brng = Math.atan2(y, x);
//
//	    brng = Math.toDegrees(brng);
//	    brng = (brng + 360) % 360;
//	    //brng = 360 - brng; // count degrees counter-clockwise - remove to make clockwise
//
//	    return (int)brng;
//	}
	
	private int[] getSmallestDistCoords(String typeBuffer2, String currentCountryName2) {
		int[] ret = new int[4];
		
		double[][] c1 = getMercatorPixelCoords(typeBuffer);
		double[][] c2 = getMercatorPixelCoords(currentCountryName);
		//System.out.println("");
		
		int smallestI = 0;
		int smallestJ = 0;
		for(int i = 0; i < c1.length; i++) {
			for(int j = 0; j < c2.length; j++) {
				if (pixelDistance((int) c1[i][0],(int) c1[i][1],(int) c2[j][0],(int) c2[j][1]) < pixelDistance((int) c1[smallestI][0],(int) c1[smallestI][1],(int) c2[smallestJ][0],(int) c2[smallestJ][1])) {
					smallestI = i;
					smallestJ = j;
				}
			}
		}
		
		ret[0] = (int) c1[smallestI][0];
		ret[1] = (int) c1[smallestI][1];
		ret[2] = (int) c2[smallestJ][0];
		ret[3] = (int) c2[smallestJ][1];
		
		//System.out.println(ret[0] + " " + ret[1] + " " + ret[2] + " " + ret[3]);
		
		return ret;
	}
	
	private int pixelDistance(int x1, int y1, int x2, int y2) {
		return (int) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
	}

	//mercator pixel math
	//https://stackoverflow.com/questions/9970281/java-calculating-the-angle-between-two-points-in-degrees
	private int angleBetweenCoords(int x1, int y1, int x2, int y2) {
		
		//System.out.println(x1 + " " + y1 + " " + x2 + " " + y2);
		//System.out.println(Math.toDegrees(Math.atan(((double)y2-(double)y1)/((double)x2-(double)x1))));
		
		float angle = (float) Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));

	    if(angle < 0){
	        angle += 360;
	    }

	    return (int) angle;
	}
	
//	private int angleBetweenCoords(double latitude1, double longitude1, double latitude2, double longitude2) {
//		double a = Math.log(Math.tan(latitude2 / 2 + Math.PI / 4) / Math.tan(latitude1 / 2 + Math.PI / 4));
//		double b = Math.abs(longitude1 - longitude2);
//		b = Math.toRadians(Math.toDegrees(b) % 180);
//		
//		return (int) Math.toDegrees(Math.atan2(b, a));
//	}

	private double[] getCountryCoords(String countryName) {
		for (int i = 0; i < countryCoords.size(); i++) {
			if (countryCoords.get(i).get(0).equalsIgnoreCase(countryName)) {
				return new double[] {Double.parseDouble(countryCoords.get(i).get(1)), Double.parseDouble((countryCoords.get(i).get(2)))};
			}
		}
		
		return null;
	}

	private void loadCountryCoords() throws IOException {
		countryCoords = new ArrayList<>();
		try (BufferedReader in = new BufferedReader(new FileReader("countrycoords.csv"))) {
		    String line;
		    while ((line = in.readLine()) != null) {
		        String[] values = line.split(",");
		        countryCoords.add(new ArrayList<>(Arrays.asList(values)));
		    }
		    in.close();
		}
	}
	
	//https://dzone.com/articles/distance-calculation-using-3
	private int distance(double lat1, double lon1, double lat2, double lon2, char unit) {
	      double theta = lon1 - lon2;
	      double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
	      dist = Math.acos(dist);
	      dist = Math.toDegrees(dist);
	      dist = dist * 60 * 1.1515;
	      if (unit == 'K') {
	        dist = dist * 1.609344;
	      } else if (unit == 'N') {
	        dist = dist * 0.8684;
	        }
	      return (int) (dist);
	    }
	

	private boolean verifyGuess(String guess) {
		for(int i = 0; i < countryNames.size(); i++) {
			if (countryNames.get(i).equalsIgnoreCase(guess)) {
				return true;
			}
		}
		return false;
	}

	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		 if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
			shift = false;
		}
	}

	public static void setCountryName(String countryName) {
		currentCountryName = countryName;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			if (drawMode.equals("game")) {
				//System.out.println(e.getX() + " " + e.getY());
				if(clickWithin(settingsBounds, e.getX(), e.getY())) {
					//System.out.println(e.getX() + " " + e.getY());
					refreshScreen = true;
					drawMode = "settings";
				}
			} else if (drawMode.equals("settings")) {
				if(clickWithin(rotateImagesOptionBounds, e.getX(), e.getY())) {
					rotateImagesOption = !rotateImagesOption;
					try {
						nextCountry();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					refreshScreen = true;
				}
				else if(clickWithin(leaveSettingsBounds, e.getX(), e.getY())) {
					refreshScreen = true;
					drawMode = "game";
				} 
				else if(clickWithin(islandsOptionBounds, e.getX(), e.getY())) {
					islandsOption = !islandsOption;
					try {
						nextCountry();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					refreshScreen = true;
				}
			}
		}
	}

	private boolean clickWithin(Rectangle rectangle, int x, int y) {
		//System.out.println( ((boolean)(x > rectangle.x)) + " " + x + ">" + rectangle.x);
		return (x > rectangle.x && x < rectangle.x + rectangle.width && y > rectangle.y && y < rectangle.y + rectangle.height);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
}