package sxq0.base.helper.form;


import kd.bos.base.AbstractBasePlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.ClientActions;
import kd.bos.form.IClientViewProxy;
import kd.bos.form.control.EntryGrid;
import kd.bos.form.control.Toolbar;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author tianjie You
 * @date 2025.07.15
 * @description 消毒方案基础资料插件
 * @metadata sxq0_disinfectionplan
 */
public class DisinfectProtocolPlugin extends AbstractBasePlugIn implements Plugin , BeforeF7SelectListener {
    private final String LEVEL = "sxq0_disinfection_level" ;
    private final String ENTRYENTITY = "sxq0_entryentity" ;
    private final String ENTRYLEVEL = "sxq0_basedatafield" ;
    private final String ENTRYSTEP = "sxq0_basedatafield1" ;
    private final String PUBLISH = "sxq0_publish" ;
    private final String SCHEME = "sxq0_scheme" ;
    private final String RECORD_DOC = "sxq0_disinfection_records" ;
    private final String PLAN_DOC = "sxq0_disinfectionplan" ;


    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        BasedataEdit baseData = this.getControl(ENTRYSTEP);
        baseData.addBeforeF7SelectListener(this);

        Toolbar tb = this.getControl("tbmain");
        tb.addItemClickListener(this);
    }

    @Override
    public void afterCreateNewData(EventObject e) {
        //创建默认三行数据
        DynamicObjectCollection query = QueryServiceHelper.query("sxq0_disinfection_level"
                , "id", new QFilter("number", QCP.not_equals, null).toArray());
        int[] newEntryRow = this.getModel().batchCreateNewEntryRow(ENTRYENTITY, query.size());
        String findField = "number,name,status,creator,enable,masterid";
        //查找消毒等级数据
        DynamicObject[] levels = BusinessDataServiceHelper.load(LEVEL, findField, null);
        //赋值给新增的单据体
        for(int i = 0 ; i < query.size();i++){
            this.getModel().setValue(ENTRYLEVEL,levels[i].getPkValue(),i);
        }
        this.getView().updateView(ENTRYENTITY);
        //设置单据体单选
        Map<String, Object> ismul = new HashMap<String, Object>();

        ismul.put("ismul", false);//false单选/true多选

        this.getView().updateControlMetadata(ENTRYENTITY, ismul);
    }

    @Override
    public void afterBindData(EventObject e) {

        //设置单据体行背景颜色
        IClientViewProxy proxy = this.getView().getService(IClientViewProxy.class);
        ClientActions.createRowStyleBuilder() .setRows(new int[]{0})
                .setBackColor("red").buildStyle().build().invokeControlMethod(proxy, ENTRYENTITY);
        ClientActions.createRowStyleBuilder() .setRows(new int[]{1})
                .setBackColor("blue").buildStyle().build().invokeControlMethod(proxy, ENTRYENTITY);
        ClientActions.createRowStyleBuilder() .setRows(new int[]{2})
                .setBackColor("green").buildStyle().build().invokeControlMethod(proxy, ENTRYENTITY);
    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent eve) {
        //过滤消毒步骤
        if(eve.getProperty().getName().equals(ENTRYSTEP)){
            // 获取单据体控件
            EntryGrid entryGrid = this.getControl(ENTRYENTITY);
            // 获取选中行，数组为行号，从0开始
            int[] selectRows = entryGrid.getSelectRows();

            // 获取单据体数据集合
            DynamicObjectCollection entity = this.getModel().getEntryEntity(ENTRYENTITY);
            List<String> list = new ArrayList<>();
            if (selectRows != null && selectRows.length > 0) {
                for (int selectRow : selectRows) {
                    DynamicObject row = entity.get(selectRow); // 获取选中行的单据体数据
                    DynamicObject levelObj = row.getDynamicObject(ENTRYLEVEL);
                    //获取消毒等级编码
                    list.add(levelObj.getString("name"));
                }
                //过滤条件为消毒等级编码为选中的
                QFilter qFilter = new QFilter("sxq0_disinfectionlevel.name", QCP.in, list);
                ListShowParameter showParameter = (ListShowParameter) eve.getFormShowParameter();
                showParameter.getListFilterParameter().getQFilters().add(qFilter);
            }else{
                this.getView().showTipNotification("请先选择消毒等级");
                eve.setCancel(true);
            }
        }
    }

    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        String name = evt.getItemKey();
        if(name.equals(PUBLISH)){
            Long userId = UserServiceHelper.getCurrentUserId();
            QFilter f1 = new QFilter("id", QFilter.equals, userId);
            //判断是否是负责人
//            QFilter f2 = new QFilter("entryentity.isincharge", QFilter.equals, true);
            f1.and(new QFilter("entryentity.isincharge", QFilter.equals, true));
//            ORM orgORM = ORM.create();
//            boolean exists = orgORM.exists("bos_user",new QFilter[]{f1});
            boolean isExist = QueryServiceHelper.exists("bos_user", new QFilter[]{f1});
            if(!isExist){
                this.getView().showTipNotification("你不是⻋间负责⼈⽆法发布");
                evt.setCancel(true);
                return;
            }
            Object versionStatus = this.getModel().getValue("sxq0_versionstatus");
            if("A".equals(versionStatus.toString())){
                this.getView().showTipNotification("当前方案不是最新版本，不可以发布");
                evt.setCancel(true);
            }
//            OperationStatus status = this.getView().getFormShowParameter().getStatus();
//            if(status == OperationStatus.ADDNEW){
//                return;
//            }else if (status == OperationStatus.EDIT){
//                Object masterid = this.getModel().getValue("masterid");
//                QFilter qFilter = new QFilter(SCHEME + ".masterid", QCP.equals, masterid);
//                boolean exist = QueryServiceHelper.exists(RECORD_DOC, qFilter.toArray());
//                if(!exist){
//                    return;
//                }else{
//                    DynamicObject scheme = BusinessDataServiceHelper.loadSingle(PLAN_DOC, "*",
//                            new QFilter[]{new QFilter("masterid", QCP.equals, masterid)});
//                    scheme.set("sxq0_versionstatus","B");
//                    SaveServiceHelper.update(scheme);
//                    int newVersion = Integer.parseInt((String) this.getModel().getValue("sxq0_version")) + 1;
//                    this.getModel().setValue("sxq0_version",newVersion);
//
//                    System.out.println();
//                }
//            }
        }
    }
}