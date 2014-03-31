/*
 * Class for drawing precision-recall curves in LaTex-format
 * Created on 2014-03-31
 * Leo Holsti <leo.holsti@aalto.fi>
 */
package ir_course;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Plotter {
	private String basedir ="";

	private static class Point {
		public double x,y;
		
		public Point(double x, double y) {
			this.x = x;
			this.y = y;
		}
		
	}
	

    public Plotter(String basedir) {
    	this.basedir = basedir;
    }

    
    /* write pr-curve as to basedir/PlotTitle.tex */
    public boolean PlotListToFile(List<DocumentInCollection> docs, String PlotTitle) {
    	try {
    		FileWriter fw = new FileWriter(PlotTitle+".tex", false);
    		fw.write(PlotListAsString(docs,PlotTitle));
    		fw.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    		return false;
    	}
    	return true;
    }

    /* return pr-curve as string */
    public String PlotListAsString(List<DocumentInCollection> docs, String plotTitle) {
    	String plotTex = "\\begin{tikzpicture} \n\\begin{axis}[\n\ttitle={";
    	plotTex += plotTitle;
    	plotTex += "},\n\txlabel={Recall}, \n\tylabel={Precision},\n\txmin=0, \n\txmax=1,\n\tymin=0, \n\tymax=1,"
    			+ "\n\txtick={0,0.2,0.4,0.6,0.8,1.0},"
    			+ "\n\tytick={0,0.2,0.4,0.6,0.8,1.0},"
    			+ "\n\tlegend pos=outer north east,]";
    	plotTex += "\n\\addplot["
    			+"\n\tcoordinates {";
    	List<Point> curvepoints = countPR(docs);
    	for(Point p : curvepoints) {
    		plotTex += "C"+p.x +","+p.y+")";
    	}
    	plotTex += "\nend{axis} \nend{tikzpicture}";
    	
    	return plotTex;
    }
    
    private List<Point> countPR(List<DocumentInCollection> docs) {
    	List<Point> curvepoints = new LinkedList<Point>();
    	for(int i=0; i<10; i++) {
    		curvepoints.add(new Point((double)i/10, 0.0));
    	}
    	
    	int relevantTotal = 0;
    	int[] relevantCount = new int[docs.size()];
    	
    	for(int i = 0; i< docs.size(); i++) {
    		DocumentInCollection d = docs.get(i);
    		if(d.isRelevant() == true && d.getSearchTaskNumber() == 4) {
    			relevantTotal++;
    			relevantCount[i] = relevantTotal;
    		}
    	}
    	
    	Iterator<Point> pointIterator = curvepoints.iterator();
    	Point nextPoint = pointIterator.next();
    	
    	nextPoint.y = 1.0;
    	nextPoint = pointIterator.next();
    	for(int i=1; i < relevantCount.length; i++) {
    		if(relevantCount[i]/relevantTotal >= nextPoint.x) {
    			nextPoint.y = (relevantCount[i]/i);
    			nextPoint = pointIterator.next();
    		}
    	}
    	
    	return curvepoints;
    }
}
