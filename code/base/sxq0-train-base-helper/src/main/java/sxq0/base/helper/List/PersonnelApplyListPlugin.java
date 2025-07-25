package sxq0.base.helper.List;

import kd.bos.bill.BillShowParameter;
import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.BadgeInfo;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.ShowType;
import kd.bos.form.control.Toolbar;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.BeforeCreateListColumnsArgs;
import kd.bos.form.events.BeforeCreateListDataProviderArgs;
import kd.bos.form.events.SetFilterEvent;
import kd.bos.form.operate.FormOperate;
import kd.bos.list.IListView;
import kd.bos.list.ListColumn;
import kd.bos.list.events.ListRowClickEvent;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.mvc.list.ListDataProvider;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import kd.bos.servicehelper.workflow.WorkflowServiceHelper;
import kd.bos.workflow.api.BizProcessStatus;
import kd.sdk.plugin.Plugin;

import java.time.Instant;
import java.util.Date;
import java.util.EventObject;
import java.util.List;
import java.util.Map;

/**
 * 标准单据列表插件
 */
public class PersonnelApplyListPlugin extends AbstractListPlugin implements Plugin {

    @Override
    public void beforeCreateListColumns(BeforeCreateListColumnsArgs args) {
        //动态添加当前处理人列
        super.beforeCreateListColumns(args);
        ListColumn listColumn = new ListColumn();
        listColumn.setCaption(new LocaleString("流程当前处理人"));
        listColumn.setKey("sxq0_currentapprover");
        listColumn.setListFieldKey("sxq0_currentapprover");
        listColumn.setParentViewKey("gridview");
        args.addListColumn(listColumn);
    }

    @Override
    public void setFilter(SetFilterEvent e) {
        long currentUserId = UserServiceHelper.getCurrentUserId();
        List<Long> inChargeOrgs = UserServiceHelper.getInchargeOrgs(currentUserId, true);
        if(inChargeOrgs != null && inChargeOrgs.size() > 0){
            //是负责人,可以看到自己的申请单和申请进入负责部门的申请单
//            QFilter f1 = new QFilter("entryentity.dpt.id", QCP.in, inChargeOrgs);
//            DynamicObjectCollection users = QueryServiceHelper.query("bos_user", "id", f1.toArray());
//            Set<Long> userSet = new HashSet<>();
//            for (DynamicObject user : users) {
//                long pkValue = user.getLong("id");
//                userSet.add(pkValue);
//            }
            //在部门下的人员都可以看到
//            e.addCustomQFilter(new QFilter("creator.id", QCP.in,userSet));
            QFilter f1 = new QFilter("sxq0_applyenterworkshop.id", QCP.in, inChargeOrgs);
            f1.or(new QFilter("creator.id", QCP.equals,currentUserId));
            e.addCustomQFilter(f1);
        }else{
            //不是负责人只能看到自己的申请单
            e.addCustomQFilter(new QFilter("creator.id", QCP.equals,currentUserId));
        }
    }

    @Override
    public void afterBindData(EventObject e) {
        //默认没有选择数据不能消毒
        this.getView().setEnable(false,"sxq0_baritemap");
        Toolbar toolbar = (Toolbar)this.getView().getControl("toolbarap");
        IListView listView = (IListView)this.getView();
        ListSelectedRowCollection rows = listView.getCurrentListAllRowCollection();
        int count = 0;

        for(int i = 0; i < rows.size(); ++i) {
            ListSelectedRow row = rows.get(i);
            if ("C".equals(row.getBillStatus())) {
                ++count;
            }
        }

        BadgeInfo info = new BadgeInfo();
        info.setColor("#ff0000");
        info.setCount(count);
        info.setShowZero(true);
        toolbar.setBadgeInfo("sxq0_baritemap", info);
    }

    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        //点击开始消毒按钮
        if(evt.getItemKey().equals("sxq0_baritemap")){

            IListView listView = (IListView) this.getView();
            ListSelectedRowCollection selectedRows = listView.getSelectedRows();
            if(selectedRows == null || selectedRows.size() != 1){
                this.getView().showTipNotification("请选择一条数据");
                evt.setCancel(true);
                return;
            }
            Object pk = selectedRows.get(0).getPrimaryKeyValue();
            QFilter f1 = new QFilter("id", QCP.equals, pk);
//            ⾮申请进⼊⻋间的负责⼈操作提示你不是申请进⼊⻋间的负责⼈，没有权限进⾏消毒！
            DynamicObject queryOne1 = QueryServiceHelper.queryOne("sxq0_personnel_apply", "sxq0_applyenterworkshop", f1.toArray());
            long workshopId = queryOne1.getLong("sxq0_applyenterworkshop");
            List<Long> ids = UserServiceHelper.getManagersOfOrg(workshopId);
            boolean isContain = ids.contains(UserServiceHelper.getCurrentUserId());
            if(!isContain){
                //不是申请进入部门的负责人，不能操作开始消毒
                this.getView().showTipNotification("你不是申请进⼊⻋间的负责⼈，没有权限进⾏消毒！");
                evt.setCancel(true);
                return;
            }

            //判断申请单的申请进入时间是不是当天
            DynamicObject queryOne2 = QueryServiceHelper.queryOne("sxq0_personnel_apply", "sxq0_applyentertime", f1.toArray());

            Date applyDate = queryOne2.getDate("sxq0_applyentertime");
            Date nowDate = Date.from(Instant.now());

            if(applyDate.getYear() != nowDate.getYear()
                    || applyDate.getMonth() != nowDate.getMonth()
                    || applyDate.getDay() != nowDate.getDay()){
                //如申请时间大于当前时间提示提示“申请⽇期当天才能开始消毒”
                this.getView().showTipNotification("申请⽇期当天才能开始消毒");
                evt.setCancel(true);
                return;
            }
        }
    }

    @Override
    public void listRowClick(ListRowClickEvent evt) {
        IListView listView = (IListView) this.getView();
        ListSelectedRowCollection selectedRows = listView.getSelectedRows();
        if(selectedRows == null || selectedRows.size() == 0){
            this.getView().setEnable(false,"sxq0_baritemap");
            return;
        }
        for (ListSelectedRow selectedRow : selectedRows) {
            String billStatus = selectedRow.getBillStatus();
            if(!"C".equals(billStatus)){
                this.getView().setEnable(false,"sxq0_baritemap");
                return;
            }
        }
        this.getView().setEnable(true,"sxq0_baritemap");
    }



    @Override
    public void beforeCreateListDataProvider(BeforeCreateListDataProviderArgs args) {
        args.setListDataProvider(new ListDataProvider(){
            private final static String KEY_CURRENTAPPROVER = "sxq0_currentapprover";

            @Override
            public DynamicObjectCollection getData(int start, int limit) {
                DynamicObjectCollection rows = super.getData(start, limit);
                for(DynamicObject row : rows){
                    String businessKey = row.getPkValue().toString();
                    //单据是否在流程中
                    boolean inProcess = WorkflowServiceHelper.inProcess(businessKey);
                    if(inProcess) {
                    //获取流程节点处理人，赋值到列表当前处理人字段
                        List<Long> approverByBusinessKey = WorkflowServiceHelper.getApproverByBusinessKey(row.getPkValue().toString());
                        Map<String, List<BizProcessStatus>> map = WorkflowServiceHelper.getBizProcessStatus(new String[] {row.getPkValue().toString()});
                        List<BizProcessStatus> node = map.get(row.getPkValue().toString());
                        if(node == null){
                            continue;
                        }
                        node.forEach((e) -> {
                            String nodeStr = e.getCurrentNodeName();
                            String auditor = e.getParticipantName();
                            if (auditor != null && !"".equals(auditor.trim())) {
                                nodeStr = nodeStr + " / " + auditor;}
                            row.set(KEY_CURRENTAPPROVER, nodeStr);
                        });
                    }
                }
                return rows;
            }
        });
    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs eventArgs) {
        String operateKey = eventArgs.getOperateKey();
        if("donothing".equals(operateKey)){
            //获取操作插件保存的id
            FormOperate formOperate  = (FormOperate) eventArgs.getSource();
            String id = formOperate.getOption().getVariableValue("id");
            BillShowParameter showParameter  = new BillShowParameter();
            showParameter.setPkId(id);
            showParameter.setFormId("sxq0_disinfection_records");
            showParameter.setStatus(OperationStatus.EDIT);
            showParameter.getOpenStyle().setShowType(ShowType.Modal);
            this.getView().showForm(showParameter);
        }
    }
}