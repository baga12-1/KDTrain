package sxq0.base.helper.workflow;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import kd.bos.workflow.api.AgentExecution;
import kd.bos.workflow.engine.extitf.IWorkflowPlugin;
import kd.sdk.plugin.Plugin;

import java.util.List;

/**
 * 工作流插件
 */
public class FindDirectSuperiorOpPlugin  implements Plugin ,IWorkflowPlugin{
    @Override
    public List<Long> calcUserIds(AgentExecution execution) {
        String businessKey = execution.getBusinessKey();
        QFilter f1 = new QFilter("id", QCP.equals, Long.parseLong(businessKey));
        DynamicObject creator = QueryServiceHelper.queryOne("sxq0_personnel_apply", "creator,org.name", f1.toArray());
        long creatorId = creator.getLong("creator");
        long userMainOrgId = UserServiceHelper.getUserMainOrgId(creatorId);
        List<Long> managersOfOrg = UserServiceHelper.getManagersOfOrg(userMainOrgId);
        return managersOfOrg;
    }
}
