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
 * �Ϫ�
 * @author Shiau
 */
public class BarChart {
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
			//�إ߸��
	        CategoryDataset dataset = createDataset();
	        //����Chart
	        JFreeChart chart = createChart(dataset);
	        //��X��JPG
	        OutputJPGFile(chart);
		}
	}

	/**
	 * create a dataset
	 */
	private CategoryDataset createDataset() {
		DefaultCategoryDataset data = new DefaultCategoryDataset();
		//�s�W���(""�O���F�]�w�P�@���άۦP�W��)
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

        ///Chart���C��]�w///
        chart.setBackgroundPaint(Color.white);
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        //�C��Bar���C��
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
     * ��XJPGs
     */
    private void OutputJPGFile(JFreeChart chart) {
    	FileOutputStream fos_jpg = null;
    	try {
			fos_jpg = new FileOutputStream(model.getProjectPath() + "/Report.jpg");
			//��XJPG (�����u�A800X400)
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
     * �]�w�Ϫ�C���椣�P�C��
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
