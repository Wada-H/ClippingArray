package hw.clippingarray;

import java.util.stream.Stream;

import ij.ImagePlus;
import ij.gui.Line;
import ij.process.ImageProcessor;

public class RotateImage{
	
	ImagePlus imp = null;
	boolean all_series = true;
	
	int stack_size = 1;
	int width;
	int height;
	
	double angle = 0.0;
	
	public RotateImage(ImagePlus img){
		imp = img;
		setProperty(imp);
			
	}

	
	public void setImage(ImagePlus img){
		imp = img;
		setProperty(imp);

	}
	
	public void setSeries(boolean b){
		all_series = b;
	}
	
	public void setLineAngle(Line l){		
		angle = l.getAngle();		
	}
	
	public void setAngle(double a){
		angle = a;
	}
	
	private void setProperty(ImagePlus img){
		stack_size = img.getStackSize();
		width = img.getWidth();
		height = img.getHeight();
	}
	
	public void rotate(ImageProcessor ip){
		ip.setInterpolationMethod(ImageProcessor.BICUBIC);
		ip.setBackgroundValue(0.0);
		ip.rotate(angle);
	}
	
	public void rotate(){

		if(all_series){
			
			ImageProcessor[] ip_array = new ImageProcessor[stack_size];
			for(int i = 0; i < stack_size; i++){
				ImageProcessor buff_p = imp.getStack().getProcessor(i+1);
				ip_array[i] = buff_p;
			}
			
			Stream<ImageProcessor> stream = Stream.of(ip_array);
			stream.parallel().forEach(ip -> rotate(ip));
			
		}else{
			for(int i = 0; i < stack_size; i++){
				if((i+1) == imp.getSlice()){
					ImageProcessor buff_p = imp.getStack().getProcessor(i+1);
					rotate(buff_p);
				}
			}
		}

		
		

	}
	

	
}