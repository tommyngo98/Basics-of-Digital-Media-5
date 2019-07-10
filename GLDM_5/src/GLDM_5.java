import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
     Opens an image window and adds a panel below the image
 */
public class GLDM_5 implements PlugIn {

	ImagePlus imp; // ImagePlus object
	private int[] origPixels;
	private int width;
	private int height;

	String[] items = {"Original", "Filter 1", "Weichzeichnen", "Hochpass", "Starke Kanten"};


	public static void main(String args[]) {

		IJ.open("C:\\Users\\tommy\\eclipse-workspace\\GLDM_5\\sail.jpg");

		GLDM_5 pw = new GLDM_5();
		pw.imp = IJ.getImage();
		pw.run("");
	}

	public void run(String arg) {
		if (imp==null) 
			imp = WindowManager.getCurrentImage();
		if (imp==null) {
			return;
		}
		CustomCanvas cc = new CustomCanvas(imp);

		storePixelValues(imp.getProcessor());

		new CustomWindow(imp, cc);
	}


	private void storePixelValues(ImageProcessor ip) {
		width = ip.getWidth();
		height = ip.getHeight();

		origPixels = ((int []) ip.getPixels()).clone();
	}


	class CustomCanvas extends ImageCanvas {

		CustomCanvas(ImagePlus imp) {
			super(imp);
		}

	} // CustomCanvas inner class


	class CustomWindow extends ImageWindow implements ItemListener {

		private String method;
		
		CustomWindow(ImagePlus imp, ImageCanvas ic) {
			super(imp, ic);
			addPanel();
		}

		void addPanel() {
			//JPanel panel = new JPanel();
			Panel panel = new Panel();

			JComboBox cb = new JComboBox(items);
			panel.add(cb);
			cb.addItemListener(this);

			add(panel);
			pack();
		}

		public void itemStateChanged(ItemEvent evt) {

			// Get the affected item
			Object item = evt.getItem();

			if (evt.getStateChange() == ItemEvent.SELECTED) {
				System.out.println("Selected: " + item.toString());
				method = item.toString();
				changePixelValues(imp.getProcessor());
				imp.updateAndDraw();
			} 

		}


		private void changePixelValues(ImageProcessor ip) {

			// Array zum Zurückschreiben der Pixelwerte
			int[] pixels = (int[])ip.getPixels();
			if (method.equals("Original")) {

				for (int y=0; y<height; y++) {
					for (int x=0; x<width; x++) {
						int pos = y*width + x;
						
						pixels[pos] = origPixels[pos];
					}
				}
				
			}
			
			if (method.equals("Filter 1")) {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int pos = y * width + x;
						int argb = origPixels[pos]; // Lesen der Originalwerte

						int r = (argb >> 16) & 0xff;
						int g = (argb >> 8) & 0xff;
						int b = argb & 0xff;

						int rn = r / 2;
						int gn = g / 2;
						int bn = b / 2;

						pixels[pos] = (0xFF << 24) | (rn << 16) | (gn << 8) | bn;
					}
				}
			}
			
			if (method.equals("Weichzeichnen")) {
				// Faktor für das Weichzeichnen bei einem 3*3 Kernel
				int faktor = 9;
				int[] filter = { 1, 1, 1, 1, 1, 1, 1, 1, 1 };

				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int pos = y * width + x;

						int rn = 0;
						int gn = 0;
						int bn = 0;

						// Erstellen einer Kernelmatrix mit den entsprechenden Werten,
						// die sich aus umliegenden Pixeln ergeben
				
						int[] kernelValues = calculateMatrix(origPixels, x, y, width, height);
						// Schleife für Zuweisung der RGB-Werte aus den umliegenden
						// Pixel
						for (int i = 0; i < kernelValues.length; i++) {
							int r = (kernelValues[i] >> 16) & 0xff;
							int g = (kernelValues[i] >> 8) & 0xff;
							int b = kernelValues[i] & 0xff;

							// am Ende wird erst geteilt, damit man keine Rundungsfehler
							// während der berechnung erhält
							rn += r * filter[i];
							gn += g * filter[i];
							bn += b * filter[i];

						}

						// hier wird erst durch 9 geteilt, damit man keine verfälschten
						// Werte erhält
						rn = (rn / faktor);
						gn = (gn / faktor);
						bn = (bn / faktor);

						pixels[pos] = (0xFF << 24) | (rn << 16) | (gn << 8) | bn;
					}
				}
				
			}
			
			if (method.equals("Starke Kanten")) {
				// Faktor für das Weichzeichnen bei einem 3*3 Kernel
				int faktor = 9;
				int[] filter = { -1, -1, -1, -1, 17, -1, -1, -1, -1 };

				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int pos = y * width + x;

						int rn = 0;
						int gn = 0;
						int bn = 0;

						// Erstellen einer Kernelmatrix mit den entsprechenden Werten,
						// die sich aus umliegenden Pixeln ergeben
						// der äußere Rand wird dabei abgefangen und originalwerte der
						// Pixel werden statt dessen genommen
						int[] kernelValues = calculateMatrix(origPixels, x, y, width, height);
						// Schleife für Zuweisung der RGB-Werte aus den umliegenden
						// Pixel
						for (int i = 0; i < kernelValues.length; i++) {
							int r = (kernelValues[i] >> 16) & 0xff;
							int g = (kernelValues[i] >> 8) & 0xff;
							int b = kernelValues[i] & 0xff;

							// am Ende wird erst geteilt, damit man keine Rundungsfehler
							// während der berechnung erhält
							rn += r * filter[i];
							gn += g * filter[i];
							bn += b * filter[i];

						}

						// hier wird erst durch 9 geteilt, damit man keine verfälschten
						// Werte erhält
						rn = (rn / faktor);
						gn = (gn / faktor);
						bn = (bn / faktor);
						
						
						// Abfangen von RGB Werten, welche über 255 oder unter 0 sind
						rn = borderColor(rn);
						gn = borderColor(gn);
						bn = borderColor(bn);

						pixels[pos] = (0xFF << 24) | (rn << 16) | (gn << 8) | bn;
					}
				}
				
				
			}
			
			if (method.equals("Hochpass")) {
				// Faktor für das Weichzeichnen bei einem 3*3 Kernel
				int faktor = 9;
				int[] filter = { -1, -1, -1, -1, 8, -1, -1, -1, -1 };

				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int pos = y * width + x;

						int rn = 0;
						int gn = 0;
						int bn = 0;

						// Erstellen einer Kernelmatrix mit den entsprechenden Werten,
						// die sich aus umliegenden Pixeln ergeben
						// der äußere Rand wird dabei abgefangen und originalwerte der
						// Pixel werden statt dessen genommen
						int[] kernelValues = calculateMatrix(origPixels, x, y, width, height);
						// Schleife für Zuweisung der RGB-Werte aus den umliegenden
						// Pixel
						for (int i = 0; i < kernelValues.length; i++) {
							int r = (kernelValues[i] >> 16) & 0xff;
							int g = (kernelValues[i] >> 8) & 0xff;
							int b = kernelValues[i] & 0xff;

							// am Ende wird erst geteilt, damit man keine Rundungsfehler
							// während der berechnung erhält
							rn += r * filter[i];
							gn += g * filter[i];
							bn += b * filter[i];

						}

						// hier wird erst durch 9 geteilt, damit man keine verfälschten
						// Werte erhält
						rn = (rn / faktor) + 128;
						gn = (gn / faktor) + 128;
						bn = (bn / faktor) +128;
						
						// Abfangen von RGB Werten, welche über 255 oder unter 0 sind
						rn = borderColor(rn);
						gn = borderColor(gn);
						bn = borderColor(bn);

						pixels[pos] = (0xFF << 24) | (rn << 16) | (gn << 8) | bn;
					}
				}
				
			}
			
		}
		
		public int[] calculateMatrix(int[] origPixels, int x, int y, int imwidth, int imheight) {
			// 3*3Matrix erstellen und mit dieser die jeweiligen Farben aus den
			// benachbarten Pixeln abgreifen
			int[] kernel = new int[9];
			int counter = 0;

			for (int vert = -1; vert < 2; vert++) {
				for (int hor = -1; hor < 2; hor++) {
					if (y == (imheight - 1) || y == 0 || x == (imwidth - 1) || x == 0) {
						kernel[counter] = origPixels[y * imwidth + x];
					} else {
						kernel[counter] = origPixels[(y + vert) * imwidth + (x + hor)];
					}
					counter++;
				}
			}
			return kernel;
		}
		
		int borderColor(int rgbValue) {
			if (rgbValue > 255) {
				rgbValue = 255;
			}
			if (rgbValue < 0) {
				rgbValue = 0;
			}
			return rgbValue;
		}


	} // CustomWindow inner class
} 

