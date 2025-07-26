package sxq0.base.helper.task;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.exception.KDException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.schedule.executor.AbstractTask;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.servicehelper.workflow.MessageCenterServiceHelper;
import kd.bos.workflow.engine.msg.info.MessageInfo;
import kd.sdk.plugin.Plugin;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台任务插件
 */
public class ExpireTask extends AbstractTask implements Plugin {


    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        //审批通过的人员申请单，如果过了申请进入时间的第二天零点还没开始消毒，则单据自动关闭（状态为废弃），
        // 且发送消息（消息中心通知和邮件）提示员工消毒申请已过期。

        LocalDate now = LocalDate.now();
        //找出审核但前一天还没开始消毒的单据
        QFilter f1 = new QFilter("billstatus", QCP.equals, "C");
        QFilter f2 = new QFilter("sxq0_applyentertime", QCP.less_than, now);
        DynamicObjectCollection applys = QueryServiceHelper.query("sxq0_personnel_apply", "id,creator", f1.and(f2).toArray());
        List<Long> applyDocId = applys.stream().map(apply -> apply.getLong("id")).collect(Collectors.toList());

        List<Long> applyers = applys.stream().map(apply -> apply.getLong("creator")).collect(Collectors.toList());
        QFilter f4 = new QFilter("id", QCP.in, applyers);
        DynamicObjectCollection emails = QueryServiceHelper.query("bos_user", "id,email", f4.toArray());

        QFilter f3 = new QFilter("id", QCP.in, applyDocId);
        //将这些单据状态改为废弃，并发送消息和邮件去通知申请人
        Object[] objects = new Object[applyDocId.size()];
        int i = 0 ;
        for (Long aLong : applyDocId) {
            objects[i++] = aLong;
        }
        DynamicObject[] applyDocs = BusinessDataServiceHelper.load(objects, EntityMetadataCache.getDataEntityType("sxq0_personnel_apply"));
        for (DynamicObject applyDoc : applyDocs) {
            applyDoc.set("billstatus","F");
        }

        SaveServiceHelper.saveOperate("sxq0_personnel_apply",applyDocs, OperateOption.create());
        //发消息
        for (DynamicObject apply : applys) {
            //获取申请人姓名，申请时间，申请进入车间
            DynamicObject applyDoc = Arrays.stream(applyDocs).filter(o -> o.getLong("id") == apply.getLong("id")).findAny().orElse(null);
            if(applyDoc == null) {
                continue;
            }
            String name = applyDoc.getDynamicObject("creator").getString("name");
            String date = applyDoc.getDate("sxq0_applyentertime").toString().substring(0,10);
            String workshop = applyDoc.getDynamicObject("sxq0_applyenterworkshop").getString("name");
            StringBuilder sb = new StringBuilder();
            String content = sb.append("亲爱的棕熊工厂员工")
                    .append(name)
                    .append(",你所提交的")
                    .append(date)
                    .append("时间进入")
                    .append(workshop)
                    .append("的申请单已超期并自动关闭，如还需进入车间，请重新发起申请！").toString();
//            MessageInfo message = new MessageInfo();
            ArrayList<Long> receivers = new ArrayList<>();
            receivers.add(apply.getLong("creator"));
//            message.setUserIds(receivers);//取领用申请人为消息接收人
//            message.setSenderId(Long.parseLong(RequestContext.get().getUserId()));//当前登录用户为消息发送人
//            message.setSendTime(new Date(System.currentTimeMillis()));
//            message.setType(MessageInfo.TYPE_MESSAGE);//消息类型为通知
//            message.setEntityNumber("sxq0_personnel_apply");
//            message.setBizDataId(Long.valueOf(String.valueOf(apply.getLong("id"))));
//            //设置消息场景和设置消息模板二选一即可
//            message.setTplScene("personnel_apply_expire");
//            String email = getUserEmail(apply.getLong("creator"), emails);
//            if(email != null){
//                EmailInfo emailInfo = new EmailInfo();
//                emailInfo.setTitle("来自棕熊工厂的邮件");
//                emailInfo.setContent(content);
//                List<String> receiver =new ArrayList<>();
//                receiver.add(email);
//                emailInfo.setReceiver(receiver);
//                EmailHandler.sendEmail(emailInfo);
//            }

            MessageInfo messageInfo = new MessageInfo();
            LocaleString title = new LocaleString();
            title.setLocaleValue_zh_CN("人员申请单过期通知");
            messageInfo.setMessageTitle(title);

            messageInfo.setMessageContent(new LocaleString(content));
            messageInfo.setUserIds(receivers);
//            messageInfo.setType(MessageInfo.TYPE_MESSAGE);
            // 发送“通知”消息
            messageInfo.setType("message");
            messageInfo.setTag("MSGTest");
            //发送消息

            MessageCenterServiceHelper.sendMessage(messageInfo);

//            //TODO 发送邮件通知（未成功）
//            MessageInfo messageEmail = new MessageInfo();
//            messageEmail.setUserIds(receivers);//取领用申请人为消息接收人
////            message.setSenderId(Long.parseLong(RequestContext.get().getUserId()));//当前登录用户为消息发送人
//            messageEmail.setSendTime(new Date(System.currentTimeMillis()));
//            messageEmail.setType(MessageInfo.TYPE_MESSAGE);//消息类型为通知
//            messageEmail.setEntityNumber("sxq0_personnel_apply");
//            messageEmail.setBizDataId(Long.valueOf(String.valueOf(apply.getLong("id"))));
//            //设置消息场景和设置消息模板二选一即可
//            messageEmail.setTplScene("personnel_apply_expire_email");
//            long l = MessageCenterServiceHelper.sendMessage(messageEmail);
//            MessageCenterServiceHelper.sendMessage(message);
        }

    }

    //获取用户邮箱
    private String getUserEmail(Long userId,DynamicObjectCollection emails){
        DynamicObject email = emails.stream().filter(emailObj -> emailObj != null
                && !StringUtils.isBlank(emailObj.getString("email"))
                && emailObj.getLong("id") == userId).findAny().orElse(null);
        return email == null ? null : email.getString("email");
    }
}