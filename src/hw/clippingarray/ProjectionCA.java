package hw.clippingarray;
import java.util.Properties;

import ij.ImagePlus;
import ij.io.FileInfo;
import ij.plugin.ZProjector;

public class ProjectionCA{

	public ImagePlus imp = null;
	public FileInfo info = null;
	public Properties pro = null;

	public ProjectionCA(ImagePlus p){		
		imp = p;
		info = imp.getOriginalFileInfo();
		pro = imp.getProperties();
		//pro_key = pro.keys(); //無い場合エラー出るのでとりあえずコメントアウト
		
	}
	
	public ImagePlus projectionHyper(int option){ //projection画像を作るだけ
		ImagePlus pro_max = null;
		
	    int nZ = imp.getNSlices();
	    
		ZProjector zp = new ZProjector(imp);
	    //int projMethod = 1; //0:ave, 1:max, 2:min, 3:sum, 4:sd, 5:median
	    int projMethod = option;
	    
	    zp.setStartSlice(1);
	    zp.setStopSlice(nZ);
	    zp.setMethod(projMethod);
	    zp.doHyperStackProjection(true);
	    pro_max = zp.getProjection();
	    pro_max.setFileInfo(info);

/* プロパティおよびファイルインフォを新しいのにセットしたい	    
	    for(int i = 0; i < pro.size(); i++){
	    	Object p_key = pro_key.nextElement();
	    	String p_key = String.valueOf();
	    	pro_max.setProperty(p_key, pro.get);
	    	
	    }
*/
	    
	    return pro_max;
	}

	
}