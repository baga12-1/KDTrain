package sxq0.base.helper.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.form.container.Wizard;
import kd.bos.form.control.Control;
import kd.bos.form.control.Steps;
import kd.bos.form.control.StepsOption;
import kd.bos.form.control.events.BeforeClickEvent;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.control.events.StepEvent;
import kd.bos.form.control.events.WizardStepsListener;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.sdk.plugin.Plugin;
import org.apache.commons.lang3.StringUtils;

import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单据界面插件
 */
public class DisRecordFormPlugin extends AbstractBillPlugIn implements Plugin, WizardStepsListener {
    public void initialize()
    {//初始化监听控件
        addClickListeners("sxq0_buttonap");
        Wizard wizard = this.getControl("sxq0_wizardap");
        wizard.addWizardStepsListener(this);
    }
//    @Override
//    public void afterCreateNewData(EventObject e) {
//        //初始化向导步骤
//        Wizard wizard = this.getControl("sxq0_wizardap");
//        // 获取设计时的步骤条设置
//        List<StepsOption> stepsOptions = wizard.getStepsOptions();
//        //初始化步骤条
//        stepsOptions.clear();
//        DynamicObjectCollection rows = this.getModel().getEntryEntity("entryentity");
//        //遍历单据体
//        int i = 0 ;
//        int currentindex = 0;
//        for (DynamicObject row : rows) {
//            StepsOption stepsOption1 = new StepsOption();
//            if(row == null || row.getDynamicObject("sxq0_basedatafield") == null
//                    || row.getDynamicObject("sxq0_basedatafield1") == null){
//                continue;
//            }
//            String levelName = row.getDynamicObject("sxq0_basedatafield").getString("name");
//            String stepName = row.getDynamicObject("sxq0_basedatafield1").getString("name");
//            stepsOption1.setTitle(new LocaleString(levelName));
//            stepsOption1.setDescription(new LocaleString(stepName));
//            //设置步骤的状态
//            String rowStatus = row.getString("sxq0_billstatusfield");
//            if("C".equals(rowStatus)){
//                stepsOption1.setStatus(Steps.FINISH);
//                i++;
//            }else if("B".equals(rowStatus)){
//                stepsOption1.setStatus(Steps.PROCESS);
//                i++;
//            }else{
//                stepsOption1.setStatus(Steps.WAIT);
//                currentindex = i;
//                i++;
//            }
//            stepsOptions.add(stepsOption1);
//        }
//        // 更新步骤条设置
//        wizard.setWizardStepsOptions(stepsOptions);
////         设置当前节点
//        Map<String, Object> currentStepMap = new HashMap<>();
//
//        if(currentindex >= 0) {
//            currentStepMap.put("currentStep", currentindex);
//            currentStepMap.put("currentStatus", Steps.PROCESS);
//            this.getModel().setValue("sxq0_upload", this.getModel().getValue("sxq0_dispicture", i-1));
//        }else {
//            currentStepMap.put("currentStep", i-1);
//            currentStepMap.put("currentStatus", Steps.FINISH);
//            this.getView().setVisible(false, "sxq0_buttonap");
//        }
//        // 更新当前节点
//        wizard.setWizardCurrentStep(currentStepMap);
//    }

    @Override
    public void beforeClick(BeforeClickEvent evt) {
        Control source = (Control) evt.getSource();
        if("sxq0_buttonap".equals(source.getKey())){
            //点击完成
            Object uploadPicture = this.getModel().getValue("sxq0_upload");
            if(uploadPicture == null || StringUtils.isBlank(uploadPicture.toString())){
                this.getView().showTipNotification("请上传图片");
                evt.setCancel(true);
                return;
            }
            //有图片。更新单据体数据
            DynamicObjectCollection rows = this.getModel().getEntryEntity("entryentity");
            boolean isEnd = false;
            for(int i = 0 ; i < rows.size();i++){
                DynamicObject row = rows.get(i);
                //进行中的步骤改成已完成
                if("B".equals(row.get("sxq0_billstatusfield"))) {
                    row.set("sxq0_billstatusfield", "C");
                    row.set("sxq0_dispicture", this.getModel().getValue("sxq0_upload"));
                    if(i == rows.size()-1){
                        isEnd = true;
                    }
                }
                //未进行的改成进行中
                if("A".equals(row.get("sxq0_billstatusfield"))){
                    row.set("sxq0_billstatusfield", "B");
                    //改完下一条就退出循环
                    break;
                }
            }
            if(isEnd) {
                //所有步骤全部完成，更新数据状态为已完成
                this.getModel().setValue("sxq0_datastatus", "B");
                DynamicObject dataEntity = this.getModel().getDataEntity(true);
                SaveServiceHelper.saveOperate("sxq0_disinfection_records",new DynamicObject[]{this.getModel().getDataEntity(true)}, OperateOption.create());
                //将源单改为消毒完成
                QFilter f1 = new QFilter("sxq0_record", QCP.equals, this.getModel().getDataEntity(true).getPkValue().toString());
                DynamicObject applyDoc = BusinessDataServiceHelper.loadSingle("sxq0_personnel_apply", f1.toArray());
                applyDoc.set("billstatus","E");
                SaveServiceHelper.saveOperate("sxq0_personnel_apply",new DynamicObject[]{applyDoc}, OperateOption.create());
            }
            this.getView().updateView();
        }
    }

    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        if("bar_close".equals(evt.getItemKey())){
            this.getModel().setDataChanged(false);
        }
    }

    @Override
    public void afterBindData(EventObject e) {
        //初始化向导步骤
        Wizard wizard = this.getControl("sxq0_wizardap");
        // 获取设计时的步骤条设置
        List<StepsOption> stepsOptions = wizard.getStepsOptions();
        //初始化步骤条
        stepsOptions.clear();
        DynamicObjectCollection rows = this.getModel().getEntryEntity("entryentity");
        //遍历单据体
        int i = 0 ;
        int currentindex = -1;
        //每次进入的时候重置一下页面缓存的当前步数,currentstep这个会在update方法中使用的
        if(this.getPageCache().get("currentstep")!=null) {
            this.getPageCache().remove("currentstep");
        }
        for (DynamicObject row : rows) {
            StepsOption stepsOption1 = new StepsOption();
            if(row == null || row.getDynamicObject("sxq0_basedatafield") == null
                    || row.getDynamicObject("sxq0_basedatafield1") == null){
                continue;
            }
            String levelName = row.getDynamicObject("sxq0_basedatafield").getString("name");
            String stepName = row.getDynamicObject("sxq0_basedatafield1").getString("name");
            stepsOption1.setTitle(new LocaleString(levelName));
            stepsOption1.setDescription(new LocaleString(stepName));
            //设置步骤的状态
            String rowStatus = row.getString("sxq0_billstatusfield");
            if("C".equals(rowStatus)){
                stepsOption1.setStatus(Steps.FINISH);
                i++;
            }else if("B".equals(rowStatus)){
                stepsOption1.setStatus(Steps.PROCESS);
                currentindex = i;
                this.getPageCache().put("currentstep",String.valueOf(currentindex));
                i++;
            }else{
                i++;
            }
            stepsOptions.add(stepsOption1);
        }
        // 更新步骤条设置
        wizard.setWizardStepsOptions(stepsOptions);
        // 设置当前节点
        Map<String, Object> currentStepMap = new HashMap<>();

        if(currentindex >= 0) {
            currentStepMap.put("currentStep", currentindex);
            currentStepMap.put("currentStatus", Steps.PROCESS);
            this.getModel().setValue("sxq0_upload", this.getModel().getValue("sxq0_dispicture", currentindex));
        }
        else {
            currentStepMap.put("currentStep", i-1);
            currentStepMap.put("currentStatus", Steps.FINISH);
            this.getView().setVisible(false, "sxq0_buttonap");
        }
        // 更新当前节点
        wizard.setWizardCurrentStep(currentStepMap);
    }

    @Override
    public void update(StepEvent stepEvent) {
        int step = stepEvent.getValue();
        this.getView().setVisible(true, "sxq0_buttonap");
        this.getView().setEnable(true,"sxq0_upload");
        String currentstep = this.getPageCache().get("currentstep");
        if(currentstep!=null) {
            if(!currentstep.equals(String.valueOf(step))) {
                this.getView().setVisible(false, "sxq0_buttonap");
                this.getView().setEnable(false,"sxq0_upload");
            }else {
                this.getView().setVisible(true, "sxq0_buttonap");
                this.getView().setEnable(true,"sxq0_upload");
            }
        }else {
            this.getView().setVisible(false, "sxq0_buttonap");
        }
        this.getModel().setValue("sxq0_upload", this.getModel().getValue("sxq0_dispicture", step));
    }
}