import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.process.*;

public class NCS_ implements PlugInFilter
	{
	ImagePlus img;
	int padTop, padLeft, padRight, padBottom, offset;

	public int setup(String arg, ImagePlus imp)
		{
		img = imp;
		return DOES_16 | DOES_32 | DOES_8C | DOES_8G | DOES_RGB;
		}

	public void run(ImageProcessor ip)
		{
		if (img == null | ip == null) return;

		int w = img.getWidth();
		int h = img.getHeight();

		GenericDialog gd = new GenericDialog ("Image Padder");
		gd.addMessage ("Current width: "+w);
		gd.addMessage ("Current height: "+h);
		gd.addNumericField ("Pad_top:",1,0);
		gd.addNumericField ("Pad_left:",1,0);
		gd.addNumericField ("Pad_right:",1,0);
		gd.addNumericField ("Pad_bottom: ",1,0);
		gd.addNumericField ("Average Offset: ",100,0);
		gd.addMessage ("(padding can also be negative)");
		gd.showDialog();
		if (gd.wasCanceled()) return;
	
		padTop = (int) gd.getNextNumber();
		padLeft = (int) gd.getNextNumber();
		padRight = (int) gd.getNextNumber();
		padBottom = (int) gd.getNextNumber();
		offset = 	 (int) gd.getNextNumber();

		System.err.println ("top,left,right,bottom="+padTop+","+padLeft+","+padRight+","+padBottom);
	
		w += padLeft+padRight;
		h += padTop+padBottom;
		float[][] pad_img = new float[w][h];
		float[][] seg_img = new float[3][3];

		if (h <= 0 || w <= 0)
			{
			IJ.error ("Resulting image would have w="+w+", h="+h);
			return;
			}
	
		int d = img.getBitDepth();
		if (d == 32)
			pad_img = padFloat ((FloatProcessor)ip);
		else
			pad_img = pad (ip);

		seg_img = segment(pad_img, w, h);
		}

	protected float[][] padFloat (FloatProcessor ip)
		{
		int oldh = ip.getHeight();
		int oldw = ip.getWidth();
		int neww = oldw+padLeft+padRight;
		int newh = oldh+padTop+padBottom;
	
		float[][] padded = new float[neww][newh];
		float[][] orig = ip.getFloatArray();
	
		for (int j=0; j < oldh; j++)
			{
			int jj = j+padTop;
			if (jj >= 0 && jj < newh)
				{
				for (int i=0; i < oldw; i++)
					{
					int ii = i+padLeft;
					if (ii >= 0 && ii <= neww)
						padded[ii][jj] = orig[i][j] - offset;
					}
				}
			}
		//SECOND LOOPSET
		for (int y = 0; y < oldw; y++){
			padded[y+1][0] = orig[y][0] - offset;
			padded[y+1][newh-1] = orig[y][oldh-1] - offset;
		}
		//THIRD LOOPSET
		for (int x = 0; x < oldh; x++){
			padded[0][x+1] = orig[0][x] - offset;
			padded[neww-1][x+1] = orig[oldw-1][x] - offset;
		}
		padded[0][0] = orig[0][0] - offset;
		padded[neww-1][0] = orig[oldw-1][0] - offset;
		padded[0][newh-1] = orig[0][oldh-1] - offset;
		padded[neww-1][newh-1] = orig[oldw-1][oldh-1] - offset;
		
		FloatProcessor p = new FloatProcessor (padded);
		ImagePlus im = new ImagePlus ("Padded "+img.getShortTitle(), p);
		im.show();
		return padded;
		}

	protected float[][] pad (ImageProcessor ip)
		{
		int oldw = ip.getWidth();
		int oldh = ip.getHeight();
		int neww = oldw+padLeft+padRight;
		int newh = oldh+padTop+padBottom;

		ImageProcessor p;
		if (ip instanceof ShortProcessor)
			p = new ShortProcessor (neww, newh);
		else if (ip instanceof ByteProcessor)
			p = new ByteProcessor (neww, newh);
		else
			p = new ColorProcessor (neww, newh);
		
		float[][] padded = new float[newh][neww];
		float[][] orig = ip.getFloatArray();

		for (int j=0; j < oldh; j++)
			{
			int jj = j+padTop;
			if (jj >= 0 && jj < newh)
				{
				for (int i=0; i < oldw; i++)
					{
					int ii = i+padLeft;
					if (ii >= 0 && ii <= neww)
						p.putPixel (ii,jj,ip.getPixel(i,j) - offset);
					}
				}
			}
		
		//SECOND LOOPSET
		for (int y = 0; y < oldw; y++){
			p.putPixel (y+1,0,ip.getPixel(y,0) - offset);
			p.putPixel (y+1,newh-1,ip.getPixel(y,oldh-1) - offset);}

		//THIRD LOOPSET
		for (int x = 0; x < oldh; x++){
			p.putPixel (0,x+1,ip.getPixel(0,x) - offset);
			p.putPixel (neww-1,x+1,ip.getPixel(oldw-1,x) - offset);}
		
		p.putPixel (0,0,ip.getPixel(0,0) - offset);
		p.putPixel (neww-1,0,ip.getPixel(oldw-1,0) - offset);
		p.putPixel (0,newh-1,ip.getPixel(0,oldh-1) - offset);
		p.putPixel (neww-1,newh-1,ip.getPixel(oldw-1,oldh-1) - offset);

		ImagePlus im = new ImagePlus ("Padded "+img.getShortTitle(), p);
		im.show();
		return padded;
		}

	protected float[][] segment(float[][] segpad, int w, int h)
		{
		float[][] temp = new float[3][3];
		for (int x = 0; x < 3 ; x++){
			for (int y = 0; y < 3 ; y++){
				temp[x][y] = segpad[x][y];
			}
		}
		/********************************************************************
		 * This would be the section that would have the function returning a 
		 * 3X3 matric.
		 * This indicates the segments of the image.
		 * A trial 3 X 3 Matrix is passed earlier in the section
		 * 
		 * for (int j=0; j < h; j++)
		 * {
		 * for (int i=0; i < w; i++)
		 * {
		 * if (j + 2 < h && i + 2 < w){
		 * } 
		 * }
		 * }
		*********************************************************************/
		return temp;
		} 

	void showAbout(){
		IJ.showMessage("About NCS_", 
		"This sample plugin is use to implement the Noise Correction Algorithm\n" +
		"This plugin subtracts the offset, pads the image, segments\n" +
		"into 3 X 3 regions and applies the noise reduction algorithm.");}
	}