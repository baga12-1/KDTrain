package sxq0.base.helper.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.form.ClientProperties;
import kd.bos.form.IFormView;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.servicehelper.user.UserServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

/**
 * @author tianjie You
 * @date 2025.07.15
 * @description 人员申请单表单插件
 * @metadata sxq0_personnel_apply
 */
public class PersonnelApplyFormPlugin extends AbstractBillPlugIn implements Plugin {
    @Override
    public void afterCreateNewData(EventObject e) {
        //设置公司为当前登录用户的公司
        ArrayList<Long> userId = new ArrayList<>();
        userId.add(UserServiceHelper.getCurrentUserId());
        Map<Long, Long> companys = UserServiceHelper.getCompanyByUserIds(userId);
        Collection<Long> values = companys.values();
        for (Long value : values) {
            this.getModel().setValue("sxq0_enterprise",value);
        }

    }

    //设置单据状态颜色
    @Override
    public void afterBindData(EventObject e) {
        IFormView view = this.getView();//界面
        IDataModel model = this.getModel();//数据
        //标准界面控件与数据绑定之后，初始化个性化控件绑定数据显示
        //根据数据初始化界面控件颜色
        String billstatus = (String) model.getValue("billstatus");
        if("A".equals(billstatus) ) {
            HashMap<String,Object> fieldMap = new HashMap<>();
            //设置前景色
            fieldMap.put(ClientProperties.ForeColor,"#2b87f3");
            //同步指定元数据到控件
            this.getView().updateControlMetadata("billstatus",fieldMap);
        }else if("B".equals(billstatus)) {
            HashMap<String,Object> fieldMap = new HashMap<>();
            //设置前景色
            fieldMap.put(ClientProperties.ForeColor,"#11ea73");
            //同步指定元数据到控件
            this.getView().updateControlMetadata("billstatus",fieldMap);
        }else if("C".equals(billstatus)) {
            HashMap<String,Object> fieldMap = new HashMap<>();
            //设置前景色X
            fieldMap.put(ClientProperties.ForeColor,"#ef5a1e");
            //同步指定元数据到控件
            this.getView().updateControlMetadata("billstatus",fieldMap);
        }else if("D".equals(billstatus)) {
            HashMap<String,Object> fieldMap = new HashMap<>();
            //设置前景色
            fieldMap.put(ClientProperties.ForeColor,"#edf10c");
            //同步指定元数据到控件
            this.getView().updateControlMetadata("billstatus",fieldMap);
        }else if("E".equals(billstatus)) {
            HashMap<String,Object> fieldMap = new HashMap<>();
            //设置前色
            fieldMap.put(ClientProperties.ForeColor,"#736f6f");
            //同步指定元数据到控件
            this.getView().updateControlMetadata("billstatus",fieldMap);
        }else if("F".equals(billstatus)) {
            HashMap<String,Object> fieldMap = new HashMap<>();
            //设置前景色
            fieldMap.put(ClientProperties.ForeColor,"#ef0c0c");
            //同步指定元数据到控件
            this.getView().updateControlMetadata("billstatus",fieldMap);
        }else if("G".equals(billstatus)) {
            HashMap<String,Object> fieldMap = new HashMap<>();
            //设置前景色
            fieldMap.put(ClientProperties.ForeColor,"#ef0caa");
            //同步指定元数据到控件
            this.getView().updateControlMetadata("billstatus",fieldMap);
        }
    }

    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        if(evt.getItemKey().equals("bar_save") || evt.getItemKey().equals("bar_submit")){
            //校验附件是否为空
            int qty = this.getModel().getDataEntity(true).getInt("sxq0_attachmentcountfield");
            if(qty == 0){
                this.getView().showTipNotification("附件不能为空，需上传身份证照片");
                evt.setCancel(true);
            }
        }
//        evt.setCancel(true);
    }
}