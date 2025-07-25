package sxq0.base.helper.openapi;

import kd.bos.bill.AbstractBillWebApiPlugin;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.exception.KDBizException;
import kd.bos.openapi.common.custom.annotation.ApiController;
import kd.bos.openapi.common.custom.annotation.ApiGetMapping;
import kd.bos.openapi.common.custom.annotation.ApiParam;
import kd.bos.openapi.common.custom.annotation.ApiPostMapping;
import kd.bos.openapi.common.result.CustomApiResult;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.AttachmentServiceHelper;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import kd.bos.servicehelper.util.DynamicObjectSerializeUtil;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 开放API插件
 */
@ApiController(value = "personnelApply",desc = "人员申请单单据")
public class PersonnelApplyApiPlugin extends AbstractBillWebApiPlugin implements Plugin {
    @ApiPostMapping(value = "/savePersonnelApply")
    public CustomApiResult<String> savePersonnelApply(@ApiParam(value = "用户id",required = true) Long applierId
                                                     ,@ApiParam(value = "申请进入车间id") Long wsId
                                                     ,@ApiParam(value = "申请进入时间")Date applyDate
                                                     ,@ApiParam(value = "附件地址") String attachUrl){
        DynamicObject newApply = BusinessDataServiceHelper.newDynamicObject("sxq0_personnel_apply",true,OperateOption.create());
        applierId = UserServiceHelper.getCurrentUserId();
        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("bos_org", new QFilter[]{new QFilter("id", QCP.equals, 2258817831518864384L)});
        newApply.set("creator",applierId);
        newApply.set("sxq0_applyenterworkshop",dynamicObject);
        newApply.set("sxq0_applyentertime",applyDate);

        List<Map<String,Object>> attachments = new ArrayList<>();

        // 创建单个附件Map
        Map<String,Object> attachment = new HashMap<>();
        attachment.put("name", "身份证"); // 文件名
        attachment.put("url", attachUrl); // 文件URL
        attachment.put("size", 1); // 文件大小
        attachment.put("uid", "rc-upload-" + System.currentTimeMillis()); // 唯一标识
        OperationResult result = SaveServiceHelper.saveOperate("sxq0_personnel_apply", new DynamicObject[]{newApply}, OperateOption.create());
        String message = result.getValidateResult().getMessage();
        // 添加到附件列表
        attachments.add(attachment);
        Object applyDocId = result.getSuccessPkIds().get(0);
        AttachmentServiceHelper.upload("sxq0_personnel_apply",applyDocId,"attachmentpanel",attachments);
        return CustomApiResult.success("1");
    }
    @ApiGetMapping(value = "/getPersonnelApply")
    public CustomApiResult<String> getPersonnelApply(@ApiParam("单据id") Long id){
        if(id == null){
            throw new KDBizException("id为空");
        }
        boolean exists = QueryServiceHelper.exists("sxq0_personnel_apply", new QFilter("id", QCP.equals, id).toArray());
        id = 2263914767364129792L;
        if(!exists){
            return CustomApiResult.fail("0001","单据id不存在");
        }
        DynamicObject applyDoc = BusinessDataServiceHelper.loadSingle(id, "sxq0_personnel_apply");


        String result = DynamicObjectSerializeUtil.serialize(new Object[]{applyDoc}, applyDoc.getDynamicObjectType());
        Object[] deserialize = DynamicObjectSerializeUtil.deserialize(result, applyDoc.getDynamicObjectType());
        return CustomApiResult.success(result);
    }
}