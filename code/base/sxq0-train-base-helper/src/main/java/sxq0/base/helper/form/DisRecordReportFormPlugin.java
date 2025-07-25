package sxq0.base.helper.form;

import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.report.events.CellStyleRule;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.sdk.plugin.Plugin;

import java.util.List;

/**
 * 报表界面插件
 */
public class DisRecordReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void setCellStyleRules(List<CellStyleRule> cellStyleRules) {
        super.setCellStyleRules(cellStyleRules);
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
    }
}