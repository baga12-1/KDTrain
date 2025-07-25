package sxq0.base.helper.openapi;

import kd.bos.bill.AbstractBillWebApiPlugin;
import kd.bos.openapi.common.custom.annotation.ApiController;
import kd.bos.openapi.common.custom.annotation.ApiParam;
import kd.bos.openapi.common.custom.annotation.ApiPostMapping;
import kd.bos.openapi.common.result.CustomApiResult;
import kd.sdk.plugin.Plugin;

/**
 * 开放API插件
 */
@ApiController(value = "TestIntegrate", desc = "测试集成单")
public class StudyOpenApiPlugin extends AbstractBillWebApiPlugin implements Plugin {
    @ApiPostMapping("/save")
    public CustomApiResult<String> saveBill(@ApiParam("单据编号") String billno,@ApiParam("描述") String desc){
        System.out.println("测试git提交");
        System.out.println("测试git提交2");
         System.out.println("测试idea拉取");
        return CustomApiResult.success("保存成功");
    }
}
