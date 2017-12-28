package x7.repository.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import x7.core.bean.BeanElement;
import x7.core.bean.Parsed;
import x7.core.bean.Parser;
import x7.core.config.Configs;
import x7.core.repository.Persistence;
import x7.core.util.BeanUtil;
import x7.core.util.BeanUtilX;
import x7.repository.ConfigKey;

public class MapperFactory implements Mapper {

	private static Map<Class, Map<String, String>> sqlsMap = new HashMap<Class, Map<String, String>>();

	/**
	 * 返回SQL
	 * 
	 * @param clz
	 *            ? extends IAutoMapped
	 * @param type
	 *            (BeanMapper.CREATE|BeanMapper.REFRESH|BeanMapper.DROP|
	 *            BeanMapper.QUERY)
	 * @return
	 */
	@SuppressWarnings({ "rawtypes" })
	public static String getSql(Class clz, String type) {

		Map<String, String> sqlMap = sqlsMap.get(clz);
		if (sqlMap == null) {
			sqlMap = new HashMap<String, String>();
			sqlsMap.put(clz, sqlMap);
			parseBean(clz);
		}

		return sqlMap.get(type);

	}

	@SuppressWarnings({ "rawtypes" })
	public static String tryToCreate(Class clz) {

		Map<String, String> sqlMap = sqlsMap.get(clz);
		if (sqlMap == null) {
			sqlMap = new HashMap<String, String>();
			sqlsMap.put(clz, sqlMap);
			parseBean(clz);
			return sqlMap.remove(CREATE_TABLE);
		}

		return "";

	}

	/**
	 * 
	 * @param clz
	 * @return
	 */
	public static List<BeanElement> getElementList(Class clz) {
		return Parser.get(clz).getBeanElementList();
	}

	@SuppressWarnings({ "rawtypes" })
	public static void parseBean(Class clz) {

		String repository = Configs.getString(ConfigKey.REPOSITORY);
		repository = repository.toLowerCase();
		switch (repository) {
		default:
			StandardSql sql = new StandardSql();
			sql.getTableSql(clz);
			sql.getRefreshSql(clz);
			sql.getRemoveSql(clz);
			sql.getQuerySql(clz);
			sql.getLoadSql(clz);
			sql.getMaxIdSql(clz);
			sql.getCreateSql(clz);
			sql.getPaginationSql(clz);
			sql.getCount(clz);
			return;
		}

	}

	public static class StandardSql implements Interpreter {
		public String getRefreshSql(Class clz) {

			Parsed parsed = Parser.get(clz);

			List<BeanElement> list = Parser.get(clz).getBeanElementList();

			String space = " ";
			StringBuilder sb = new StringBuilder();
			sb.append("UPDATE ");
			sb.append(BeanUtil.getByFirstLower(parsed.getClzName())).append(space);
			sb.append("SET ");

			String keyOne = parsed.getKey(Persistence.KEY_ONE);

			List<BeanElement> tempList = new ArrayList<BeanElement>();
			for (BeanElement p : list) {
				String column = p.property;
				if (column.equals(keyOne))
					continue;

				tempList.add(p);
			}

			int size = tempList.size();
			for (int i = 0; i < size; i++) {
				String column = tempList.get(i).property;

				sb.append(column).append(" = ?");
				if (i < size - 1) {
					sb.append(", ");
				}
			}

			sb.append(" WHERE ");

			parseKey(sb, clz);

			String sql = sb.toString();

			sql = BeanUtilX.mapper(sql, parsed);

			sqlsMap.get(clz).put(REFRESH, sql);

			System.out.println(sql);

			return sql;

		}

		public String getRemoveSql(Class clz) {
			Parsed parsed = Parser.get(clz);
			String space = " ";
			StringBuilder sb = new StringBuilder();
			sb.append("DELETE FROM ");
			sb.append(BeanUtil.getByFirstLower(parsed.getClzName())).append(space);
			sb.append("WHERE ");

			parseKey(sb, clz);

			String sql = sb.toString();

			sql = BeanUtilX.mapper(sql, parsed);

			sqlsMap.get(clz).put(REMOVE, sql);

			System.out.println(sql);

			return sql;

		}

		public void parseKey(StringBuilder sb, Class clz) {
			Parsed parsed = Parser.get(clz);

			sb.append(parsed.getKey(Persistence.KEY_ONE));
			sb.append(" = ?");

		}

		public String getQuerySql(Class clz) {

			Parsed parsed = Parser.get(clz);
			String space = " ";
			StringBuilder sb = new StringBuilder();
			sb.append("SELECT * FROM ");
			sb.append(BeanUtil.getByFirstLower(parsed.getClzName())).append(space);
			sb.append("WHERE ");

			sb.append(parsed.getKey(Persistence.KEY_ONE));
			sb.append(" = ?");

			String sql = sb.toString();
			sql = BeanUtilX.mapper(sql, parsed);

			sqlsMap.get(clz).put(QUERY, sql);

			System.out.println(sql);

			return sql;

		}

		public String getLoadSql(Class clz) {

			Parsed parsed = Parser.get(clz);
			StringBuilder sb = new StringBuilder();
			sb.append("SELECT * FROM ");
			sb.append(BeanUtil.getByFirstLower(parsed.getClzName()));

			String sql = sb.toString();

			sql = BeanUtilX.mapper(sql, parsed);

			sqlsMap.get(clz).put(LOAD, sql);

			System.out.println(sql);

			return sql;

		}

		public String getMaxIdSql(Class clz) {

			Parsed parsed = Parser.get(clz);

			StringBuilder sb = new StringBuilder();
			sb.append("SELECT MAX(");

			sb.append(parsed.getKey(Persistence.KEY_ONE));

			sb.append(") maxId FROM ");
			sb.append(BeanUtil.getByFirstLower(parsed.getClzName()));

			String sql = sb.toString();

			sql = BeanUtilX.mapper(sql, parsed);

			sqlsMap.get(clz).put(MAX_ID, sql);

			System.out.println(sql);

			return sql;
		}

		public String getCreateSql(Class clz) {

			List<BeanElement> list = Parser.get(clz).getBeanElementList();

			Parsed parsed = Parser.get(clz);

			List<BeanElement> tempList = new ArrayList<BeanElement>();
			for (BeanElement p : list) {

				tempList.add(p);
			}

			String space = " ";
			StringBuilder sb = new StringBuilder();
			sb.append("INSERT INTO ");
			sb.append(BeanUtil.getByFirstLower(parsed.getClzName())).append(space);

			sb.append("(");
			int size = tempList.size();
			for (int i = 0; i < size; i++) {
				String p = tempList.get(i).property;

				sb.append(p);
				if (i < size - 1) {
					sb.append(",");
				}
			}
			sb.append(") VALUES (");

			for (int i = 0; i < size; i++) {

				sb.append("?");
				if (i < size - 1) {
					sb.append(",");
				}
			}
			sb.append(")");

			String sql = sb.toString();
			sql = BeanUtilX.mapper(sql, parsed);
			sqlsMap.get(clz).put(CREATE, sql);

			System.out.println(sql);

			return sql;

		}

		public String getTableSql(Class clz) {

			String repository = Configs.getString(ConfigKey.REPOSITORY);

			List<BeanElement> temp = Parser.get(clz).getBeanElementList();
			Map<String, BeanElement> map = new HashMap<String, BeanElement>();
			List<BeanElement> list = new ArrayList<BeanElement>();
			for (BeanElement be : temp) {
				if (be.sqlType != null && be.sqlType.equals("text")) {
					list.add(be);
					continue;
				}
				map.put(be.property, be);
			}
			Parsed parsed = Parser.get(clz);

			String keyOne = parsed.getKey(Persistence.KEY_ONE);

			StringBuilder sb = new StringBuilder();
			sb.append("CREATE TABLE IF NOT EXISTS ").append(BeanUtil.getByFirstLower(parsed.getClzName())).append(" (")
					.append("\n");

			sb.append("   ").append(keyOne);

			BeanElement be = map.get(keyOne);
			String sqlType = Mapper.getSqlTypeRegX(be);

			System.out.println("p = " + be.property + " sqlType = " + sqlType);
			if (sqlType.equals(Dialect.INT)) {
				sb.append(Dialect.INT + " NOT NULL");
			} else if (sqlType.equals(Dialect.LONG)) {
				sb.append(Dialect.LONG + " NOT NULL");
			} else if (sqlType.equals(Dialect.STRING)) {
				sb.append(Dialect.STRING).append("(").append(be.length).append(") NOT NULL");
			}

			sb.append(", ");// FIXME ORACLE

			sb.append("\n");
			map.remove(keyOne);

			for (BeanElement bet : map.values()) {
				sqlType = Mapper.getSqlTypeRegX(bet);
				sb.append("   ").append(bet.property).append(" ");

				sb.append(sqlType);

				if (sqlType.equals(Dialect.BIG)) {
					sb.append(" DEFAULT 0.00 ");
				} else if (sqlType.equals(Dialect.DATE)) {
					if (bet.property.equals("createTime")) {
						sb.append(" NULL DEFAULT CURRENT_TIMESTAMP");
					} else if (bet.property.equals("refreshTime")) {
						sb.append(" NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
					} else {
						sb.append(" NULL");
					}
				} else if (sqlType.equals(Dialect.STRING)) {
					sb.append("(").append(bet.length).append(") NOT NULL");
				} else {
					if (bet.clz == Boolean.class || bet.clz == boolean.class || bet.clz == Integer.class
							|| bet.clz == int.class || bet.clz == Long.class || bet.clz == long.class) {
						sb.append(" DEFAULT 0");
					} else {
						sb.append(" DEFAULT NULL");
					}
				}
				sb.append(",").append("\n");
			}

			for (BeanElement bet : list) {
				sqlType = Mapper.getSqlTypeRegX(bet);
				sb.append("   ").append(bet.property).append(" ").append(sqlType).append(",").append("\n");
			}

			sb.append("   PRIMARY KEY (").append(keyOne).append(")");

			sb.append("\n");
			sb.append(") ").append(Dialect.ENGINE).append(";");

			String sql = sb.toString();

			sql = Dialect.match(sql, repository, CREATE_TABLE);

			sql = BeanUtilX.mapper(sql, parsed);
			System.out.println(sql);

			sqlsMap.get(clz).put(CREATE_TABLE, sql);

			return sql;
		}

		public String getPaginationSql(Class clz) {
			Parsed parsed = Parser.get(clz);
			String space = " ";
			StringBuilder sb = new StringBuilder();
			sb.append("SELECT " + Persistence.PAGINATION + " FROM ");
			sb.append(BeanUtil.getByFirstLower(parsed.getClzName())).append(space);
			sb.append("WHERE 1=1 ");

			String sql = sb.toString();

			sql = BeanUtilX.mapper(sql, parsed);
			sqlsMap.get(clz).put(PAGINATION, sql);

			System.out.println(sql);

			return sql;

		}

		public String getCount(Class clz) {

			Parsed parsed = Parser.get(clz);

			StringBuilder sb = new StringBuilder();
			sb.append("SELECT COUNT(");
			
			sb.append(parsed.getKey(Persistence.KEY_ONE));
			
			sb.append(") count FROM ");
			sb.append(BeanUtil.getByFirstLower(parsed.getClzName()));

			String sql = sb.toString();
			sql = BeanUtilX.mapper(sql, parsed);
			sqlsMap.get(clz).put(COUNT, sql);

			System.out.println(sql);

			return sql;

		}

	}

	public static String getTableName(Class clz) {

		Parsed parsed = Parser.get(clz);
		return parsed.getTableName();
	}

	public static String getTableName(Parsed parsed) {

		return parsed.getTableName();
	}
}