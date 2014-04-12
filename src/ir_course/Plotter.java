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
import org.apache.lucene.document.*;

public class Plotter {
	private String basedir ="";
	
	private float[] points;
	private int count;

	private static class Point {
		public double x,y;
		
		public Point(double x, double y) {
			this.x = x;
			this.y = y;
		}
		
	}
	

    public Plotter(String basedir) {
    	this.basedir = basedir;
    	this.count = 0;
    	this.points = new float[11];
    }

    
    /* write pr-curve as to basedir/PlotTitle.tex */
    public boolean PlotListToFile(List<Document> docs, String PlotTitle, int totalRecall) {
    	try {
    		FileWriter fw = new FileWriter(PlotTitle+".tex", false);
    		fw.write(PlotListAsString(docs,PlotTitle, totalRecall));
    		fw.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    		return false;
    	}
    	return true;
    }

    /* return pr-curve as string */
    public String PlotListAsString(List<Document> docs, String plotTitle, int totalRecall) {
    	String plotTex = "\\begin{tikzpicture} \n\\begin{axis}[\n\ttitle={";
    	plotTex += plotTitle;
    	plotTex += "},\n\txlabel={Recall}, \n\tylabel={Precision},\n\txmin=0, \n\txmax=1,\n\tymin=0, \n\tymax=1,"
    			+ "\n\txtick={0,0.2,0.4,0.6,0.8,1.0},"
    			+ "\n\tytick={0,0.2,0.4,0.6,0.8,1.0},"
    			+ "\n\tlegend pos=outer north east,]";
    	plotTex += "\n\\addplot[color=blue,mark=*,]"
    			+"\n\tcoordinates {";
    	List<Point> curvepoints = countPR(docs, totalRecall);
    	for(Point p : curvepoints) {
    		plotTex += "("+p.x +","+p.y+") ";
    	}
    	plotTex += "};\n\\end{axis} \n\\end{tikzpicture}";
    	
    	return plotTex;
    }
 
    public String PlotResultsAsString(String plotTitle) {
    	String plotTex = "\\begin{tikzpicture} \n\\begin{axis}[\n\ttitle={";
    	plotTex += plotTitle;
    	plotTex += "},\n\txlabel={Recall}, \n\tylabel={Precision},\n\txmin=0, \n\txmax=1,\n\tymin=0, \n\tymax=1,"
    			+ "\n\txtick={0,0.2,0.4,0.6,0.8,1.0},"
    			+ "\n\tytick={0,0.2,0.4,0.6,0.8,1.0},"
    			+ "\n\tlegend pos=outer north east,]";
    	plotTex += "\n\\addplot[color=blue,mark=*,]"
    			+"\n\tcoordinates {";
    	for(int i=0 ; i<11 ; i++) {
    		plotTex += "("+((float)i/10)+","+(points[i]/count)+") ";
    	}
    	plotTex += "};\n\\end{axis} \n\\end{tikzpicture}";
    	
    	return plotTex;
    }
   
    public String PlotResultsAsStringAddPlotOnly(String color, String mark) {
    	String plotTex = "\n\\addplot[color="+color+",mark="+mark+",]"
    			+"\n\tcoordinates {";
    	for(int i=0 ; i<11 ; i++) {
    		plotTex += "("+((float)i/10)+","+(points[i]/count)+") ";
    	}
    	plotTex += "};\n";
    	
    	return plotTex;
    }   
    
    public void AddListToResults(List<Document> docs, int totalRecall) {
    	List<Point> curvepoints = countPR(docs, totalRecall);
    	for(Point p : curvepoints) {
    		//eg. point x = 0.3 => index 3 in table
    		int i = (int)(p.x*10);
    		this.points[i] += p.y;
    		i++;
    	}
    	this.count++;
    	
    }
    
    private List<Point> countPR(List<Document> docs, int totalRecall) {
    	List<Point> curvepoints = new LinkedList<Point>();
    	for(int i=0; i<11; i++) {
    		curvepoints.add(new Point((double)i/10, 0.0));
    	}
    	
    	int relevantTotal = 0;
    	int[] relevantCount = new int[docs.size()];
    	
    	for(int i = 0; i< docs.size(); i++) {
    		Document d = docs.get(i);
    		if(d.get(LuceneSearchApp.RELEVANT).toString().equalsIgnoreCase("true")) {
    			relevantTotal++;
    			relevantCount[i] = relevantTotal;
    		}
    	}
    	
//    	System.out.println("totalrelevant: " +relevantTotal);
    	
    	Iterator<Point> pointIterator = curvepoints.iterator();
    	Point nextPoint = pointIterator.next();
    	
    	nextPoint.y = 1.0;
    	nextPoint = pointIterator.next();
    	for(int i=1; i < relevantCount.length; i++) {
    		if((double)relevantCount[i]/totalRecall >= nextPoint.x) {
    			nextPoint.y = ((double)relevantCount[i]/(i+1));
    			if(pointIterator.hasNext()) {
    				nextPoint = pointIterator.next();
    			}
    		}
    	}
    	List<Point> extraPoints = new LinkedList<Point>();
    	if(nextPoint.y==0.0) {
    		extraPoints.add(nextPoint);
    	}
    	while(pointIterator.hasNext()) {
    		extraPoints.add(pointIterator.next());
    	}
    	curvepoints.removeAll(extraPoints);
    	
    	return curvepoints;
    }
}
