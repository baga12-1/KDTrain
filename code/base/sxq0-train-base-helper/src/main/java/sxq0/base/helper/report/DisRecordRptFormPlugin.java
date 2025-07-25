package sxq0.base.helper.report;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.events.CellStyleRule;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.List;

/**
 * 报表界面插件
 */
public class DisRecordRptFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void setCellStyleRules(List<CellStyleRule> cellStyleRules) {
        super.setCellStyleRules(cellStyleRules);
        CellStyleRule cellStyleRule = new CellStyleRule();
        //字段标识
        cellStyleRule.setFieldKey("sxq0_applyenterworkshop");
        //背景⾊
        cellStyleRule.setBackgroundColor("red");
        //前置条件
        cellStyleRule.setCondition("sxq0_year <= 10 && sxq0_applyenterworkshop != '合计'");
        cellStyleRules.add(cellStyleRule);
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        int size = rowData.size();
        if(size > 0){
            DynamicObject org = BusinessDataServiceHelper.newDynamicObject("bos_org");
            org.set("name","合计");
            rowData.get(rowData.size()-1).set("sxq0_applyenterworkshop",org);
        }
    }
}