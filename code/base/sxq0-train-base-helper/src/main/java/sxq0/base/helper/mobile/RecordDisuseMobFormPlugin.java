package sxq0.base.helper.mobile;

import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.control.Button;
import kd.bos.form.events.CustomEventArgs;
import kd.bos.form.plugin.AbstractMobFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.EventObject;

/**
 * 动态表单插件(移动端)
 */
public class RecordDisuseMobFormPlugin extends AbstractMobFormPlugin implements Plugin {
    @Override
    public void registerListener(EventObject e) {
        this.addClickListeners("sxq0_disuse");
    }

    @Override
    public void click(EventObject evt) {
//        HashMap<String,String> args = new HashMap<>();
//        // 调用钉钉扫码，qr：二维码扫码框；bar：条形码扫码框
//        args.put("type", "qr");
//        HashMap<String, Object> map = new HashMap<>();
//        map.put("method", "scanQRCode");
//        map.put("args", args);
//        this.getView().executeClientCommand("callAPPApi", map);
        Button source = (Button) evt.getSource();
        if(source != null && "sxq0_disuse".equals(source.getKey())){
            Object obj = this.getModel().getValue("sxq0_disrecord_no");
            if(obj == null){
                this.getView().showTipNotification("消毒记录单编号为空");
                return;
            }
            DynamicObject id = QueryServiceHelper.queryOne("sxq0_disinfection_records"
                    , "id"
                    , new QFilter("billno", QCP.equals, obj).toArray());
            Object[] objs = new Object[]{id.getLong("id")};
            OperationResult result = OperationServiceHelper.executeOperate("disuse"
                    , "sxq0_disinfection_records"
                    , objs
                    , OperateOption.create());
            if(result.isSuccess()){
                this.getView().showTipNotification("废弃成功");
            }else{
                this.getView().showTipNotification("废弃失败");
            }
        }
    }

    @Override
    public void customEvent(CustomEventArgs e) {
        String eventName = e.getEventName();
        String eventArgs = e.getEventArgs();
        int i = eventArgs.indexOf(":");
        int i1 = eventArgs.lastIndexOf("}");
        String substring = eventArgs.substring(i+2, i1-1);
        this.getModel().setValue("sxq0_disrecord_no",substring);
    }

}