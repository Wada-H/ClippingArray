import hw.clippingarray.*;
import ij.*;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.plugin.Duplicator;
import ij.plugin.HyperStackConverter;
import ij.plugin.Straightener;
import ij.plugin.frame.PlugInFrame;
import ij.process.ImageProcessor;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.stream.IntStream;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;


// ClippingArray ver 1.0	20151207 - Wada Housei
//	とりあえず目的の動作を達成
//
//	このpluginはROIで囲んだ部分を切り取り、任意の方向へ並べた画像を作成する。
//	また、ROI部分をprojectionした画像を任意の幅で拡張し並べた画像も作成可能。

// ClippingArray ver 1.1	20151208 - Wada Housei
//	リファクタリング: ClippingArray classを作成し、別ファイルへ。
// ClippingArray ver 1.2 20151211 - Wada Housei
//	Rotate機能を追加 : Line　Roiを画像に引っ張ってRotate　Buttonを押す。
//	一部ラムダ式を採用。個人的な練習による。
// 20190405 ImageJ 1.52n以降におけるduplicate()の仕様変更に対する修正
// 20190905 Lineで選択した部分で作る仕様を追加

public class ClippingArray_ extends PlugInFrame implements ImageListener,WindowListener, MouseListener {


	ImagePlus imp;
	ImagePlus rotate_buff_img;
	ImagePlus check_img;
	
	ImageCanvas ic;
	
	/// image property ///
	int nT;
	int nC;
	int nZ;
	String file_name;
	
	Roi roi;
	
	int s_position;
	int e_position;
	
	double angle;
	
	ClippingArray clipping_array;
	
	/// for panel //
	
	String[] method_name = {"Ave","Max","Min","Sum"};
		
	JPanel gd_panel;
	
	JPanel projection_panel;
	JCheckBox projection_checkbox;
	
	JPanel pro_method_panel;	
	JLabel pro_method_label;
	JComboBox<String> pro_method_combo;	
	
	JPanel pro_extention_panel;
	JCheckBox pro_extention_checkbox;
	JTextField extention_value_tfield;
	
	JPanel s_and_e_panel;
	JLabel start_label;
	JLabel end_label;
	JTextField s_position_tfield;
	JTextField e_position_tfield;
	
	JPanel vh_panel;
	ButtonGroup vh_group;
	JRadioButton vertical_radio;
	JRadioButton horizon_radio;
	
	JPanel button_panel;
	JButton make_button;
	JButton cancel_button;
	
	JPanel rotate_panel;
	JButton rotate_button;
	
	////////////////
	
	
	
	
	public ClippingArray_() {
		super("ClippingArray_ver_1.3(20190905)");
				
		if(!checkImage()){
			IJ.noImage();
			return;
		}

		check_img = WindowManager.getCurrentImage();
		ClippingArray.toHyperStack(check_img);

		getProperty(); // impをハイパースタック画像からよむ
		makePanel();
		setListener();
		
		clipping_array = new ClippingArray(imp);		
		clipping_array.getProperty();


		
	}

	
	private boolean checkImage(){
		// 現在のイメージの取得
		boolean check = true;
		check_img = WindowManager.getCurrentImage();
		if (check_img == null) {
			check = false;
		}
		
		return check;
	}
	
	private void getProperty(){
		//元画像からの情報を取り出すためのmethod
		imp = WindowManager.getCurrentImage();
		nT = imp.getNFrames();
		nC = imp.getNChannels();
		nZ = imp.getNSlices();
		
		if(imp.getOriginalFileInfo() != null){
			file_name = imp.getOriginalFileInfo().fileName;
		}else{
			file_name = imp.getTitle();
		}
	}

	
	private void makePanel(){
		FlowLayout gd_layout = new FlowLayout();
		
		gd_panel = new JPanel(gd_layout);
		gd_panel.setPreferredSize(new Dimension(280, 200));

		// projection panel //
		projection_panel = new JPanel((new GridLayout(3, 1)));
		projection_checkbox = new JCheckBox("Projection");
		
		
		pro_method_panel = new JPanel((new GridLayout(1, 2)));

		pro_method_label = new JLabel("Method:");
		pro_method_combo = new JComboBox<String>(method_name);
		pro_method_panel.add(pro_method_label);
		pro_method_panel.add(pro_method_combo);
		pro_method_label.setEnabled(false);
		pro_method_combo.setEnabled(false);
		
		pro_extention_panel = new JPanel((new GridLayout(1, 3)));

		pro_extention_checkbox = new JCheckBox("Extention:x",true);
		extention_value_tfield = new JTextField("5");
		pro_extention_panel.add(pro_extention_checkbox);
		pro_extention_panel.add(extention_value_tfield);
		pro_extention_checkbox.setEnabled(false);
		extention_value_tfield.setEditable(false);
		

		projection_panel.add(projection_checkbox);
		projection_panel.add(pro_method_panel);
		projection_panel.add(pro_extention_panel);
		
		
		// start end panel //
		s_and_e_panel = new JPanel((new GridLayout(1, 4)));
		start_label = new JLabel("Start:");
		end_label = new JLabel("End:");
		s_position_tfield = new JTextField("1");
		e_position_tfield = new JTextField(String.valueOf(nT));

	

		s_and_e_panel.add(start_label);
		s_and_e_panel.add(s_position_tfield);
		s_and_e_panel.add(end_label);
		s_and_e_panel.add(e_position_tfield);

		
		// v or h panel //
		vh_panel = new JPanel((new GridLayout(1, 2)));
		vertical_radio = new JRadioButton("Vertical",true);
		horizon_radio = new JRadioButton("Horizon");

		vh_group = new ButtonGroup();
		vh_group.add(vertical_radio);
		vh_group.add(horizon_radio);
		
		vh_panel.add(vertical_radio);
		vh_panel.add(horizon_radio);
		
		// button panel //
		button_panel = new JPanel((new GridLayout(1, 2)));
		//cancel_button = new JButton("Cancel");
		rotate_button = new JButton("Rotate");
		make_button = new JButton("Make");
		
		//button_panel.add(cancel_button);
		button_panel.add(rotate_button);
		button_panel.add(make_button);
		

		gd_panel.add(projection_panel);
		gd_panel.add(s_and_e_panel);
		gd_panel.add(vh_panel);
		gd_panel.add(button_panel);
		
		///////////////////////////////////

		this.add(gd_panel);
		this.pack(); // 推奨サイズのｗindow

		Point imp_point = imp.getWindow().getLocation();
		int imp_window_width = imp.getWindow().getWidth();
		// int imp_window_height = imp.getWindow().getHeight();

		double set_x_point = imp_point.getX() + imp_window_width;
		double set_y_point = imp_point.getY();

		this.setLocation((int) set_x_point, (int) set_y_point);

		this.setVisible(true);// thisの表示
	}
	
	private void setListener(){
		projection_checkbox.addMouseListener(this);
		
		//cancel_button.addMouseListener(this);
		make_button.addMouseListener(this);
		rotate_button.addMouseListener(this);
		
		vertical_radio.addMouseListener(this);
		horizon_radio.addMouseListener(this);
		
		ic = imp.getCanvas();
		ic.addMouseListener(this);
		
		
	}
	
	private void check_projection_box(){
		if(projection_checkbox.isSelected()){
			pro_method_label.setEnabled(true);
			pro_method_combo.setEnabled(true);
			pro_extention_checkbox.setEnabled(true);
			extention_value_tfield.setEnabled(true);
			extention_value_tfield.setEditable(true);
		}else{
			pro_method_label.setEnabled(false);
			pro_method_combo.setEnabled(false);
			pro_extention_checkbox.setEnabled(false);
			extention_value_tfield.setEnabled(false);
			extention_value_tfield.setEditable(false);
		}
	}
	
	private boolean checkROI(){
		boolean check = false;
		
		roi = imp.getRoi();
		if(roi != null){
			check = true;
		}
		
		return check;
	}
	

	
	private String getSelectedRadio(ButtonGroup bg){
		String result_stg;
		result_stg = "vertical";
		Enumeration<AbstractButton> elements = vh_group.getElements();
		
		while(elements.hasMoreElements()) {
            AbstractButton btn = (AbstractButton) elements.nextElement();
            if(btn.isSelected()) {
            	result_stg = btn.getText();
            	break;
            }
      }
		
		return result_stg;
	}
	
	private boolean checkField(){
		boolean check = true;
		
		//int ext_value = Integer.valueOf(extention_value_tfield.getText()).intValue();
		 s_position = Integer.valueOf(s_position_tfield.getText()).intValue();
		 e_position = Integer.valueOf(e_position_tfield.getText()).intValue();
		

		if((s_position > nT)|(e_position > nT)){
			IJ.showMessage("Start or End < Max of T ");
			check = false;
		}else if((s_position < 1)|(e_position < 1)){
			IJ.showMessage("Start or End > 1");
			check = false;
		}else if(s_position > e_position){
			IJ.showMessage("Start <= End");
			check = false;
		//}else if(ext_value >= 100){ //この規制はいるか？
		//	IJ.showMessage("Extention value < 100");
		//	check = false;
		}
		
		return check;
	}
	
	private void checkRoi(){
		if(imp.getRoi() != null){
			String v_or_h = getSelectedRadio(vh_group);
			Roi r = imp.getRoi();
			
			int roi_width = (int)r.getFloatWidth();
			int roi_height = (int)r.getFloatHeight();
			
			if(v_or_h == "Vertical"){
				extention_value_tfield.setText(String.valueOf(roi_height));
			}else if(v_or_h == "Horizon"){
				extention_value_tfield.setText(String.valueOf(roi_width));				
			}
		}		
	}
	
	private void setClippingArray(){
		clipping_array.setProjectionMethod(pro_method_combo.getSelectedIndex());
		clipping_array.projection(projection_checkbox.isSelected());
		clipping_array.extention(pro_extention_checkbox.isSelected());
		clipping_array.setExtentionValue(Integer.valueOf(extention_value_tfield.getText()).intValue());
		clipping_array.setDirection(getSelectedRadio(vh_group));
		clipping_array.setStart(s_position);
		clipping_array.setEnd(e_position);
		clipping_array.setZposition(imp.getZ());
	}

	public ImagePlus getStraightenImage(){

		int c = imp.getNChannels();
		int z = imp.getNSlices();
		int t = imp.getNFrames();


		if(roi.getStrokeWidth() < 1){
			roi.setStrokeWidth(1);
		}
		int width = Math.round(roi.getStrokeWidth());

		ArrayList<ImagePlus> impList = new ArrayList<>(imp.getStackSize());

		for(int i = 0; i < imp.getStackSize(); i++){
			impList.add(new ImagePlus());
		}
		Straightener straightener = new Straightener();

		IntStream intStream  = IntStream.range(0, imp.getStackSize());
		intStream.parallel().forEach(i ->{
			ImageProcessor ip = imp.getStack().getProcessor(i+1).duplicate();
			ImagePlus buffImage = new ImagePlus();
			buffImage.setProcessor(ip);
			buffImage.setRoi(roi);
			ImageProcessor buffProcessor = straightener.straighten(buffImage, roi, width);
			buffProcessor.setMinAndMax(0, imp.getDisplayRangeMax());
			//ImagePlus streachedImage = new ImagePlus();
			//streachedImage.setProcessor(String.valueOf(i+1), buffProcessor);
			//impList.add(i, streachedImage);
			impList.get(i).setProcessor(buffProcessor);
		});


		ImageStack imageStack = new ImageStack(impList.get(0).getWidth(), impList.get(0).getHeight());
		for(int i = 0; i < impList.size(); i++){
			imageStack.addSlice(impList.get(i).getProcessor());
		}
		ImagePlus preresultImage = new ImagePlus();
		preresultImage.setStack("StretchedImage", imageStack);
		ImagePlus resultImage = new ImagePlus();
		if (c > 1) {
			resultImage = HyperStackConverter.toHyperStack(preresultImage, c, z, t, "xyczt", "grayscale");

		}else{
			resultImage = preresultImage;
		}


		return resultImage;
	}

	
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseReleased(MouseEvent e) {
		

			
		if(e.getSource() == projection_checkbox){
			check_projection_box();
		
		}else if(e.getSource() == make_button){
			if(!checkField()){
				return;
			}	
			
			if(checkROI()){
				if(roi.isLine() == false) {
					setClippingArray();
					//clipping_array.makeClippingArray(imp.duplicate());
					clipping_array.makeClippingArray(new Duplicator().run(imp));
				}else if(roi.isLine() == true){
					setClippingArray();

					ImagePlus straightenImage = this.getStraightenImage();
					straightenImage.setC(imp.getC());
					straightenImage.setZ(imp.getZ());
					straightenImage.setT(imp.getT());
					clipping_array.makeClippingArray(straightenImage);
				}else{
					IJ.showMessage("Please select ROI");
				}
			}else{
				IJ.showMessage("Please select ROI");
			}
		
		}else if(e.getSource() == cancel_button){
			this.close();
		
		}else if(e.getSource() == ic){
			checkRoi();

		}else if(e.getSource() == vertical_radio){
			checkRoi();

		}else if(e.getSource() == horizon_radio){
			checkRoi();
		}else if(e.getSource() == rotate_button){
			if(checkROI()){
				Roi l = imp.getRoi();
				if(l.isLine() == true){
					if(rotate_buff_img == null){
						angle = l.getAngle();
						imp.deleteRoi();
						//rotate_buff_img = imp.duplicate(); //元画像の保持
						rotate_buff_img = new Duplicator().run(imp);
						imp.setRoi(l);
					}else{
						angle = angle + l.getAngle();
						//imp.setImage(rotate_buff_img.duplicate());
						imp.setImage(new Duplicator().run(rotate_buff_img));
					}
									
					RotateImage rt = new RotateImage(imp);
					rt.setAngle(angle);
					rt.setSeries(true);
					rt.rotate();
					imp.updateAndDraw();
				}else{
					IJ.showMessage("Please select Line ROI");
				}
			}else{
				IJ.showMessage("Please select Line ROI");

			}
		}
		

		
	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void imageOpened(ImagePlus imp) {
		// TODO Auto-generated method stub
		
	}

	public void imageClosed(ImagePlus imp) {
		// TODO Auto-generated method stub
		
	}

	public void imageUpdated(ImagePlus imp) {
		// TODO Auto-generated method stub
		
	}

	
}