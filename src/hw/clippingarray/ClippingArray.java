package hw.clippingarray;


import java.awt.*;
import java.util.stream.Stream;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.*;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;
import ij.process.LUT;

public class ClippingArray{
	ImagePlus imp;

	int z_position;
	int start_position;
	int end_position;
	int extention_value;
	
	/// image property ///
	int nT;
	int nC;
	int nZ;
	String file_name;
	
	Roi roi;
	
	String v_or_h;

	// projection method //
	int projection_method_id = 0; // 0 - 3
	
	// checkbox //
	boolean projection = false;
	boolean extention = true;
	
	
	
	public ClippingArray(ImagePlus img) {		
		imp = img;
	
		getProperty();
	}
	
	
	static boolean checkImage(){
		// 現在のイメージの取得
		ImagePlus check_img;
		boolean check = true;
		check_img = WindowManager.getCurrentImage();
		if (check_img == null) {
			check = false;
		}
		check_img.close();
		return check;
	}
	
	public void getProperty(){
		//元画像からの情報を取り出すためのmethod
		nT = imp.getNFrames();
		nC = imp.getNChannels();
		nZ = imp.getNSlices();
		
		if(imp.getOriginalFileInfo() != null){
			file_name = imp.getOriginalFileInfo().fileName;
		}else{
			file_name = imp.getTitle();
		}
	}
	
	public void setProperty(ImagePlus new_img){ //使う用途があるか？
		//元画像からの情報を新画像へ反映させるためのメソッド
		
	}
	
	
	public static void toHyperStack(ImagePlus img){
		if(img.isHyperStack() == false){
			IJ.run(img, "Stack to Hyperstack...", "");
		}
	}
	

	
	public boolean checkROI(){
		boolean check = false;
		
		roi = imp.getRoi();
		if(roi != null){
			check = true;
		}
		
		return check;
	}
	
	public void setDirection(String direction){
		v_or_h = direction; // "Vertical" or "Horizon"
	}
	
	public void makeClippingArray(ImagePlus roi_img){
		ImagePlus calc_img;
		ImagePlus extract_img;
		extract_img = extractZofT(roi_img, z_position);
		int extract_img_nT = extract_img.getNFrames();
		
		calc_img = extract_img;
				
		ImagePlus[] sep_ch_img = ChannelSplitter.split(calc_img);
		
		if(projection == true){
			ImagePlus[] sep_projection_img = new ImagePlus[nC];
			sep_projection_img = makeProjection(sep_ch_img, v_or_h);
			sep_ch_img = sep_projection_img;
		}

		ImagePlus[] sep_ch_montage = new ImagePlus[nC];		
		MontageMaker mm = new MontageMaker();
		
		int colums = 1;
		int rows = extract_img_nT;
		double scale = 1.0;
		int first = 1;
		int last = extract_img_nT;
		int inc = 1; //increment
		int borderWidth = 0;
		boolean labels = false;
		
		if(v_or_h == "Horizon"){
			colums = extract_img_nT;
			rows = 1;
		}

		for(int n = 0; n < nC; n++){
			ImagePlus buff_img = sep_ch_img[n];
			sep_ch_montage[n] = mm.makeMontage2(buff_img,colums,rows,scale,first,last,inc,borderWidth,labels);
		}

	
		if(nC == 1){
			ImagePlus result_img;
			result_img = sep_ch_montage[0];
			LUT lut = imp.getProcessor().getLut();

			if(( projection == true) && (extention == true)){
				resizeImage(result_img);
			}
			result_img.getProcessor().setLut(lut);
			result_img.setTitle("ClippingArray_" + file_name);
			
			result_img.show();
			
		}else if(imp.isComposite()){
			CompositeImage result_img;
			//CompositeImage ci_imp = (CompositeImage)imp;

			result_img = (CompositeImage)RGBStackMerge.mergeChannels(sep_ch_montage, true);
			if((projection == true) && (extention == true)){
				resizeImage(result_img);
			}
			
			//result_img.setMode(ci_imp.getMode());
			result_img.setTitle("ClippinArray" + file_name);			
			setLut(result_img);
			setMode(result_img);
			result_img.show();
		}else {
			ImagePlus result_img;
			result_img = RGBStackMerge.mergeChannels(sep_ch_montage, true);
			if((projection == true) && (extention == true)){
				resizeImage(result_img);
			}
			result_img.setTitle("ClippinArray" + file_name);			
			result_img.show();

		}
		
	}

	public void setLut(ImagePlus img){
		LUT lut[] = imp.getLuts();
		for(int c = 0; c < nC; c++){
			img.setC(c);
			img.getProcessor().setLut(lut[c]);
		}
		img.setC(1);
		
	}
	
	public void setMode(CompositeImage img){
		CompositeImage ci_img = (CompositeImage)imp;
		img.setMode(ci_img.getMode());
	}
	
	public ImagePlus extractZofT(ImagePlus img, int z_position){
		ImagePlus result_img;
		result_img = new ImagePlus();
		String cString = "1-" + nC;
		String zString = String.valueOf(z_position) + "-" + String.valueOf(z_position);
		String tString = String.valueOf(start_position) + "-" + String.valueOf(end_position);
		result_img = SubHyperstackMaker.makeSubhyperstack(img, cString, zString, tString);
		
		return result_img;
	}
	
	
	public ImagePlus[] makeProjection(ImagePlus[] sep_img, String option){
		ImagePlus[] result_img = new ImagePlus[nC];
		ImageStack[] buff_stack = new ImageStack[nC];
		int t_length = sep_img[0].getNFrames();
		
		for(int c = 0; c < nC; c++){
			if(option == "Vertical"){
				buff_stack[c] = new ImageStack(sep_img[c].getWidth(), 1);
				
			}else if(option == "Horizon"){
				buff_stack[c] = new ImageStack(1, sep_img[c].getHeight());

			}
		}
		

		ImagePlus[][] sep_img_projection = new ImagePlus[t_length][nC];
		ImagePlus[][] sep_img_line = new ImagePlus[t_length][nC];
		sep_img_line = convertLineStack(sep_img, v_or_h);

		ProjectionCA pkg;

		
		for(int t = 0; t < t_length; t++){
			for(int c = 0; c < nC; c++){

				pkg = new ProjectionCA(sep_img_line[t][c]);
				sep_img_projection[t][c] = pkg.projectionHyper(projection_method_id);

				buff_stack[c].addSlice(sep_img_projection[t][c].getProcessor());
			}			
		}
		
		for(int c = 0; c < nC; c++){
			ImagePlus buff_img = new ImagePlus();
			buff_img.setStack(buff_stack[c]);
			result_img[c] = buff_img;
		}
		return result_img;
	}
	
	private void resizeImage(ImagePlus img){
		//long start = System.currentTimeMillis();
		int ext_value = extention_value;
		int nSize = img.getStackSize();
		int width = img.getWidth();
		int height = img.getHeight();
		

		ImagePlus buff_img;
		buff_img = new ImagePlus();		
		
		if(v_or_h == "Vertical"){
			height = (height * ext_value);
			ImageStack stack_img = new ImageStack(width, height);

			//for(int i = 0; i < nSize; i++){
			//	ImageProcessor buff = img.getStack().getProcessor(i+1).resize(width, height, true);
			//	gb_filter.blurGaussian(buff, 0, ( ext_value * 0.4), 0.02);
			//	stack_img.addSlice(buff);
			//}
			//buff_img.setStack(stack_img);
			
			//ラムダ式にチャレンジ。速度差がわからず、、、どちらも3-4ms。参考までにこのまま残す。
			final int f_width = width;
			final int f_height = height;
			ImageProcessor[] ip_array = new ImageProcessor[nSize];
			for(int i = 0; i < nSize; i++){
				ip_array[i] = img.getStack().getProcessor(i+1);
			}
			Stream<ImageProcessor> stream = Stream.of(ip_array);
			stream.parallel().forEach(ip -> stack_img
						.addSlice(makeProjectionGauss((ip.resize(f_width, f_height, true)), ext_value)
							)
							);
			buff_img.setStack(stack_img);
			
		}else if(v_or_h == "Horizon"){
			width = (width * ext_value);
			ImageStack stack_img = new ImageStack(width, height);

			for(int i = 0; i < nSize; i++){
				ImageProcessor buff = img.getStack().getProcessor(i+1).resize(width, height, true);
				gb_filter.blurGaussian(buff, ( ext_value * 0.4), 0, 0.02);
				stack_img.addSlice(buff);
			}
			buff_img.setStack(stack_img);
		}
		
		buff_img.setDimensions(nC, 1, 1);
		
		img.setImage(buff_img);
		//img.setDimensions(nC, 1, 1);

		//long end = System.currentTimeMillis();
		//System.out.println("process time : " + (end - start) + "ms");
	}

	GaussianBlur gb_filter = new GaussianBlur();
	public ImageProcessor makeProjectionGauss(ImageProcessor ip, int ext_value){
		
		if(v_or_h == "Vertical"){
			gb_filter.blurGaussian(ip, 0, ( ext_value * 0.4), 0.02);
		}else if(v_or_h == "Horizon"){
			gb_filter.blurGaussian(ip, ( ext_value * 0.4), 0, 0.02);			
		}
		return ip;
	}
	
	
	public ImagePlus[][] convertLineStack(ImagePlus[] img, String option){ //[ch][width or height]
		ImagePlus[][] result_img = new ImagePlus[nT][nC];
		ImageStack[][] stack_t_img = new ImageStack[nT][nC];
		
		for(int t = 0; t < nT; t++){
			for(int c = 0; c < nC; c++){
				result_img[t][c] = new ImagePlus();
				stack_t_img[t][c] = new ImageStack();
			}
		}
		
		int height = img[0].getHeight();
		int width = img[0].getWidth();

		if(option == "Vertical"){
			
			for(int t = 0; t < nT; t++){
				for(int c = 0; c < nC; c++){
					stack_t_img[t][c] = new ImageStack(width, 1);
				}
			}

			Roi r;

			for(int c = 0; c < nC; c++){
				for(int y = 0; y < height; y++){
					r = new Roi(0, y, width, 1);
					img[c].setRoi(r);
					//ImagePlus d_img = img[c].duplicate();
					ImagePlus d_img = new Duplicator().run(img[c]);//1.52n対策
					//result_img[c][y] = d_img;
					for(int t = 0; t < nT; t++){
						d_img.setT(t);
						//System.out.println("ch,y,t = " +  c + ","+ y + "," + t + ":" + d_img.getProcessor().get(0,0));
						stack_t_img[t][c].addSlice(d_img.getProcessor());
					}
				}
			}
			
		}else if(option == "Horizon"){
			
			for(int t = 0; t < nT; t++){
				for(int c = 0; c < nC; c++){
					stack_t_img[t][c] = new ImageStack(1, height);
				}
			}
						
			Roi r;

			for(int c = 0; c < nC; c++){
				for(int x = 0; x < width; x++){
					r = new Roi(x, 0, 1, height);
					img[c].setRoi(r);
					//ImagePlus d_img = img[c].duplicate();
					ImagePlus d_img = new Duplicator().run(img[c]);
					//result_img[c][y] = d_img;
					for(int t = 0; t < nT; t++){
						d_img.setT(t);
						//System.out.println("ch,y,t = " +  c + ","+ y + "," + t + ":" + d_img.getProcessor().get(0,0));
						stack_t_img[t][c].addSlice(d_img.getProcessor());
					}
				}
			}
			
		}
		
		
		// ImageStack to ImagePlus
		for(int t = 0; t < nT; t++){
			for(int c = 0; c < nC; c++){
				ImagePlus buff_img = new ImagePlus();
				buff_img.setStack(stack_t_img[t][c]);
				result_img[t][c] = buff_img;
			}
		}
		
		return result_img;
	}
	
	public void setStart(int s){
		start_position = s;
	}
	
	public void setEnd(int e){
		end_position = e;
	}
	
	public void setZposition(int z){
		z_position = z;
	}
	
	
	public void setProjectionMethod(int m){
		projection_method_id = m;
	}
	
	public void setExtentionValue(int v){
		extention_value = v;
	}
	
	public void projection(boolean b){
		projection = b;
	}
	
	public void extention(boolean b){
		extention = b;
	}
	
	public int getNFrames(){
		return nT;
	}
	
	public int getNChannels(){
		return nC;
	}
	
	public int getNSlices(){
		return nZ;
	}

	public String getFileName(){
		return file_name;
	}
	
}