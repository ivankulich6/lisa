import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;


@SuppressWarnings("serial")
public class DrawingPanel extends JPanel{
	
	private BufferedImage img;
	
	public void draw(BufferedImage img){
		this.img = img;
		this.repaint();
	}
	
	@Override
	public void paint(Graphics g) {
		g.setColor(new Color(255, 255, 255));
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
		g.drawImage(img, 0, 0, this);
	}
}
