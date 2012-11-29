package ntut.csie.csdet.report;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.AbstractCategoryItemLabelGenerator;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.TextAnchor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 圖表
 * @author Shiau
 */
public class BarChart {
	public enum LEVEL { PROJECT_LEVEL, PACKAGE_LEVEL, CLASS_LEVEL }
	private static Logger logger = LoggerFactory.getLogger(BarChart.class);

	//Report的資料
	private ReportModel model;
	
	BarChart(ReportModel model) {
		this.model = model;
	}

	/**
	 * 產生圖表
	 */
	public void build() {
		if (model != null) {
			//TODO 使用enum簡化程式

			//建立Project Summary的Smell資訊
	        CategoryDataset dataset = createProjectDataset();
	        //產生Chart
	        JFreeChart chart = createChart(dataset, "Exception Handling Code Smells Chart", "", true);
	        //輸出成JPG
	        outputJPGFile(chart, "Report", 800, 500);
	        //建立Package Level的Smell資訊
	        dataset = createPackageDataset();
	        //產生Chart
	        chart = createChart(dataset, "Packages List", "Package Name",  false);
	        //輸出成JPG
	        outputJPGFile(chart, "PackageReport", 800, 420);

	        for (int i = 0; i < model.getPackagesSize(); i++) {
		        //建立Class Level的Smell資訊
		        dataset = createClassDataset(model.getPackage(i));
		        //產生Chart
		        chart = createChart(dataset, model.getPackage(i).getPackageName(), "Class Name", false);
		        //輸出成JPG
		        outputJPGFile(chart, "ClassReport_" + i, 600, 420);
	        }
		}
	}

	/**
	 * create a dataset
	 */
	private CategoryDataset createProjectDataset() {
		DefaultCategoryDataset data = new DefaultCategoryDataset();
		//新增資料(""是為了設定同一欄位用相同名稱)
		if(model.getTotalSmellCount() == 0){
			return null;
		}
		data.addValue(model.getIgnoreTotalSize() , "", "Ignored checked exception");
		data.addValue(model.getDummyTotalSize(), "", "Dummy handler");
		data.addValue(model.getUnMainTotalSize(), "", "Unprotected main program");
		data.addValue(model.getNestedTryTotalSize(), "", "Nested try statemet");
		data.addValue(model.getCarelessCleanUpTotalSize(), "", "Careless Cleanup");
		data.addValue(model.getOverLoggingTotalSize(), "", "Over Logging");
		data.addValue(model.getOverwrittenTotalSize(), "", "Overwritten lead exception");
		return data;				
	}
	
	/**
	 * create a dataset
	 */
	private CategoryDataset createPackageDataset() {
		DefaultCategoryDataset data = new DefaultCategoryDataset();
		if(model.getTotalSmellCount() == 0){
			return null;
		}
		for (int i=0; i < model.getPackagesSize(); i++) {
			PackageModel packageModel = model.getPackage(i);
			//Package 名稱太常所以使用代碼
			//data.addValue(packageModel.getTotalSmellSize(), "", packageModel.getPackageName());
			//新增資料(""是為了設定同一欄位用相同名稱)
			data.addValue(packageModel.getTotalSmellSize(), "", "P" + String.valueOf(i));
		}
		return data;
	}

	/**
	 * create a dataset
	 */
	private CategoryDataset createClassDataset(PackageModel packageModel) {		
		DefaultCategoryDataset data = new DefaultCategoryDataset();
		if (packageModel.getTotalSmellSize() == 0)
			return null;
		for (int i=0; i < packageModel.getClassSize(); i++) {
			ClassModel classModel = packageModel.getClass(i);
			//新增資料(""是為了設定同一欄位用相同名稱)
			data.addValue(classModel.getTotalSmell(), "", classModel.getClassName());
		}
		return data;
	}

	/**
	 * create a chart and return it
	 */
	private JFreeChart createChart(CategoryDataset dataset, String title, String level, boolean isDifferColor) {
		
		final JFreeChart chart = ChartFactory.createBarChart(
			title,			        	// chart title
			level,						// domain axis label
			"Number of Code Smells",	// range axis label
			dataset,                    // data
			PlotOrientation.VERTICAL,   // the plot orientation
			false,						// legend
			false,                      // tooltips
			false                       // urls
		);

		CategoryPlot plot = chart.getCategoryPlot();
		plot.setNoDataMessage("No data available");
		///Chart的顏色設定///
		chart.setBackgroundPaint(Color.white);
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		//每條Bar的顏色
		CategoryItemRenderer renderer;
		if (isDifferColor) {
			renderer = new CustomRenderer(new Paint[] {	Color.red, Color.blue, Color.green,
														Color.yellow, Color.magenta, Color.cyan,
														Color.pink, Color.YELLOW});
		} else
			renderer = new CustomRenderer(new Paint[] {Color.red});

		renderer.setBaseItemLabelGenerator(new LabelGenerator(null));
		renderer.setBaseItemLabelsVisible(true);
		//調整使圖表間距不出現小數點
		NumberAxis numberaxis = (NumberAxis)plot.getRangeAxis();
		numberaxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		//設定每條Bar上面的字樣
		renderer.setBaseItemLabelFont(new Font("Arial", Font.BOLD, 14));
		renderer.setBasePositiveItemLabelPosition(new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.BASELINE_CENTER));
		plot.setRenderer(renderer);
		
		CategoryAxis domainAxis = plot.getDomainAxis();

//		domainAxis.setMaximumCategoryLabelWidthRatio(25);
//		domainAxis.setMaximumCategoryLabelLines(100);

		//若為Package Level將圖片文字改成直的(Package Level文字使用代碼)
		if (title.equals("Packages List"))
			domainAxis.setCategoryLabelPositions(CategoryLabelPositions.STANDARD);
		else
			domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

		//若要顯示相同顏色，則將Bar與Bar之間的間距縮小
		if (isDifferColor)
			domainAxis.setCategoryMargin(0.5f);
		else
			domainAxis.setCategoryMargin(0.05f);

		return chart;
	}

    /**
     * 輸出JPGs
     */
    private void outputJPGFile(JFreeChart chart, String fileName, int sizeX, int sizeY) {
    	FileOutputStream fos_jpg = null;
    	try {
			fos_jpg = new FileOutputStream(model.getFilePath(fileName + ".jpg", true));
			//TODO 試著將圖片改為Class欄改為縱座標
			//若為Class Level，當Class個數超過14個時，將圖片放大
			if (sizeX == 600 && chart.getCategoryPlot().getCategories() != null)
				if (chart.getCategoryPlot().getCategories().size() > 14)
					sizeX += (chart.getCategoryPlot().getCategories().size() - 14)*45;

			//輸出JPG (註：不失真的長度800X400)
			ChartUtilities.writeChartAsJPEG(fos_jpg, 1.0f, chart, sizeX, sizeY);
		} catch (FileNotFoundException e) {
			logger.error("[File Not Found Exception] EXCEPTION ", e);
		} catch (IOException e) {
			logger.error("[IOException] EXCEPTION ", e);
    	} finally {
			closeJPG(fos_jpg);
    	}
    }

    /**
     * Close JPG File
     * @param fos_jpg
     */
	private void closeJPG(FileOutputStream fos_jpg) {
		try {
			if(fos_jpg != null)
				fos_jpg.close();
		} catch (IOException e) {
			logger.error("[IOException] EXCEPTION ", e);
		}
	}


    /**
     * 設定圖表每個欄不同顏色
     * @author Shiau
     */
    @SuppressWarnings("serial")
	class CustomRenderer extends BarRenderer {
        /** The colors. */
        private Paint[] colors;
        /**
         * Creates a new renderer.
         * @param colors  the colors.
         */
        public CustomRenderer(Paint[] colors) {
            this.colors = colors;
        }
        /**
         * Returns the paint for an item.  Overrides the default behaviour inherited from
         * AbstractSeriesRenderer.
         *
         * @param row  the series.
         * @param column  the category.
         *
         * @return The item color.
         */
        public Paint getItemPaint(int row, int column) {
            return this.colors[column % this.colors.length];
        }
    }
    
    @SuppressWarnings("serial")
	static class LabelGenerator extends AbstractCategoryItemLabelGenerator implements CategoryItemLabelGenerator {
		private Integer category;
		private NumberFormat formatter = NumberFormat.getPercentInstance();// a percent format
		private NumberFormat nf = NumberFormat.getInstance(); 
		/**
		 * Creates a new label generator that displays the item value and a 
		 * percentage relative to the value in the same series for the 
		 * specified category.
		 * @param category
		 */
		public LabelGenerator(int category) {
			this(new Integer(category));
		}
		
		public LabelGenerator(Integer category) {
			super("", NumberFormat.getInstance());
			this.category = category;
		}
		
		/**
		 * Generates a label for the specified item. The label is typicall
		 * a formatted version of the data value, but any text can be used.
		 */
		public String generateLabel(CategoryDataset dataset, int series, int category) {
			String result = null;
			double base =0.0;
			if (this.category != null) {
				final Number b = dataset.getValue(series, this.category.intValue());
				base = b.doubleValue();
			} else {
				base = calculateSeriesTotal(dataset, series);
			}

			Number value = dataset.getValue(series, category);
			nf.setMaximumFractionDigits(2);
			if(value !=null) {
				double v = value.doubleValue();
				if (v != 0)
					//you can apply something format here
					result = nf.format(value).toString()+ "("+ this.formatter.format(v/base) + ")";
				//避免value為0時顯示錯誤，直接顯示為0
				else
					result = nf.format(value).toString()+ "("+ this.formatter.format(0) + ")";
			}						
			
			return result;
		}

		/**
		 * 計算圖表欄位百分比
		 * @param dataset
		 * @param series
		 * @return
		 */
		private double calculateSeriesTotal(CategoryDataset dataset, int series) {
			double result = 0.0;
			for(int i=0;i<dataset.getColumnCount();i++) {
				Number value = dataset.getValue(series, i);
				if(value != null) {
					result = result + value.doubleValue();
				}
			}
			return result;
		}
    }
}
