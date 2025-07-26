package sxq0.base.helper.Operation;

import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.AttachmentServiceHelper;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.web.actions.utils.FilePathUtil;
import kd.sdk.plugin.Plugin;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

/**
 * 单据操作插件
 */
public class StartDisOpPlugin extends AbstractOperationServicePlugIn implements Plugin {
    private List<Map<String, Object>> attachmentData;
    private Object pk;
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        e.getFieldKeys().addAll(this.billEntityType.getAllFields().keySet());
    }

    @Override
    public void beginOperationTransaction(BeginOperationTransactionArgs e) {
        DynamicObject[] data = e.getDataEntities();
        DynamicObject startDisDoc = data[0];
        //获取选中的申请单申请进入部门组织使用的消毒方案
        Object rowId = startDisDoc.getPkValue();
        DynamicObject applyDoc = BusinessDataServiceHelper.loadSingle(rowId,"sxq0_personnel_apply");
        //获取申请进入车间
        DynamicObject workshop = applyDoc.getDynamicObject("sxq0_applyenterworkshop");
        //获取该车间使用的消毒方案（可能没有分配）
        QFilter f1 = new QFilter("sxq0_entryentity1.sxq0_orgfield.id", QCP.equals, workshop.getPkValue());
        DynamicObject disPlan = BusinessDataServiceHelper.loadSingle("sxq0_disinfectionplan", f1.toArray());
        if(disPlan == null){
            //不存在消毒方案
            e.setCancelOperation(true);
            throw new KDBizException("申请进入的车间尚未分配消毒方案，请联系负责人分配消毒方案");
        }
        //生成消毒记录单
        DynamicObject newRecord = BusinessDataServiceHelper.newDynamicObject("sxq0_disinfection_records", true, OperateOption.create());
        //设置字段值
        newRecord.set("sxq0_applyer",applyDoc.getDynamicObject("creator").getPkValue());
        newRecord.set("sxq0_applyenterworkshop",applyDoc.getDynamicObject("sxq0_applyenterworkshop").getPkValue());
        newRecord.set("sxq0_scheme",disPlan.getPkValue());
        newRecord.set("sxq0_appenterdate",applyDoc.getDate("sxq0_applyentertime"));
        newRecord.set("sxq0_picturefield1",applyDoc.getString("sxq0_picturefield"));

        //遍历消毒方案的消毒等级、消毒步骤赋值到分录
        DynamicObjectCollection rows = newRecord.getDynamicObjectCollection("entryentity");
        //消毒等级单据体
        DynamicObjectCollection levelEntry = disPlan.getDynamicObjectCollection("sxq0_entryentity");
        for(int i = 0 ; i < 3 ; i++){
            DynamicObject level = levelEntry.get(i);
            //消毒步骤单据体
            DynamicObjectCollection stepEntry = level.getDynamicObjectCollection("sxq0_subentryentity");
            for (DynamicObject step : stepEntry) {
                //消毒步骤为空跳过
                if(step.getDynamicObject("sxq0_basedatafield1") == null){
                    continue;
                }
                DynamicObject newRow = rows.addNew();
                newRow.set("sxq0_basedatafield",level.getDynamicObject("sxq0_basedatafield"));
                newRow.set("sxq0_basedatafield1",step.getDynamicObject("sxq0_basedatafield1").getPkValue());
            }
        }
        //修改申请单的状态为消毒中
        applyDoc.set("billstatus","D");
        //消毒记录单步骤分录的第一条分录消毒状态为进行中，其他分录的消毒状态为未进行
        for(int i = 0 ; i < rows.size(); i++){
            if(i == 0){
                rows.get(i).set("sxq0_billstatusfield","B");
            }else{
                rows.get(i).set("sxq0_billstatusfield","A");
            }
        }
        //保存修改后的申请单和创建的记录单
        OperationResult recordResult = SaveServiceHelper.saveOperate("sxq0_disinfection_records", new DynamicObject[]{newRecord}, OperateOption.create());
        if(!recordResult.isSuccess()){
            throw new KDBizException("消毒记录单保存失败");
        }
        List<Object> successPkIds = recordResult.getSuccessPkIds();
        //申请单附件面板赋值到记录单附件面板
        attachmentData = AttachmentServiceHelper.getAttachments("sxq0_personnel_apply", applyDoc.getLong("id"), "attachmentpanel");
        attachmentData.forEach(attach -> {
            try {
                //源附件数据的lastModified是timestamp，会出现强转long错误在此置null
                attach.put("lastModified", null);
                //源附件数据的url已经过URL编码，需要在此先解码获取原始下载路径，AttachmentServiceHelper.upload()会进行URL编码
                attach.put("url", getPathFromDownloadUrl(URLDecoder.decode(String.valueOf(attach.get("url")), "UTF-8")));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        //调用AttachmentServiceHelper.upload(formId, pkId, attachKey,  attachments)将附件数据上传到目标附件面板
//        AttachmentServiceHelper.upload(getView().getEntityId(), getModel().getValue("id"), "attachmentpanel", attachmentData);
        pk = successPkIds.get(0);

        //将记录单id赋值回申请单
        applyDoc.set("sxq0_record",successPkIds.get(0).toString());

        SaveServiceHelper.saveOperate("sxq0_personnel_apply", new DynamicObject[]{applyDoc}, OperateOption.create());

        //将保存的记录单id存起来，列表afterdooperation方法去打开新增的记录单
        this.getOption().setVariableValue("id",pk.toString());
    }
    private String getPathFromDownloadUrl(String url) throws IOException {
        String path = StringUtils.substringAfter(url, "path=");
        path = URLDecoder.decode(path, "UTF-8");
        return FilePathUtil.dealPath(path, "attach");
    }

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        AttachmentServiceHelper.upload("sxq0_disinfection_records",pk,"attachmentpanel",attachmentData);
    }
}