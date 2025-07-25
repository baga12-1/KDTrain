package sxq0.base.helper.workflow;


import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.workflow.api.AgentExecution;
import kd.bos.workflow.api.constants.WFTaskResultEnum;
import kd.bos.workflow.engine.dynprocess.freeflow.WFDecisionOption;
import kd.bos.workflow.engine.extitf.IWorkflowPlugin;
import kd.sdk.plugin.Plugin;

/**
 * 工作流插件
 */
public class SecondAuditPointPlugin implements Plugin, IWorkflowPlugin {
    @Override
    public void notify(AgentExecution execution) {
        //获取审批意见
        String taskType = execution.getCurrentTaskResult(WFTaskResultEnum.auditType).toString();
        String businessKey = execution.getBusinessKey();
        DynamicObject disPlan = BusinessDataServiceHelper.loadSingle(businessKey, "sxq0_personnel_apply");
        if(taskType.equals(WFDecisionOption.AUDIT_TYPE_APPROVE)){
            //如果是同意，设置单据状态为已审核
            disPlan.set("billstatus","C");
            //向申请人发送邮件通知
            String billNo = disPlan.getString("billno");
            //获取申请人的邮箱
            long applyerId = disPlan.getDynamicObject("creator").getLong("id");
            QFilter f1 = new QFilter("id", QCP.equals, applyerId);
            DynamicObject emailObj = QueryServiceHelper.queryOne("bos_user", "email", f1.toArray());

//            if(emailObj != null && !StringUtils.isBlank(emailObj.getString("email"))){
//                EmailInfo emailInfo = new EmailInfo();
//                emailInfo.setTitle("来自苍穹的邮件");
//                emailInfo.setContent("单据编号为【"+billNo+"】的人员申请单审核通过");
//
//                List<String> receivers =new ArrayList<>();
//                String email = emailObj.getString("email");
//                receivers.add(email);
//
//                emailInfo.setReceiver(receivers);
//                EmailHandler.sendEmail(emailInfo);
//            }
        }else if(taskType.equals(WFDecisionOption.AUDIT_TYPE_REJECT)){
            //驳回改为已提交
            disPlan.set("billstatus","B");
        }
        SaveServiceHelper.saveOperate("sxq0_personnel_apply", new DynamicObject[]{disPlan}, OperateOption.create());
        IWorkflowPlugin.super.notify(execution);
    }
}