package sxq0.base.helper.report;

import kd.bos.algo.Algo;
import kd.bos.algo.DataSet;
import kd.bos.algo.DataType;
import kd.bos.algo.GroupbyDataSet;
import kd.bos.algo.Row;
import kd.bos.algo.RowMeta;
import kd.bos.algo.RowMetaFactory;
import kd.bos.algo.input.CollectionInput;
import kd.bos.entity.report.AbstractReportColumn;
import kd.bos.entity.report.AbstractReportListDataPlugin;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 报表取数插件
 */
public class DisRecordListReportPlugin extends AbstractReportListDataPlugin{
    private static final Log log = LogFactory.getLog(DisRecordListReportPlugin.class.getName());
    private int[] isNull = new int[13];
    private  String[] field = {"sxq0_applyenterworkshop","sxq0_january","sxq0_february","sxq0_march","sxq0_april",
            "sxq0_may","sxq0_june","sxq0_july","sxq0_august","sxq0_september","sxq0_october","sxq0_november","sxq0_december","sxq0_year"};
    private  DataType[] dataTypes = {DataType.LongType,DataType.IntegerType,DataType.IntegerType,DataType.IntegerType,DataType.IntegerType,DataType.IntegerType
            ,DataType.IntegerType,DataType.IntegerType,DataType.IntegerType,DataType.IntegerType,DataType.IntegerType,DataType.IntegerType,DataType.IntegerType,DataType.IntegerType};
    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
            // 获取当前年份的第一天
            LocalDate firstDayOfYear = LocalDate.now().withDayOfYear(1);

            Date firstDate = Date.from(firstDayOfYear.atStartOfDay(ZoneId.systemDefault()).toInstant());
            // 获取当前年份的最后一天
            LocalDate lastDayOfYear = LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear());
            Date lastDate = Date.from(lastDayOfYear.atStartOfDay(ZoneId.systemDefault()).toInstant());
            //查询本年记录单并且已完成的记录单
            QFilter f1 = new QFilter("sxq0_appenterdate", QCP.large_equals, firstDate);
            f1.and("sxq0_appenterdate", QCP.less_equals, lastDate)
                    .and("sxq0_datastatus", QCP.equals, "B");
            String selField = "sxq0_applyenterworkshop,sxq0_appenterdate";
            DataSet dataSet = QueryServiceHelper.queryDataSet(this.getClass().getName()
                    , "sxq0_disinfection_records", selField, f1.toArray(), null);

            DataSet groupDataSet = dataSet.copy().groupBy(new String[]{"sxq0_applyenterworkshop", "substr(sxq0_appenterdate,0,7) as applydate"})
                    .count().finish();
            Collection<Object[]> coll = new ArrayList<>();
            RowMeta rowMeta = RowMetaFactory.createRowMeta(field, dataTypes);
            CollectionInput inputs = new CollectionInput(rowMeta, coll);
            DataSet resultDataSet = Algo.create(this.getClass().getName()).createDataSet(inputs);
            Object[] tempData = new Object[field.length];
            String workshop = "";
            List<String> existProduct = new ArrayList<>();
            for (Row row : groupDataSet.copy()) {
                workshop = row.getString("sxq0_applyenterworkshop");
                if (!existProduct.contains(workshop)) {
                    tempData = new Object[field.length];
                    coll.add(tempData);
                    existProduct.add(workshop);
                    tempData[0] = workshop;
                }
                switch (row.getString("applydate").substring(5, 7)) {
                    case "01":
                        tempData[1] = row.getLong("count");
                        break;
                    case "02":
                        tempData[2] = row.getLong("count");
                        break;
                    case "03":
                        tempData[3] = row.getLong("count");
                        break;
                    case "04":
                        tempData[4] = row.getLong("count");
                        break;
                    case "05":
                        tempData[5] = row.getLong("count");
                        break;
                    case "06":
                        tempData[6] = row.getLong("count");
                        break;
                    case "07":
                        tempData[7] = row.getLong("count");
                        break;
                    case "08":
                        tempData[8] = row.getLong("count");
                        break;
                    case "09":
                        tempData[9] = row.getLong("count");
                        break;
                    case "10":
                        tempData[10] = row.getLong("count");
                        break;
                    case "11":
                        tempData[11] = row.getLong("count");
                        break;
                    case "12":
                        tempData[12] = row.getLong("count");
                        break;
                    default:
                        break;
                }
            }

//        计算年总申请数量
            DataSet yearTotalDataSet = dataSet.copy()
                    .groupBy(new String[]{field[0], "substr(sxq0_appenterdate,0, 4) as applydate"})
                    .count(field[13]).finish();
            resultDataSet = resultDataSet.join(yearTotalDataSet).on(field[0], field[0]).select(
                    new String[]{field[0], field[1], field[2], field[3], field[4], field[5], field[6], field[7], field[8], field[9], field[10], field[11], field[12]}
                    , new String[]{field[13]}
            ).finish();
            DataSet totalDataSetCopy = resultDataSet.copy();
            GroupbyDataSet totalGroupDataSet = totalDataSetCopy.groupBy(null);
            totalGroupDataSet.max("sxq0_applyenterworkshop", "sxq0_applyenterworkshop_total")
                    .sum(field[1], field[1] + "_total")
                    .sum(field[2], field[2] + "_total")
                    .sum(field[3], field[3] + "_total")
                    .sum(field[4], field[4] + "_total")
                    .sum(field[5], field[5] + "_total")
                    .sum(field[6], field[6] + "_total")
                    .sum(field[7], field[7] + "_total")
                    .sum(field[8], field[8] + "_total")
                    .sum(field[9], field[9] + "_total")
                    .sum(field[10], field[10] + "_total")
                    .sum(field[11], field[11] + "_total")
                    .sum(field[12], field[12] + "_total")
                    .sum(field[13], field[13] + "_total");

            DataSet totalDataSet = totalGroupDataSet.finish().select(new String[]{"sxq0_applyenterworkshop_total"
                    , field[1] + "_total"
                    , field[2] + "_total"
                    , field[3] + "_total"
                    , field[4] + "_total"
                    , field[5] + "_total"
                    , field[6] + "_total"
                    , field[7] + "_total"
                    , field[8] + "_total"
                    , field[9] + "_total"
                    , field[10] + "_total"
                    , field[11] + "_total"
                    , field[12] + "_total"
                    , field[13] + "_total"});
            for (Row row : totalDataSet.copy()) {
                for (int i = 1; i < field.length - 1; i++) {
                    Long totalQty = row.getLong(i);
                    if (totalQty == null) {
                        isNull[i] = 1;
                    }
                }
            }

            return resultDataSet.union(totalDataSet);
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        for(int i = isNull.length-1 ; i >= 1 ; i--){
            if(isNull[i] == 1){
                columns.remove(i);
            }
        }
        return super.getColumns(columns);
    }
}
