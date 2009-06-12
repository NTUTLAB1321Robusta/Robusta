package ntut.csie.csdet.report;

import java.awt.Color;
import java.awt.Paint;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 圖表
 * @author Shiau
 */
public class BarChart {
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
			//建立資料
	        CategoryDataset dataset = createDataset();
	        //產生Chart
	        JFreeChart chart = createChart(dataset);
	        //輸出成JPG
	        OutputJPGFile(chart);
		}
	}

	/**
	 * create a dataset
	 */
	private CategoryDataset createDataset() {
		DefaultCategoryDataset data = new DefaultCategoryDataset();
		//新增資料(""是為了設定同一欄位用相同名稱)
		data.addValue(model.getIgnoreTotalSize() , "", "Ignore checked exception");
		data.addValue(model.getDummyTotalSize(), "", "Dummy handler");
		data.addValue(model.getUnMainTotalSize(), "", "Unprotected main program");
		data.addValue(model.getNestedTryTotalSize(), "", "Nested try block");
		return data;				
	}

	/**
	 * create a chart and return it
	 */
    private JFreeChart createChart(CategoryDataset dataset) {

        final JFreeChart chart = ChartFactory.createBarChart(
            "EH Smell Chart",         	// chart title
            "",							// domain axis label
            "EH Smell Numbers",			// range axis label
            dataset,                    // data
            PlotOrientation.VERTICAL,   // the plot orientation
            false,                      // legend
            true,                       // tooltips
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
        CategoryItemRenderer renderer = new CustomRenderer2(
                new Paint[] {Color.red, Color.blue, Color.green,
                    Color.yellow, Color.magenta, Color.cyan,
                    Color.pink, Color.YELLOW}
        );

        renderer.setBaseItemLabelsVisible(true);

        plot.setRenderer(renderer);
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        domainAxis.setCategoryMargin(0.5f);

        return chart;
    }

    /**
     * 輸出JPGs
     */
    private void OutputJPGFile(JFreeChart chart) {
    	FileOutputStream fos_jpg = null;
    	try {
			fos_jpg = new FileOutputStream(model.getProjectPath() + "/Report.jpg");
			//輸出JPG (不失真，800X400)
			ChartUtilities.writeChartAsJPEG(fos_jpg, 1.0f, chart, 800,400);
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
			e.printStackTrace();
		}
	}

    /**
     * 設定圖表每個欄不同顏色
     * @author Shiau
     */
    class CustomRenderer2 extends BarRenderer {
        /** The colors. */
        private Paint[] colors;
        /**
         * Creates a new renderer.
         * @param colors  the colors.
         */
        public CustomRenderer2(Paint[] colors) {
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
}
