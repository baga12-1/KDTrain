package sxq0.base.helper.List;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.CloseCallBack;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.list.BillList;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.servicehelper.org.OrgUnitServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.EventObject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 标准单据列表插件
 */
public class DisinfectProtocolListPlugin extends AbstractListPlugin implements Plugin  {
    @Override
    public void registerListener(EventObject e) {
//        Toolbar tb = this.getControl("tbmain");
//        tb.addItemClickListener(this);
    }

    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        //分配组织
        if(evt.getItemKey().equals("sxq0_baritemap")){
            //获取列表选中数据
            BillList billList = this.getControl(AbstractListPlugin.BILLLISTID);
            ListSelectedRowCollection selectedRows = billList.getSelectedRows();
            if(selectedRows == null || selectedRows.size() == 0){
                this.getView().showTipNotification("请选中一条行数据");
                return;
            }
            Set<Long> disPlanIds = new HashSet<>();
            for (ListSelectedRow selectedRow : selectedRows) {
                Object pk = selectedRow.getPrimaryKeyValue();
                disPlanIds.add(Long.parseLong(pk.toString()));
            }
            if(disPlanIds.size() != 1){
                this.getView().showTipNotification("请选中一条行数据");
                return;
            }
            QFilter f1 = new QFilter("id", QCP.equals, selectedRows.get(0).getPrimaryKeyValue());
            DynamicObjectCollection queryRes = QueryServiceHelper.query("sxq0_disinfectionplan", "sxq0_versionstatus", f1.toArray());
            String billStatus = queryRes.get(0).getString("sxq0_versionstatus");
            if("A".equals(billStatus)){
                this.getView().showTipNotification(ResManager.LoadKDString("消毒方案是最新版本才能分配组织","1111"));
                return;
            }
            Object formID = selectedRows.get(0).getPrimaryKeyValue();
            this.getView().getPageCache().put("billId",formID.toString());
            FormShowParameter formShowParameter = new FormShowParameter();
            // 弹窗案例-动态表单 页面标识
            formShowParameter.setFormId("sxq0_allocation_org");
            // 自定义传参，把当前单据的文本字段传过去

            formShowParameter.setCustomParam("parent", formID);
            // 设置回调事件，回调插件为当前插件
            formShowParameter.setCloseCallBack(new CloseCallBack(this,"sxq0_disinfectionplan"));
            // 设置打开类型为模态框（不设置的话指令参数缺失，没办法打开页面）
            formShowParameter.getOpenStyle().setShowType(ShowType.Modal);
            // 当前页面发送showform指令。注意也可以从其他页面发送指令，后续有文章介绍
            this.getView().showForm(formShowParameter);



            long userMainOrgId = UserServiceHelper.getUserMainOrgId(UserServiceHelper.getCurrentUserId());
            Map<String, Object> companyfromOrg = OrgUnitServiceHelper.getCompanyfromOrg(userMainOrgId);
        }
    }
    @Override
    public void closedCallBack(ClosedCallBackEvent e) {
        super.closedCallBack(e);
        // 验证回调标识
        if ("sxq0_disinfectionplan".equalsIgnoreCase(e.getActionId())) {
//            BillList billList = this.getControl(AbstractListPlugin.BILLLISTID);
//            ListSelectedRowCollection selectedRows = billList.getSelectedRows();
            DynamicObjectCollection returnData = (DynamicObjectCollection) e.getReturnData();
            if (null != returnData) {
            //将返回的数据插入消毒方案的使用组织
                String billId = this.getView().getPageCache().get("billId");
                DynamicObject disPlan = BusinessDataServiceHelper.loadSingle(billId, "sxq0_disinfectionplan");
                DynamicObjectCollection useOrgEntry = disPlan.getDynamicObjectCollection("sxq0_entryentity1");
                useOrgEntry.clear();
                for (DynamicObject org : returnData) {
                    if(org.getDynamicObject("sxq0_orgfield") == null) {
                        continue;
                    }
                    Object orgId = org.getDynamicObject("sxq0_orgfield").getPkValue();
                    DynamicObject newOrgRow = useOrgEntry.addNew();
                    newOrgRow.set("sxq0_orgfield",orgId);
                }
                SaveServiceHelper.save(new DynamicObject[]{disPlan});
                this.getView().showSuccessNotification("分配成功");
                this.getView().updateView();
            }
        }
    }

}