package sxq0.base.helper.Operation;

import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.CloneUtils;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.sdk.plugin.Plugin;

/**
 * 单据操作插件
 */
public class DisPlanPublishOpPlugin extends AbstractOperationServicePlugIn implements Plugin {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        e.getFieldKeys().addAll(this.billEntityType.getAllFields().keySet());
    }

    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        DynamicObject[] data = e.getDataEntities();
        //判断消毒方案是新增还是编辑
        Object curDisPlan = data[0].getPkValue();
        boolean exists = QueryServiceHelper.exists("sxq0_disinfectionplan", curDisPlan);
        if(!exists){
            //新增，直接保存
            OperationResult result = SaveServiceHelper.saveOperate("sxq0_disinfectionplan", data, OperateOption.create());

        }else {
            //编辑
            //判断消毒方案是否被消毒记录单引用
            QFilter f1 = new QFilter("sxq0_scheme.id", QCP.equals, curDisPlan);
            boolean isExist = QueryServiceHelper.exists("sxq0_disinfection_records", f1.toArray());
            if(isExist){
                //被引用过
                DynamicObject oldObj = BusinessDataServiceHelper.loadSingle(curDisPlan, "sxq0_disinfectionplan");
                DynamicObject newObj = (DynamicObject) new CloneUtils(false,true).clone(oldObj);
                //新对象的版本改为历史版本，页面对象的版本号+1
                newObj.set("sxq0_versionstatus","A");
                SaveServiceHelper.saveOperate("sxq0_disinfectionplan", new DynamicObject[]{newObj}, OperateOption.create());
                long version = data[0].getLong("sxq0_version");
                data[0].set("sxq0_version",version+1);
                SaveServiceHelper.saveOperate("sxq0_disinfectionplan",data, OperateOption.create());
            }else{
                SaveServiceHelper.saveOperate("sxq0_disinfectionplan",data, OperateOption.create());
            }
        }

    }
}