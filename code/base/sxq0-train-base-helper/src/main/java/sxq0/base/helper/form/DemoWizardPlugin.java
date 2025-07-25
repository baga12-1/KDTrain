package sxq0.base.helper.form;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.form.container.Wizard;
import kd.bos.form.control.Button;
import kd.bos.form.control.Steps;
import kd.bos.form.control.StepsOption;
import kd.bos.form.control.events.BeforeClickEvent;
import kd.bos.form.control.events.StepEvent;
import kd.bos.form.control.events.WizardStepsListener;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.sdk.plugin.Plugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态表单插件
 */
public class DemoWizardPlugin extends AbstractFormPlugin implements Plugin, WizardStepsListener {
    //需求：通过向导控件来实现一个电商订单的功能，以状态流转为例,订单有五种状态,分别是下单,付款,发货,收货,完结,
    // 每当一个状态想要完成,必须上传一张图片,并点击完成按钮,状态就变化, 等待下一次完成。
    public void initialize()
    {//初始化监听控件
        addClickListeners("bidt_buttonap");
        Wizard wizard = this.getControl("bidt_wizardap");
        wizard.addWizardStepsListener(this);
    }
    private HashMap statusMap=new HashMap();
    {        //初始化单据状态的映射关系
        statusMap.put("1", "下单");
        statusMap.put("2", "付款");
        statusMap.put("3", "发货");
        statusMap.put("4", "收货");
        statusMap.put("5", "完结");
    }

    @Override
    public void beforeClick(BeforeClickEvent evt) {
        //把处理逻辑放在beforeClick上,因为点击上已经有保存操作了
        super.beforeClick(evt);
        Button button=(Button) evt.getSource();
        if (button.getKey().equals("bidt_buttonap")) {
            if( this.getModel().getValue("bidt_picturefield")==null||"".equals(this.getModel().getValue("bidt_picturefield"))) {
                this.getView().showMessage("必须上传照片！");
                evt.setCancel(true);
            }else{
                //获取当前单据体的内容
                DynamicObjectCollection xxrecordentrycollection=this.getModel().getEntryEntity("bidt_entry_detail");
                //是否是最后一步的变量
                boolean ifEndFlag=true;
                for(int k=0;k<xxrecordentrycollection.size();k++) {
                    DynamicObject entrydata=xxrecordentrycollection.get(k);
                    //进行中的步骤改成已完成
                    if("B".equals(entrydata.get("bidt_wizardstatus"))) {
                        entrydata.set("bidt_wizardstatus", "C");
                        entrydata.set("bidt_picturefield1", this.getModel().getValue("bidt_picturefield"));
                        entrydata.set("bidt_finishtime",new Date());

                    }
                    //未进行的改成进行中
                    if("A".equals(entrydata.get("bidt_wizardstatus"))&&ifEndFlag){
                        entrydata.set("bidt_wizardstatus", "B");
                        //如果有未进行的改成进行中,则说明不是最后一步
                        ifEndFlag=false;
                        //并且把单据状态改成对应的状态
                        this.getModel().setValue("billstatus", k);
                        //改完下一条就退出循环
                        break;
                    }
                }

                if(ifEndFlag) {
                    //所有步骤全部完成，则 都整单完成
                    this.getModel().setValue("billstatus", "5");
                }
                this.getView().updateView();
            }

        }

    }

    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        //获取当前订单状态
        String billstatus= (String) this.getModel().getValue("billstatus");
        //如果是默认的下单状态就要初始化一下
        if("1".equals(billstatus)) {
            for (int i=0;i<5;i++) {
                this.getModel().setValue("bidt_billstatusfield",i+1,i);
            }
            this.getModel().setValue("bidt_wizardstatus","B",0);
        }

        Wizard wizard = this.getControl("bidt_wizardap");
        // 获取设计时的步骤条设置
        List<StepsOption> stepsOptions = wizard.getStepsOptions();
        //初始化步骤条
        stepsOptions.clear();
        DynamicObjectCollection xxrecordentrycollection=this.getModel().getEntryEntity("bidt_entry_detail");


        int i=0;
        //设置当前进行到哪步的变量---当前步数,
        int currentindex=-1;
        //每次进入的时候重置一下页面缓存的当前步数,currentstep这个会在update方法中使用的
        if(this.getPageCache().get("currentstep")!=null) {
            this.getPageCache().remove("currentstep");
        }
        for(DynamicObject entrydata:xxrecordentrycollection ) {
            StepsOption stepsOption0 = new StepsOption();
            String levelobject = entrydata.getString("bidt_billstatusfield");
            stepsOption0.setTitle(new LocaleString((String) statusMap.get(levelobject)));
            //设置完成时间
            Date finishtime=(Date) entrydata.get("bidt_finishtime");
            if(finishtime!=null) {
                String pattern = "yyyy-MM-dd HH:mm:ss";
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
                String timeStr=simpleDateFormat.format(finishtime);
                stepsOption0.setDescription(new LocaleString(timeStr));
            }else {
                stepsOption0.setDescription(new LocaleString(" "));
            }

            //根据不同的单据体内的状态设定不一样步骤状态
            if("C".equals(entrydata.get("bidt_wizardstatus"))) {
                stepsOption0.setStatus(Steps.FINISH);
                i++;
            }
            else if("A".equals(entrydata.get("bidt_wizardstatus"))){
                stepsOption0.setStatus(Steps.PROCESS);
                i++;
            }else if("B".equals(entrydata.get("bidt_wizardstatus"))) {
                currentindex=i;
                this.getPageCache().put("currentstep",String.valueOf(currentindex));
                i++;
            }
            stepsOptions.add(stepsOption0);
        }
        // 更新步骤条设置
        wizard.setWizardStepsOptions(stepsOptions);
        // 设置当前节点
        Map<String, Object> currentStepMap = new HashMap<>();

        if(currentindex>=0) {
            currentStepMap.put("currentStep", currentindex);
            currentStepMap.put("currentStatus", Steps.PROCESS);
            this.getModel().setValue("bidt_picturefield", this.getModel().getValue("bidt_picturefield1", i-1));
        }else {
            currentStepMap.put("currentStep", i-1);
            currentStepMap.put("currentStatus", Steps.FINISH);
            this.getView().setVisible(false, "bidt_buttonap");
        }
        // 更新当前节点
        wizard.setWizardCurrentStep(currentStepMap);

    }
    //设置完成按钮可见性
    @Override
    public void update(StepEvent paramStepEvent) {
        // TODO Auto-generated method stub
        int stepint = paramStepEvent.getValue();
        this.getView().setVisible(true, "bidt_buttonap");
        String currentstep = this.getPageCache().get("currentstep");
        if(currentstep!=null) {
            if(!currentstep.equals(String.valueOf(stepint))) {
                this.getView().setVisible(false, "bidt__buttonap");
            }else {
                this.getView().setVisible(true, "bidt_buttonap");
            }
        }else {
            this.getView().setVisible(false, "bidt_buttonap");
        }
        this.getModel().setValue("bidt_picturefield", this.getModel().getValue("bidt_picturefield1", stepint));
    }

}