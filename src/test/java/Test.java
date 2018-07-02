import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import sqlrunner.StatisticDate;
import sqlrunner.generator.SQLCodeGenerator;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String path = "/mnt/navi/136750836/2017/35715029e0a2661db8b8763528b3f4a5.xlsx";
		String root = "\\\\file02.gvl.local/NAVI";
	}

	private static void testStatisticDate() throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date d1 = sdf.parse("2015-01-11 14:22:00");
		Date d2 = sdf.parse("2015-02-28 13:44:00");
		StatisticDate sd = new StatisticDate();
		sd.addValue(d1);
		sd.addValue(d2);
		System.out.println(sd.toString());
	}
	
	
	public static void test1() {
		byte[] ba = new byte[] {0x23, 0x34, 0x66};
		StringBuilder sb = new StringBuilder();
		for (byte b : ba) {
			sb.append(Integer.toHexString(b));
		}
		System.out.println(sb.toString());
	}
	
	public static String toHexString(byte[] bin) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bin) {
			sb.append(Integer.toHexString(b));
		}
		return sb.toString();
	}
	
	public static void testSQLDynamic() {
		StringBuilder sql = new StringBuilder();
		sql.append("WITH data (\n");
		sql.append("    product_id,\n");
		sql.append("    participation_id_core, participant_id, shooting_days, takes, ensemblesize, role_id, function_id, participation_rating,\n");
		sql.append("    rightsownership_id_core, right_holder_id, previous_right_holder_id, share_percentage, region_ids, period_from, period_to, use_type_ids,\n");
		sql.append("    use_type_id, account_type_id, mandate_id, region_id, collecting_society_id,\n");
		sql.append("    valid,\n");
		sql.append("    monetarized_value, payout_share, bibale_rating, bibale_owner_utilization, rating, owner_utilization,\n");
		sql.append("    product_usage_minutes_initial, product_usage_minutes_rated, product_usage_minutes_degraded, product_usage_minutes_projected,\n");
		sql.append("    usage_medium_id, business_partner_type_id,\n");
		sql.append("    year, inserted_at)\n");
		sql.append("AS (SELECT\n");
		sql.append("      (SELECT x.product_id from dwh_vts_marts.product_dim x WHERE x.product_id_core = COALESCE (br.product_id, 0) AND x.inserted_at <= '2017-12-14 17:04:56.028' AND (x.modified_at IS NULL OR x.modified_at > '2017-12-14 17:04:56.028')),");
		sql.append("      p.participation_id,\n");
		sql.append("      COALESCE ((SELECT x.business_partner_id FROM dwh_vts_marts.business_partner_dim x WHERE x.business_partner_id_core = p.participant_id AND x.inserted_at <= '2017-12-14 17:04:56.028' AND (x.modified_at IS NULL OR x.modified_at > '2017-12-14 17:04:56.028')), 0),\n");
		sql.append("      p.shooting_days,\n");
		sql.append("      p.takes,\n");
		sql.append("      p.ensemblesize,\n");
		sql.append("      COALESCE ((SELECT x.role_id FROM dwh_vts_marts.role_dim x WHERE x.role_id_core = p.role_id AND x.inserted_at <= '2017-12-14 17:04:56.028' AND (x.modified_at IS NULL OR x.modified_at > '2017-12-14 17:04:56.028')), 0),\"\n");
		sql.append("      COALESCE ((SELECT x.function_id FROM dwh_vts_marts.function_dim x WHERE x.function_id_core = p.function_id AND x.inserted_at <= '2017-12-14 17:04:56.028' AND (x.modified_at IS NULL OR x.modified_at > '2017-12-14 17:04:56.028')), 0),\"\n");
		sql.append("      (SELECT pr.rating FROM dwh_vts_base.participation_result pr WHERE pr.run_id = '17c94d48-e0f4-11e7-818f-5d1146319eed' AND br.creation_id = pr.creation_id AND br.run_id = pr.run_id AND br.participation_id = pr.participation_id),\n");
		sql.append("      r.rightsownership_id,\n");
		sql.append("      COALESCE ((SELECT x.business_partner_id FROM dwh_vts_marts.business_partner_dim x WHERE x.business_partner_id_core = r.rightsholder AND x.inserted_at <= '2017-12-14 17:04:56.028' AND (x.modified_at IS NULL OR x.modified_at > '2017-12-14 17:04:56.028')), 0),\"\n");
		sql.append("      COALESCE ((SELECT x.business_partner_id FROM dwh_vts_marts.business_partner_dim x INNER JOIN dwh_vts_base.rightsownership y ON x.business_partner_id_core = y.rightsholder WHERE x.inserted_at <= '2017-12-14 17:04:56.028' AND (x.modified_at IS NULL OR x.modified_at > '2017-12-14 17:04:56.028') AND y.rightsownership_id = r.predecessor_id AND y.transmission = '2017-12-14 17:04:56.028'), 0),\n");
		sql.append("      r.share_0_through_100,\n");
		sql.append("      (SELECT (SELECT ARRAY_AGG(x.region_id ORDER BY x.region_id) FROM dwh_vts_marts.region_dim x WHERE x.region_id_core = ANY (v.include_region) AND x.inserted_at <= '2017-12-14 17:04:56.028' AND (x.modified_at IS NULL OR x.modified_at > '2017-12-14 17:04:56.028'))),\n");
		sql.append("      t.period_from,\n");
		sql.append("      t.period_to,\n");
		sql.append("      (SELECT (SELECT ARRAY_AGG(x.use_type_id ORDER BY x.use_type_id) FROM dwh_vts_marts.use_type_dim x WHERE x.use_type_id_core = ANY (v.include_usage) AND x.inserted_at <= '2017-12-14 17:04:56.028' AND (x.modified_at IS NULL OR x.modified_at > '2017-12-14 17:04:56.028'))),\n");
		sql.append("      COALESCE ((SELECT x.use_type_id FROM dwh_vts_marts.use_type_dim x INNER JOIN dwh_vts_base.usetypes y ON x.use_type_id_core = y.usetypes_id WHERE x.inserted_at <= '2017-12-14 17:04:56.028' AND (x.modified_at IS NULL OR x.modified_at > '2017-12-14 17:04:56.028') AND y.sysname = rr.usagetype_sysname and y.transmission = '2017-12-14 17:04:56.028'), 0),\n");
		sql.append("      COALESCE ((SELECT x.account_type_id FROM dwh_vts_marts.account_type_dim x INNER JOIN dwh_vts_base.account_type y ON x.account_type_id_core = y.account_type_id WHERE x.inserted_at <= '2017-12-14 17:04:56.028' AND (x.modified_at IS NULL OR x.modified_at > '2017-12-14 17:04:56.028') AND y.sysname = rr.account_type_sysname and y.transmission = '2017-12-14 17:04:56.028'), 0),\n");
		sql.append("      COALESCE ((SELECT x.mandate_id FROM dwh_vts_marts.mandate_dim x WHERE x.mandate_id_core = br.mandate_id AND x.inserted_at <= '2017-12-14 17:04:56.028' AND (x.modified_at IS NULL OR x.modified_at > '2017-12-14 17:04:56.028')), 0),\n");
		sql.append("      COALESCE ((SELECT x.region_id FROM dwh_vts_marts.region_dim x INNER JOIN dwh_vts_base.region y ON x.region_id_core = y.region_id WHERE x.inserted_at <= '2017-12-14 17:04:56.028' AND (x.modified_at IS NULL OR x.modified_at > '2017-12-14 17:04:56.028') AND y.sysname = rr.region_sysname AND y.transmission = '2017-12-14 17:04:56.028'), 0),\n");
		sql.append("      COALESCE ((SELECT x.business_partner_id FROM dwh_vts_marts.business_partner_dim x WHERE x.business_partner_id_core = rr.collecting_society AND x.inserted_at <= '2017-12-14 17:04:56.028' AND (x.modified_at IS NULL OR x.modified_at > '2017-12-14 17:04:56.028')), 0),\n");
		sql.append("      COALESCE (ror.valid, FALSE),\n");
		sql.append("      COALESCE (br.monetarized_value, (SELECT mr.monetarized_value FROM dwh_vts_base.rightsownership_mon_result mr WHERE mr.run_id = '17c94d48-e0f4-11e7-818f-5d1146319eed' AND br.creation_id = mr.creation_id AND br.run_id = mr.run_id AND br.rightsownership_id = mr.rightsownership_id AND br.participation_id = mr.participation_id AND br.product_id = mr.product_id AND br.mandate_id = mr.mandate_id)),\n");
		sql.append("      br.payout_share,\n");
		sql.append("      br.rating,\n");
		sql.append("      br.rating_ownerutilization,\n");
		sql.append("      ror.rating,\n");
		sql.append("      ror.rating_ownerutilization,\n");
		sql.append("      pr.usage_minutes_initial_value,\n");
		sql.append("      pr.usage_minutes_rated_value,\n");
		sql.append("      pr.usage_minutes_degraded_value,\n");
		sql.append("      pr.projected_value,\n");
		sql.append("      COALESCE ((SELECT x.usage_medium_id FROM dwh_vts_marts.usage_medium_dim x WHERE x.usage_medium_id_core = br.usagemedium_id AND x.inserted_at <= '2017-12-14 17:04:56.028' AND (x.modified_at IS NULL OR x.modified_at > '2017-12-14 17:04:56.028')), 0),\n");
		sql.append("      (SELECT t.business_partner_type_id FROM dwh_vts_marts.distribution_run_dim d INNER JOIN dwh_vts_marts.business_partner_type_dim t ON d.beneficiaries_group = t.business_partner_type AND t.inserted_at <= '2017-12-14 17:04:56.028' AND (t.modified_at IS NULL OR t.modified_at > '2017-12-14 17:04:56.028') WHERE d.distribution_run_id = 634),\n");
		sql.append("      rr.year, rr.started\n");
		sql.append("    FROM dwh_vts_base.vts_run_result rr\n");
		sql.append("      INNER JOIN dwh_vts_base.rightsownership_bibale_res br\n");
		sql.append("        ON rr.creation_id = br.creation_id AND rr.bibale_id = br.run_id\n");
		sql.append("      LEFT OUTER JOIN dwh_vts_base.product_result pr\n");
		sql.append("        ON rr.creation_id = pr.creation_id AND rr.bibale_id = pr.run_id AND br.product_id = pr.product_id\n");
		sql.append("      LEFT OUTER JOIN dwh_vts_base.rightsownership_result ror\n");
		sql.append("        ON br.creation_id = ror.creation_id AND br.run_id = ror.run_id AND br.rightsownership_id = ror.rightsownership_id AND br.participation_id = ror.participation_id AND br.product_id = ror.product_id AND COALESCE (ror.usagemedium_id, 0) = COALESCE (br.usagemedium_id) AND br.mandate_id = ror.mandate_id\n");
		sql.append("      LEFT OUTER JOIN dwh_vts_base.participation p\n");
		sql.append("        ON br.participation_id = p.participation_id AND p.transmission = '2017-12-14 17:04:56.028'\n");
		sql.append("      LEFT OUTER JOIN dwh_vts_base.rightsownership r\n");
		sql.append("        ON br.rightsownership_id = r.rightsownership_id AND r.transmission = '2017-12-14 17:04:56.028'\n");
		sql.append("      LEFT outer JOIN dwh_vts_base.tpu t\n");
		sql.append("        ON r.tpu_id = t.tpu_id AND t.transmission = '2017-12-14 17:04:56.028'\n");
		sql.append("      LEFT OUTER JOIN dwh_vts_base.tu v\n");
		sql.append("        ON t.tu_id = v.tu_id AND v.transmission = '2017-12-14 17:04:56.028'\n");
		sql.append("    WHERE rr.bibale_id = '17c94d48-e0f4-11e7-818f-5d1146319eed'\n");
		sql.append("          AND rr.abacus_id = '758872f1-e0f7-11e7-818f-5d1146319eed')\n");
		sql.append("-- place here the insert statement\n");
		sql.append("SELECT 634,\n");
		sql.append("  d.product_id,\n");
		sql.append("  d.participation_id_core, d.participant_id, d.shooting_days, d.takes, d.ensemblesize, d.role_id, d.function_id, d.participation_rating,\n");
		sql.append("  d.rightsownership_id_core, d.right_holder_id, d.previous_right_holder_id, d.share_percentage, d.region_ids, d.period_from, d.period_to, d.use_type_ids,\n");
		sql.append("  d.use_type_id, d.account_type_id, d.mandate_id, d.region_id, d.collecting_society_id,\n");
		sql.append("  d.valid,\n");
		sql.append("  d.monetarized_value, d.payout_share, d.bibale_rating, d.bibale_owner_utilization, d.rating, d.owner_utilization,\n");
		sql.append("  d.product_usage_minutes_initial, d.product_usage_minutes_rated, d.product_usage_minutes_degraded, d.product_usage_minutes_projected,\n");
		sql.append("  d.usage_medium_id,\n");
		sql.append("  COALESCE ((SELECT a.distribution_amount_id\n");
		sql.append("             FROM dwh_vts_marts.distribution_amount_dim a\n");
		sql.append("             WHERE a.business_partner_type_id = d.business_partner_type_id\n");
		sql.append("                   AND d.year = a.year\n");
		sql.append("                   AND d.use_type_id = a.use_type_id\n");
		sql.append("                   AND d.region_id = a.region_id\n");
		sql.append("                   AND d.account_type_id = a.account_type_id\n");
		sql.append("                   AND d.collecting_society_id = a.collecting_society_id), 0),\n");
		sql.append("  d.inserted_at\n");
		sql.append("FROM data d;");
		System.out.println(SQLCodeGenerator.convertSQLToDynamicSQLString(sql.toString()));
		
	}
	
}
