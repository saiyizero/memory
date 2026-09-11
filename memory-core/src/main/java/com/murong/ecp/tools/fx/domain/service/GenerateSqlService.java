package com.murong.ecp.tools.fx.domain.service;

import com.murong.ecp.tools.fx.infrastructure.repository.po.EnumDictPO;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class GenerateSqlService {
    /**
     * 根据枚举生成 t_bui_hlp 表的 insert 语句
     */
    public List<String> generateBuiHlpInsertSql(List<EnumDictPO> enumList) {
        List<String> sqlList = new ArrayList<>();
        int order = 1;
        for (EnumDictPO po : enumList) {
            String fldExp = po.getDescCn() == null ? "" : po.getDescCn();
            String fldEnExp = po.getDescEn() == null ? "" : po.getDescEn();
            String fldExpI18n = String.format("t_bui_hlp:fld_exp_i18n:REF_%s_%s", po.getDbName(), po.getEnumVal());
            String sql = String.format(
                "insert into bsdbui.t_bui_hlp (fld_ord, fld_nm, fld_val, fld_exp, fld_typ, fld_en_exp, tm_smp, nod_id, upd_dt, upd_tm, upd_desc, cre_dt, cre_tm, rmk, req_id, cre_opr, upd_opr, ath_opr, show_flg, inp_tm_smp, ath_tm_smp, chk_tm_smp, app_name, tenant_id, timezone, fld_exp_i18n)\n" +
                "VALUES (%d, '%s', '%s', '%s', 'x', '%s', '20220923050937', 'mbu_region.mbu', ' ', ' ', ' ', '20220923', ' ', ' ', ' ', 'keT00001', 'keT00001', ' ', ' ', '20220923050937', ' ', '20220923050937', 'neo', ' ', ' ', '%s');",
                order++,
                po.getDbName(),
                po.getEnumVal(),
                fldExp.replace("'", "''"),
                fldEnExp.replace("'", "''"),
                fldExpI18n
            );
            sqlList.add(sql);
        }
        return sqlList;
    }

    /**
     * 根据枚举生成 t_pub_i18n 表的 insert 语句
     */
    public List<String> generatePubI18nInsertSql(List<EnumDictPO> enumList) {
        List<String> sqlList = new ArrayList<>();
        for (EnumDictPO po : enumList) {
            String fldExpI18n = String.format("t_bui_hlp:fld_exp_i18n:REF_%s_%s", po.getDbName(), po.getEnumVal());
            // 中文
            String sqlZh = String.format(
                "insert into bsdbui.t_pub_i18n (id, lang, value, tenant_id, timezone, req_id, nod_id, tm_smp)\n" +
                "values ('%s', 'zh-CN', '%s', ' ', ' ', ' ', ' ', ' ');",
                fldExpI18n,
                po.getDescCn() == null ? "" : po.getDescCn().replace("'", "''")
            );
            sqlList.add(sqlZh);
            // 英文
            String sqlEn = String.format(
                "insert into bsdbui.t_pub_i18n (id, lang, value, tenant_id, timezone, req_id, nod_id, tm_smp)\n" +
                "values ('%s', 'en-US', '%s', ' ', ' ', ' ', ' ', ' ');",
                fldExpI18n,
                po.getDescEn() == null ? "" : po.getDescEn().replace("'", "''")
            );
            sqlList.add(sqlEn);
        }
        return sqlList;
    }

    /**
     * 生成清理 t_bui_hlp 表的 delete 语句
     */
    public List<String> generateBuiHlpDeleteSql(List<EnumDictPO> enumList) {
        List<String> sqlList = new ArrayList<>();
        for (EnumDictPO po : enumList) {
            String sql = String.format(
                "delete from bsdbui.t_bui_hlp WHERE fld_nm = '%s' AND fld_val = '%s';",
                po.getDbName(),
                po.getEnumVal()
            );
            sqlList.add(sql);
        }
        return sqlList;
    }

    /**
     * 生成清理 t_pub_i18n 表的 delete 语句
     */
    public List<String> generatePubI18nDeleteSql(List<EnumDictPO> enumList) {
        List<String> sqlList = new ArrayList<>();
        for (EnumDictPO po : enumList) {
            String fldExpI18n = String.format("t_bui_hlp:fld_exp_i18n:REF_%s_%s", po.getDbName(), po.getEnumVal());
            String sql = String.format(
                "delete from bsdbui.t_pub_i18n WHERE id = '%s';",
                fldExpI18n
            );
            sqlList.add(sql);
        }
        return sqlList;
    }
}
