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
 * �Ϫ�
 * @author Shiau
 */
public class BarChart {
	public enum LEVEL { PROJECT_LEVEL, PACKAGE_LEVEL, CLASS_LEVEL }
	private static Logger logger = LoggerFactory.getLogger(BarChart.class);

	//Report�����
	private ReportModel model;
	
	BarChart(ReportModel model) {
		this.model = model;
	}

	/**
	 * ���͹Ϫ�
	 */
	public void build() {
		if (model != null) {
			//TODO �ϥ�enum²�Ƶ{��

			//�إ�Project Summary��Smell��T
	        CategoryDataset dataset = createProjectDataset();
	        //����Chart
	        JFreeChart chart = createChart(dataset, "EH Smells Chart", "", true);
	        //��X��JPG
	        outputJPGFile(chart, "Report", 800, 500);

	        //�إ�Package Level��Smell��T
	        dataset = createPackageDataset();
	        //����Chart
	        chart = createChart(dataset, "Packages List", "Package Name",  false);
	        //��X��JPG
	        outputJPGFile(chart, "PackageReport", 800, 420);

	        for (int i=0; i<model.getPackagesSize(); i++) {
		        //�إ�Class Level��Smell��T
		        dataset = createClassDataset(model.getPackage(i));
		        //����Chart
		        chart = createChart(dataset, model.getPackage(i).getPackageName(), "Class Name", false);
		        //��X��JPG
		        outputJPGFile(chart, "ClassReport_" + i, 600, 420);
	        }
		}
	}

	/**
	 * create a dataset
	 */
	private CategoryDataset createProjectDataset() {
		DefaultCategoryDataset data = new DefaultCategoryDataset();
		//�s�W���(""�O���F�]�w�P�@���άۦP�W��)
		if(model.getTotalSmellCount() == 0){
			return null;
		}
		data.addValue(model.getIgnoreTotalSize() , "", "Ignore checked exception");
		data.addValue(model.getDummyTotalSize(), "", "Dummy handler");
		data.addValue(model.getUnMainTotalSize(), "", "Unprotected main program");
		data.addValue(model.getNestedTryTotalSize(), "", "Nested try block");
		data.addValue(model.getCarelessCleanUpTotalSize(), "", "Careless Clean Up");
		data.addValue(model.getOverLoggingTotalSize(), "", "OverLogging");
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

			//Package �W�٤ӱ`�ҥH�ϥΥN�X
			//data.addValue(packageModel.getTotalSmellSize(), "", packageModel.getPackageName());
			//�s�W���(""�O���F�]�w�P�@���άۦP�W��)
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
			//�s�W���(""�O���F�]�w�P�@���άۦP�W��)
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
			"Number of EH Smells",		// range axis label
			dataset,                    // data
			PlotOrientation.VERTICAL,   // the plot orientation
			false,						// legend
			false,                      // tooltips
			false                       // urls
		);

		CategoryPlot plot = chart.getCategoryPlot();
		plot.setNoDataMessage("No data available");

		///Chart���C��]�w///
		chart.setBackgroundPaint(Color.white);
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		//�C��Bar���C��
		CategoryItemRenderer renderer;
		if (isDifferColor)
			renderer = new CustomRenderer(new Paint[] {Color.red, Color.blue, Color.green,
					Color.yellow, Color.magenta, Color.cyan,
					Color.pink, Color.YELLOW});
		else
			renderer = new CustomRenderer(new Paint[] {Color.red});

		renderer.setBaseItemLabelGenerator(new LabelGenerator(null));
		renderer.setBaseItemLabelsVisible(true);
		        
		//�վ�ϹϪ��Z���X�{�p���I
		NumberAxis numberaxis = (NumberAxis)plot.getRangeAxis();
		numberaxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		        
		//�]�w�C��Bar�W�����r��
		renderer.setBaseItemLabelFont(new Font("Arial",Font.BOLD,14));
		renderer.setBasePositiveItemLabelPosition(new ItemLabelPosition(
				ItemLabelAnchor.OUTSIDE12, TextAnchor.BASELINE_CENTER));
		plot.setRenderer(renderer);
		
		CategoryAxis domainAxis = plot.getDomainAxis();

//		domainAxis.setMaximumCategoryLabelWidthRatio(25);
//		domainAxis.setMaximumCategoryLabelLines(100);

		//�Y��Package Level�N�Ϥ���r�令����(Package Level��r�ϥΥN�X)
		if (title.equals("Packages List"))
			domainAxis.setCategoryLabelPositions(CategoryLabelPositions.STANDARD);
		else
			domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

		//�Y�n��ܬۦP�C��A�h�NBar�PBar���������Z�Y�p
		if (isDifferColor)
			domainAxis.setCategoryMargin(0.5f);
		else
			domainAxis.setCategoryMargin(0.05f);

		return chart;
	}

    /**
     * ��XJPGs
     */
    private void outputJPGFile(JFreeChart chart, String fileName, int sizeX, int sizeY) {
    	FileOutputStream fos_jpg = null;
    	try {
			fos_jpg = new FileOutputStream(model.getFilePath(fileName + ".jpg", true));

			//TODO �յ۱N�Ϥ��אּClass��אּ�a�y��
			//�Y��Class Level�A��Class�ӼƶW�L14�ӮɡA�N�Ϥ���j
			if (sizeX == 600 && chart.getCategoryPlot().getCategories() != null)
				if (chart.getCategoryPlot().getCategories().size() > 14)
					sizeX += (chart.getCategoryPlot().getCategories().size() - 14)*45;

			//��XJPG (���G�����u������800X400)
			ChartUtilities.writeChartAsJPEG(fos_jpg, 1.0f, chart, sizeX, sizeY);
		} catch (FileNotFoundException e) {
			logger.error("[File Not Found Exception] EXCEPTION ",e);
		} catch (IOException e) {
			logger.error("[IOException] EXCEPTION ",e);
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
			logger.error("[IOException] EXCEPTION ",e);
		}
	}


    /**
     * �]�w�Ϫ�C���椣�P�C��
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
	static class LabelGenerator extends AbstractCategoryItemLabelGenerator implements CategoryItemLabelGenerator{
		private Integer category;
		private NumberFormat formatter = NumberFormat.getPercentInstance();// a percent format
		private NumberFormat nf = NumberFormat.getInstance(); 
		/**
		 * Creates a new label generator that displays the item value and a 
		 * percentage relative to the value in the same series for the 
		 * specified category.
		 * @param category
		 */
		public LabelGenerator(int category){
			this(new Integer(category));
		}
		
		public LabelGenerator(Integer category){
			super("",NumberFormat.getInstance());
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
				base = calculateSeriesTotal(dataset,series);
			}

			Number value = dataset.getValue(series, category);
			nf.setMaximumFractionDigits(2);
			if(value !=null){
				double v = value.doubleValue();
				if (v != 0)
					//you can apply something format here
					result = nf.format(value).toString()+ "("+ this.formatter.format(v/base) + ")";
				//�קKvalue��0����ܿ��~�A������ܬ�0
				else
					result = nf.format(value).toString()+ "("+ this.formatter.format(0) + ")";
			}						
			
			return result;
		}

		/**
		 * �p��Ϫ����ʤ���
		 * @param dataset
		 * @param series
		 * @return
		 */
		private double calculateSeriesTotal(CategoryDataset dataset, int series){
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
