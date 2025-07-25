package sxq0.base.helper.form;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.control.Button;
import kd.bos.form.control.Control;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.EventObject;

/**
 * 消毒方案-分配组织表单插件
 */
public class AssignOrgFormPlugin extends AbstractFormPlugin implements Plugin {
    @Override
    public void afterCreateNewData(EventObject e) {
        Object id = this.getView().getFormShowParameter().getCustomParam("parent");
        if(id != null){
            DynamicObject disPlan = BusinessDataServiceHelper.loadSingle(id, "sxq0_disinfectionplan");
            DynamicObjectCollection useOrgEntry = disPlan.getDynamicObjectCollection("sxq0_entryentity1");
            if(useOrgEntry != null && useOrgEntry.size() != 0){
                int[] newRows = this.getModel().batchCreateNewEntryRow("sxq0_entryentity", useOrgEntry.size());
                for(int i = 0 ; i < useOrgEntry.size();i++){
                    this.getModel().setValue("sxq0_orgfield",useOrgEntry.get(i).getDynamicObject("sxq0_orgfield").getPkValue(),i);
                    this.getModel().setValue("sxq0_textfield",useOrgEntry.get(i).getDynamicObject("sxq0_orgfield").getString("name"),i);
                }
                this.getView().updateView("sxq0_entryentity");
            }
        }
    }
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        // 注册按钮点击监听（注意itemClick和click的区别）
        Button button = this.getControl("btnok");
        button.addClickListener(this);

    }


    @Override
    public void click(EventObject e) {
        super.click(e);
        // 如果是确定按钮，则取到人员的数据，返回给父页面
        Control control = (Control) e.getSource();
        if ("btnok".equalsIgnoreCase(control.getKey())) {
            DynamicObjectCollection billEntry = this.getModel().getEntryEntity("sxq0_entryentity");
            if (null == billEntry) {
                this.getView().showTipNotification("请选择数据");
            } else {
                this.getView().returnDataToParent(billEntry);
                // 关闭当前页面
                this.getView().close();
            }
        }
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        //变化值为适用组织
        if(e.getProperty().getName().equals("sxq0_orgfield")){
            ChangeData[] changeSet = e.getChangeSet();
            DynamicObject newValue = (DynamicObject) changeSet[0].getNewValue();
            //选择的组织是否被某个最新版本的消毒方案引用
            QFilter f1 = new QFilter("sxq0_versionstatus", QCP.equals, "B");
            f1.and(new QFilter("sxq0_entryentity1.sxq0_orgfield.id", QCP.equals,newValue.getPkValue()));
            boolean isExist = QueryServiceHelper.exists("sxq0_disinfectionplan", f1.toArray());
            if(isExist){
                //存在即提示信息并清空选择的数据
                DynamicObject disPlan = BusinessDataServiceHelper.loadSingle("sxq0_disinfectionplan", "name", f1.toArray());
                this.getView().showTipNotification("组织已被"+disPlan.getString("name")+"⽅案引⽤");
                this.getModel().initValue("sxq0_orgfield",null, e.getChangeSet() [0]. getRowIndex());
                this.getView().updateView("sxq0_orgfield",  e.getChangeSet() [0]. getRowIndex());
                this.getModel().initValue("sxq0_textfield",null, e.getChangeSet() [0]. getRowIndex());
                this.getView().updateView("sxq0_textfield",  e.getChangeSet() [0]. getRowIndex());
            }
        }
    }
}