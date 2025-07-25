package sxq0.base.helper.botp;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.ExtendedDataEntitySet;
import kd.bos.entity.botp.plugin.AbstractConvertPlugIn;
import kd.bos.entity.botp.plugin.args.AfterConvertEventArgs;
import kd.sdk.plugin.Plugin;

/**
 * 单据转换插件
 */
public class ApplyToRecordPlugin extends AbstractConvertPlugIn implements Plugin {
    @Override
    public void afterConvert(AfterConvertEventArgs e) {
        ExtendedDataEntitySet targetExtDataEntitySet = e.getTargetExtDataEntitySet();
        ExtendedDataEntity[] records = targetExtDataEntitySet.FindByEntityKey("sxq0_disinfection_records");
        for (ExtendedDataEntity record : records) {
            DynamicObject dataEntity = record.getDataEntity();
            long id = dataEntity.getLong("id");
//            long l = DB.genGlobalLongId();
//            dataEntity.set("id",l);
            id = dataEntity.getLong("id");

        }
    }
}